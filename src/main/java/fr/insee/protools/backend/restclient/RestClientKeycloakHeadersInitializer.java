package fr.insee.protools.backend.restclient;


import fr.insee.protools.backend.restclient.configuration.APIProperties;
import fr.insee.protools.backend.restclient.exception.KeycloakTokenConfigBPMNError;
import fr.insee.protools.backend.restclient.exception.KeycloakTokenConfigUncheckedBPMNError;
import fr.insee.protools.backend.restclient.keycloak.KeycloakService;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestInitializer;

/**
 * Class in charge of adding the correct bearer token for API calls
 * It will retrieve a fresh token if needed using our KeycloakService
 */
class RestClientKeycloakHeadersInitializer implements ClientHttpRequestInitializer {

    final KeycloakService keycloakService;
    //Configuration of the connexion to the auth server
    private final APIProperties.AuthProperties authProperties;

    public RestClientKeycloakHeadersInitializer(KeycloakService keycloakService, APIProperties.AuthProperties authProperties) {
        this.keycloakService = keycloakService;
        this.authProperties = authProperties;
    }

    @Override
    public void initialize(ClientHttpRequest request) {
        try {
            request.getHeaders().setBearerAuth(keycloakService.getToken(authProperties));
        } catch (KeycloakTokenConfigBPMNError e) {
            throw new KeycloakTokenConfigUncheckedBPMNError(e);
        }
        request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    }
}
