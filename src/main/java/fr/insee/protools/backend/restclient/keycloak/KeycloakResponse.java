package fr.insee.protools.backend.restclient.keycloak;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class KeycloakResponse {
    @JsonProperty("access_token")
    private String accesToken;

    @JsonProperty("expires_in")
    private Integer expiresIn;

    public KeycloakResponse() {
    }

    public KeycloakResponse(String accesToken, Integer expiresIn) {
        this.accesToken = accesToken;
        this.expiresIn = expiresIn;
    }
}
