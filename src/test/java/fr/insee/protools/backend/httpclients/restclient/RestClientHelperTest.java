package fr.insee.protools.backend.httpclients.restclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.ProtoolsTestUtils;
import fr.insee.protools.backend.httpclients.configuration.APIProperties;
import fr.insee.protools.backend.httpclients.configuration.ApiConfigProperties;
import fr.insee.protools.backend.httpclients.exception.ApiNotConfiguredBPMNError;
import fr.insee.protools.backend.httpclients.exception.KeycloakTokenConfigUncheckedBPMNError;
import fr.insee.protools.backend.httpclients.exception.runtime.HttpClient4xxBPMNError;
import fr.insee.protools.backend.httpclients.keycloak.KeycloakResponse;
import fr.insee.protools.backend.httpclients.keycloak.KeycloakService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RestClientHelperTest {

    @Spy
    private Environment environment;

    private KeycloakService keycloakService;

    @Mock
    private ApiConfigProperties apiConfigProperties;

    private RestClientHelper restClientHelper;


    private MockWebServer mockWebServer;
    private static final int port = 80;
    private static String getDummyUriWithPort() { return getServerHostPort()+"/api/test"; }
    private static String getServerHostPort() { return "http://localhost:"+port; }

    @BeforeEach
    public void prepare() {
        keycloakService= new KeycloakService(environment);
        restClientHelper = new RestClientHelper(keycloakService,apiConfigProperties);
        this.keycloakService.initialize();
    }

    //close the mocked web server if it has been initialized
    @AfterEach
    void mockServerCleanup() throws Exception {
        if(this.mockWebServer!=null){
            this.mockWebServer.close();
        }
    }

    private void initMockWebServer() throws IOException {
        this.mockWebServer = new MockWebServer();
        mockWebServer.start(port);
    }

    @Test
    @DisplayName("Test getRestClient method without specifying an API")
    void testGetRestClientWithoutApi() {
        RestClient client = restClientHelper.getRestClient();
        assertThat(client).isNotNull();
    }

    private void testGetRestClientWithIncompleteKCConfig(APIProperties.AuthProperties kcAuth){
        when(apiConfigProperties.getAPIProperties(any())).thenReturn(new APIProperties(getServerHostPort(), kcAuth, true));
        var restClient = restClientHelper.getRestClient(ApiConfigProperties.KNOWN_API.KNOWN_API_ERA);
        assertThat(restClient).isNotNull();
        //Should throw an exception as the realm is missing
        assertThrows(KeycloakTokenConfigUncheckedBPMNError.class , ()  -> restClient.get().uri(getDummyUriWithPort()).retrieve());
    }
    @Test
    @DisplayName("Test getRestClient method without incomplete keycloak configuration")
    void getRestClientWithIncompleteKCConfig() throws IOException {
        //Missing realm
        APIProperties.AuthProperties kcAuth = new APIProperties.AuthProperties(getDummyUriWithPort(),null, "toto","toto");
        //Should throw an exception as the realm is missing
        testGetRestClientWithIncompleteKCConfig(kcAuth);

        //Missing url
        kcAuth = new APIProperties.AuthProperties("null","realm", "toto","toto");
        testGetRestClientWithIncompleteKCConfig(kcAuth);

        //Missing clientId
        kcAuth = new APIProperties.AuthProperties(getDummyUriWithPort(),"realm", null,"toto");
        testGetRestClientWithIncompleteKCConfig(kcAuth);

        //Missing secret
        kcAuth = new APIProperties.AuthProperties(getDummyUriWithPort(),"realm", "toto",null);
        testGetRestClientWithIncompleteKCConfig(kcAuth);
    }

    @Test
    void getRestClient() throws IOException {

        APIProperties.AuthProperties kcAuth = new APIProperties.AuthProperties();
        kcAuth.setClientId("clientId-toto");
        kcAuth.setClientSecret("client-pwd-toto");
        kcAuth.setRealm("realm-toto");
        kcAuth.setUrl(getDummyUriWithPort());


        when(apiConfigProperties.getAPIProperties(any())).thenReturn(new APIProperties(getServerHostPort(), kcAuth, true));
        RestClient restClient = restClientHelper.getRestClient(ApiConfigProperties.KNOWN_API.KNOWN_API_ERA);
        assertThat(restClient).isNotNull();

        KeycloakResponse kcResponse = new KeycloakResponse();
        kcResponse.setAccesToken("MYTOKEN");
        kcResponse.setExpiresIn(5*60*1000);
        MockResponse mockResponseKC = new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setResponseCode(HttpStatus.OK.value())
                .setBody(new ObjectMapper().writeValueAsString(kcResponse));

        MockResponse mockResponse = new MockResponse()
                .setResponseCode(HttpStatus.NO_CONTENT.value())
                .setBody("XXX");

        initMockWebServer();
        mockWebServer.enqueue(mockResponseKC);
        mockWebServer.enqueue(mockResponse);



        assertThat(restClient.get().uri(getDummyUriWithPort()).retrieve().toBodilessEntity().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void getRestClient_withInvalidApiConfig() {
        when(apiConfigProperties.getAPIProperties(any())).thenReturn(null);
        assertThatThrownBy(() -> restClientHelper.getRestClient(any()))
                .isInstanceOf(ApiNotConfiguredBPMNError.class)
                .hasMessageContaining("is not configured in properties");
    }

    @Test
    void getRestClient_withDisabledApiConfig() {
        when(apiConfigProperties.getAPIProperties(any())).thenReturn(new APIProperties("http://localhost:8080", new APIProperties.AuthProperties(), false ));
        assertThatThrownBy(() -> restClientHelper.getRestClient(any()))
                .isInstanceOf(ApiNotConfiguredBPMNError.class)
                .hasMessageContaining("is disabled in properties");
    }

    @Test
    @DisplayName("Test that the retrieval of spring private field still works")
    void extractClientResponseRequestDescriptionPrivateFiledUsingReflexion_shouldWork() throws IOException {
        RestClient restClient = restClientHelper.getRestClient();
        assertThat(restClient).isNotNull();

        //Mock an error response
        MockResponse mockResponse = new MockResponse()
                .setResponseCode(HttpStatus.BAD_REQUEST.value())
                .setBody("XXX");

        initMockWebServer();
        mockWebServer.enqueue(mockResponse);

        //Call method under test
        HttpClient4xxBPMNError exception = assertThrows(HttpClient4xxBPMNError.class , ()  ->restClient.get().uri(getDummyUriWithPort()).retrieve()
                .toBodilessEntity());

        //Post call conditions (we get more or less the expected message with the original request)
        //IF it is not the case, check that the spring private field has not changed or been renamed
        String actualMessage = exception.getMessage();
        assertThat(actualMessage)
                .contains("GET")
                .contains(getDummyUriWithPort())
                .contains(String.valueOf(HttpStatus.BAD_REQUEST.value()));
    }
}