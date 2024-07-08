package fr.insee.protools.backend.service.common.platine_sabiane;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.insee.protools.backend.dto.platine_sabiane_questionnaire.campaign.CampaignDto;
import fr.insee.protools.backend.dto.platine_sabiane_questionnaire.campaign.MetadataValue;
import fr.insee.protools.backend.dto.platine_sabiane_questionnaire.surveyunit.SurveyUnitResponseDto;
import fr.insee.protools.backend.dto.rem.REMSurveyUnitDto;
import fr.insee.protools.backend.repository.IUniteEnquetee;
import fr.insee.protools.backend.service.DelegateContextVerifier;
import fr.insee.protools.backend.service.context.ContextService;
import fr.insee.protools.backend.service.exception.JsonParsingBPMNError;
import fr.insee.protools.backend.service.nomenclature.NomenclatureService;
import fr.insee.protools.backend.service.platine.utils.PlatineHelper;
import fr.insee.protools.backend.service.questionnaire_model.QuestionnaireModelService;
import fr.insee.protools.backend.service.sabiane.SabianeIdHelper;
import fr.insee.protools.backend.service.utils.FlowableVariableUtils;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static fr.insee.protools.backend.service.FlowableVariableNameConstants.*;
import static fr.insee.protools.backend.service.context.ContextConstants.*;
import static fr.insee.protools.backend.service.utils.ContextUtils.getCurrentPartitionNode;

@Slf4j
public class QuestionnaireHelper {

    private static final ObjectMapper objectMapper =
            new ObjectMapper()
                    .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(FAIL_ON_MISSING_CREATOR_PROPERTIES, true);

    private QuestionnaireHelper() {
    }

    public static void createQuestionnaire(JsonNode contextRootNode,
                                           QuestionnairePlatineSabianeService questionnairePlatineSabianeService,
                                           NomenclatureService nomenclatureService,
                                           QuestionnaireModelService questionnaireModelService,
                                           String processInstanceId,
                                           MetadataValue metadataDto
    ) {
        //Get the list of nomenclatures defined in Protools Context
        //Create them if needed
        var nomenclatureIterator = contextRootNode.path(CTX_NOMENCLATURES).elements();
        if (!nomenclatureIterator.hasNext()) {
            log.info("ProcessInstanceId={} - does not declare any nomenclature", processInstanceId);
        } else {
            initRequiredNomenclatures(questionnairePlatineSabianeService, nomenclatureService, processInstanceId, nomenclatureIterator);
        }

        //Get the list of Questionnaire Models defined in Protools Context
        Set<String> questionnaireModelIds = initQuestionnaireModels(questionnairePlatineSabianeService, questionnaireModelService, processInstanceId, contextRootNode);
        CampaignDto campaignDto = CampaignDto.builder()
                .id(contextRootNode.path(CTX_CAMPAGNE_ID).textValue())
                .label(contextRootNode.path(CTX_CAMPAGNE_LABEL).textValue())
                .metadata(metadataDto)
                .questionnaireIds(questionnaireModelIds)
                .build();
        questionnairePlatineSabianeService.postCampaign(campaignDto);
    }

    static void initRequiredNomenclatures(QuestionnairePlatineSabianeService questionnairePlatineSabianeService, NomenclatureService nomenclatureService, String processInstanceId, Iterator<JsonNode> nomenclatureIterator) {
        //Get the list of existing nomenclatures on platine
        Set<String> existingNomenclatures = questionnairePlatineSabianeService.getNomenclaturesId();

        //Create the nomenclatures not existing yet on platine
        while (nomenclatureIterator.hasNext()) {
            var node = nomenclatureIterator.next();
            String nomenclatureId = node.get(CTX_NOMENCLATURE_ID).asText();
            String nomenclatureCheminRepertoire = node.path(CTX_NOMENCLATURE_CHEMIN_REPERTOIRE).asText();
            String nomenclatureLabel = node.get(CTX_NOMENCLATURE_LABEL).asText();
            //check if platine know this nomenclature
            if (existingNomenclatures.contains(nomenclatureId)) {
                log.info("ProcessInstanceId={} - nomenclature ID={} already exists in collect platform ", processInstanceId, nomenclatureId);
            } else {
                //Retrieve the nomenclature from remote source
                String nomenclatureValueStr = nomenclatureService.getNomenclatureContent(nomenclatureId, nomenclatureCheminRepertoire);
                JsonNode nomenclatureValue;
                try {
                    nomenclatureValue = objectMapper.readTree(nomenclatureValueStr);
                } catch (JsonProcessingException e) {
                    throw new JsonParsingBPMNError("Error while parsing the json retrieved for nomenclatureId=" + nomenclatureId, e);
                }

                //Write this nomenclature to platine/sabiane
                questionnairePlatineSabianeService.postNomenclature(nomenclatureId, nomenclatureLabel, nomenclatureValue);
                //TODO : handles the exceptions here?
                log.info("ProcessInstanceId={} - nomenclature ID={} created in remote collect platform", processInstanceId, nomenclatureId);
            }
        }
    }

    /**
     * For every questionnaireModel defined in context : check if it exists in platine/sabiane questionnaire.
     * If it does not exists : retrieve it from questionnaireModelService and create it in platine/sabiane
     *
     * @param questionnairePlatineSabianeService : the service object used to access Sabiane Or Platine
     * @param questionnaireModelService          : The service object used to retrieve questionnaire models (ex: from gitlab)
     * @param processInstanceId                  : used in logs
     * @param contextRootNode                    : the protools context
     * @return the set of questionnaireModel ids
     */
    public static Set<String> initQuestionnaireModels(QuestionnairePlatineSabianeService questionnairePlatineSabianeService, QuestionnaireModelService questionnaireModelService, String processInstanceId, JsonNode contextRootNode) {
        var questionnaireModelIterator = contextRootNode.path(CTX_QUESTIONNAIRE_MODELS).elements();
        Set<String> questionnaireModelIds = new HashSet<>(); //Used to build the CampaignDto later
        while (questionnaireModelIterator.hasNext()) {
            var node = questionnaireModelIterator.next();
            String questionnaireId = node.get(CTX_QUESTIONNAIRE_MODEL_ID).asText();
            String questionnaireCheminRepertoire = node.get(CTX_QUESTIONNAIRE_MODEL_CHEMIN_REPERTOIRE).asText();
            String questionnaireLabel = node.get(CTX_QUESTIONNAIRE_MODEL_LABEL).asText();
            questionnaireModelIds.add(questionnaireId);

            //check if platine/sabiane know this questionnaire
            if (questionnairePlatineSabianeService.questionnaireModelExists(questionnaireId)) {
                log.info("getProcessInstanceId={} - questionnaireId ID={} already exists", processInstanceId, questionnaireId);
            } else {
                //Get the questionnaire model from remote source
                String questionnaireValueStr = questionnaireModelService.getQuestionnaireModel(questionnaireId, questionnaireCheminRepertoire);

                JsonNode questionnaireValue;
                try {
                    questionnaireValue = objectMapper.readTree(questionnaireValueStr);
                } catch (JsonProcessingException e) {
                    throw new JsonParsingBPMNError("Error while parsing the json retrieved for Model questionnaireId=" + questionnaireId, e);
                }
                //get the list of nomenclatures needed by this Questionnaire Model
                JsonNode nomenclaturesArrayNode = node.get(CTX_QUESTIONNAIRE_MODEL_REQUIRED_NOMENCLATURES);
                Set<String> requiredNomenclatures = new HashSet<>(nomenclaturesArrayNode.size());
                nomenclaturesArrayNode.forEach(jsonNode -> requiredNomenclatures.add(jsonNode.asText()));

                //TODO : should we check context coherence here and verify that we dont have any unknown nomenclature?
                //Write this questionnaire model  to platine/sabiane
                questionnairePlatineSabianeService.postQuestionnaireModel(questionnaireId, questionnaireLabel, questionnaireValue, requiredNomenclatures);
                //TODO : handles the exceptions here?
            }
        }
        return questionnaireModelIds;
    }

    @SuppressWarnings("java:S3776") //disable the warning about cognitive complexity as it is long but simple
    public static Set<String> getCreateCtxContextErrors(JsonNode contextRootNode) {
        if (contextRootNode == null) {
            return Set.of(String.format("Class=%s : Context is missing ", QuestionnaireHelper.class.getSimpleName()));
        }

        Set<String> requiredNodes =
                Set.of(CTX_CAMPAGNE_ID, CTX_CAMPAGNE_LABEL, CTX_NOMENCLATURES, CTX_QUESTIONNAIRE_MODELS, CTX_METADONNEES);
        Set<String> missingNodes = new HashSet<>(DelegateContextVerifier.computeMissingChildrenMessages(requiredNodes, contextRootNode, QuestionnaireHelper.class));

        if (contextRootNode.get(CTX_NOMENCLATURES) != null) {
            //Check on nomenclatures
            var nomenclatureIterator = contextRootNode.path(CTX_NOMENCLATURES).elements();
            int i = 0;
            while (nomenclatureIterator.hasNext()) {
                i++;
                var nomenclatureNode = nomenclatureIterator.next();
                if (nomenclatureNode.get(CTX_NOMENCLATURE_ID) == null || nomenclatureNode.get(CTX_NOMENCLATURE_ID).asText().isEmpty()) {
                    missingNodes.add(missingCTXMessage(CTX_NOMENCLATURES, i, CTX_NOMENCLATURE_ID));
                }
                if (nomenclatureNode.get(CTX_NOMENCLATURE_LABEL) == null || nomenclatureNode.get(CTX_NOMENCLATURE_LABEL).asText().isEmpty()) {
                    missingNodes.add(missingCTXMessage(CTX_NOMENCLATURES, i, CTX_NOMENCLATURE_LABEL));
                }
                if (nomenclatureNode.get(CTX_NOMENCLATURE_CHEMIN_REPERTOIRE) == null || nomenclatureNode.get(CTX_NOMENCLATURE_CHEMIN_REPERTOIRE).asText().isEmpty()) {
                    missingNodes.add(missingCTXMessage(CTX_NOMENCLATURES, i, CTX_NOMENCLATURE_CHEMIN_REPERTOIRE));
                }
            }

        }

        if (contextRootNode.get(CTX_QUESTIONNAIRE_MODELS) != null) {
            //Check on questionnaire models
            var questionnaireModelsIterator = contextRootNode.get(CTX_QUESTIONNAIRE_MODELS).elements();
            int i = 0;
            while (questionnaireModelsIterator.hasNext()) {
                i++;
                var nomenclatureNode = questionnaireModelsIterator.next();
                if (nomenclatureNode.get(CTX_QUESTIONNAIRE_MODEL_ID) == null || nomenclatureNode.get(CTX_QUESTIONNAIRE_MODEL_ID).asText().isEmpty()) {
                    missingNodes.add(missingCTXMessage(CTX_QUESTIONNAIRE_MODELS, i, CTX_QUESTIONNAIRE_MODEL_ID));
                }
                if (nomenclatureNode.get(CTX_QUESTIONNAIRE_MODEL_LABEL) == null || nomenclatureNode.get(CTX_QUESTIONNAIRE_MODEL_LABEL).asText().isEmpty()) {
                    missingNodes.add(missingCTXMessage(CTX_QUESTIONNAIRE_MODELS, i, CTX_QUESTIONNAIRE_MODEL_LABEL));
                }
                if (nomenclatureNode.get(CTX_QUESTIONNAIRE_MODEL_CHEMIN_REPERTOIRE) == null || nomenclatureNode.get(CTX_QUESTIONNAIRE_MODEL_CHEMIN_REPERTOIRE).asText().isEmpty()) {
                    missingNodes.add(missingCTXMessage(CTX_QUESTIONNAIRE_MODELS, i, CTX_QUESTIONNAIRE_MODEL_CHEMIN_REPERTOIRE));
                }
            }
        }
        return missingNodes;
    }

    private static String missingCTXMessage(String parent, int index, String child) {
        return DelegateContextVerifier.computeMissingMessage(String.format("%s[%s].%s", parent, index, child), QuestionnaireHelper.class);
    }

    private static SurveyUnitResponseDto computeDtoPlatine(JsonNode remSUNode, JsonNode currentPartitionNode) {
        REMSurveyUnitDto remSurveyUnitDto = PlatineHelper.parseRemSUNode(objectMapper, VARNAME_REM_SURVEY_UNIT, remSUNode);
        String id = remSurveyUnitDto.getRepositoryId().toString();
        String nameKey = "name";
        String valueKey = "value";

        ArrayNode personalizationNode = objectMapper.createArrayNode();
        personalizationNode.add(objectMapper.createObjectNode()
                .put(nameKey, "whoAnswers1")
                .put(valueKey, currentPartitionNode.path(CTX_PARTITION_QUIREPOND1).asText()));
        personalizationNode.add(objectMapper.createObjectNode()
                .put(nameKey, "whoAnswers2")
                .put(valueKey, currentPartitionNode.path(CTX_PARTITION_QUIREPOND2).asText()));
        personalizationNode.add(objectMapper.createObjectNode()
                .put(nameKey, "whoAnswers3")
                .put(valueKey, currentPartitionNode.path(CTX_PARTITION_QUIREPOND3).asText()));

        return SurveyUnitResponseDto.builder()
                .id(id) //Platine
                .questionnaireId(currentPartitionNode.path(CTX_PARTITION_QUESTIONNAIRE_MODEL).asText())
                .data(remSurveyUnitDto.getExternals())
                .personalization(personalizationNode)
                .comment(objectMapper.createObjectNode())
                //.stateData(objectMapper.createObjectNode())
                .build();
    }

    private static SurveyUnitResponseDto computeDtoSabiane(JsonNode remSUNode, JsonNode currentPartitionNode) {
        REMSurveyUnitDto remSurveyUnitDto = PlatineHelper.parseRemSUNode(objectMapper, VARNAME_REM_SURVEY_UNIT, remSUNode);
        String id = SabianeIdHelper.computeSabianeID(currentPartitionNode.path(CTX_PARTITION_ID).asText(),remSurveyUnitDto.getRepositoryId().toString());

        return SurveyUnitResponseDto.builder()
                .id(id)//Sabiane uses identified of the form IdPartition P idREM
                .questionnaireId(currentPartitionNode.path(CTX_PARTITION_QUESTIONNAIRE_MODEL).asText())
                .data(remSurveyUnitDto.getExternals())
                .personalization(objectMapper.createObjectNode())//No personalization for sabiane
                .comment(objectMapper.createObjectNode())
                //.stateData(objectMapper.createObjectNode())
                .build();
    }

    /**
     * Create a SU in Platine Questionnaire
     * @param execution
     * @param protoolsContext
     * @param service
     */
    public static void createSUTaskPlatine(DelegateExecution execution, ContextService protoolsContext, QuestionnairePlatineSabianeService service) {
        createSUTaskPlatineSabiane(execution, protoolsContext, service, false);
    }

    public static void createAllSUTaskPlatine(DelegateExecution execution, ContextService protoolsContext, QuestionnairePlatineSabianeService service) {
        JsonNode contextRootNode = protoolsContext.getContextByProcessInstance(execution.getProcessInstanceId());

        Long currentPartitionId = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_CURRENT_PARTITION_ID, Long.class);
        List<JsonNode> listeUe =   FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_REM_SU_LIST, List.class);


        Boolean parallele = FlowableVariableUtils.getVariableOrThrow(execution, "parallele", Boolean.class);
        JsonNode currentPartitionNode = getCurrentPartitionNode(contextRootNode, currentPartitionId);
        log.info("parallele="+parallele+"- Boolean.FALSE.equals(parallele)="+Boolean.FALSE.equals(parallele));

        String idCampaign = contextRootNode.path(CTX_CAMPAGNE_ID).asText();

        if(Boolean.FALSE.equals(parallele)) {
            for (JsonNode remSUNode : listeUe){
                //Create the DTO object
                SurveyUnitResponseDto dto =
                        QuestionnaireHelper.computeDtoPlatine(remSUNode, currentPartitionNode);
                log.trace("ProcessInstanceId={} - mode={} - currentPartitionId={} - remSU.id={}",
                        execution.getProcessInstanceId(), "platine", currentPartitionId, dto.getId());

                //Call service
                postSUWithRetry(service, dto, idCampaign);
            }
        }
        else{

                listeUe.stream().parallel().forEach(remSUNode -> {
                    //Create the DTO object
                    SurveyUnitResponseDto dto =
                            QuestionnaireHelper.computeDtoPlatine(remSUNode, currentPartitionNode);
                    log.trace("ProcessInstanceId={} - mode={} - currentPartitionId={} - remSU.id={}",
                            execution.getProcessInstanceId(), "platine", currentPartitionId, dto.getId());

                    postSUWithRetry(service, dto, idCampaign);
                });
            }

        log.debug("ProcessInstanceId={}  end", execution.getProcessInstanceId());
    }

    private static void postSUWithRetry(QuestionnairePlatineSabianeService service, SurveyUnitResponseDto dto, String idCampaign) {
        int retryCount = 0;
        boolean finished=false;
        while(retryCount<10 && !finished)
        try {
            //Call service
            service.postSurveyUnit(dto, idCampaign);
            finished=true;
        }
        catch (Throwable e){
            int sleepMs = 1000 + retryCount*10000;
            retryCount++;
            log.error(" Exception : remSU.id={} - retryCount={} - sleepMs={} - msg={} ",
                    dto.getId(), retryCount,sleepMs,e.getMessage());;
            if(retryCount>=10){
                throw e;
            }


            try {
                Thread.sleep(15000);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Create a SU in Sabiane Questionnaire
     * @param execution
     * @param protoolsContext
     * @param service
     */
    public static void createSUTaskSabiane(DelegateExecution execution, ContextService protoolsContext, QuestionnairePlatineSabianeService service) {
        createSUTaskPlatineSabiane(execution, protoolsContext, service, true);
    }

    private static void createSUTaskPlatineSabiane(DelegateExecution execution, ContextService protoolsContext, QuestionnairePlatineSabianeService service, boolean modeSabiane) {
        JsonNode contextRootNode = protoolsContext.getContextByProcessInstance(execution.getProcessInstanceId());

        Long currentPartitionId = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_CURRENT_PARTITION_ID, Long.class);
        JsonNode remSUNode = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_REM_SURVEY_UNIT, JsonNode.class);
        JsonNode currentPartitionNode = getCurrentPartitionNode(contextRootNode, currentPartitionId);

        //Create the DTO object
        SurveyUnitResponseDto dto =
                (modeSabiane)
                        ?
                        QuestionnaireHelper.computeDtoSabiane(remSUNode, currentPartitionNode)
                        :
                        QuestionnaireHelper.computeDtoPlatine(remSUNode, currentPartitionNode);

        log.debug("ProcessInstanceId={} - mode={} - currentPartitionId={} - remSU.id={}",
                execution.getProcessInstanceId(), modeSabiane ? "sabiane" : "platine", currentPartitionId, dto.getId());

        //Call service
        service.postSurveyUnit(dto, contextRootNode.path(CTX_CAMPAGNE_ID).asText());

        log.debug("ProcessInstanceId={}  end", execution.getProcessInstanceId());
    }

    /**
     * Get the context errors for an SU creation in Platine
     * @param contextRootNode : the context to verify
     * @return a Set with the errors
     */
    public static Set<String> getCreateSUContextErrorsPlatine(JsonNode contextRootNode){
        Set<String> requiredPartition =
                Set.of(CTX_PARTITION_ID,CTX_PARTITION_QUESTIONNAIRE_MODEL,CTX_PARTITION_QUIREPOND1,CTX_PARTITION_QUIREPOND2,CTX_PARTITION_QUIREPOND3);
        return getCreateSUContextErrorsCommonPlatineSabiane(contextRootNode,requiredPartition);
    }

    /**
     * Get the context errors for an SU creation in Sabiane
     * @param contextRootNode : the context to verify
     * @return a Set with the errors
     */
    public static Set<String> getCreateSUContextErrorsSabiane(JsonNode contextRootNode){
        Set<String> requiredPartition =
                Set.of(CTX_PARTITION_ID,CTX_PARTITION_QUESTIONNAIRE_MODEL);
        return getCreateSUContextErrorsCommonPlatineSabiane(contextRootNode,requiredPartition);
    }

    private static Set<String> getCreateSUContextErrorsCommonPlatineSabiane(JsonNode contextRootNode, Set<String> requiredPartition){
        if(contextRootNode==null){
            return Set.of("Context is missing");
        }
        Set<String> results=new HashSet<>();
        Set<String> requiredNodes =
                Set.of(
                        //Global & Campaign
                        CTX_PARTITIONS, CTX_QUESTIONNAIRE_MODELS
                );

        results.addAll(DelegateContextVerifier.computeMissingChildrenMessages(requiredNodes,contextRootNode,QuestionnaireHelper.class));
        //Maybe one day we will have partitions for platine and partitions for sabiane and we will only validate the platine ones
        //Partitions
        var partitionIterator =contextRootNode.path(CTX_PARTITIONS).elements();
        while (partitionIterator.hasNext()) {
            var partitionNode = partitionIterator.next();
            results.addAll(DelegateContextVerifier.computeMissingChildrenMessages(requiredPartition,partitionNode,QuestionnaireHelper.class));
        }
        return results;
    }

    public static void createSUTaskPlatineAsync(DelegateExecution execution, ContextService protoolsContext, IUniteEnquetee iUniteEnquetee, QuestionnairePlatineSabianeService service) {
        JsonNode contextRootNode = protoolsContext.getContextByProcessInstance(execution.getProcessInstanceId());

        Long currentPartitionId = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_CURRENT_PARTITION_ID, Long.class);
        List<JsonNode> listeUe =   FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_REM_SU_LIST, List.class);

        Boolean parallele = FlowableVariableUtils.getVariableOrThrow(execution, "parallele", Boolean.class);
        JsonNode currentPartitionNode = getCurrentPartitionNode(contextRootNode, currentPartitionId);
        String questionnaireId = currentPartitionNode.path(CTX_PARTITION_QUESTIONNAIRE_MODEL).asText();
        log.info("parallele="+parallele+"- Boolean.FALSE.equals(parallele)="+Boolean.FALSE.equals(parallele));

        String processInstanceId = execution.getProcessInstanceId();
        String campaignId = contextRootNode.path(CTX_CAMPAGNE_ID).asText();

        iUniteEnquetee.addManyUniteEnquetee(listeUe, processInstanceId, campaignId, questionnaireId);
//        iUniteEnquetee.addManyUniteEnqueteeDeleteColonneClass(listeUe);
    }

}
