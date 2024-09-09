package fr.insee.protools.backend.service.traiterXXXX;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.restclient.RestClientHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.List;

import static fr.insee.protools.backend.restclient.configuration.ApiConfigProperties.KNOWN_API.KNOWN_API_TRAITERXXX;

@Service
@Slf4j
@RequiredArgsConstructor
public class TraiterXXXService {

    private final RestClientHelper restClientHelper;

    public List<JsonNode> getRemiseEnCollecteForPartition(String partitionId) {
        log.debug("getRemiseEnCollecteForPartition - partitionId={} ",partitionId);
        ParameterizedTypeReference<List<JsonNode>> typeReference = new ParameterizedTypeReference<>() { };

        List<JsonNode> response = restClientHelper.getRestClient(KNOWN_API_TRAITERXXX)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("remise-en-collecte")
                        .queryParam("partition_id", partitionId)
                        .build())
                .retrieve()
                .body(typeReference);
        log.trace("getRemiseEnCollecteForPartition={} - response={} ", partitionId, response);
        return response;
    }

    public void postContext(String campaignId,JsonNode contextRootNode) {
        log.trace("postContext: campaignId={}",campaignId);
        var response = restClientHelper.getRestClient(KNOWN_API_TRAITERXXX)
                .post()
                .uri("/context")
                .body(contextRootNode)
                .retrieve()
                .body(String.class);
        log.trace("postContext: campaignId={} - response={} ",campaignId,response);
    }
}
