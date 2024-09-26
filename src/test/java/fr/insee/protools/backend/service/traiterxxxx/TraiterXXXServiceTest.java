package fr.insee.protools.backend.service.traiterxxxx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import fr.insee.protools.backend.service.utils.TestServiceWithRestClient;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.util.UriBuilder;

import java.util.List;
import java.util.UUID;

import static fr.insee.protools.backend.restclient.configuration.ApiConfigProperties.KNOWN_API.KNOWN_API_TRAITERXXX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TraiterXXXServiceTest extends TestServiceWithRestClient {

    private static ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    TraiterXXXServiceImpl service;

    @Test
    void postContext_should_call_correctURIAndBody() {
        //Prepare
        JsonNode contextNode = JsonNodeFactory.instance.objectNode();

        //Call method under test
        service.postContext("TOTO", contextNode);

        //Verifications
        // Then - Verify that the uri method was called with "/context"
        verify(restClientHelper).getRestClient(KNOWN_API_TRAITERXXX);
        verify(restClient).post();
        verify(requestBodyUriSpec).uri("/context");
        // and with expected body
        verify(requestBodyUriSpec).body(contextNode);
    }

    @Test
    void getRemiseEnCollecteForPartition_should_makeCorrectCalls() {
        //Prepare
        String partitionId = "PartitionIDXXREM";
        String expectedID1 = UUID.randomUUID().toString();
        String expectedID2 = UUID.randomUUID().toString();


        List<JsonNode> mockedResponse = List.of(
                objectMapper.createObjectNode().put("id", expectedID1).put("count", "1"),
                objectMapper.createObjectNode().put("id", expectedID2).put("compta", "2")
        );
        mockRetrieveBody(mockedResponse);

        //Call method under tests
        List<JsonNode> response = service.getRemiseEnCollecteForPartition(partitionId);

        //Verify
        verify(restClientHelper).getRestClient(KNOWN_API_TRAITERXXX);
        verify(restClient).get();

        UriBuilder mockUriBuilder = mockGETURIBuilderAndPrepareForTest();

        // Verify that the correct path and query parameters were used
        verify(mockUriBuilder).path("/remise-en-collecte");
        verify(mockUriBuilder).queryParam("partition_id", partitionId);

        assertThat(response,
                IsIterableContainingInAnyOrder.containsInAnyOrder(mockedResponse.toArray()));
    }
}