package fr.insee.protools.backend.httpclients.restclient;


import fr.insee.protools.backend.httpclients.keycloak.KeycloakService;
import fr.insee.protools.backend.httpclients.configuration.APIProperties;
import fr.insee.protools.backend.httpclients.exception.KeycloakTokenConfigBPMNError;
import fr.insee.protools.backend.httpclients.exception.KeycloakTokenConfigUncheckedBPMNError;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.*;

import java.io.IOException;

/**
 * Class in charge of adding the correct bearer token for API calls
 * It will retrieve a fresh token if needed using our KeycloakService
 */
class RestClientKeycloakHeadersInitializer implements ClientHttpRequestInitializer {

        KeycloakService keycloakService;
        //Configuration of the connexion to the auth server
        private APIProperties.AuthProperties authProperties;

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
