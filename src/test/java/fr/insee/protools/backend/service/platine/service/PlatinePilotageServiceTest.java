package fr.insee.protools.backend.service.platine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import fr.insee.protools.backend.restclient.RestClientHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlatinePilotageServiceTest {

    @Mock
    RestClientHelper restClientHelper;
    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec; // Mock post() call
    @Mock
    private RestClient.ResponseSpec responseSpec; // Mock retrieve() call

    @InjectMocks
    PlatinePilotageService service;

    @Test
    void postContext_should_call_correctURIAndBody(){
        //Prepare
        RestClient mockRestClient = mock(RestClient.class);
        doReturn(mockRestClient).when(restClientHelper).getRestClient(any());
        // Mock the chaining of RestClient calls
        when(mockRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(any(Object.class))).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);

        JsonNode contextNode = JsonNodeFactory.instance.objectNode();

        //Call method under test
        service.postContext("TOTO", contextNode);

        //Verifications
        // Then - Verify that the uri method was called with "/context"
        verify(requestBodyUriSpec).uri("/context");
        // and with expected body
        verify(requestBodyUriSpec).body(contextNode);
    }

}