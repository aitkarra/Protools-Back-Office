package fr.insee.protools.backend.httpclients.webclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.ProtoolsTestUtils;
import fr.insee.protools.backend.httpclients.keycloak.KeycloakResponse;
import fr.insee.protools.backend.httpclients.keycloak.KeycloakService;
import fr.insee.protools.backend.httpclients.configuration.APIProperties;
import fr.insee.protools.backend.httpclients.configuration.ApiConfigProperties;
import fr.insee.protools.backend.httpclients.exception.ApiNotConfiguredBPMNError;
import fr.insee.protools.backend.httpclients.exception.KeycloakTokenConfigUncheckedBPMNError;
import fr.insee.protools.backend.httpclients.exception.runtime.HttpClient4xxBPMNError;
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
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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
class WebClientHelperTest {

    @Spy
    private Environment environment;

    private KeycloakService keycloakService;

    @Mock
    private ApiConfigProperties apiConfigProperties;

    private WebClientHelper webClientHelper;


    private MockWebServer mockWebServer;
    private static final int port = 80;
    private static String getDummyUriWithPort() { return getServerHostPort()+"/api/test"; }
    private static String getServerHostPort() { return "http://localhost:"+port; }

    @BeforeEach
    public void prepare() {
        keycloakService= new KeycloakService(environment);
        webClientHelper = new WebClientHelper(keycloakService,apiConfigProperties);
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


    private File createDummyFile(int sizeInByte, String extension) throws IOException {
        File file = File.createTempFile("tempFile", ".json");
        file.deleteOnExit();
        RandomAccessFile rafile;
        rafile = new RandomAccessFile(file, "rw");
        //In Bytes ==> 1024 = 1Ko ==> 1024X1024 : 1Mo
        rafile.setLength(sizeInByte);
        return file;
    }

    private MockResponse fileToResponse(String contentType, File file) throws IOException {
        return new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .setBody(ProtoolsTestUtils.fileToBytes(file))
                .addHeader("content-type: " + contentType);
    }

    @Test
    @DisplayName("Test getWebClient method without specifying an API")
    void testGetWebClientWithoutApi() {
        WebClient client = webClientHelper.getWebClient();
        assertThat(client).isNotNull();
    }

    private void testGetWebClientWithIncompleteKCConfig(APIProperties.AuthProperties kcAuth){
        when(apiConfigProperties.getAPIProperties(any())).thenReturn(new APIProperties(getServerHostPort(), kcAuth, true));
        var webClient = webClientHelper.getWebClient(ApiConfigProperties.KNOWN_API.KNOWN_API_ERA);
        assertThat(webClient).isNotNull();
        //Should throw an exception as the realm is missing
        assertThrows(KeycloakTokenConfigUncheckedBPMNError.class , ()  -> webClient.get().uri(getDummyUriWithPort()).exchange().block());
    }
    @Test
    @DisplayName("Test getWebClient method without incomplete keycloak configuration")
    void getWebClientWithIncompleteKCConfig() throws IOException {
        //Missing realm
        APIProperties.AuthProperties kcAuth = new APIProperties.AuthProperties(getDummyUriWithPort(),null, "toto","toto");
        //Should throw an exception as the realm is missing
        testGetWebClientWithIncompleteKCConfig(kcAuth);

        //Missing url
        kcAuth = new APIProperties.AuthProperties("null","realm", "toto","toto");
        testGetWebClientWithIncompleteKCConfig(kcAuth);

        //Missing clientId
        kcAuth = new APIProperties.AuthProperties(getDummyUriWithPort(),"realm", null,"toto");
        testGetWebClientWithIncompleteKCConfig(kcAuth);

        //Missing secret
        kcAuth = new APIProperties.AuthProperties(getDummyUriWithPort(),"realm", "toto",null);
        testGetWebClientWithIncompleteKCConfig(kcAuth);
    }

    @Test
    void getWebClient() throws IOException {

        APIProperties.AuthProperties kcAuth = new APIProperties.AuthProperties();
        kcAuth.setClientId("clientId-toto");
        kcAuth.setClientSecret("client-pwd-toto");
        kcAuth.setRealm("realm-toto");
        kcAuth.setUrl(getDummyUriWithPort());


        when(apiConfigProperties.getAPIProperties(any())).thenReturn(new APIProperties(getServerHostPort(), kcAuth, true));
        WebClient webClient = webClientHelper.getWebClient(ApiConfigProperties.KNOWN_API.KNOWN_API_ERA);
        assertThat(webClient).isNotNull();

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

        assertThat(webClient.get().uri(getDummyUriWithPort()).exchange().block().statusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("Test getWebClientForFile method - get the webclient and download files of different sizes")
    void getWebClientForFile() throws IOException {
        WebClient webClient = webClientHelper.getWebClientForFile();
        assertThat(webClient).isNotNull();

        //Create a 1Mo File
        File file1mo = createDummyFile(1024 * 1024 * 1,".json");
        File file19mo = createDummyFile(WebClientHelper.getDefaultFileBufferSize()-1024,".json");
        int tooBigSize=WebClientHelper.getDefaultFileBufferSize()+1024;
        File fileTooBig = createDummyFile(tooBigSize,".json");

        MockResponse fileToResponse_1Mo = fileToResponse(MediaType.APPLICATION_JSON_VALUE, file1mo);
        MockResponse fileToResponse_19Mo = fileToResponse(MediaType.APPLICATION_JSON_VALUE, file19mo);
        MockResponse fileToResponseTooBig = fileToResponse(MediaType.APPLICATION_JSON_VALUE, fileTooBig);


/*

      KeycloakResponse kcResponse = new KeycloakResponse();
        kcResponse.setAccesToken("MYTOKEN");
        kcResponse.setExpiresIn(5*60*1000);


        MockResponse mockResponseKC = new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setResponseCode(HttpStatus.OK.value())
                .setBody(new ObjectMapper().writeValueAsString(kcResponse));

        final Dispatcher dispatcher = new Dispatcher() {
            public MockResponse dispatch(RecordedRequest request) {
                switch (request.getPath()) {
                    case "/users/1":
                        return new MockResponse().setResponseCode(200);
                    case "/users/2":
                        return new MockResponse().setResponseCode(500);
                    case "/users/3":
                        return new MockResponse().setResponseCode(200).setBody("{\"id\": 1, \"name\":\"duke\"}");
                }
                return new MockResponse().setResponseCode(404);
            }

                    mockWebServer.enqueue(mockResponseKC);

        };*/

        initMockWebServer();
        mockWebServer.enqueue(fileToResponse_1Mo);
        mockWebServer.enqueue(fileToResponse_19Mo);
        mockWebServer.enqueue(fileToResponseTooBig);
        mockWebServer.enqueue(fileToResponseTooBig);

        //Check for 1Mo file
        assertDoesNotThrow(()->webClientHelper.getWebClientForFile()
                .get()
                .uri(getDummyUriWithPort())
                .retrieve()
                .bodyToMono(String.class)
                .block());

        //Check for 19Mo file
        assertDoesNotThrow(()->webClientHelper.getWebClientForFile()
                .get()
                .uri(getDummyUriWithPort())
                .retrieve()
                .bodyToMono(String.class)
                .block());

        //Check for a too big file : Should throw and exception as default buffer is 20Mo
        assertThrows(WebClientResponseException.class, () -> webClientHelper.getWebClientForFile()
                    .get()
                    .uri(getDummyUriWithPort())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block());

        //Check for a too big file with a custom buffer ==> Should be ok
        assertDoesNotThrow(() -> webClientHelper.getWebClientForFile(tooBigSize)
                .get()
                .uri(getDummyUriWithPort())
                .retrieve()
                .bodyToMono(String.class)
                .block());
    }


    @Test
    void getWebClient_withInvalidApiConfig() {
        when(apiConfigProperties.getAPIProperties(any())).thenReturn(null);
        assertThatThrownBy(() -> webClientHelper.getWebClient(any()))
                .isInstanceOf(ApiNotConfiguredBPMNError.class)
                .hasMessageContaining("is not configured in properties");
    }

    @Test
    void getWebClient_withDisabledApiConfig() {
        when(apiConfigProperties.getAPIProperties(any())).thenReturn(new APIProperties("http://localhost:8080", new APIProperties.AuthProperties(), false ));
        assertThatThrownBy(() -> webClientHelper.getWebClient(any()))
                .isInstanceOf(ApiNotConfiguredBPMNError.class)
                .hasMessageContaining("is disabled in properties");
    }

    @Test
    @DisplayName("Test that the retrieval of spring private field still works")
    void extractClientResponseRequestDescriptionPrivateFiledUsingReflexion_shouldWork() throws IOException {
        WebClient webClient = webClientHelper.getWebClient();
        assertThat(webClient).isNotNull();

        //Mock an error response
        MockResponse mockResponse = new MockResponse()
                .setResponseCode(HttpStatus.BAD_REQUEST.value())
                .setBody("XXX");

        initMockWebServer();
        mockWebServer.enqueue(mockResponse);

        //Call method under test
        HttpClient4xxBPMNError exception = assertThrows(HttpClient4xxBPMNError.class , ()  ->webClient.get().uri(getDummyUriWithPort()).retrieve()
                .bodyToMono(String.class)
                .block());

        //Post call conditions (we get more or less the expected message with the original request)
        //IF it is not the case, check that the spring private field has not changed or been renamed
        String actualMessage = exception.getMessage();
        assertThat(actualMessage)
                .contains("GET")
                .contains(getDummyUriWithPort())
                .contains(String.valueOf(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    @DisplayName("Test that containsCauseOfType find an existing cause")
    void containsCauseOfType_shouldFindCauseIfExists() {
        //Prepare
        String rootMessage="TEST";
        ArithmeticException exRoot=new ArithmeticException(rootMessage);
        Exception exLvl1=new Exception("dummy",exRoot );
        Exception ex = new Exception("dummy",exLvl1 );

        //Call
        boolean found = WebClientHelper.containsCauseOfType(ex, List.of(ArithmeticException.class));
        //Check
        assertTrue(found,"ArithmeticException should be found");

        //Call
        found = WebClientHelper.containsCauseOfType(exLvl1, List.of(ArithmeticException.class));
        //Check
        assertTrue(found,"ArithmeticException should be found");

        //Call
        found = WebClientHelper.containsCauseOfType(exRoot, List.of(ArithmeticException.class));
        //Check
        assertTrue(found,"ArithmeticException should be found");

        //Call
        found = WebClientHelper.containsCauseOfType(exRoot, List.of(RuntimeException.class));
        //Check
        assertTrue(found,"RuntimeException should be found");

        //Call
        found = WebClientHelper.containsCauseOfType(exRoot, List.of(IOException.class,RuntimeException.class));
        //Check
        assertTrue(found,"RuntimeException should be found");

        //Call
        found = WebClientHelper.containsCauseOfType(exRoot, List.of(IOException.class));
        //Check (should not be found)
        assertFalse(found,"IOException should not be found");
    }

}