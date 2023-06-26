package fr.insee.protools.backend.service.rem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.service.context.ContextService;
import fr.insee.protools.backend.service.context.exception.BadContextIncorrectException;
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
import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_REM_SU_ID_LIST;
import static fr.insee.protools.backend.service.utils.FlowableVariableUtils.getMissingVariableMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
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

    private void initContexteMockFromString(String contextAsString) throws JsonProcessingException {
        JsonNode contextRootNode = new ObjectMapper().readTree(contextAsString);
        when(protoolsContext.getContextByProcessInstance(anyString())).thenReturn(contextRootNode);
    }

    @Test
    void execute_should_throw_BadContextIncorrectException_when_noContext() {
        //Precondition
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getProcessInstanceId()).thenReturn(dumyId);

        //Execute the unit under test
        assertThrows(BadContextIncorrectException.class, () -> remGetPartitionListOfSuIdTask.execute(execution));
    }

    @Test
    void execute_should_throw_FlowableIllegalArgumentException_when_variableCurrentPartition_notDefined() throws JsonProcessingException {
        //Precondition
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getProcessInstanceId()).thenReturn(dumyId);
        initContexteMockFromString(json1Partition);

        //Execute the unit under test
        FlowableIllegalArgumentException exception = assertThrows(FlowableIllegalArgumentException.class, () -> remGetPartitionListOfSuIdTask.execute(execution));
        assertThat(exception.getMessage()).isEqualTo(getMissingVariableMessage(VARNAME_CURRENT_PARTITION_ID));
    }


    void execute_should_work_when_contextNpartition_and_variable_OK(String contexte, String currentPartitionId, Long[] remSuIdList) throws JsonProcessingException {
        //Préconditions
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getProcessInstanceId()).thenReturn(dumyId);

        initContexteMockFromString(contexte);
        when(execution.getVariable(VARNAME_CURRENT_PARTITION_ID, String.class)).thenReturn(currentPartitionId);
        List<Long> expectedResult = List.of(remSuIdList);
        when(remService.getSampleSuIds(currentPartitionId)).thenReturn(remSuIdList);
        //Execute the unit under test
        remGetPartitionListOfSuIdTask.execute(execution);


        //Post conditions
        //Service called once and for the right partition
        verify(remService).getSampleSuIds(currentPartitionId);
        //Process instance variable set with the list of retrieved Ids
        verify(execution).setVariable(VARNAME_REM_SU_ID_LIST, expectedResult);
    }

    @Test
    void execute_should_work_when_context1partition_and_variable_OK() throws JsonProcessingException {
        Long[] remSuIdList = {1l, 2l, 3l};
        String json1Partition =
                "{ \"partitions\": [{ " +
                        "    \"id\": 1" +
                        "  }]" +
                        "}";
        execute_should_work_when_contextNpartition_and_variable_OK(json1Partition, "1", remSuIdList);
    }

    @Test
    void execute_should_work_when_context3partition_and_variable_OK() throws JsonProcessingException {
        Long[] remSuIdList = {1l, 2l, 3l};
        String json3Partition =
                "{ \"partitions\": [{ \"id\": 1 },{ \"id\": 56 }, { \"id\": 99 }] }";
        execute_should_work_when_contextNpartition_and_variable_OK(json3Partition, "1", remSuIdList);
    }
}