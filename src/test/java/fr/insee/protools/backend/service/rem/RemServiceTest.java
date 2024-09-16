package fr.insee.protools.backend.service.rem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.dto.rem_tmp.InterrogationAccountDto;
import fr.insee.protools.backend.service.utils.TestServiceWithRestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static fr.insee.protools.backend.restclient.configuration.ApiConfigProperties.KNOWN_API.KNOWN_API_REM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RemServiceTest extends TestServiceWithRestClient {
    static ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks
    RemService service;

    @Test
    void getPartitionAllInterroPaginated_should_call_correctURIAndBody() {
    }

    @Test
    void getInterrogationIdsWithoutAccountForPartition() {
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
        List<JsonNode> contacts = IntStream.range(0, 25)
                .mapToObj(i ->
                        objectMapper.createObjectNode().put("id", UUID.randomUUID().toString()).put("toto", i)
                )
                .collect(Collectors.toList());

        //Call method under tests
        service.putContactsPlatine(contacts);

        //Verify
        verify(requestBodyUriSpec).uri("contacts-platine");
        verify(restClient).put();
        // and with expected body
        verify(requestBodyUriSpec).body(contacts);
    }

    @Test
    void putContactsPlatine_should_NotCallIfEmpty() {
        //Prepare
        List<JsonNode> contacts = List.of();

        //Call method under tests
        service.putContactsPlatine(contacts);

        //Verify
        verify(restClient,never()).put();
        verify(requestBodyUriSpec, never()).uri(anyString());
    }

    @Test
    void putContactsPlatine_should_NotCallIfNull() {
        //Prepare
        List<JsonNode> contacts = null;

        //Call method under tests
        service.putContactsPlatine(contacts);

        //Verify
        verify(restClient,never()).put();
        verify(requestBodyUriSpec, never()).uri(anyString());
    }

    @Test
    void postRemiseEnCollecte() {
    }
}