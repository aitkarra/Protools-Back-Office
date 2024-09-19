package fr.insee.protools.backend.service.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import fr.insee.protools.backend.restclient.RestClientHelper;
import fr.insee.protools.backend.restclient.exception.runtime.HttpClient4xxBPMNError;
import fr.insee.protools.backend.restclient.pagination.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static fr.insee.protools.backend.restclient.configuration.ApiConfigProperties.KNOWN_API.KNOWN_API_TRAITERXXX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        lenient().when(requestBodyUriSpec.uri(ArgumentMatchers.<Function<UriBuilder, URI>>any())).thenReturn(requestBodyUriSpec);

        lenient().when(requestBodyUriSpec.body(any(Object.class))).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);

        //Specific for get
        lenient().when(restClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestBodyUriSpec);
        lenient().when(requestHeadersUriSpec.uri(ArgumentMatchers.<Function<UriBuilder, URI>>any())).thenReturn(requestBodyUriSpec);
        lenient().when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
    }

    public UriBuilder mockPostPutPatchURIBuilderAndPrepareForTest() {
        // let's manually build a URI using the captured function and a mock UriBuilder
        ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);
        verify(requestBodyUriSpec).uri(uriCaptor.capture());
        UriBuilder mockUriBuilder = mock(UriBuilder.class);
        lenient().when(mockUriBuilder.path(anyString())).thenReturn(mockUriBuilder);
        lenient().when(mockUriBuilder.queryParam(anyString(), ArgumentMatchers.<Object>any())).thenReturn(mockUriBuilder);
        lenient().when(mockUriBuilder.queryParamIfPresent(anyString(), ArgumentMatchers.<Optional<Object>>any())).thenReturn(mockUriBuilder);

        lenient().when(mockUriBuilder.build(ArgumentMatchers.<Object>any())).thenReturn(URI.create("http://mockeduri"));

        // Call the captured function with the mocked UriBuilder
        uriCaptor.getValue().apply(mockUriBuilder);

        return mockUriBuilder;
    }


    public UriBuilder mockGETURIBuilderAndPrepareForTest() {
        // let's manually build a URI using the captured function and a mock UriBuilder
        ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);
        verify(requestHeadersUriSpec).uri(uriCaptor.capture());
        UriBuilder mockUriBuilder = mock(UriBuilder.class);
        when(mockUriBuilder.path(anyString())).thenReturn(mockUriBuilder);
        when(mockUriBuilder.queryParam(anyString(), ArgumentMatchers.<Object>any())).thenReturn(mockUriBuilder);
        lenient().when(mockUriBuilder.queryParamIfPresent(anyString(), ArgumentMatchers.<Optional<Object>>any())).thenReturn(mockUriBuilder);

        when(mockUriBuilder.build()).thenReturn(URI.create("http://mockeduri"));

        // Call the captured function with the mocked UriBuilder
        uriCaptor.getValue().apply(mockUriBuilder);

        return mockUriBuilder;
    }

    public void mockRetrieveBodyResponse(List l) {
        PageResponse<JsonNode> mockResponse = PageResponse.builder().content(l).build();
        lenient().when(responseSpec.body(ArgumentMatchers.<ParameterizedTypeReference>any())).thenReturn(mockResponse);
        lenient().when(responseSpec.body(ArgumentMatchers.<Class>any())).thenReturn(mockResponse);
    }

    public void mockRetrieveBody(Object l) {
        lenient().when(responseSpec.body(ArgumentMatchers.<ParameterizedTypeReference>any())).thenReturn(l);
        lenient().when(responseSpec.body(ArgumentMatchers.<Class>any())).thenReturn(l);
    }

    public void mockMakeRetrieveThrow(HttpStatusCode errorCode) {
        HttpClient4xxBPMNError ex = new HttpClient4xxBPMNError("msg", errorCode);
        lenient().when(requestBodyUriSpec.retrieve()).thenThrow(ex);
        //Specific for get
        lenient().when(requestHeadersUriSpec.retrieve()).thenThrow(ex);
    }

    public void assertCorrectPageResponseForNull(PageResponse pageResponse){
        assertNotNull(pageResponse,"The response should not be null");
        assertThat("The response should have empty content",pageResponse.getContent().isEmpty());
        assertEquals(5000,pageResponse.getPageSize(),"The response should have pageSize == 5000");
        assertEquals(0,pageResponse.getPageCount(),"The response should have pageCount == 0");
        assertEquals(0,pageResponse.getCurrentPage(),"The response should have currentPage == 0");
        assertEquals(0,pageResponse.getTotalElements(),"The response should have totalElements == 0");
    }

}
