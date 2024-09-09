package fr.insee.protools.backend.service.meshuggah;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.insee.protools.backend.ProtoolsTestUtils;
import fr.insee.protools.backend.deprecated.service.meshuggah.MeshuggahSendOpeningMailCommunicationForSUTask;
import fr.insee.protools.backend.deprecated.service.meshuggah.MeshuggahService;
import fr.insee.protools.backend.service.context.exception.BadContextIncorrectBPMNError;
import fr.insee.protools.backend.dto.meshuggah.MeshuggahComDetails;
import fr.insee.protools.backend.service.utils.TestWithContext;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.test.FlowableTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.stream.Stream;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_CURRENT_PARTITION_ID;
import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_PLATINE_CONTACT;
import static fr.insee.protools.backend.service.utils.FlowableVariableUtils.getMissingVariableMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@FlowableTest
class MeshuggahSendOpeningMailCommunicationForSUTaskTest extends TestWithContext {
    static ObjectMapper objectMapper = new ObjectMapper();

    private final String platineContactContent = """
            {
                        "email": "toto@insee.fr",
                        "identifier": "id"
            }
            """;

    private static Stream<Arguments> contextErrorArguments() {
        return Stream.of(
                Arguments.of(MeshuggahCtxExamples.ctx_ERROR_no_part),
                Arguments.of(MeshuggahCtxExamples.ctx_ERROR_no_com),
                Arguments.of(MeshuggahCtxExamples.ctx_ERROR_no_moyen),
                Arguments.of(MeshuggahCtxExamples.ctx_ERROR_typo_phase),
                Arguments.of(MeshuggahCtxExamples.ctx_ERROR_typo_moyen));
    }

    @Test
    void execute_should_throwError_when_null_context(){
        assertThat_delegate_throwError_when_null_context(meshuggahTask);
    }

    @Mock
    MeshuggahService meshuggahService;
    @InjectMocks
    MeshuggahSendOpeningMailCommunicationForSUTask meshuggahTask;

    @Test
    @DisplayName("Test execute method - should throw if VARNAME_CURRENT_PARTITION_ID or VARNAME_PLATINE_CONTACT not initialized")
    void execute_should_throw_FlowableIllegalArgumentException_when_variables_notDefined() throws JsonProcessingException {

        //Precondition
        DelegateExecution execution = mock(DelegateExecution.class);
        lenient().when(execution.getProcessInstanceId()).thenReturn(dumyId);
        initContexteMockWithString(MeshuggahCtxExamples.ctx_OK_envoi_mail_1part_ouverture_mail);

        //Execute the unit under test
        FlowableIllegalArgumentException exception = assertThrows(FlowableIllegalArgumentException.class, () -> meshuggahTask.execute(execution));
        //Post conditions
        assertThat(exception.getMessage()).isEqualTo(getMissingVariableMessage(VARNAME_CURRENT_PARTITION_ID));

        //Create First variable
        when(execution.getVariable(VARNAME_CURRENT_PARTITION_ID, String.class)).thenReturn("99");
        //Execute again
        exception = assertThrows(FlowableIllegalArgumentException.class, () -> meshuggahTask.execute(execution));
        //Check the error
        assertThat(exception.getMessage()).isEqualTo(getMissingVariableMessage(VARNAME_PLATINE_CONTACT));

        //Create 2nd variable
        when(execution.getVariable(VARNAME_PLATINE_CONTACT, JsonNode.class)).thenReturn(objectMapper.readTree(platineContactContent));
        //Execute again
        assertDoesNotThrow(() -> meshuggahTask.execute(execution));
    }

    @ParameterizedTest
    @MethodSource("contextErrorArguments")
    @DisplayName("Test execute method - should throw if Context is not correct")
    void execute_should_throw_BadContext_when_contextIncorrect(String context_json) throws JsonProcessingException {
        //Precondition
        DelegateExecution execution = mock(DelegateExecution.class);
        lenient().when(execution.getProcessInstanceId()).thenReturn(dumyId);
        //Variables
        lenient().when(execution.getVariable(VARNAME_CURRENT_PARTITION_ID, Long.class)).thenReturn(MeshuggahCtxExamples.ctx_partition1);
        lenient().when(execution.getVariable(VARNAME_PLATINE_CONTACT, JsonNode.class)).thenReturn(objectMapper.readTree(platineContactContent));
        //Ctx
        ProtoolsTestUtils.initContexteMockFromString(protoolsContext, context_json);


        //Run test
        assertThrows(BadContextIncorrectBPMNError.class, () -> meshuggahTask.execute(execution));
        Mockito.reset(protoolsContext);
    }

    @Test
    @DisplayName("Test execute method - should work and make correct call to service")
    void execute_should_work_when_variables_OK() throws JsonProcessingException {
        //Precondition
        DelegateExecution execution = mock(DelegateExecution.class);
        lenient().when(execution.getProcessInstanceId()).thenReturn(dumyId);
        initContexteMockWithString(MeshuggahCtxExamples.ctx_OK_envoi_mail_1part_ouverture_mail);

        when(execution.getVariable(VARNAME_CURRENT_PARTITION_ID, String.class)).thenReturn(MeshuggahCtxExamples.ctx_partition1.toString());
        JsonNode platineContact = objectMapper.readTree(platineContactContent);
        when(execution.getVariable(VARNAME_PLATINE_CONTACT, JsonNode.class)).thenReturn(platineContact);
        //Run method under test
        assertDoesNotThrow(() -> meshuggahTask.execute(execution));

        //Post conditions
        MeshuggahComDetails expectedDetails = MeshuggahComDetails.builder()
                .campaignId("AAC2023A00")
                .phase("OUVERTURE")
                .medium("EMAIL")
                .mode("WEB")
                .partitioningId(MeshuggahCtxExamples.ctx_partition1.toString())
                .avecQuestionnaire(false)
                .protocol("DEFAULT")
                .operation("RELANCE_LIBRE")
                .build();

        ObjectNode expectedBody = objectMapper.createObjectNode();
        expectedBody.put("email", platineContact.path("email"));
        expectedBody.put("Ue_CalcIdentifiant", platineContact.path("identifier"));


        ArgumentCaptor<MeshuggahComDetails> acDetails = ArgumentCaptor.forClass(MeshuggahComDetails.class);
        ArgumentCaptor<JsonNode> acBody = ArgumentCaptor.forClass(JsonNode.class);
        verify(meshuggahService,times(1)).postSendCommunication(acDetails.capture(),acBody.capture());
        MeshuggahComDetails details = acDetails.getValue();
        assertEquals(expectedDetails,details);

        JsonNode resultBody = acBody.getValue();
        assertEquals(expectedBody,resultBody);

    }
}