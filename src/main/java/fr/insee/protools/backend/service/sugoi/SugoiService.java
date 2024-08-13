package fr.insee.protools.backend.service.sugoi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.insee.protools.backend.dto.sugoi.User;
import fr.insee.protools.backend.service.exception.UsernameAlreadyExistsSugoiBPMNError;
import fr.insee.protools.backend.httpclients.webclient.WebClientHelper;
import fr.insee.protools.backend.httpclients.exception.runtime.HttpClient4xxBPMNError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import static fr.insee.protools.backend.httpclients.configuration.ApiConfigProperties.KNOWN_API.KNOWN_API_SUGOI;

@Service
@Slf4j
@RequiredArgsConstructor
public class SugoiService {
    //TODO:  a quel niveau configure on ça?

    static final String STORAGE = "default";
    private final WebClientHelper webClientHelper;
    private final ObjectMapper objectMapper;
    @Value("${fr.insee.protools.api.sugoi.dmz-account-creation-realm:questionnaire-particuliers}")
    private String realm;

    public User postCreateUsers(User userBody) {
        log.debug("postCreateUsers");
        try {
            User userCreated = webClientHelper.getWebClient(KNOWN_API_SUGOI)
                    .post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/realms/{realm}/storages/{storage}/users")
                            .build(realm, STORAGE))
                    .bodyValue(userBody)
                    .retrieve()
                    .bodyToMono(User.class)
                    .block();
            log.info("postCreateUsers - response={} ", userCreated);
            return userCreated;
        } catch (HttpClient4xxBPMNError e) {
            if (e.getHttpStatusCodeError().equals(HttpStatus.CONFLICT)) {
                String msg =
                        "Error 409/CONFLICT during SUGOI post create users userBody.username=" + ((userBody == null) ? "null" : userBody.getUsername())
                        + " (check that the username already exists in SUGOI) - msg=" + e.getMessage();
                log.error(msg);
                throw new UsernameAlreadyExistsSugoiBPMNError(msg);
            }
            //Currently no remediation so just rethrow
            throw e;
        }
    }


    public void postInitPassword(String userId, String password) {
        log.debug("postInitPassword - userId={} begin", userId);
        ObjectNode body = objectMapper.createObjectNode();
        body.put("password", password);
        webClientHelper.getWebClient(KNOWN_API_SUGOI)
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/realms/{realm}/users/{id}/init-password")
                        .queryParam("change-password-reset-status", true)
                        .build(realm, userId))
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block();
        log.info("postInitPassword - userId={} end", userId);
    }
}
