package fr.insee.protools.backend.service.meshuggah;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface IMeshuggahService {

    void postContext(String campaignId, JsonNode contextRootNode);

    void postCommunicationRequest(String campaignId, String communicationId, List<JsonNode> list);
}
