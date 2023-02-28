package fr.insee.protools.backend.service.delegate;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SampleServiceTask  implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) {
        String currentActivityId=delegateExecution.getCurrentActivityId();
        String messageLog = "";

        String unit = delegateExecution.getVariable("unit", String.class);
        if (unit!=null) {
            messageLog+=" - unit="+unit;
        }

        log.info("\t << Sample service getCurrentActivityId={} - getCurrentFlowElement={} {}", currentActivityId,
            delegateExecution.getCurrentFlowElement().getName(), messageLog);

        if (currentActivityId.equals("createAccountTask")) {
            try {
                log.info("{} before sleep 5000",unit);
                Thread.sleep(5000);
                log.info("{} after sleep 5000",unit);
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if (currentActivityId.equals("AddToSurveyTask")) {
            try {
                log.info("{} before sleep 1000",unit);
                Thread.sleep(1000);
                log.info("{} after sleep 1000",unit);
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        log.info("\t >> Sample service getCurrentActivityId={} - getCurrentFlowElement={} {}"
            ,delegateExecution.getCurrentActivityId(), delegateExecution.getCurrentFlowElement().getName(),messageLog);
    }
}
