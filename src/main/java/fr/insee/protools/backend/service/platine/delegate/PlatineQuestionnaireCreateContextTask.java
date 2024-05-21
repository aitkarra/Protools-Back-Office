package fr.insee.protools.backend.service.platine.delegate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.dto.platine_sabiane_questionnaire.MetadataConstants;
import fr.insee.protools.backend.dto.platine_sabiane_questionnaire.campaign.MetadataValue;
import fr.insee.protools.backend.dto.platine_sabiane_questionnaire.campaign.MetadataValueItem;
import fr.insee.protools.backend.dto.platine_sabiane_questionnaire.campaign.MetadataVariables;
import fr.insee.protools.backend.service.DelegateContextVerifier;
import fr.insee.protools.backend.service.common.platine_sabiane.QuestionnaireHelper;
import fr.insee.protools.backend.service.context.ContextService;
import fr.insee.protools.backend.service.nomenclature.NomenclatureService;
import fr.insee.protools.backend.service.platine.questionnaire.PlatineQuestionnaireService;
import fr.insee.protools.backend.service.questionnaire_model.QuestionnaireModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

import static fr.insee.protools.backend.service.context.ContextConstants.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class PlatineQuestionnaireCreateContextTask implements JavaDelegate, DelegateContextVerifier {

    private final ContextService protoolsContext;
    private final ObjectMapper objectMapper;
    private final NomenclatureService nomenclatureService;
    private final QuestionnaireModelService questionnaireModelService;
    private final PlatineQuestionnaireService platineQuestionnaireService;

    @Override
    public void execute(DelegateExecution execution) {
            JsonNode contextRootNode = protoolsContext.getContextByProcessInstance(execution.getProcessInstanceId());
            //check context
            checkContextOrThrow(log, execution.getProcessInstanceId(), contextRootNode);

            MetadataValue metadataDto = createMetadataDto(contextRootNode);
            log.info("ProcessInstanceId={}  - campagne={}"
                    , execution.getProcessInstanceId(), contextRootNode.path(CTX_CAMPAGNE_CONTEXTE).asText());

            QuestionnaireHelper.createQuestionnaire(contextRootNode, platineQuestionnaireService, nomenclatureService,
                    questionnaireModelService, execution.getProcessInstanceId(), metadataDto);
        log.debug("ProcessInstanceId={}  end",execution.getProcessInstanceId());
    }

    @Override
    public Set<String> getContextErrors(JsonNode contextRootNode) {
        if(contextRootNode==null){
            return Set.of(String.format("Class=%s : Context is missing ", this.getClass().getSimpleName()));
        }

        //Standard verifications (common between platine and sabiane)
        Set<String> missingNodes =
                QuestionnaireHelper.getCreateCtxContextErrors(contextRootNode);

        //Metadata used by platine
        Set<String> requiredMetadonnes =
                Set.of(CTX_META_LABEL_LONG_OPERATION, CTX_META_OBJECTIFS_COURTS, CTX_META_CARACTERE_OBLIGATOIRE,
                        CTX_META_NUMERO_VISA, CTX_META_MINISTERE_TUTELLE, CTX_META_PARUTION_JO, CTX_META_DATE_PARUTION_JO,
                        CTX_META_RESPONSABLE_OPERATIONNEL, CTX_META_RESPONSABLE_TRAITEMENT, CTX_META_ANNEE_VISA,
                        CTX_META_QUALITE_STATISTIQUE, CTX_META_TEST_NON_LABELLISE,
                        //Legal
                        CTX_META_URL_LOI_RGPD,CTX_META_URL_LOI_INFORMATIQUE,CTX_META_URL_LOI_STATISTIQUE
                );

        if (contextRootNode.get(CTX_METADONNEES) != null) {
            missingNodes.addAll(DelegateContextVerifier.computeMissingChildrenMessages(requiredMetadonnes,contextRootNode.path(CTX_METADONNEES),getClass()));
        }

        return missingNodes;
    }



    MetadataValue createMetadataDto(JsonNode contextRootNode){
        JsonNode metadataNode = contextRootNode.path(CTX_METADONNEES);
        return MetadataValue.builder()
                .value(MetadataVariables.builder()
                        .variables(
                                List.of(
                                        new MetadataValueItem(MetadataConstants.Enq_LibelleEnquete,metadataNode.path(CTX_META_LABEL_LONG_OPERATION).asText()),
                                        new MetadataValueItem(MetadataConstants.Enq_ObjectifsCourts,metadataNode.path(CTX_META_OBJECTIFS_COURTS).asText()),
                                        new MetadataValueItem(MetadataConstants.Enq_CaractereObligatoire,metadataNode.path(CTX_META_CARACTERE_OBLIGATOIRE).asBoolean()),
                                        new MetadataValueItem(MetadataConstants.Enq_NumeroVisa,metadataNode.path(CTX_META_NUMERO_VISA).asText()),
                                        new MetadataValueItem(MetadataConstants.Enq_MinistereTutelle,metadataNode.path(CTX_META_MINISTERE_TUTELLE).asText()),
                                        new MetadataValueItem(MetadataConstants.Enq_ParutionJo,metadataNode.path(CTX_META_PARUTION_JO).asBoolean()),
                                        new MetadataValueItem(MetadataConstants.Enq_DateParutionJo,metadataNode.path(CTX_META_DATE_PARUTION_JO).asText()),
                                        new MetadataValueItem(MetadataConstants.Enq_RespOperationnel,metadataNode.path(CTX_META_RESPONSABLE_OPERATIONNEL).asText()),
                                        new MetadataValueItem(MetadataConstants.Enq_RespTraitement,metadataNode.path(CTX_META_RESPONSABLE_TRAITEMENT).asText()),
                                        new MetadataValueItem(MetadataConstants.Enq_AnneeVisa,metadataNode.path(CTX_META_ANNEE_VISA).asInt()),
                                        new MetadataValueItem(MetadataConstants.Enq_QualiteStatistique,metadataNode.path(CTX_META_QUALITE_STATISTIQUE).asBoolean()),
                                        new MetadataValueItem(MetadataConstants.Enq_TestNonLabellise,metadataNode.path(CTX_META_TEST_NON_LABELLISE).asBoolean()),
                                        new MetadataValueItem(MetadataConstants.Loi_statistique,metadataNode.path(CTX_META_URL_LOI_STATISTIQUE).asText()),
                                        new MetadataValueItem(MetadataConstants.Loi_rgpd,metadataNode.path(CTX_META_URL_LOI_RGPD).asText()),
                                        new MetadataValueItem(MetadataConstants.Loi_informatique,metadataNode.path(CTX_META_URL_LOI_INFORMATIQUE).asText())
                                        )
                        )
                        .inseeContext(contextRootNode.path(CTX_CAMPAGNE_CONTEXTE).asText())
                        .build())
                .build();
    }
}
