package fr.insee.protools.backend.service.sabiane.delegate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.ProtoolsTestUtils;
import fr.insee.protools.backend.dto.platine_sabiane_questionnaire.surveyunit.SurveyUnitResponseDto;
import fr.insee.protools.backend.service.context.exception.BadContextIncorrectBPMNError;
import fr.insee.protools.backend.service.sabiane.SabianeIdHelper;
import fr.insee.protools.backend.service.sabiane.questionnaire.SabianeQuestionnaireService;
import fr.insee.protools.backend.service.utils.TestWithContext;
import fr.insee.protools.backend.service.utils.data.CtxExamples;
import fr.insee.protools.backend.service.utils.data.RemSUData;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Stream;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_CURRENT_PARTITION_ID;
import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_REM_INTERROGATION;
import static fr.insee.protools.backend.service.utils.FlowableVariableUtils.getMissingVariableMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class SabianeQuestionnaireCreateSUTaskTest extends TestWithContext {


    static final String minimal_ctx_ok =
        """   
            {    "id": "TEST_ID" , "partitions": [{ "id":99,  "questionnaireModel": "ID_1"}] , "questionnaireModels": [{"id": "ID_1"}]}
        """;

    @Mock SabianeQuestionnaireService sabianeQuestionnaireService;

    @InjectMocks
    SabianeQuestionnaireCreateSUTask sabianePilotageTask;

    private static Stream<Arguments> contextErrorArguments() {
        return Stream.of(
                Arguments.of(CtxExamples.ctx_no_part),
                Arguments.of(CtxExamples.ctx_idCampagne_questionnaireModels_idPartition),
                Arguments.of(CtxExamples.ctx_questionnaireModels_no_part),
                Arguments.of(CtxExamples.ctx_idCampagne_questionnaireModels_1emptyPartition),
                Arguments.of(CtxExamples.ctx_idCampagne_emptyQuestionnaireModels_idPartition)
        );
    }

    static Stream<Arguments> initExecuteParameters() {
        return Stream.of(
                Arguments.of(
                        SabianeCtxExamples.ctx_ok_idCampagne_idQuestionnaireModel_idPartition_questionnaireModelPartition,
                        RemSUData.rem_su_1personne,
                        Boolean.TRUE)
        );
    }

    @Test
    void execute_should_throwError_when_null_context(){
        assertThat_delegate_throwError_when_null_context(sabianePilotageTask);
    }

    @Test
    @DisplayName("Test execute method - should throw if VARNAME_CURRENT_PARTITION_ID or VARNAME_REM_SURVEY_UNIT not initialized")
    void execute_should_throw_FlowableIllegalArgumentException_when_variables_notDefined() {
        //Precondition
        DelegateExecution execution = createMockedExecution();
        initContexteMockWithString(minimal_ctx_ok);

        //Execute the unit under test
        FlowableIllegalArgumentException exception = assertThrows(FlowableIllegalArgumentException.class, () -> sabianePilotageTask.execute(execution));
        //Post conditions
        assertThat(exception.getMessage()).isEqualTo(getMissingVariableMessage(VARNAME_CURRENT_PARTITION_ID));

        //Create First variable
        when(execution.getVariable(VARNAME_CURRENT_PARTITION_ID, Long.class)).thenReturn(99L);
        //Execute again
        exception = assertThrows(FlowableIllegalArgumentException.class, () -> sabianePilotageTask.execute(execution));
        //Check the error
        assertThat(exception.getMessage()).isEqualTo(getMissingVariableMessage(VARNAME_REM_INTERROGATION));

        //Create 2nd variable
        when(execution.getVariable(VARNAME_REM_INTERROGATION, JsonNode.class)).thenReturn(ProtoolsTestUtils.asJsonNode(RemSUData.rem_su_1personne));
        //Execute again
        assertDoesNotThrow(() -> sabianePilotageTask.execute(execution));
    }

    @Test
    void execute_should_throw_BadContextIncorrectException_when_noContext() {
        DelegateExecution execution = createMockedExecution();
        //Execute the unit under test
        assertThrows(BadContextIncorrectBPMNError.class, () -> sabianePilotageTask.execute(execution));
    }

    @ParameterizedTest
    @MethodSource("contextErrorArguments")
    @DisplayName("Test execute method - should throw if Context is not correct")
    void execute_should_throw_BadContext_when_contextIncorrect(String context_json) throws JsonProcessingException {
        //Precondition
        DelegateExecution execution = createMockedExecution();
        //Variables
        lenient().when(execution.getVariable(VARNAME_CURRENT_PARTITION_ID, Long.class)).thenReturn(CtxExamples.ctx_partition1);
        lenient().when(execution.getVariable(VARNAME_REM_INTERROGATION, JsonNode.class)).thenReturn(ProtoolsTestUtils.asJsonNode(RemSUData.rem_su_1personne));
        //Ctx
        ProtoolsTestUtils.initContexteMockFromString(protoolsContext, context_json);

        //Run test
        assertThrows(BadContextIncorrectBPMNError.class, () -> sabianePilotageTask.execute(execution));
        Mockito.reset(protoolsContext);
    }

    @ParameterizedTest
    @MethodSource("initExecuteParameters")
    @DisplayName("Test execute method - should work and make correct call to service when context has one partition")
    void execute_should_work_when_ctx_1part_logement(String inputCtx, String inputRemSU) {
        //Precondition
        DelegateExecution execution = mock(DelegateExecution.class);
        lenient().when(execution.getProcessInstanceId()).thenReturn(dumyId);
        lenient().when(execution.getVariable(VARNAME_CURRENT_PARTITION_ID, Long.class)).thenReturn(CtxExamples.ctx_partition1);
        JsonNode remSU = ProtoolsTestUtils.asJsonNode(inputRemSU);
        lenient().when(execution.getVariable(VARNAME_REM_INTERROGATION, JsonNode.class)).thenReturn(remSU);
        //Ctx
        JsonNode ctxNode = initContexteMockWithString(inputCtx);

        //Run method under test
        assertDoesNotThrow(() -> sabianePilotageTask.execute(execution));

        //Post conditions
        ArgumentCaptor<SurveyUnitResponseDto> acSUDto = ArgumentCaptor.forClass(SurveyUnitResponseDto.class);
        verify(sabianeQuestionnaireService, times(1)).postSurveyUnit(acSUDto.capture(), eq("AAC2023A00"));
        List<SurveyUnitResponseDto> allValues = acSUDto.getAllValues();
        assertEquals(1, allValues.size(), "We should have exactly one SU Created");

        SurveyUnitResponseDto actualSU = allValues.get(0);
        assertEquals(
                ctxNode.path("partitions").path(0).path("questionnaireModel").asText(),
                actualSU.getQuestionnaireId());
        assertEquals(
                remSU.path("externals"),
                actualSU.getData());

        String idSabiane = SabianeIdHelper.computeSabianeID(CtxExamples.ctx_partition1.toString(), remSU.path("repositoryId").asText());
        assertEquals(
                idSabiane,
                actualSU.getId(),
                "Expected a correctly formed sabiane SU ID");

        assertThat(actualSU.getPersonalization()).isEmpty();
        assertThat(actualSU.getComment()).isEmpty();
        //assertThat(actualSU.getStateData()).isEmpty();

        Mockito.reset(protoolsContext);
    }

}