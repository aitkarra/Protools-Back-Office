package fr.insee.protools.backend.service.rem.delegate;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.insee.protools.backend.service.context.ContextService;
import fr.insee.protools.backend.service.rem.RemService;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.test.FlowableTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ClassUtils;

import java.util.List;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_CURRENT_PARTITION_ID;
import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_REM_INTERRO_ID_LIST;
import static fr.insee.protools.backend.service.utils.FlowableVariableUtils.getMissingVariableMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@FlowableTest
class RemGetPartitionListOfSuIdTaskTest {


    @Mock RemService remService;
    @Spy ContextService protoolsContext;

    @InjectMocks RemGetPartitionListOfSuIdTask remGetPartitionListOfSuIdTask;

    String dumyId = "ID1";

    final static String ressourceFolder = ClassUtils.convertClassNameToResourcePath(RemGetPartitionListOfSuIdTaskTest.class.getPackageName());

    final String json1Partition =
            "{ \"partitions\": [{ " +
                    "    \"id\": 1" +
                    "  }]" +
                    "}";

    /*
    @Test
    void execute_should_throw_BadContextIncorrectException_when_noContext() {
        //Precondition
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getProcessInstanceId()).thenReturn(dumyId);

        //Execute the unit under test
        assertThrows(BadContextIncorrectException.class, () -> remGetPartitionListOfSuIdTask.execute(execution));
    }
    */


    @Test
    void execute_should_throw_FlowableIllegalArgumentException_when_variableCurrentPartition_notDefined() throws JsonProcessingException {
        //Precondition
        DelegateExecution execution = mock(DelegateExecution.class);
        lenient().when(execution.getProcessInstanceId()).thenReturn(dumyId);
        //No context used for this taks

        //Execute the unit under test
        FlowableIllegalArgumentException exception = assertThrows(FlowableIllegalArgumentException.class, () -> remGetPartitionListOfSuIdTask.execute(execution));
        assertThat(exception.getMessage()).isEqualTo(getMissingVariableMessage(VARNAME_CURRENT_PARTITION_ID));
    }


    void execute_should_work_when_contextNpartition_and_variable_OK(String contexte, Long currentPartitionId, Long[] remSuIdList) throws JsonProcessingException {
        //Préconditions
        DelegateExecution execution = mock(DelegateExecution.class);
        DelegateExecution executionParent = mock(DelegateExecution.class);

        when(execution.getProcessInstanceId()).thenReturn(dumyId);

        //No context used by this task
        when(execution.getVariable(VARNAME_CURRENT_PARTITION_ID, Long.class)).thenReturn(currentPartitionId);
        when(execution.getParent()).thenReturn(executionParent);

        List<Long> expectedResult = List.of(remSuIdList);
        when(remService.getSampleSuIds(currentPartitionId)).thenReturn(remSuIdList);
        //Execute the unit under test
        remGetPartitionListOfSuIdTask.execute(execution);


        //Post conditions
        //Service called once and for the right partition
        verify(remService).getSampleSuIds(currentPartitionId);
        //Process instance variable set with the list of retrieved Ids
        verify(executionParent).setVariableLocal(VARNAME_REM_INTERRO_ID_LIST, expectedResult);
    }

    @Test
    void execute_should_work_when_context1partition_and_variable_OK() throws JsonProcessingException {
        Long[] remSuIdList = {1l, 2l, 3l};
        String json1Partition =
                "{ \"partitions\": [{ " +
                        "    \"id\": 1" +
                        "  }]" +
                        "}";
        execute_should_work_when_contextNpartition_and_variable_OK(json1Partition, 1l, remSuIdList);
    }

    @Test
    void execute_should_work_when_context3partition_and_variable_OK() throws JsonProcessingException {
        Long[] remSuIdList = {1l, 2l, 3l};
        String json3Partition =
                "{ \"partitions\": [{ \"id\": 1 },{ \"id\": 56 }, { \"id\": 99 }] }";
        execute_should_work_when_contextNpartition_and_variable_OK(json3Partition, 1l, remSuIdList);
    }
}