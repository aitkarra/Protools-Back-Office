package fr.insee.protools.backend.service.platine.delegate;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.repository.IUniteEnquetee;
import fr.insee.protools.backend.service.DelegateContextVerifier;
import fr.insee.protools.backend.service.common.platine_sabiane.QuestionnaireHelper;
import fr.insee.protools.backend.service.context.ContextService;
import fr.insee.protools.backend.service.platine.questionnaire.PlatineQuestionnaireService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Set;

import static fr.insee.protools.backend.service.context.ContextConstants.CTX_CAMPAGNE_CONTEXTE;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlatineQuestionnaireCreateSurveyUnitTaskAsync implements JavaDelegate, DelegateContextVerifier {

    private final ContextService protoolsContext;
    private final PlatineQuestionnaireService platineQuestionnaireService;
    private final IUniteEnquetee iUniteEnquetee;
    @Override
    public void execute(DelegateExecution execution) {
        JsonNode contextRootNode = protoolsContext.getContextByProcessInstance(execution.getProcessInstanceId());
        //TODO: delete this log if necessary
        log.debug("ProcessInstanceId={}  - campagne={} - begin"
                ,execution.getProcessInstanceId(),contextRootNode.path(CTX_CAMPAGNE_CONTEXTE).asText());

        checkContextOrThrow(log,execution.getProcessInstanceId(), contextRootNode);
        QuestionnaireHelper.createSUTaskPlatineAsync(execution,protoolsContext,iUniteEnquetee);
        log.debug("ProcessInstanceId={}  - campagne={} - end",
                execution.getProcessInstanceId(),contextRootNode.path(CTX_CAMPAGNE_CONTEXTE).asText());


    }

    @Override
    public Set<String> getContextErrors(JsonNode contextRootNode) {
        return QuestionnaireHelper.getCreateSUContextErrorsPlatine(contextRootNode);
    }

}
