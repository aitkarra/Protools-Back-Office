package fr.insee.protools.backend.service.utils;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.restclient.RestClientHelper;
import fr.insee.protools.backend.restclient.exception.runtime.HttpClient4xxBPMNError;
import fr.insee.protools.backend.restclient.pagination.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.List;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TestServiceWithRestClient {
    @Mock
    protected RestClient restClient;
    @Mock
    protected RestClientHelper restClientHelper;
    @Mock
    protected RestClient.RequestBodyUriSpec requestBodyUriSpec; // Mock post() call
    @Mock
    protected RestClient.ResponseSpec responseSpec; // Mock retrieve() call
    @Mock
    protected RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @BeforeEach
    void initRestClientMock() {
        //Prepare
        lenient().doReturn(restClient).when(restClientHelper).getRestClient(any());
        // Mock the chaining of RestClient calls for post/put/patch
        lenient().when(restClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(restClient.put()).thenReturn(requestBodyUriSpec);
        lenient().when(restClient.patch()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestBodyUriSpec);
        lenient().when(requestHeadersUriSpec.uri(ArgumentMatchers.<Function<UriBuilder, URI>>any())).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.body(any(Object.class))).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);

        //Specific for get
        lenient().when(restClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
    }


    public void mockGetPageableBodyResponse(List l){
        PageResponse<JsonNode> mockResponse = PageResponse.builder().content(l).build();
        lenient().when(responseSpec.body(ArgumentMatchers.<ParameterizedTypeReference>any())).thenReturn(mockResponse);
        lenient().when(responseSpec.body(ArgumentMatchers.<Class>any())).thenReturn(mockResponse);
    }

    public void mockMakeRetrieveThrow(HttpStatusCode errorCode){
        HttpClient4xxBPMNError ex = new HttpClient4xxBPMNError("msg",errorCode);
        lenient().when(requestBodyUriSpec.retrieve()).thenThrow(ex);
        //Specific for get
        lenient().when(requestHeadersUriSpec.retrieve()).thenThrow(ex);
    }

}
