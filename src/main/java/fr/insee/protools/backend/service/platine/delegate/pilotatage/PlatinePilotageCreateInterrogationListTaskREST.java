package fr.insee.protools.backend.service.platine.delegate.pilotatage;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.dto.ContexteProcessus;
import fr.insee.protools.backend.service.platine.service.PlatinePilotageService;
import fr.insee.protools.backend.service.utils.delegate.DefaultCallServiceInterroListTask;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class PlatinePilotageCreateInterrogationListTaskREST extends DefaultCallServiceInterroListTask {

    private final PlatinePilotageService platinePilotageService;

    @Override
    protected void serviceAction(ContexteProcessus context, List<JsonNode> list, String... params) {
        platinePilotageService.postInterrogations(context.getId().toString(),list);
    }
}
