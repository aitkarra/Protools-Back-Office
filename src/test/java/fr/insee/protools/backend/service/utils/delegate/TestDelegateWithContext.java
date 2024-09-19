package fr.insee.protools.backend.service.utils.delegate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.ProtoolsTestUtils;
import fr.insee.protools.backend.dto.ContexteProcessus;
import fr.insee.protools.backend.service.DelegateContextVerifier;
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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
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

    protected abstract String minimalValidCtxt();

    @Test
    @Override
    public void execute_should_work_when_params_notNull() {
        initContexteMockWithString(minimalValidCtxt());
        IDelegateWithVariables.super.execute_should_work_when_params_notNull();
    }

    @Override
    public void initExtraMocks(DelegateExecution execution) {
        initContexteMockWithString(minimalValidCtxt());
        IDelegateWithVariables.super.initExtraMocks(execution);
    }

    @Override
    @Test
    public void execute_should_throw_FlowableIllegalArgumentException_when_variables_notDefined(){
        initContexteMockWithString(minimalValidCtxt());
        IDelegateWithVariables.super.execute_should_throw_FlowableIllegalArgumentException_when_variables_notDefined();
    }

    @SneakyThrows
    protected JsonNode initContexteMockWithFile(String contexteToLoad) {
        JsonNode contextRootNode = ProtoolsTestUtils.asJsonNode(contexteToLoad);
        ContexteProcessus schema = objectMapper.readValue(contexteToLoad, ContexteProcessus.class);

        lenient().doReturn(contextRootNode).when(protoolsContext).getContextJsonNodeByProcessInstance(anyString());
        lenient().doReturn(schema).when(protoolsContext).getContextDtoByProcessInstance(anyString());
        return contextRootNode;
    }

    @SneakyThrows
    protected JsonNode initContexteMockWithString(String contexteAsString){
        JsonNode contextRootNode = new ObjectMapper().readTree(contexteAsString);
        ContexteProcessus schema = objectMapper.readValue(contexteAsString, ContexteProcessus.class);

        lenient().doReturn(contextRootNode).when(protoolsContext).getContextJsonNodeByProcessInstance(anyString());
        lenient().doReturn(schema).when(protoolsContext).getContextDtoByProcessInstance(anyString());
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

    @Test
    @DisplayName("execute should throw an error if the context is null")
    void execute_should_throwError_when_null_context_2() {
        if (getTaskUnderTest() instanceof DelegateContextVerifier) {
            //Precondition
            DelegateExecution execution = mock(DelegateExecution.class);
            lenient().when(execution.getProcessInstanceId()).thenReturn(dumyId);
            lenient().doReturn(null).when(protoolsContext).getContextDtoByProcessInstance(anyString());
            lenient().doReturn(null).when(protoolsContext).getContextJsonNodeByProcessInstance(anyString());

            assertThrows(BadContexMissingBPMNError.class, () -> getTaskUnderTest().execute(execution));
        }
    }

    @Test
    void getContextErrors_should_work_when_valid_contexte() throws JsonProcessingException {
        ContexteProcessus schema = objectMapper.readValue(minimalValidCtxt(), ContexteProcessus.class);

        if(getTaskUnderTest() instanceof DelegateContextVerifier){
            Set<String> errors=((DelegateContextVerifier) getTaskUnderTest()).getContextErrors(schema);
            assertTrue(errors.isEmpty(),"We should not have any error in a valid contexte");
        }
    }
}
