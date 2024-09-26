package fr.insee.protools.backend.service.platine.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface IPlatineQuestionnaireService {

    void postContext(String campaignId, JsonNode contextRootNode);

    void postInterrogations(String campaignId, List<JsonNode> interrogations);
}
