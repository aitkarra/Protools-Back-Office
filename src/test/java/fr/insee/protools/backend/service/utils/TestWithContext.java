package fr.insee.protools.backend.service.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.ProtoolsTestUtils;
import fr.insee.protools.backend.dto.ContexteProcessus;
import fr.insee.protools.backend.service.context.ContextService;
import fr.insee.protools.backend.service.context.exception.BadContexMissingBPMNError;
import fr.insee.protools.backend.service.context.exception.BadContextIncorrectBPMNError;
import lombok.SneakyThrows;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public abstract class TestWithContext {

    @Spy protected ContextService protoolsContext;

    protected final String dumyId="ID1";
    private ObjectMapper objectMapper=new ObjectMapper();

    @AfterEach
    void mockitoResetContext() {
        Mockito.reset(protoolsContext);
    }
    @SneakyThrows
    protected JsonNode initContexteMockWithFile(String contexteToLoad) {
        JsonNode contextRootNode = ProtoolsTestUtils.asJsonNode(contexteToLoad);
        ContexteProcessus schema = objectMapper.readValue(contexteToLoad, ContexteProcessus.class);
        doReturn(contextRootNode).when(protoolsContext).getContextJsonNodeByProcessInstance(anyString());
        doReturn(schema).when(protoolsContext).getContextDtoByProcessInstance(anyString());
        return contextRootNode;
    }

    protected JsonNode initContexteMockWithString(String contexteAsString){
        try{
            return ProtoolsTestUtils.initContexteMockFromString(protoolsContext,contexteAsString);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    public DelegateExecution createMockedExecution(){
        DelegateExecution execution = mock(DelegateExecution.class);
        doReturn(dumyId).when(execution).getProcessInstanceId();
        return execution;
    }

    protected void assertThat_delegate_throwError_when_null_context(JavaDelegate delegate) {
        //Precondition
        DelegateExecution execution = mock(DelegateExecution.class);
        lenient().when(execution.getProcessInstanceId()).thenReturn(dumyId);
        doThrow(new BadContexMissingBPMNError("Erreur")).when(protoolsContext).getContextDtoByProcessInstance(anyString());
        doThrow(new BadContexMissingBPMNError("Erreur")).when(protoolsContext).getContextJsonNodeByProcessInstance(anyString());

        assertThrows(BadContexMissingBPMNError.class, () -> delegate.execute(execution));
    }
}
