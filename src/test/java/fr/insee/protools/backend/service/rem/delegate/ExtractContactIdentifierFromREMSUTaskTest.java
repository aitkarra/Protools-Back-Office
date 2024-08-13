package fr.insee.protools.backend.service.rem.delegate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.service.exception.IncorrectSUBPMNError;
import fr.insee.protools.backend.service.utils.TestWithContext;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.test.FlowableTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_DIRECTORYACCESS_ID_CONTACT;
import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_REM_INTERROGATION;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FlowableTest
class ExtractContactIdentifierFromREMSUTaskTest extends TestWithContext {


    @Test
    @DisplayName("ExtractContactIdentifierFromREMSUTask should throw an exception when the VARNAME_REM_SURVEY_UNIT variable is missing")
    void execute_should_throw_when_variable_missing() {
        //Prepare
        DelegateExecution execution = createMockedExecution();
        ExtractContactIdentifierFromREMSUTask extractTask = new ExtractContactIdentifierFromREMSUTask();

        //No variable set ==> Error
        assertThrows(FlowableIllegalArgumentException.class, () -> extractTask.execute(execution));

        //Wrong Type ==> Error
        lenient().doReturn(Boolean.FALSE).when(execution).getVariable(VARNAME_REM_INTERROGATION, Boolean.class);
        assertThrows(FlowableIllegalArgumentException.class, () -> extractTask.execute(execution));

    }

    @Test
    void execute_should_thow_when_variable_OKand_UEJson_NOT_OK() throws JsonProcessingException {
        //Prepare
        DelegateExecution execution = mock(DelegateExecution.class);
        doReturn(dumyId).when(execution).getProcessInstanceId();
        ExtractContactIdentifierFromREMSUTask extractTask = new ExtractContactIdentifierFromREMSUTask();
        ObjectMapper objectMapper = new ObjectMapper();


        //JSon content with missing additionalInformations node
        String content =
                """
                                {
                                            "otherIdentifier": null,
                                            "missing_tag": [ ]
                                }
                        """;
        doReturn(objectMapper.readTree(content)).when(execution).getVariable(VARNAME_REM_INTERROGATION, JsonNode.class);
        //Execute
        assertThrows(IncorrectSUBPMNError.class, () -> extractTask.execute(execution));

        //JSon content with missing identifiantCompte key
        content =
                """
                                {
                                "otherIdentifier": null,
                                "additionalInformations": [
                                  {
                                    "key": "xxxx",
                                    "value": "xxxxxxxxxx"
                                  },
                                  {
                                    "key": "yyyyyy",
                                    "value": "yyyyyyyyyyyyyyyy"
                                  } ]
                                }
                        """;
        doReturn(objectMapper.readTree(content)).when(execution).getVariable(VARNAME_REM_INTERROGATION, JsonNode.class);
        //Execute
        assertThrows(IncorrectSUBPMNError.class, () -> extractTask.execute(execution));
    }

    @Test
    void execute_should_work_when_variable_and_UEJson_OK() throws JsonProcessingException {
        //Prepare
        DelegateExecution execution = mock(DelegateExecution.class);
        doReturn(dumyId).when(execution).getProcessInstanceId();

        ExtractContactIdentifierFromREMSUTask extractTask = new ExtractContactIdentifierFromREMSUTask();
        ObjectMapper objectMapper = new ObjectMapper();
        final String content =
                """
                        {
                                    "otherIdentifier": null,
                                    "additionalInformations": [
                                      {
                                        "key": "xxxx",
                                        "value": "xxxxxxxxxx"
                                      },
                                      {
                                        "key": "identifiantCompte",
                                        "value": "TOTO_55"
                                      },
                                      {
                                        "key": "yyyyyy",
                                        "value": "yyyyyyyyyyyyyyyy"
                                      } ]
                        }
                        """;
        doReturn(objectMapper.readTree(content)).when(execution).getVariable(VARNAME_REM_INTERROGATION, JsonNode.class);

        //Execute
        assertDoesNotThrow(() -> extractTask.execute(execution));

        verify(execution).setVariableLocal(VARNAME_DIRECTORYACCESS_ID_CONTACT, "TOTO_55");
    }
}