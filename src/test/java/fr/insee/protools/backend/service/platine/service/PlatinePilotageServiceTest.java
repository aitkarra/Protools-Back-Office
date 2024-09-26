package fr.insee.protools.backend.service.platine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import fr.insee.protools.backend.dto.platine.pilotage.PlatinePilotageCommunicationEventDto;
import fr.insee.protools.backend.dto.platine.pilotage.PlatinePilotageCommunicationEventType;
import fr.insee.protools.backend.restclient.exception.runtime.HttpClient4xxBPMNError;
import fr.insee.protools.backend.restclient.pagination.PageResponse;
import fr.insee.protools.backend.service.utils.TestServiceWithRestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.util.UriBuilder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static fr.insee.protools.backend.restclient.configuration.ApiConfigProperties.KNOWN_API.KNOWN_API_PLATINE_PILOTAGE;
import static fr.insee.protools.backend.utils.data.InterroExamples.generateEmptyInterro;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlatinePilotageServiceTest extends TestServiceWithRestClient {

    private static ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    PlatinePilotageServiceImpl service;
    @Value("${fr.insee.protools.api.platine-pilotage.interrogation.page.size:5000}")
    private int pageSizeGetInterro;

    @Test
    void postContext_should_call_correctURIAndBody() {
        //Prepare
        JsonNode contextNode = JsonNodeFactory.instance.objectNode();

        //Call method under test
        service.postContext("TOTO", contextNode);

        //Verifications
        // Then - Verify that the uri method was called with "/context"
        verify(restClientHelper).getRestClient(KNOWN_API_PLATINE_PILOTAGE);
        verify(restClient).post();
        verify(requestBodyUriSpec).uri("/context");
        // and with expected body
        verify(requestBodyUriSpec).body(contextNode);
    }


    @Test
    void postInterrogations_should_makeCorrectCall() {
        //Prepare
        List<JsonNode> interroList = IntStream.range(0, 100)
                .mapToObj(i -> generateEmptyInterro().interro()) // Call your function and get the JsonNode
                .collect(Collectors.toList());

        //Call method under tests
        service.postInterrogations("TOTO", interroList);

        //Verify
        verify(restClientHelper).getRestClient(KNOWN_API_PLATINE_PILOTAGE);
        verify(restClient).post();
        verify(requestBodyUriSpec).uri("/interrogations");
        // and with expected body
        verify(requestBodyUriSpec).body(interroList);
    }

    @Test
    void postCommunicationEvents_should_makeCorrectCall() {
        //Prepare
        List<PlatinePilotageCommunicationEventDto> eventList = IntStream.range(0, 10)
                .mapToObj(i ->
                        PlatinePilotageCommunicationEventDto.builder()
                                .interrogationId(UUID.randomUUID().toString())
                                .state(PlatinePilotageCommunicationEventType.COMMUNICATION_STATE_SENT)
                                .communcationId(UUID.randomUUID().toString()).build()
                ) // Call your function and get the JsonNode
                .collect(Collectors.toList());

        //Call method under tests
        service.postCommunicationEvents(eventList);

        //Verify
        verify(restClient).post();
        verify(requestBodyUriSpec).uri("/interrogations/communication-events");
        // and with expected body
        verify(requestBodyUriSpec).body(eventList);
    }


    @Test
    void getInterrogationToFollowUpPaginated_shouldMakeCorrectCalls() {
        //Prepare
        String partitionId = "PartitionIDXX";
        long page = 69;
        Optional<Boolean> isToFollowUp = Optional.of(Boolean.FALSE);

        List<JsonNode> mockedResponse = List.of(objectMapper.createObjectNode().put("id", 1).put("id", "toto"));
        mockRetrieveBodyResponse(mockedResponse);
        //Call method under tests
        PageResponse<JsonNode> response = service.getInterrogationToFollowUpPaginated(partitionId, page, isToFollowUp);

        //Verify
        verify(restClientHelper).getRestClient(KNOWN_API_PLATINE_PILOTAGE);
        verify(restClient).get();

        UriBuilder mockUriBuilder = mockGETURIBuilderAndPrepareForTest();

        // Verify that the correct path and query parameters were used
        verify(mockUriBuilder).path("/interrogations");
        verify(mockUriBuilder).queryParam("page", page);
        verify(mockUriBuilder).queryParam("size", pageSizeGetInterro);
        verify(mockUriBuilder).queryParam("partition_id", partitionId);
        verify(mockUriBuilder).queryParamIfPresent(eq("follow-up"), eq(isToFollowUp));

        assertEquals(mockedResponse, response.getContent(), "The response is not what was expected/mocked");
    }


    @Test
    void getInterrogationToFollowUpPaginated_shouldCatch() {
        //Prepare
        String partitionId = "PartitionIDYY";
        long page = 0;
        Optional<Boolean> isToFollowUp = Optional.of(Boolean.TRUE);

        mockMakeRetrieveThrow(HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()));
        //Call method under tests
        assertThrows(HttpClient4xxBPMNError.class, () -> service.getInterrogationToFollowUpPaginated(partitionId, page, isToFollowUp));
    }

    @Test
    void getInterrogationToFollowUpPaginated_shouldCatch_404() {
        //Prepare
        String partitionId = "PartitionIDYY";
        long page = 0;
        Optional<Boolean> isToFollowUp = Optional.of(Boolean.TRUE);

        mockMakeRetrieveThrow(HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value()));
        //Call method under tests
        HttpClient4xxBPMNError exception = assertThrows(HttpClient4xxBPMNError.class, () -> service.getInterrogationToFollowUpPaginated(partitionId, page, isToFollowUp));
        assertThat(exception.getMessage(), containsString("404"));
    }

    @Test
    void getInterrogationToFollowUpPaginated_shouldWork_when_responseIsNull() {
        //Prepare
        String partitionId = "PartitionIDXX77";
        long page = 68;
        Optional<Boolean> isToFollowUp = Optional.of(Boolean.TRUE);

        mockRetrieveBody(null);
        //Call method under tests
        PageResponse<JsonNode> response = service.getInterrogationToFollowUpPaginated(partitionId, page, isToFollowUp);

        assertCorrectPageResponseForNull(response);
    }
}