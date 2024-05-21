package fr.insee.protools.backend.service.sugoi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.dto.sugoi.User;
import fr.insee.protools.backend.service.utils.password.PasswordService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.test.FlowableTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_DIRECTORYACCESS_ID_CONTACT;
import static fr.insee.protools.backend.service.sugoi.SugoiCreateUserTask.PLATINE_HABILITATION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FlowableTest
class SugoiCreateUserTaskTest {

    @Mock SugoiService sugoiService;
    @Mock PasswordService passwordService;
    @InjectMocks SugoiCreateUserTask task;

    protected final String dumyId="ID1";

    @Test
    void execute_should_work() throws JsonProcessingException {
        //Prepare
        DelegateExecution execution = mock(DelegateExecution.class);
        doReturn(dumyId).when(execution).getProcessInstanceId();
        String expectedPwd="veryComplicatedPassword";
        doReturn(expectedPwd).when(passwordService).generatePassword();
        final String userId="D96QSST";
        final String sugoiResponse =
        """
                {
                  "username": "D96QSST",
                  "groups": [],
                  "habilitations": [
                    {
                      "id": "repondant_platine",
                      "application": "platine",
                      "role": "repondant",
                      "property": null
                    }
                  ],
                  "metadatas": {
                    "userStorage": "default",
                    "realm": "questionnaire-particuliers",
                    "modifyTimestamp": "20231106141045Z"
                  },
                  "attributes": {
                    "hasPassword": false
                  }
                }
        """;
        doReturn(
                new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .readValue(sugoiResponse,User.class))
                .when(sugoiService).postCreateUsers(any());

        //Execute
        assertDoesNotThrow(() -> task.execute(execution));

        //post conditions
        verify(execution).setVariableLocal(VARNAME_DIRECTORYACCESS_ID_CONTACT, userId);
        //verif on user creation
        ArgumentCaptor<User> acUserDto = ArgumentCaptor.forClass(User.class);
        verify(sugoiService).postCreateUsers(acUserDto.capture());
        assertEquals(1, acUserDto.getAllValues().size(),"We should have exactly one call to postCreateUsers");
        User userParam = acUserDto.getValue();
        assertEquals(1, userParam.getHabilitations().size(),"We should have exactly one habilitation");
        assertTrue(userParam.getHabilitations().contains(PLATINE_HABILITATION),"Platine habilitiation not found");
        assertEquals("repondant_platine",userParam.getHabilitations().get(0).getId(),"Platine habilitation should be repondant_platine");

        //verif on password
        verify(sugoiService).postInitPassword(userId,expectedPwd);
    }
}