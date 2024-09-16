package fr.insee.protools.backend.service.utils.delegate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.ProtoolsTestUtils;
import fr.insee.protools.backend.dto.ContexteProcessus;
import fr.insee.protools.backend.service.context.ContextService;
import fr.insee.protools.backend.service.context.exception.BadContexMissingBPMNError;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static fr.insee.protools.backend.utils.data.CtxExamples.ctx_empty;
import static fr.insee.protools.backend.utils.data.CtxExamples.ctx_empty_id;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
public abstract class TestDelegateWithContext implements IDelegateWithVariables{

    @Spy protected ContextService protoolsContext;

    private ObjectMapper objectMapper=new ObjectMapper();

    @AfterEach
    void mockitoResetContext() {
        reset(protoolsContext);
    }

    @Test
    @Override
    public void execute_should_work_when_params_notNull() {
        initContexteMockWithString(ctx_empty);
        IDelegateWithVariables.super.execute_should_work_when_params_notNull();
    }


    @Override
    @Test
    public void execute_should_throw_FlowableIllegalArgumentException_when_variables_notDefined(){
        initContexteMockWithString(ctx_empty);
        IDelegateWithVariables.super.execute_should_throw_FlowableIllegalArgumentException_when_variables_notDefined();
    }

    @SneakyThrows
    protected JsonNode initContexteMockWithFile(String contexteToLoad) {
        JsonNode contextRootNode = ProtoolsTestUtils.asJsonNode(contexteToLoad);
        ContexteProcessus schema = objectMapper.readValue(contexteToLoad, ContexteProcessus.class);

        doReturn(contextRootNode).when(protoolsContext).getContextJsonNodeByProcessInstance(anyString());
        doReturn(schema).when(protoolsContext).getContextDtoByProcessInstance(anyString());
        return contextRootNode;
    }

    @SneakyThrows
    protected JsonNode initContexteMockWithString(String contexteAsString){
        JsonNode contextRootNode = new ObjectMapper().readTree(contexteAsString);
        ContexteProcessus schema = objectMapper.readValue(contexteAsString, ContexteProcessus.class);

        doReturn(contextRootNode).when(protoolsContext).getContextJsonNodeByProcessInstance(anyString());
        doReturn(schema).when(protoolsContext).getContextDtoByProcessInstance(anyString());
        return contextRootNode;
    }

    @Test
    @DisplayName("execute should throw an error if the context is null")
    void execute_should_throwError_when_null_context(){
        //Precondition
        DelegateExecution execution = mock(DelegateExecution.class);
        lenient().when(execution.getProcessInstanceId()).thenReturn(dumyId);
        doThrow(new BadContexMissingBPMNError("Erreur")).when(protoolsContext).getContextDtoByProcessInstance(anyString());
        doThrow(new BadContexMissingBPMNError("Erreur")).when(protoolsContext).getContextJsonNodeByProcessInstance(anyString());

        assertThrows(BadContexMissingBPMNError.class, () -> getTaskUnderTest().execute(execution));
    }



}
