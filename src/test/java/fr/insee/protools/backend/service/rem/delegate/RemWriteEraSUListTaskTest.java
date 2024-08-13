package fr.insee.protools.backend.service.rem.delegate;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.insee.protools.backend.dto.era.CensusJsonDto;
import fr.insee.protools.backend.service.rem.RemService;
import fr.insee.protools.backend.dto.rem.SuIdMappingJson;
import fr.insee.protools.backend.dto.rem.SuIdMappingRecord;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.test.FlowableTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.*;
import static fr.insee.protools.backend.service.utils.FlowableVariableUtils.getMissingVariableMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FlowableTest
class RemWriteEraSUListTaskTest {

    @Mock RemService remService;
    @InjectMocks RemWriteEraSUListTask remWriteEraSUListTask;

    String dumyId = "ID999";

    @Test
    @DisplayName("Test execute method - should throw if VARNAME_ERA_RESPONSE or VARNAME_CURRENT_PARTITION_ID not initialized")
    void execute_should_throw_FlowableIllegalArgumentException_when_variables_notDefined() throws JsonProcessingException {
        //Precondition
        DelegateExecution execution = mock(DelegateExecution.class);
        lenient().when(execution.getProcessInstanceId()).thenReturn(dumyId);

        //Execute the unit under test
        FlowableIllegalArgumentException exception = assertThrows(FlowableIllegalArgumentException.class, () -> remWriteEraSUListTask.execute(execution));
        //Post conditions
        assertThat(exception.getMessage()).isEqualTo(getMissingVariableMessage(VARNAME_ERA_RESPONSE));


        //Create First variable
        when(execution.getVariable(VARNAME_ERA_RESPONSE, List.class)).thenReturn(List.of(CensusJsonDto.builder().build()));

        //Execute again
        exception = assertThrows(FlowableIllegalArgumentException.class, () -> remWriteEraSUListTask.execute(execution));
        //Check the error
        assertThat(exception.getMessage()).isEqualTo(getMissingVariableMessage(VARNAME_CURRENT_PARTITION_ID));

    }

    @Test
    @DisplayName("Test execute method - should tVARNAME_ERA_RESPONSE or VARNAME_CURRENT_PARTITION_ID not initialized")
    void execute_should_work_when_variables_Ok() throws JsonProcessingException {
        //Precondition
        DelegateExecution execution = mock(DelegateExecution.class);
        DelegateExecution executionParent = mock(DelegateExecution.class);
        when(execution.getParent()).thenReturn(executionParent);
        lenient().when(execution.getProcessInstanceId()).thenReturn(dumyId);

        //PArtitionID
        Long partitionID = 99l;
        //Stubbed list of ERA Values
        Long eraId1=500l, eraId2=1000l;
        CensusJsonDto eraSU1 = CensusJsonDto.builder()
                .id(eraId1)
                .build();
        CensusJsonDto eraSU2 = CensusJsonDto.builder()
                .id(eraId2)
                .build();
        List<CensusJsonDto> listOfEraSU = List.of(eraSU1,eraSU2);
        lenient().when(execution.getVariable(VARNAME_ERA_RESPONSE, List.class)).thenReturn(listOfEraSU);
        lenient().when(execution.getVariable(VARNAME_CURRENT_PARTITION_ID, Long.class)).thenReturn(partitionID);

        //Stubbed list id mapping between ERA SU and created REM SU
        long remId1=1l,remId2=2l;
        when(remService.writeERASUList(partitionID,listOfEraSU))
                .thenReturn(SuIdMappingJson.builder()
                        .count(2)
                        .partitionId(partitionID)
                        .data(
                                List.of(
                                        new SuIdMappingRecord(remId1,String.valueOf(eraId1)),
                                        new SuIdMappingRecord(remId2,String.valueOf(eraId2))))
                        .build());
        //Execute method under test
        assertDoesNotThrow(() -> remWriteEraSUListTask.execute(execution));

        //Post conditions
        verify(remService,times(1)).writeERASUList(partitionID,listOfEraSU);
        verify(executionParent,times(1)).setVariableLocal(VARNAME_REM_INTERRO_ID_LIST,List.of(remId1,remId2));
    }
}