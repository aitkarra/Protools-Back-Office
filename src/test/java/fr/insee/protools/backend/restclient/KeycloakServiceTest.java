package fr.insee.protools.backend.restclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.restclient.configuration.APIProperties;
import fr.insee.protools.backend.restclient.exception.KeycloakTokenConfigBPMNError;
import fr.insee.protools.backend.restclient.keycloak.KeycloakResponse;
import fr.insee.protools.backend.restclient.keycloak.KeycloakService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class KeycloakServiceTest {

    @Spy
    private Environment environment;

    @InjectMocks
    private KeycloakService keycloakService;

    private MockWebServer mockWebServer;
    private static final int port = 80;
    private static String getDummyUriWithPort() { return getServerHostPort()+"/toto/test"; }
    private static String getServerHostPort() { return "http://localhost:"+port; }

    @BeforeEach
    public void prepare() {
        this.keycloakService.initialize();
    }

    //close the mocked web server if it has been initialized
    @AfterEach
    void mockServerCleanup() throws IOException {
        if(this.mockWebServer!=null){
            this.mockWebServer.close();
        }
    }

    private void initMockWebServer() throws IOException {
        this.mockWebServer = new MockWebServer();
        mockWebServer.start(port);
    }

    @Test
    void getToken_should_throw_when_authIncorrect(){
        //Missing realm
        APIProperties.AuthProperties kcAuthNullRealm = new APIProperties.AuthProperties(getDummyUriWithPort(),null, "toto","toto");
        //Should throw an exception as the realm is missing
        assertThrows(KeycloakTokenConfigBPMNError.class , ()  -> keycloakService.getToken(kcAuthNullRealm));

        //Blank realm
        APIProperties.AuthProperties kcAuthBlankRealm = new APIProperties.AuthProperties(getDummyUriWithPort(),"  ", "toto","toto");
        assertThrows(KeycloakTokenConfigBPMNError.class , ()  -> keycloakService.getToken(kcAuthBlankRealm));

        //Missing url
        APIProperties.AuthProperties kcAuthNullUrl = new APIProperties.AuthProperties(null,"realm", "toto","toto");
        assertThrows(KeycloakTokenConfigBPMNError.class , ()  -> keycloakService.getToken(kcAuthNullUrl));

        //Blank url
        APIProperties.AuthProperties kcAuthBlankUrl = new APIProperties.AuthProperties("","realm", "toto","toto");
        assertThrows(KeycloakTokenConfigBPMNError.class , ()  -> keycloakService.getToken(kcAuthBlankUrl));

        //Incorrect Url
        APIProperties.AuthProperties kcAuthWrongUrl = new APIProperties.AuthProperties("UrlError:::-*","realm", "toto","toto");
        assertThrows(KeycloakTokenConfigBPMNError.class , ()  -> keycloakService.getToken(kcAuthWrongUrl));

        //Missing clientId
        APIProperties.AuthProperties kcAuthNullClientId = new APIProperties.AuthProperties(getDummyUriWithPort(),"realm", null,"toto");
        assertThrows(KeycloakTokenConfigBPMNError.class , ()  -> keycloakService.getToken(kcAuthNullClientId));

        //Blank ClientId
        APIProperties.AuthProperties kcAuthBlanklClientId = new APIProperties.AuthProperties(getDummyUriWithPort(),"realm", "                     ","toto");
        assertThrows(KeycloakTokenConfigBPMNError.class , ()  -> keycloakService.getToken(kcAuthBlanklClientId));


        //Missing secret
        APIProperties.AuthProperties kcAuthNullSecret = new APIProperties.AuthProperties(getDummyUriWithPort(),"realm", "toto",null);
        assertThrows(KeycloakTokenConfigBPMNError.class , ()  -> keycloakService.getToken(kcAuthNullSecret));

        //Blank secret
        APIProperties.AuthProperties kcAuthBlankSecret = new APIProperties.AuthProperties(getDummyUriWithPort(),"realm", "toto","");
        assertThrows(KeycloakTokenConfigBPMNError.class , ()  -> keycloakService.getToken(kcAuthBlankSecret));
    }


    @Test
    @DisplayName("getToken should check if a token already exists for this AuthProperties and reuse the known token if it is not exipred")
    void getToken_should_refreshTokenWhenNeeded() throws IOException, KeycloakTokenConfigBPMNError, InterruptedException {
        APIProperties.AuthProperties kcAuth = new APIProperties.AuthProperties(getDummyUriWithPort(),"realm", "toto","toto");

        //Prepare 2 KC responses
        KeycloakResponse kcResponse1 = new KeycloakResponse("MYTOKEN-NB1",300 /*5 minutes*/);
        MockResponse mockResponseKC1 = new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setResponseCode(HttpStatus.OK.value())
                .setBody(new ObjectMapper().writeValueAsString(kcResponse1));

        KeycloakResponse kcResponse2 = new KeycloakResponse("MYTOKEN-NB2",300 /*5 minutes*/);
        MockResponse mockResponseKC2 = new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setResponseCode(HttpStatus.OK.value())
                .setBody(new ObjectMapper().writeValueAsString(kcResponse2));

        initMockWebServer();
        mockWebServer.enqueue(mockResponseKC1);
        mockWebServer.enqueue(mockResponseKC2);

        //Test getToken
        //We mock the Instant.now() method interally used by KeycloakService
        Instant instant = Instant.now();
        Instant instant1 = instant.plus(1,ChronoUnit.MINUTES);
        Instant instant2 = instant.plus(2,ChronoUnit.MINUTES);
        Instant instant3 = instant.plus(3,ChronoUnit.MINUTES);
        Instant instant4 = instant.plus(4,ChronoUnit.MINUTES);
        Instant instant5 = instant.plus(5,ChronoUnit.MINUTES);
        try (MockedStatic<Instant> mockedStatic = mockStatic(Instant.class)) {
            mockedStatic.when(Instant::now)
                    .thenReturn(instant);
            assertEquals(kcResponse1.getAccesToken(), keycloakService.getToken(kcAuth));
        }

        //In 1-2-3-4 minute the token is not expired and we should get the same token
        try (MockedStatic<Instant> mockedStatic = mockStatic(Instant.class)) {
            mockedStatic.when(Instant::now)
                    .thenReturn(instant1);
            assertEquals(kcResponse1.getAccesToken(), keycloakService.getToken(kcAuth));
            mockedStatic.when(Instant::now)
                    .thenReturn(instant2);
            assertEquals(kcResponse1.getAccesToken(), keycloakService.getToken(kcAuth));
            mockedStatic.when(Instant::now)
                    .thenReturn(instant3);
            assertEquals(kcResponse1.getAccesToken(), keycloakService.getToken(kcAuth));
            mockedStatic.when(Instant::now)
                    .thenReturn(instant4);
            assertEquals(kcResponse1.getAccesToken(), keycloakService.getToken(kcAuth));
        }

        //In 5 minutes the token is expired, we should get a refreshed one
        try (MockedStatic<Instant> mockedStatic = mockStatic(Instant.class)) {
            mockedStatic.when(Instant::now)
                    .thenReturn(instant5);
            assertEquals(kcResponse2.getAccesToken(), keycloakService.getToken(kcAuth));
        }
    }
}
