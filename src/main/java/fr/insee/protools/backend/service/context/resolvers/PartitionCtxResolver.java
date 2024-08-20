package fr.insee.protools.backend.service.context.resolvers;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.exception.ProtoolsBpmnError;
import fr.insee.protools.backend.service.context.ContextService;
import fr.insee.protools.backend.service.exception.IncoherentBPMNContextError;
import fr.insee.protools.backend.service.utils.FlowableVariableUtils;
import fr.insee.protools.backend.service.utils.log.TimeLogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAmount;
import java.util.*;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.*;
import static fr.insee.protools.backend.service.context.ContextConstants.*;
import static fr.insee.protools.backend.service.utils.ContextUtils.*;

/**
 * Used to make protools context variables available in BPMN expressions
 * example:
 *
 *   <intermediateCatchEvent id="id1" name="dummy">
 *     <timerEventDefinition>
 *       <timeDate>${partitionCtxResolver.getCollectionStartDate(execution,current_partition_id)}</timeDate>
 *     </timerEventDefinition>
 *   </intermediateCatchEvent>
 *
 *
 *   Flowable doc : https://documentation.flowable.com/latest/develop/be/be-expressions#customization
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PartitionCtxResolver {

    private final ContextService protoolsContext;

    //TODO: à faire valider par le métier
    private final TemporalAmount maxSendCommunicationWindowHours = Duration.ofHours(24);;

    //Date in a far away future
    private final static Instant farAwayInstant = LocalDate.parse("9999-12-31").atStartOfDay(ZoneId.of("Europe/Paris")).toInstant();


    private Serializable getVariableOfPartition(ExecutionEntity execution, String partitionId, String key) {
        HashMap<Long,HashMap<String, Serializable>> variablesByPartition = execution.getVariable(VARNAME_CONTEXT_PARTITION_VARIABLES_BY_ID, HashMap.class);
        HashMap<String, Serializable> partitionVariables = variablesByPartition.get(partitionId);
        if(partitionVariables==null) {
            throw new FlowableException("Could not get variable "+VARNAME_CONTEXT_PARTITION_VARIABLES_BY_ID+ " of partitionId="+partitionId);
        }
        return partitionVariables.get(key);
    }

    public Instant getCollectionStartDate(ExecutionEntity execution, String partitionId) {
        return (Instant) getVariableOfPartition(execution,partitionId,CTX_PARTITION_DATE_DEBUT_COLLECTE);
    }


    public Instant getCollectionEndDate(ExecutionEntity execution, String partitionId) {
        //TODO : a supprimer ; Voir si on n'utilise pas le contexte json directement au lieu de variables initalisées au lancement
        /*Instant s =  (Instant) getVariableOfPartition(execution,partitionId,CTX_PARTITION_DATE_FIN_COLLECTE);
        System.out.println("partitionId="+partitionId+" getCollectionEndDate="+s);
        LocalDateTime ldt = LocalDateTime.ofInstant(s, ZoneOffset.systemDefault());	//2023-02-02T13:52:04.824071900
        System.out.println("partitionId="+partitionId+" getCollectionEndDate LOCAL="+ldt);*/

        return (Instant) getVariableOfPartition(execution,partitionId,CTX_PARTITION_DATE_FIN_COLLECTE);

    }

    public String getCommunicationType(ExecutionEntity execution, String partitionId, String communicationId) {
        JsonNode contextRootNode = protoolsContext.getContextByProcessInstance(execution.getProcessInstanceId());
        JsonNode partitionNode = getPartitionNodeIfExists(contextRootNode, partitionId)
                .orElseThrow(() -> new IncoherentBPMNContextError("Tried to get communication type on an unknown partition"));

        JsonNode communicationNode = getCommunicationFromPartition(partitionNode,communicationId).orElseThrow(()-> new IncoherentBPMNContextError("Tried to get communication type on an undefined communicationId="+communicationId));
        return communicationNode.path(CTX_PARTITION_COMMUNICATION_TYPE).asText();
    }


    //TODO: documenter l'histoire des 24H
    public Instant scheduleNextCommunication(ExecutionEntity execution, String partitionId) {

        Instant now = Instant.now();

        //Context
        JsonNode contextRootNode = protoolsContext.getContextByProcessInstance(execution.getProcessInstanceId());
        Set<String> sentCommunicationIds = FlowableVariableUtils.getVariableOrNull(execution, VARNAME_ALREADY_SCHEDULED_COMMUNICATION_ID_SET, Set.class);
        Set<String> errorCommunicationIds = FlowableVariableUtils.getVariableOrNull(execution, VARNAME_COMMUNICATION_ERROR_ID_SET, Set.class);


        if(sentCommunicationIds==null){
            sentCommunicationIds=new HashSet<>();
        }
        if(errorCommunicationIds==null){
            errorCommunicationIds=new HashSet<>();
        }
        JsonNode partitionNode = getPartitionNodeIfExists(contextRootNode, partitionId)
                .orElseThrow(() -> new IncoherentBPMNContextError("Tried to schedule next communication of an undefined partition"));

        //if this partition is not defined; obviously there is something wrong
        String end = partitionNode.get(CTX_PARTITION_DATE_FIN_COLLECTE).asText();
        Instant collectionEndCollecte = Instant.parse(end);

        if(collectionEndCollecte.isBefore(now)){
            log.error("ProcessInstanceId={} - partitionId={} - dateFinCollecte={} is in the past  ==> Timer is set to a far away Instant ",
                    execution.getProcessInstanceId(), partitionId, TimeLogUtils.format(collectionEndCollecte));
            return farAwayInstant;
        }

        List<JsonNode> communicationNodes = getCommunicationsFromPartition(partitionNode);
        Instant nextCommEcheance = null;
        String nextCommId=null;


        for (JsonNode communicationNode : communicationNodes){
            Instant echeance = Instant.parse(communicationNode.path(CTX_PARTITION_COMMUNICATION_ECHEANCE).asText());
            String communicationId=communicationNode.path(CTX_PARTITION_COMMUNICATION_ID).asText();

            //null is an error in config so we ignore it; a
            // and we also discard communications that have already been sent or that have been marked as in error
            if(communicationId==null || sentCommunicationIds.contains(communicationId) || errorCommunicationIds.contains(communicationId)){
                continue;
            }

            //verify that echeance of communication to be sent is not too old
            if(echeance.plus(maxSendCommunicationWindowHours).isBefore(now)){
                log.warn("Partition id={} : Communication id={} has not been sent. Its echeance [ {} ] is too far past so it will not be sent",
                        partitionId,communicationId,TimeLogUtils.format(echeance));

                errorCommunicationIds.add(communicationId);
                execution.getRootProcessInstance().setVariableLocal(VARNAME_COMMUNICATION_ERROR_ID_SET,errorCommunicationIds);
            }

            //Among the not treated communications we compute the one with the first echeance
            else if(nextCommEcheance == null || nextCommEcheance.isAfter(echeance)){
                nextCommEcheance=echeance;
                nextCommId=communicationNode.path(CTX_PARTITION_COMMUNICATION_ID).asText();
            }
        }

        //If no communication left (all have already been treated) ==> far away future
        if(nextCommId==null){
            nextCommEcheance = farAwayInstant;
        }
        else{
            sentCommunicationIds.add(nextCommId);

            execution.getParent().setVariableLocal(VARNAME_CURRENT_COMMUNICATION_ID,nextCommId);
            execution.getRootProcessInstance().setVariableLocal(VARNAME_ALREADY_SCHEDULED_COMMUNICATION_ID_SET,sentCommunicationIds);
        }

        //TODO: remove
        System.out.println("part="+partitionId+" - nextCom="+TimeLogUtils.format(nextCommEcheance));
        return nextCommEcheance;
    }

}