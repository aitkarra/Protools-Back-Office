package fr.insee.protools.backend.service.platine.delegate;

import fr.insee.protools.backend.service.DelegateContextVerifier;
import fr.insee.protools.backend.service.context.ContextService;
import fr.insee.protools.backend.service.exception.ProtoolsTaskBPMNError;
import fr.insee.protools.backend.service.platine.pilotage.PlatinePilotageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

//TODO : à supprimer

@Slf4j
@Component
@RequiredArgsConstructor
public class PlatinePilotageCreateSurveyUnitTask implements JavaDelegate, DelegateContextVerifier {

    private final ContextService protoolsContext;
    private final PlatinePilotageService platinePilotageService;

    @Override
    public void execute(DelegateExecution execution) {
        throw new ProtoolsTaskBPMNError("Removed");

    }

}
