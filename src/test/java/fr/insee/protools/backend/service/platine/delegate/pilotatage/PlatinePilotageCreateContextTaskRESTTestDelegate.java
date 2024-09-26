package fr.insee.protools.backend.service.platine.delegate.pilotatage;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.service.platine.service.PlatinePilotageServiceImpl;
import fr.insee.protools.backend.service.utils.delegate.TestDelegateWithContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.test.FlowableTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static fr.insee.protools.backend.utils.data.CtxExamples.CTX_EMPTY;
import static fr.insee.protools.backend.utils.data.CtxExamples.CTX_EMPTY_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@FlowableTest
class PlatinePilotageCreateContextTaskRESTTestDelegate extends TestDelegateWithContext {

    @Mock
    PlatinePilotageServiceImpl platinePilotageService;

    @InjectMocks
    PlatinePilotageCreateContextTaskREST platinePilotageTask;

    @Test
    void execute_should_work_when_contextOK() {
        DelegateExecution execution = createMockedExecution();
        JsonNode expectedContext = initContexteMockWithString(CTX_EMPTY);

        //Execute the unit under test
        platinePilotageTask.execute(execution);

        //Verify postContext
        ArgumentCaptor<JsonNode> acCtx = ArgumentCaptor.forClass(JsonNode.class);
        verify(platinePilotageService,times(1)).postContext(eq(CTX_EMPTY_ID),acCtx.capture());
        List<JsonNode> allValues = acCtx.getAllValues();
        assertEquals(1, allValues.size(),"We should have exactly one value");

        assertEquals(expectedContext,allValues.get(0),"Wrong context passed");

    }

    @Override
    public JavaDelegate getTaskUnderTest() {
        return platinePilotageTask;
    }

    @Override
    public Map<String, Class> getVariablesAndTypes() {
        return Map.of();
    }

    @Override
    protected String minimalValidCtxt() {
        return CTX_EMPTY;
    }
}