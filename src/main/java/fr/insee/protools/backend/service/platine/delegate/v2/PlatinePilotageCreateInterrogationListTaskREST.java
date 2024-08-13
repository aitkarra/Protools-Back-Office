package fr.insee.protools.backend.service.platine.delegate.v2;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.service.platine.pilotage.PlatinePilotageService;
import fr.insee.protools.backend.service.utils.delegate.DefaultCallServiceInterroListTask;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class PlatinePilotageCreateInterrogationListTaskREST extends DefaultCallServiceInterroListTask {

    private final PlatinePilotageService platinePilotageService;

    @Override
    protected void serviceAction(String campaignId, List<JsonNode> list, String... params) {
        platinePilotageService.putQuestionings(campaignId,list);
    }
}
