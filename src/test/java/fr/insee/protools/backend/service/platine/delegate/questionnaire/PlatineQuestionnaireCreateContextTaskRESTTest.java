package fr.insee.protools.backend.service.platine.delegate.questionnaire;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.service.platine.service.PlatineQuestionnaireService;
import fr.insee.protools.backend.service.utils.TestWithContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static fr.insee.protools.backend.utils.data.CtxExamples.ctx_empty;
import static fr.insee.protools.backend.utils.data.CtxExamples.ctx_empty_id;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PlatineQuestionnaireCreateContextTaskRESTTest  extends TestWithContext {

    @Mock
    PlatineQuestionnaireService platineQuestionnaireService;

    @InjectMocks
    PlatineQuestionnaireCreateContextTaskREST platineQuestionnaireTask;


    @Test
    void execute_should_throwError_when_null_context(){
        assertThat_delegate_throwError_when_null_context(platineQuestionnaireTask);
    }

    @Test
    void execute_should_work_when_contextOK() {
        DelegateExecution execution = createMockedExecution();
        JsonNode expectedContext = initContexteMockWithString(ctx_empty);

        //Execute the unit under test
        platineQuestionnaireTask.execute(execution);

        //Verify postContext
        ArgumentCaptor<JsonNode> acCtx = ArgumentCaptor.forClass(JsonNode.class);
        verify(platineQuestionnaireService,times(1)).postContext(eq(ctx_empty_id),acCtx.capture());
        List<JsonNode> allValues = acCtx.getAllValues();
        assertEquals(1, allValues.size(),"We should have exactly one value");

        assertEquals(expectedContext,allValues.get(0),"Wrong context passed");

    }
}