package fr.insee.protools.backend.service.platine.delegate.questionnaire;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.service.platine.service.PlatineQuestionnaireService;
import fr.insee.protools.backend.service.utils.delegate.TestDelegateWithContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_CURRENT_PARTITION_ID;
import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_REM_INTERRO_LIST;
import static fr.insee.protools.backend.utils.data.CtxExamples.ctx_empty;
import static fr.insee.protools.backend.utils.data.CtxExamples.ctx_empty_id;
import static fr.insee.protools.backend.utils.data.InterroExamples.generateEmptyInterro;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PlatineQuestionnaireCreateInterrogationListTaskRESTTestDelegate extends TestDelegateWithContext {

    @Mock
    PlatineQuestionnaireService platineQuestionnaireService;

    @InjectMocks
    PlatineQuestionnaireCreateInterrogationListTaskREST task;

    @Test
    void execute_should_work_when_contextOKVarOK() {
        DelegateExecution execution = createMockedExecution();
        JsonNode expectedContext = initContexteMockWithString(ctx_empty);

        //Process variables
        lenient().doReturn(String.valueOf(UUID.randomUUID())).when(execution).getVariable(VARNAME_CURRENT_PARTITION_ID, String.class);

        List<JsonNode> interroList = IntStream.range(0, 100)
                .mapToObj(i -> generateEmptyInterro().interro()) // Call your function and get the JsonNode
                .collect(Collectors.toList());
        lenient().doReturn(interroList).when(execution).getVariable(VARNAME_REM_INTERRO_LIST, List.class);

        //Execute the unit under test
        task.execute(execution);

        //Verify postContext
        final ArgumentCaptor<List<JsonNode> > listCaptor
                = ArgumentCaptor.forClass((Class) List.class);

        verify(platineQuestionnaireService,times(1)).postInterrogations(eq(ctx_empty_id),listCaptor.capture());
        List<List<JsonNode>> allValues = listCaptor.getAllValues();
        assertEquals(1, allValues.size(),"We should have exactly one value");

        assertEquals(interroList,allValues.get(0),"Wrong list of interro passed");
    }

    @Override
    public JavaDelegate getTaskUnderTest() {
        return task;
    }

    @Override
    public Map<String, Class> getVariablesAndTypes() {
        return Map.of(
                VARNAME_CURRENT_PARTITION_ID,String.class,
                VARNAME_REM_INTERRO_LIST,List.class);
    }
}