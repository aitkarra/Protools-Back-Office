package fr.insee.protools.backend.service.platine.delegate;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.repository.IUniteEnquetee;
import fr.insee.protools.backend.service.DelegateContextVerifier;
import fr.insee.protools.backend.service.common.platine_sabiane.QuestionnaireHelper;
import fr.insee.protools.backend.service.context.ContextService;
import fr.insee.protools.backend.service.platine.questionnaire.PlatineQuestionnaireService;
import fr.insee.protools.backend.service.scheduled.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static fr.insee.protools.backend.service.context.ContextConstants.CTX_CAMPAGNE_CONTEXTE;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlatineQuestionnaireCreateSurveyUnitTaskAsync implements JavaDelegate, DelegateContextVerifier {

    private final ContextService protoolsContext;
    private final PlatineQuestionnaireService platineQuestionnaireService;
    private final IUniteEnquetee iUniteEnquetee;
    @Autowired
    TaskService taskService;

    @Override
    public void execute(DelegateExecution execution) {
        Boolean start = Boolean.FALSE;
        JsonNode contextRootNode = protoolsContext.getContextByProcessInstance(execution.getProcessInstanceId());
        //TODO: delete this log if necessary
        log.debug("ProcessInstanceId={}  - campagne={} - begin"
                ,execution.getProcessInstanceId(),contextRootNode.path(CTX_CAMPAGNE_CONTEXTE).asText());

        checkContextOrThrow(log,execution.getProcessInstanceId(), contextRootNode);
        QuestionnaireHelper.createSUTaskPlatineAsync(execution,protoolsContext,iUniteEnquetee,platineQuestionnaireService);

//        TODO gestion des timeouts
        while (!start) {
            log.info("start : "+ start);
            try {
                long nbInterogation = iUniteEnquetee.getCommandesByProcessInstanceIdAndCurrentActivityId(execution.getProcessInstanceId(), execution.getCurrentActivityId());
                start = taskService.isTerminated(execution.getProcessInstanceId(), execution.getCurrentActivityId(), nbInterogation);
                log.info("Tâche planifiée en cours pour le processInstanceId : " + execution.getProcessInstanceId() + ", le currentActivityId " + execution.getCurrentActivityId() + ",et le nbInterogation totale " + nbInterogation);
                Thread.sleep(5000);
                if (start) {
                    break;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        log.debug("ProcessInstanceId={}  - campagne={} - end",
                execution.getProcessInstanceId(),contextRootNode.path(CTX_CAMPAGNE_CONTEXTE).asText());
    }

    @Override
    public Set<String> getContextErrors(JsonNode contextRootNode) {
        return QuestionnaireHelper.getCreateSUContextErrorsPlatine(contextRootNode);
    }


}
