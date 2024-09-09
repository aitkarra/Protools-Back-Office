package fr.insee.protools.backend.service.platine.delegate.pilotatage;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.service.platine.service.PlatinePilotageService;
import fr.insee.protools.backend.service.utils.TestWithContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.test.FlowableTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static fr.insee.protools.backend.utils.data.CtxExamples.ctx_empty;
import static fr.insee.protools.backend.utils.data.CtxExamples.ctx_empty_id;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@FlowableTest
class PlatinePilotageCreateContextTaskRESTTest extends TestWithContext {

    @Mock
    PlatinePilotageService platinePilotageService;

    @InjectMocks
    PlatinePilotageCreateContextTaskREST platinePilotageTask;


    @Test
    void execute_should_throwError_when_null_context(){
        assertThat_delegate_throwError_when_null_context(platinePilotageTask);
    }

    @Test
    void execute_should_work_when_contextOK() {
        DelegateExecution execution = createMockedExecution();
        JsonNode expectedContext = initContexteMockWithString(ctx_empty);

        //Execute the unit under test
        platinePilotageTask.execute(execution);

        //Verify postContext
        ArgumentCaptor<JsonNode> acCtx = ArgumentCaptor.forClass(JsonNode.class);
        verify(platinePilotageService,times(1)).postContext(eq(ctx_empty_id),acCtx.capture());
        List<JsonNode> allValues = acCtx.getAllValues();
        assertEquals(1, allValues.size(),"We should have exactly one value");

        assertEquals(expectedContext,allValues.get(0),"Wrong context passed");

    }

}