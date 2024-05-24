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

@Slf4j
@Component
@RequiredArgsConstructor
public class PlatineQuestionnaireCreateSurveyUnitTask implements JavaDelegate, DelegateContextVerifier {

    private final ContextService protoolsContext;
//    private final PlatineQuestionnaireService platineQuestionnaireService;
    private final IUniteEnquetee iUniteEnquetee;

    @Override
    public void execute(DelegateExecution execution) {
        JsonNode contextRootNode = protoolsContext.getContextByProcessInstance(execution.getProcessInstanceId());
        checkContextOrThrow(log,execution.getProcessInstanceId(), contextRootNode);
//        QuestionnaireHelper.createSUTaskPlatine(execution,protoolsContext,platineQuestionnaireService);
        QuestionnaireHelper.createSUTaskPlatineAsync(execution,protoolsContext,iUniteEnquetee);
    }

    @Override
    public Set<String> getContextErrors(JsonNode contextRootNode) {
        return QuestionnaireHelper.getCreateSUContextErrorsPlatine(contextRootNode);
    }


}
