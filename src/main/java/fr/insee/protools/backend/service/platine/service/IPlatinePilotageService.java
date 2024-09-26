package fr.insee.protools.backend.service.platine.service;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.dto.platine.pilotage.PlatinePilotageCommunicationEventDto;
import fr.insee.protools.backend.restclient.pagination.PageResponse;

import java.util.List;
import java.util.Optional;

public interface IPlatinePilotageService {

    void postCommunicationEvents(List<PlatinePilotageCommunicationEventDto> platinePilotageCommunicationEventList);

    void postContext(String campaignId, JsonNode contextRootNode);

    void postInterrogations(String campaignId, List<JsonNode> interrogations);

    PageResponse<JsonNode> getInterrogationToFollowUpPaginated(String partitionId, long page, Optional<Boolean> isToFollowUp);
}
