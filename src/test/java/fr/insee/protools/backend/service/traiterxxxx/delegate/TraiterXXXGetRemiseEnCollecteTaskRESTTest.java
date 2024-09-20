package fr.insee.protools.backend.service.traiterxxxx.delegate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.service.traiterxxxx.TraiterXXXService;
import fr.insee.protools.backend.service.utils.delegate.IDelegateWithVariables;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_CURRENT_PARTITION_ID;
import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_INTERRO_REMISE_EN_COLLECTE_LIST;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraiterXXXGetRemiseEnCollecteTaskRESTTest implements IDelegateWithVariables {

    private static ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    TraiterXXXService service;

    @InjectMocks
    TraiterXXXGetRemiseEnCollecteTaskREST task;

    @Override
    public JavaDelegate getTaskUnderTest() {
        return task;
    }

    @Override
    public Map<String, Class> getVariablesAndTypes() {
        return Map.of(
                VARNAME_CURRENT_PARTITION_ID, String.class
        );
    }

    @Test
    @DisplayName("Test execute method - should work and make correct call to service")
    void execute_should_work_whenEverythingOk() {
        //Prepare
        String currentPartitionId = UUID.randomUUID().toString();
        DelegateExecution execution = createMockedExecution();
        doReturn(execution).when(execution).getParent();
        doReturn(currentPartitionId).when(execution).getVariable(VARNAME_CURRENT_PARTITION_ID,String.class);

        List<JsonNode> mockedResponse = List.of(
                objectMapper.createObjectNode().put("id", 1).put("id", "toto"),
                objectMapper.createObjectNode().put("identifier", 1).put("tatxa", "toto"));

        doReturn(mockedResponse).when(service).getRemiseEnCollecteForPartition(eq(currentPartitionId));
        //Call method under tests
        task.execute(execution);

        //Verify
        verify(execution).setVariableLocal(eq(VARNAME_INTERRO_REMISE_EN_COLLECTE_LIST), eq(mockedResponse));
    }
}