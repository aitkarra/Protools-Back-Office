package fr.insee.protools.backend.service.sugoi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.dto.sugoi.Habilitation;
import fr.insee.protools.backend.dto.sugoi.User;
import fr.insee.protools.backend.restclient.exception.runtime.HttpClient4xxBPMNError;
import fr.insee.protools.backend.service.exception.UsernameAlreadyExistsSugoiBPMNError;
import fr.insee.protools.backend.service.utils.TestServiceWithRestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.util.UriBuilder;

import java.util.List;

import static fr.insee.protools.backend.restclient.configuration.ApiConfigProperties.KNOWN_API.KNOWN_API_SUGOI;
import static fr.insee.protools.backend.service.sugoi.SugoiService.STORAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SugoiServiceTest extends TestServiceWithRestClient {

    @Spy
    private ObjectMapper objectMapper; // Mock the ObjectMapper


    @InjectMocks
    SugoiService service;

    @Value("${fr.insee.protools.api.sugoi.dmz-account-creation-realm:questionnaire-particuliers}")
    private String realm;

    @Test
    void postCreateUser_should_call_correctURIAndBody() {
        //Prepare
        Habilitation PLATINE_HABILITATION = new Habilitation("platine", "repondant", null);
        User createSugoiUserBody = User.builder().habilitations(List.of(PLATINE_HABILITATION)).build();
        User mockedResponse = User.builder().username("TOTO").habilitations(List.of(PLATINE_HABILITATION)).build();
        mockRetrieveBody(mockedResponse);

        //Call method under tests
        User returnerUser = service.postCreateUser(createSugoiUserBody);

        //Verify
        verify(restClientHelper).getRestClient(KNOWN_API_SUGOI);
        verify(restClient).post();

        UriBuilder mockUriBuilder = mockPostPutPatchURIBuilderAndPrepareForTest();

        // Verify that the correct path and query parameters were used
        verify(mockUriBuilder).path(eq("/realms/{realm}/storages/{storage}/users"));
        verify(mockUriBuilder).build(eq(realm), eq(STORAGE));

        // and with expected body
        verify(requestBodyUriSpec).body(createSugoiUserBody);
    }

    @Test
    void postCreateUsers_shouldCatch() {
        //Prepare
        Habilitation PLATINE_HABILITATION = new Habilitation("platine", "repondant", null);
        User createSugoiUserBody = User.builder().habilitations(List.of(PLATINE_HABILITATION)).build();

        mockMakeRetrieveThrow(HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()));
        //Call method under tests
        assertThrows(HttpClient4xxBPMNError.class, () -> service.postCreateUser(createSugoiUserBody));

        //Call method under tests
        createSugoiUserBody.setUsername(null);
        assertThrows(HttpClient4xxBPMNError.class, () -> service.postCreateUser(createSugoiUserBody));
    }


    @Test
    void postCreateUser_shouldCatch_409Conflict() {
        //Prepare
        Habilitation PLATINE_HABILITATION = new Habilitation("platine", "repondant", null);
        User createSugoiUserBody = User.builder().habilitations(List.of(PLATINE_HABILITATION)).build();


        mockMakeRetrieveThrow(HttpStatusCode.valueOf(HttpStatus.CONFLICT.value()));
        //Call method under tests
        UsernameAlreadyExistsSugoiBPMNError exception = assertThrows(UsernameAlreadyExistsSugoiBPMNError.class, () -> service.postCreateUser(createSugoiUserBody));
        assertThat(exception.getMessage(), containsString("during SUGOI post create users"));


        //Call method under tests
        createSugoiUserBody.setUsername(null);
        UsernameAlreadyExistsSugoiBPMNError exception2 = assertThrows(UsernameAlreadyExistsSugoiBPMNError.class, () -> service.postCreateUser(createSugoiUserBody));
        assertThat(exception.getMessage(), containsString("during SUGOI post create users"));
        assertThat(exception.getMessage(), containsString("null"));


    }

    @Test
    void postInitPassword_should_call_correctURIAndBody() {
        //Prepare
       String userId="GéGé";
       String pwd = "****";

        //Call method under tests
        service.postInitPassword(userId,pwd);

        //Verify
        verify(restClientHelper).getRestClient(KNOWN_API_SUGOI);
        verify(restClient).post();

        UriBuilder mockUriBuilder = mockPostPutPatchURIBuilderAndPrepareForTest();

        // Verify that the correct path and query parameters were used
        verify(mockUriBuilder).path(eq("/realms/{realm}/users/{id}/init-password"));
        verify(mockUriBuilder).build(eq(realm), eq(userId));
        ArgumentCaptor<JsonNode> bodyCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(requestBodyUriSpec).body(bodyCaptor.capture());

        assertEquals(1, bodyCaptor.getAllValues().size(),"We should have exactly one body");
        JsonNode body=bodyCaptor.getValue();
        assertEquals(pwd,body.path("password").asText(),"The passed password is incorrect");
    }
}