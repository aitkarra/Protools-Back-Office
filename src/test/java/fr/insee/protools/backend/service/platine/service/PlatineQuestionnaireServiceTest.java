package fr.insee.protools.backend.service.platine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import fr.insee.protools.backend.service.utils.TestServiceWithRestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static fr.insee.protools.backend.restclient.configuration.ApiConfigProperties.KNOWN_API.KNOWN_API_PLATINE_QUESTIONNAIRE;
import static fr.insee.protools.backend.utils.data.InterroExamples.generateEmptyInterro;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlatineQuestionnaireServiceTest extends TestServiceWithRestClient {

    @InjectMocks
    PlatineQuestionnaireServiceImpl service;

    @Test
    void postContext_should_call_correctURIAndBody(){
        //Prepare
        JsonNode contextNode = JsonNodeFactory.instance.objectNode();

        //Call method under test
        service.postContext("TOTO", contextNode);

        //Verifications
        verify(restClientHelper).getRestClient(KNOWN_API_PLATINE_QUESTIONNAIRE);
        verify(restClient).post();
        // Then - Verify that the uri method was called with "/context"
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
        verify(restClientHelper).getRestClient(KNOWN_API_PLATINE_QUESTIONNAIRE);
        verify(restClient).post();
        verify(requestBodyUriSpec).uri("/interrogations");
        // and with expected body
        verify(requestBodyUriSpec).body(interroList);
    }

}