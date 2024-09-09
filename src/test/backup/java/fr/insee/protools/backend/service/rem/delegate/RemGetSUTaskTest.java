package fr.insee.protools.backend.service.rem.delegate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.service.rem.RemService;
import fr.insee.protools.backend.dto.rem_tmp.REMSurveyUnitDto;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.test.FlowableTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_REM_INTERROGATION;
import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_REM_SURVEY_UNIT_IDENTIFIER;
import static fr.insee.protools.backend.service.utils.FlowableVariableUtils.getMissingVariableMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FlowableTest
class RemGetSUTaskTest {

    @Mock RemService remService;
    @Spy ObjectMapper objectMapper;
    @InjectMocks RemGetSUTask remGetSUTask;

    String dumyId = "ID1";

    @Test
    void execute_should_throw_FlowableIllegalArgumentException_when_variableremSurveyUnitIdentifier_notDefined() throws JsonProcessingException {
        //Precondition
        DelegateExecution execution = mock(DelegateExecution.class);
        lenient().when(execution.getProcessInstanceId()).thenReturn(dumyId);

        //Execute the unit under test
        FlowableIllegalArgumentException exception = assertThrows(FlowableIllegalArgumentException.class, () -> remGetSUTask.execute(execution));
        assertThat(exception.getMessage()).isEqualTo(getMissingVariableMessage(VARNAME_REM_SURVEY_UNIT_IDENTIFIER));
    }

    @Test
    void execute_should_work_when_variable_OK() {
        //Préconditions
        DelegateExecution execution = mock(DelegateExecution.class);
        Long variableSuId=98455435l;
        REMSurveyUnitDto expectedREMSU = REMSurveyUnitDto.builder().externalId("TOTO").build();

        when(execution.getProcessInstanceId()).thenReturn(dumyId);
        when(execution.getVariable(VARNAME_REM_SURVEY_UNIT_IDENTIFIER, Long.class)).thenReturn(variableSuId);
        when(remService.getSurveyUnit(variableSuId)).thenReturn(expectedREMSU);

        //Execute the unit under test
        remGetSUTask.execute(execution);

        //Post conditions
        //Service called once and for the right SU ID
        verify(remService).getSurveyUnit(variableSuId);
        //Process instance variable set with the retrieved SU content
        verify(execution).setVariableLocal(VARNAME_REM_INTERROGATION, objectMapper.valueToTree(expectedREMSU));
    }
}