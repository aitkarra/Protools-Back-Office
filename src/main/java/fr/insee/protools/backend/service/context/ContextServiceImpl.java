package fr.insee.protools.backend.service.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import fr.insee.protools.backend.service.DelegateContextVerifier;
import fr.insee.protools.backend.service.context.exception.BadContextDateTimeParseBPMNError;
import fr.insee.protools.backend.service.context.exception.BadContextIOException;
import fr.insee.protools.backend.service.context.exception.BadContextIncorrectBPMNError;
import fr.insee.protools.backend.service.context.exception.BadContextNotJSONBPMNError;
import fr.insee.protools.backend.service.exception.ProcessDefinitionNotFoundException;
import fr.insee.protools.backend.service.exception.TaskNotFoundException;
import fr.insee.protools.backend.service.utils.log.TimeLogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.*;
import static fr.insee.protools.backend.service.context.ContextConstants.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContextServiceImpl implements ContextService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final RepositoryService repositoryService;
    private final ApplicationContext springApplicationContext;

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ObjectReader defaultReader = mapper.reader(); // maybe with configs

    private static final Map<String, JsonNode> contextCache = new ConcurrentHashMap<>();

    @Override
    public void processContextFileAndCompleteTask(MultipartFile file, String taskId) {
        //Check if task exists
        if (StringUtils.isBlank(taskId)) {
            log.error("taskId is null or blank");
            throw new TaskNotFoundException(taskId);
        }
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null || task.getProcessInstanceId() == null) {
            log.error("taskId={} does not exist", taskId);
            throw new TaskNotFoundException(taskId);
        }

        //check context
        Pair<Map<String, Object>, JsonNode> contextPair = processContextFile(file, task.getProcessDefinitionId());
        //Store context in cache
        contextCache.put(task.getProcessInstanceId(), contextPair.getValue());
        // Complete task and store context within process variables
        taskService.complete(taskId, contextPair.getKey());
    }

    @Override
    public String processContextFileAndCreateProcessInstance(MultipartFile file, String processDefinitionId, String businessKey) {
        if (StringUtils.isBlank(processDefinitionId)) {
            log.error("processDefinitionId is null or blank");
            throw new ProcessDefinitionNotFoundException(processDefinitionId);
        }

        try {
            //check context
            Pair<Map<String, Object>, JsonNode> contextPair = processContextFile(file, processDefinitionId);
            //Create process instance
            ProcessInstance processInstance;
            if (StringUtils.isBlank(businessKey)) {
                processInstance = runtimeService.startProcessInstanceByKey(processDefinitionId, contextPair.getKey());
            } else {
                processInstance = runtimeService.startProcessInstanceByKey(processDefinitionId, businessKey, contextPair.getKey());
            }
            log.info("Created new process instance with processDefinitionId={} - ProcessInstanceId={}", processDefinitionId, processInstance.getProcessInstanceId());
            //Store context in cache
            contextCache.put(processInstance.getProcessInstanceId(), contextPair.getValue());
            return processInstance.getProcessInstanceId();
        } catch (FlowableObjectNotFoundException e) {
            log.error("processDefinitionId={} is unknown", processDefinitionId);
            throw new ProcessDefinitionNotFoundException(processDefinitionId);
        }
    }


    @Override
    public JsonNode getContextByProcessInstance(String processInstanceId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if (processInstance == null) {
            throw new FlowableObjectNotFoundException("Could not find a process instance with id '" + processInstanceId + "'.", ProcessInstance.class);
        }
        JsonNode result = contextCache.get(processInstanceId);
        //If value does not exist in cache yet : Retrieve it and update cache
        if (result == null) {
            String contextStr = runtimeService.getVariable(processInstanceId, VARNAME_CONTEXT, String.class);
            if(contextStr == null || contextStr.isBlank()){
                throw new BadContextIncorrectBPMNError(String.format("Context retrieved from engine is null or empty processInstanceId=[%s] ", processInstanceId));
            }

            try {
                result = defaultReader.readTree(contextStr);
            } catch (JsonProcessingException e) {
                throw new BadContextIncorrectBPMNError(String.format("Context retrieved from engine could not be parsed for processInstanceId=[%s] - Exception : %s", processInstanceId, e.getMessage()));
            }
            contextCache.put(processInstanceId, result);
        }
        return result;
    }

    private Pair<Map<String, Object>, JsonNode> processContextFile(MultipartFile file, String processDefinitionKey) {
        //Validate file name (JSON)
        var fileExtension = getFileExtension(file.getOriginalFilename());
        if (fileExtension.isEmpty()) {
            throw new BadContextNotJSONBPMNError(String.format("Uploaded file %s has incorrect filename without extension", file.getOriginalFilename()));
        } else if (!fileExtension.get().equalsIgnoreCase("json")) {
            throw new BadContextNotJSONBPMNError(String.format("Uploaded file %s has incorrect extension. Expected json", file.getOriginalFilename()));
        }

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            log.debug("Context File content : " + content);

            JsonNode rootContext = defaultReader.readTree(content);
            Set<String> contextErrors = isContextOKForBPMN(processDefinitionKey, rootContext);
            if (!contextErrors.isEmpty()) {
                throw new BadContextIncorrectBPMNError(contextErrors.toString());
            }
            log.info("idCampaign="+rootContext.path(CTX_CAMPAGNE_ID).textValue());

            //Variables to store for this process
            Map<String, Object> variables = new HashMap<>();
            //Store the raw json as string
            variables.put(VARNAME_CONTEXT, content);

            // Extraction of campaign TIMER START/END dates
            //        Do extraction of important BPMN Variables in separates functions
            JsonNode partitions = rootContext.path(CTX_PARTITIONS);
            if (partitions.isMissingNode()) {
                String msg = String.format("Missing %s in context", CTX_PARTITIONS);
                log.error(msg);
                throw new BadContextIncorrectBPMNError(msg);
            }

            List<String> partitionIds = new ArrayList<>();
            HashMap<String,HashMap<String, Serializable>> variablesByPartition= new HashMap<>();

            for (JsonNode partition : partitions) {
                Pair<Instant, Instant> startEndDT = getCollectionStartAndEndFromPartition(partition);
                String partitionId = partition.path(CTX_PARTITION_ID).asText();
                partitionIds.add(partitionId);

                HashMap<String,Serializable> partitionVariables = new HashMap<>();
                partitionVariables.put(CTX_PARTITION_DATE_DEBUT_COLLECTE,startEndDT.getKey() );
                partitionVariables.put(CTX_PARTITION_DATE_FIN_COLLECTE,startEndDT.getValue() );

                variablesByPartition.put(partitionId,partitionVariables);
            }

            variables.put(VARNAME_CONTEXT_PARTITION_ID_LIST, partitionIds);
            variables.put(VARNAME_CONTEXT_PARTITION_VARIABLES_BY_ID, variablesByPartition);

            return Pair.of(variables, rootContext);
        } catch (IOException e) {
            throw new BadContextIOException("Error while reading context content: "+e.getMessage(), e);
        }
    }

    private Optional<String> getFileExtension(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    //TODO : soit les json schema permettent de valider les dates, soit il faudra valider toutes les dates comme ça
    public static Pair<Instant, Instant> getCollectionStartAndEndFromPartition(JsonNode partitionNode) {
        String start = partitionNode.get(CTX_PARTITION_DATE_DEBUT_COLLECTE).asText();
        String end = partitionNode.get(CTX_PARTITION_DATE_FIN_COLLECTE).asText();

        if(start==null || end==null){
            throw new BadContextIncorrectBPMNError(String.format("%s and %s must be defined on every partition", CTX_PARTITION_DATE_DEBUT_COLLECTE, CTX_PARTITION_DATE_FIN_COLLECTE));
        }

        try {
            Instant collectionStart = Instant.parse(start);
            Instant collectionEnd = Instant.parse(end);
            log.info("partition_id={} - CollectionStartDate={} - CollectionEndDate={}", partitionNode.path(CTX_PARTITION_ID), TimeLogUtils.format(collectionStart), TimeLogUtils.format(collectionEnd));
            return Pair.of(collectionStart,
                    collectionEnd);

        } catch (DateTimeParseException e) {
            throw new BadContextIncorrectBPMNError(String.format("%s or %s cannot be read as Instant : %s", CTX_PARTITION_DATE_DEBUT_COLLECTE, CTX_PARTITION_DATE_FIN_COLLECTE, e.getMessage()));
        }
    }

    public static Instant getInstantFromPartition(JsonNode partitionNode, String subnode) throws BadContextDateTimeParseBPMNError {
        JsonNode instantNode = partitionNode.get(subnode);
        if (instantNode == null) {
            throw new BadContextDateTimeParseBPMNError(String.format("node %s of partition %s does not exists", subnode, partitionNode.path(CTX_PARTITION_ID).asText()));
        }
        String valueTxt = partitionNode.path(subnode).asText();
        if (valueTxt.isBlank()) {
            throw new BadContextDateTimeParseBPMNError(String.format("node %s of partition %s is blank", subnode, partitionNode.path(CTX_PARTITION_ID).asText()));
        }

        try {
            return Instant.parse(valueTxt);
        } catch (DateTimeParseException e) {
            throw new BadContextDateTimeParseBPMNError(String.format("node %s of partition %s having value [%s] cannot be parsed : %s", subnode, partitionNode.path(CTX_PARTITION_ID).asText(), valueTxt, e.getMessage()));
        }
    }

    /**
     * Check if protoolsContextRootNode allows every Task implementing {@link  fr.insee.protools.backend.service.DelegateContextVerifier#getContextErrors  DelegateContextVerifier}  interface to run correctly
     * @param processDefinitionKey The process (BPMN) identifier
     * @param protoolsContextRootNode The context to check
     * @return A list of the problems found
     * @throws FlowableObjectNotFoundException if no process definition (BPMN) matches processDefinitionKey
     */
    public Set<String> isContextOKForBPMN(String processDefinitionKey, JsonNode protoolsContextRootNode) {
        //At least, the campaign ID should be defined so we can write it on process variables to be used un groovy scripts
        Set<String> errors=DelegateContextVerifier.computeMissingChildrenMessages(Set.of(CTX_CAMPAGNE_ID),protoolsContextRootNode,getClass());

        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery();
        processDefinitionQuery.processDefinitionKey(processDefinitionKey);
        processDefinitionQuery.latestVersion();
        ProcessDefinition definition = processDefinitionQuery.singleResult();
        if(definition==null){
            throw new FlowableObjectNotFoundException("Cannot find process definition with key " + processDefinitionKey, ProcessDefinition.class);
        }
        BpmnModel model = repositoryService.getBpmnModel(definition.getId());
        if(model==null){
            throw new FlowableObjectNotFoundException("Cannot find process BPMN model definition with key " + processDefinitionKey, ProcessDefinition.class);
        }

        org.flowable.bpmn.model.Process processModel = model.getProcessById(processDefinitionKey);
        if(processModel==null){
            throw new FlowableObjectNotFoundException("Cannot find process Model with key " + processDefinitionKey, ProcessDefinition.class);
        }

        processModel.getFlowElements().stream()
                .filter(flowElement -> (flowElement instanceof ServiceTask || flowElement instanceof SubProcess))
                .forEach(flowElement -> errors.addAll(analyseProcess(flowElement,protoolsContextRootNode)));
        return errors;
    }

    private Set<String> analyseProcess(FlowElement flowElement, JsonNode protoolsContextRootNode){
        if (flowElement instanceof ServiceTask serviceTask) {
            if (serviceTask.getImplementationType().equals("delegateExpression")) {
                String delegateExpression = serviceTask.getImplementation().replace("${", "").replace("}", "");
                try {
                    Object bean = springApplicationContext.getBean(delegateExpression);
                    if (bean instanceof DelegateContextVerifier beanDelegateCtxVerifier) {
                        return beanDelegateCtxVerifier.getContextErrors(protoolsContextRootNode);
                    }
                }
                catch (NoSuchBeanDefinitionException e){}
            }
        } else if (flowElement instanceof SubProcess subProcessFlowElement) {
            Set<String> errors = new HashSet<>();
            subProcessFlowElement.getFlowElements().stream()
                    .forEach(subFlowElement -> errors.addAll(analyseProcess(subFlowElement,protoolsContextRootNode)));
            return errors;
        }
        return Set.of();
    }
}
