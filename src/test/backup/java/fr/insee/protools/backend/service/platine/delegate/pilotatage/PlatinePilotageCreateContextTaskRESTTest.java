package fr.insee.protools.backend.service.platine.delegate.pilotatage;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.ProtoolsTestUtils;
import fr.insee.protools.backend.service.context.exception.BadContextIncorrectBPMNError;
import fr.insee.protools.backend.service.platine.delegate.PlatineQuestionnaireCreateContextTaskTest;
import fr.insee.protools.backend.service.platine.service.PlatinePilotageService;
import fr.insee.protools.backend.service.utils.TestWithContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.test.FlowableTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ClassUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@FlowableTest
class PlatinePilotageCreateContextTaskRESTTest extends TestWithContext {
    final static String ressourceFolder = ClassUtils.convertClassNameToResourcePath(PlatineQuestionnaireCreateContextTaskTest.class.getPackageName());
    final static String platine_context_json = ressourceFolder+"/protools-contexte-platine-individu.json";


    @Mock
    PlatinePilotageService platinePilotageService;
    @InjectMocks
    PlatinePilotageCreateContextTaskREST platinePilotageTask;


    @Test
    void execute_should_throwError_when_null_context(){
        assertThat_delegate_throwError_when_null_context(platinePilotageTask);
    }

    @Test
    void execute_should_throw_BadContextIncorrectException_when_noContext() {
        DelegateExecution execution = createMockedExecution();

        //Execute the unit under test
        assertThrows(BadContextIncorrectBPMNError.class,() -> platinePilotageTask.execute(execution));
    }

    @Test
    void execute_should_work_when_contextOK() {
        DelegateExecution execution = createMockedExecution();
        initContexteMockWithFile(platine_context_json);
        String partitionId="1";
        String campaignId="DEM2022X00";

        //Execute the unit under test
        platinePilotageTask.execute(execution);

        //Verify postContext
        ArgumentCaptor<JsonNode> acCtx = ArgumentCaptor.forClass(JsonNode.class);
        verify(platinePilotageService,times(1)).postContext(eq(campaignId),acCtx.capture());
        List<JsonNode> allValues = acCtx.getAllValues();
        assertEquals(1, allValues.size(),"We should have exactly one partition");

        JsonNode expectedContext = ProtoolsTestUtils.asJsonNode(platine_context_json);
        assertEquals(expectedContext,allValues.get(0),"Wrong context passed");
    }

}