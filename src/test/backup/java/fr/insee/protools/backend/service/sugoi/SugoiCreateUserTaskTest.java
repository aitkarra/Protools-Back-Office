package fr.insee.protools.backend.service.sugoi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.ProtoolsTestUtils;
import fr.insee.protools.backend.dto.sugoi.User;
import fr.insee.protools.backend.service.context.exception.BadContextIncorrectBPMNError;
import fr.insee.protools.backend.service.utils.TestWithContext;
import fr.insee.protools.backend.service.utils.password.PasswordService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.test.FlowableTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_DIRECTORYACCESS_ID_CONTACT;
import static fr.insee.protools.backend.service.sugoi.SugoiCreateUserTask.PLATINE_HABILITATION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FlowableTest
class SugoiCreateUserTaskTest extends TestWithContext {

    @Mock SugoiService sugoiService;
    @Mock PasswordService passwordService;
    @InjectMocks SugoiCreateUserTask task;

    protected final String dumyId="ID1";

    @Test
    void execute_should_work() throws JsonProcessingException {
        //Prepare
        DelegateExecution execution = mock(DelegateExecution.class);
        doReturn(dumyId).when(execution).getProcessInstanceId();

        final String context =
                "{\"contexte\": \"household\" } }";
        ProtoolsTestUtils.initContexteMockFromString(protoolsContext, context);

        String expectedPwd="veryComplicatedPassword";
        doReturn(expectedPwd).when(passwordService).generatePassword(anyInt());
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

        //verif on password
        verify(sugoiService).postInitPassword(userId,expectedPwd);
    }

    @Test
    @DisplayName("Test execute method - should throw if Context is not correct")
    void execute_should_throw_BadContext_when_contextIncorrect() throws JsonProcessingException {
        //Precondition
        DelegateExecution execution = mock(DelegateExecution.class);
        lenient().when(execution.getProcessInstanceId()).thenReturn(dumyId);

        //Erreur
        final String contextKO1 =
                "{ \"partitions\": [{ \"id\": 1  }]   }";
        final String contextKO2 =
                "{\"contexte\": \"xxxx\" } }";

        List<String> contextErrorTestList=List.of(contextKO1,contextKO2);
        for(String context : contextErrorTestList){
            //Precondition
            ProtoolsTestUtils.initContexteMockFromString(protoolsContext, context);
            //Run test
            assertThrows(BadContextIncorrectBPMNError.class, () -> task.execute(execution));
            Mockito.reset(protoolsContext);
        }
    }
    @Test
    @DisplayName("Test getPasswordSize method - should return 8 for household")
    void getPasswordSize_should_return_8_for_household() throws JsonProcessingException {

        final String context =
                "{\"contexte\": \"household\" } }";
        ProtoolsTestUtils.initContexteMockFromString(protoolsContext, context);
        assertEquals(8,task.getPasswordSize(protoolsContext.getContextByProcessInstance(dumyId)));
    }

    @Test
    @DisplayName("Test getPasswordSize method - should return 12 for non household")
    void getPasswordSize_should_return_12_for_NonHousehold() throws JsonProcessingException {

        final String context =
                "{\"contexte\": \"business\" } }";
        ProtoolsTestUtils.initContexteMockFromString(protoolsContext, context);
        assertEquals(12,task.getPasswordSize(protoolsContext.getContextByProcessInstance(dumyId)));
    }
}