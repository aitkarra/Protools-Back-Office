package fr.insee.protools.backend.service.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import fr.insee.protools.backend.ProtoolsTestUtils;
import fr.insee.protools.backend.dto.ContexteProcessus;
import fr.insee.protools.backend.service.context.ContextService;
import fr.insee.protools.backend.service.context.exception.BadContexMissingBPMNError;
import fr.insee.protools.backend.service.exception.VariableClassCastException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
public abstract class TestWithContext {

    @Spy protected ContextService protoolsContext;

    protected final String dumyId="ID1";
    private ObjectMapper objectMapper=new ObjectMapper();

    @AfterEach
    void mockitoResetContext() {
        reset(protoolsContext);
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

    public DelegateExecution createMockedExecution(){
        DelegateExecution execution = mock(DelegateExecution.class);
        doReturn(dumyId).when(execution).getProcessInstanceId();
        return execution;
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
    protected void execute_should_throw_FlowableIllegalArgumentException_when_variables_notDefined(){
        Map<String, Class> typeByVariable = getVariablesAndTypes();
        //No variables==> No test
        if(typeByVariable.isEmpty()){
            log.info("No variable configured as mandatory for this delegate");
            assertTrue(true);
            return;
        }

        //Precondition
        DelegateExecution execution = mock(DelegateExecution.class);
        lenient().when(execution.getProcessInstanceId()).thenReturn(dumyId);

        String[] variables=getVariablesAndTypes().keySet().toArray(new String[0]);
        combinations(variables)
                .forEach(variablesSubset -> {
                    log.info("subset of initialized variables: "+variablesSubset);
                    //Initailize the mocked variables
                    for (String variable :variablesSubset) {
                        Class<?> clazz = typeByVariable.get(variable);
                        var dummyValue = initDefaultValue(clazz);
                        lenient().doReturn(dummyValue).when(execution).getVariable(eq(variable), eq(clazz));
                        lenient().doReturn(dummyValue).when(execution).getVariableLocal(eq(variable), eq(clazz));

                    }

                    //Run the test for this subset of initialized variables
                    //Verify
                    FlowableIllegalArgumentException exception = assertThrows(FlowableIllegalArgumentException.class, () -> getTaskUnderTest().execute(execution));
                    assertThat(exception.getMessage(), containsString("was not found"));
                    // Reset the mock (this clears all stubbings and invocations)
                    Mockito.reset(execution);
                } );
    }


    @Test
    protected void execute_should_throw_FlowableIllegalArgumentException_when_variables_wrongType() {
        Map<String, Class> typeByVariable = getVariablesAndTypes();
        //No variables==> No test
        if(typeByVariable.isEmpty()){
            assertTrue(true);
            log.info("No variable configured as mandatory for this delegate");
            return;
        }


        //Precondition
        DelegateExecution execution = mock(DelegateExecution.class);
        lenient().when(execution.getProcessInstanceId()).thenReturn(dumyId);

        typeByVariable
                .keySet()
                .forEach(variable -> {
                    Class<?> clazzOriginal = typeByVariable.get(variable);
                    Class<?> clazz;
                    if (clazzOriginal.equals(String.class)) {
                        clazz = List.class;
                    } else if (clazzOriginal.equals(List.class)) {
                        clazz = JsonNode.class;
                    } else {
                        clazz = String.class;
                    }
                    var dummyValue = initDefaultValue(clazz);
                    lenient().doReturn(dummyValue).when(execution).getVariable(eq(variable), eq(clazzOriginal));
                    lenient().doReturn(dummyValue).when(execution).getVariableLocal(eq(variable), eq(clazzOriginal));
                });
        //Run the test
        //Verify
        assertThrows(VariableClassCastException.class, () -> getTaskUnderTest().execute(execution));
    }

    private  <T> T initDefaultValue(Class<T> clazz){
        if(clazz.equals(List.class)){
            return (T) List.of();
        }
        else if(clazz.equals(Map.class)){
            return (T) Map.of();
        }
        else if(clazz.equals(Set.class)){
            return (T) Set.of();
        }
        else if(clazz.equals(JsonNode.class)){
            return (T) JsonNodeFactory.instance.objectNode();
        }

        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    //Generate a Stream of all the combinaisons of elements of the array including an empty List but without the full list
    private static <T> Stream<List<T>> combinations(T[] arr) {
        final long N = (long) Math.pow(2, arr.length)-1;
        return StreamSupport.stream(new Spliterators.AbstractSpliterator<List<T>>(N, Spliterator.SIZED) {
            long i = 0;
            @Override
            public boolean tryAdvance(Consumer<? super List<T>> action) {
                if(i < N) {
                    List<T> out = new ArrayList<T>(Long.bitCount(i));
                    for (int bit = 0; bit < arr.length; bit++) {
                        if((i & (1<<bit)) != 0) {
                            out.add(arr[bit]);
                        }
                    }
                    action.accept(out);
                    ++i;
                    return true;
                }
                else {
                    return false;
                }
            }
        }, false);
    }

    //protected abstract void execute_should_throw_BadContext_when_contextIncorrect(String context_json);

    protected abstract JavaDelegate getTaskUnderTest();
    protected abstract Map<String,Class> getVariablesAndTypes();


}
