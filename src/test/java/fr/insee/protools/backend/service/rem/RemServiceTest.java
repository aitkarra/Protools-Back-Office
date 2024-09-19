package fr.insee.protools.backend.service.rem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.dto.rem_tmp.InterrogationAccountDto;
import fr.insee.protools.backend.dto.rem_tmp.InterrogationIdentifiersDto;
import fr.insee.protools.backend.restclient.exception.runtime.HttpClient4xxBPMNError;
import fr.insee.protools.backend.restclient.pagination.PageResponse;
import fr.insee.protools.backend.service.utils.TestServiceWithRestClient;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.util.UriBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static fr.insee.protools.backend.restclient.configuration.ApiConfigProperties.KNOWN_API.KNOWN_API_REM;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RemServiceTest extends TestServiceWithRestClient {
    static ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks
    RemService service;

    @Value("${fr.insee.protools.api.rem.interrogation.page.size:5000}")
    private int pageSizeGetInterro;

    @Test
    void getPartitionAllInterroPaginated_should_makeCorrectCalls() {
        //Prepare
        String partitionId = "PartitionIDXXREM";
        long page = 1024;

        List<JsonNode> mockedResponse = List.of(objectMapper.createObjectNode().put("xxx", 1).put("66969jj", "toto"));
        mockRetrieveBodyResponse(mockedResponse);
        //Call method under tests
        PageResponse<JsonNode> response = service.getPartitionAllInterroPaginated(partitionId, page);

        //Verify
        verify(restClientHelper).getRestClient(KNOWN_API_REM);
        verify(restClient).get();

        UriBuilder mockUriBuilder = mockGETURIBuilderAndPrepareForTest();

        // Verify that the correct path and query parameters were used
        verify(mockUriBuilder).path("/interrogations");
        verify(mockUriBuilder).queryParam("page", page);
        verify(mockUriBuilder).queryParam("size", pageSizeGetInterro);
        verify(mockUriBuilder).queryParam("partition_id", partitionId);

        assertEquals(mockedResponse, response.getContent(), "The response is not what was expected/mocked");
    }


    @Test
    void getPartitionAllInterroPaginated_shouldCatch() {
        //Prepare
        String partitionId = "PartitionIDYY";
        long page = 0;

        mockMakeRetrieveThrow(HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()));
        //Call method under tests
        assertThrows(HttpClient4xxBPMNError.class, () -> service.getPartitionAllInterroPaginated(partitionId, page));
    }

    @Test
    void getPartitionAllInterroPaginated_shouldCatch_404() {
        //Prepare
        String partitionId = "PartitionIDYY";
        long page = 0;

        mockMakeRetrieveThrow(HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value()));
        //Call method under tests
        HttpClient4xxBPMNError exception = assertThrows(HttpClient4xxBPMNError.class, () -> service.getPartitionAllInterroPaginated(partitionId, page));
        assertThat(exception.getMessage(), containsString("404"));
    }


    @Test
    void getPartitionAllInterroPaginated_shouldWork_when_responseIsNull() {
        //Prepare
        String partitionId = "PartitionIDYY";
        long page = 0;

        mockRetrieveBody(null);
        //Call method under tests
        PageResponse<JsonNode> response = service.getPartitionAllInterroPaginated(partitionId, page);

        assertCorrectPageResponseForNull(response);
    }

    @Test
    void getInterrogationIdsWithoutAccountForPartition_should_makeCorrectCalls() {
        //Prepare
        String partitionId = "PartitionIDXXREM";
        UUID expectedID1=UUID.randomUUID();
        UUID expectedID2=UUID.randomUUID();


        List<InterrogationIdentifiersDto> mockedResponse =
                List.of(
                        InterrogationIdentifiersDto.builder()
                        .interrogationId(expectedID1).surveyUnitId(UUID.randomUUID()).originId(UUID.randomUUID().toString())
                        .build()
                        ,
                        InterrogationIdentifiersDto.builder()
                                .interrogationId(expectedID2).surveyUnitId(UUID.randomUUID()).originId(UUID.randomUUID().toString())
                                .build()
                );
        mockRetrieveBody(mockedResponse);

        //Call method under tests
        List<String> response = service.getInterrogationIdsWithoutAccountForPartition(partitionId);

        //Verify
        verify(restClientHelper).getRestClient(KNOWN_API_REM);
        verify(restClient).get();

        UriBuilder mockUriBuilder = mockGETURIBuilderAndPrepareForTest();

        // Verify that the correct path and query parameters were used
        verify(mockUriBuilder).path("/interrogations/ids");
        verify(mockUriBuilder).queryParam("hasAccount", false);
        verify(mockUriBuilder).queryParam("partition_id", partitionId);

        assertThat(response,
                IsIterableContainingInAnyOrder.containsInAnyOrder(expectedID1.toString(),expectedID2.toString()));
    }


    @Test
    void getInterrogationIdsWithoutAccountForPartition_shouldCatch() {
        //Prepare
        String partitionId = "PartitionIDYY";

        mockMakeRetrieveThrow(HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()));
        //Call method under tests
        assertThrows(HttpClient4xxBPMNError.class, () -> service.getInterrogationIdsWithoutAccountForPartition(partitionId));
    }

    @Test
    void getInterrogationIdsWithoutAccountForPartition_shouldCatch_404() {
        //Prepare
        String partitionId = "PartitionIDYxY";

        mockMakeRetrieveThrow(HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value()));
        //Call method under tests
        HttpClient4xxBPMNError exception = assertThrows(HttpClient4xxBPMNError.class, () -> service.getInterrogationIdsWithoutAccountForPartition(partitionId));
        assertThat(exception.getMessage(), containsString("404"));
    }

    @Test
    void patchInterrogationsSetAccounts() {
        //Prepare
        Map<String, String> userByInterroId = Map.of(
                UUID.randomUUID().toString(), "USER1",
                UUID.randomUUID().toString(), "USER2",
                UUID.randomUUID().toString(), "USER3",
                UUID.randomUUID().toString(), "USER4");

        //Call method under tests
        service.patchInterrogationsSetAccounts(userByInterroId);

        //Verify
        verify(restClientHelper).getRestClient(KNOWN_API_REM);
        verify(restClient).patch();
        verify(requestBodyUriSpec).uri("/interrogations/account");
        // and with expected body
        final ArgumentCaptor<List<InterrogationAccountDto>> bodyCaptor
                = ArgumentCaptor.forClass((Class) List.class);
        verify(requestBodyUriSpec).body(bodyCaptor.capture());

        List<List<InterrogationAccountDto>> bodyValues = bodyCaptor.getAllValues();
        assertEquals(1, bodyValues.size(), "We should have exactly one value");
        List<InterrogationAccountDto> bodyListDto = bodyValues.get(0);

        assertEquals(userByInterroId.size(), bodyListDto.size(), "Wrong number of elements in the body");

        boolean allMatch = userByInterroId.entrySet().stream()
                .allMatch(entry ->
                        bodyListDto.stream().anyMatch(dto ->
                                dto.getInterrogationId().toString().equals(entry.getKey()) &&
                                        dto.getAccount().equals(entry.getValue())
                        )
                );
        assertTrue(allMatch, "Body does not match the input");
    }


    @Test
    void putContactsPlatine_should_call_correctURIAndBody() {
        //Prepare
        List<JsonNode> listNodes = IntStream.range(0, 25)
                .mapToObj(i ->
                        objectMapper.createObjectNode().put("id", UUID.randomUUID().toString()).put("toto", i)
                )
                .collect(Collectors.toList());

        //Call method under tests
        service.putContactsPlatine(listNodes);

        //Verify
        verify(restClientHelper).getRestClient(KNOWN_API_REM);        verify(restClient).put();
        verify(restClient).put();

        verify(requestBodyUriSpec).uri("/contacts-platine");
        // and with expected body
        verify(requestBodyUriSpec).body(listNodes);
    }

    @Test
    void putContactsPlatine_should_NotCallIfEmpty() {
        //Prepare
        List<JsonNode> listNodes = List.of();

        //Call method under tests
        service.putContactsPlatine(listNodes);

        //Verify
        verify(restClient, never()).put();
        verify(requestBodyUriSpec, never()).uri(anyString());
    }

    @Test
    void putContactsPlatine_should_NotCallIfNull() {
        //Prepare
        List<JsonNode> listNodes = null;

        //Call method under tests
        service.putContactsPlatine(listNodes);

        //Verify
        verify(restClient, never()).put();
        verify(requestBodyUriSpec, never()).uri(anyString());
    }

    @Test
    void postRemiseEnCollecte_should_call_correctURIAndBody() {
        //Prepare
        List<JsonNode> listNodes = IntStream.range(0, 69)
                .mapToObj(i ->
                        objectMapper.createObjectNode().put("id", UUID.randomUUID().toString()).put("toto", i)
                )
                .collect(Collectors.toList());

        //Call method under tests
        service.postRemiseEnCollecte(listNodes);

        //Verify
        verify(requestBodyUriSpec).uri("/remise-en-collecte");
        verify(restClient).post();
        // and with expected body
        verify(requestBodyUriSpec).body(listNodes);
    }

    @Test
    void postRemiseEnCollecte_should_NotCallIfEmpty() {
        //Prepare
        List<JsonNode> listNodes = List.of();

        //Call method under tests
        service.postRemiseEnCollecte(listNodes);

        //Verify
        verify(restClient, never()).post();
        verify(requestBodyUriSpec, never()).uri(anyString());
    }

    @Test
    void postRemiseEnCollecte_should_NotCallIfNull() {
        //Prepare
        List<JsonNode> listNodes = null;

        //Call method under tests
        service.postRemiseEnCollecte(listNodes);

        //Verify
        verify(restClient, never()).post();
        verify(requestBodyUriSpec, never()).uri(anyString());
    }
}