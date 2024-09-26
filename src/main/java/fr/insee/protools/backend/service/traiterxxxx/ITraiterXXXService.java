package fr.insee.protools.backend.service.traiterxxxx;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public interface ITraiterXXXService {

    List<JsonNode> getRemiseEnCollecteForPartition(String partitionId);

    void postContext(String campaignId, JsonNode contextRootNode);
}
