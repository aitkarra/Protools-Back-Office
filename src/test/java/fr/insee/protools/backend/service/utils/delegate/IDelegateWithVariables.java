package fr.insee.protools.backend.service.utils.delegate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import fr.insee.protools.backend.restclient.pagination.PageResponse;
import fr.insee.protools.backend.service.exception.VariableClassCastException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public interface IDelegateWithVariables {

    org.slf4j.Logger ilogger = org.slf4j.LoggerFactory.getLogger(IDelegateWithVariables.class);
    String dumyId = "ID1";

    //TODO: activer ce test pour que tous les delegates le prennent en compte
    //void execute_should_throw_BadContext_when_contextIncorrect(String context_json);

    JavaDelegate getTaskUnderTest();
    Map<String,Class> getVariablesAndTypes();

    // extra mocked needed so that it works
    default void initExtraMocks(DelegateExecution execution) {}


    @Test
    default void execute_should_work_when_params_notNull(){
        //Preconditions
        DelegateExecution execution = mock(DelegateExecution.class);
        lenient().when(execution.getProcessInstanceId()).thenReturn(dumyId);
        lenient().when(execution.getParent()).thenReturn(execution);

        Map<String, Class> typeByVariable = getVariablesAndTypes();
        typeByVariable
                .keySet()
                .forEach(variable -> {
                    Class<?> clazz = typeByVariable.get(variable);
                    var dummyValue = getDefaultValue(clazz);
                    lenient().doReturn(dummyValue).when(execution).getVariable(eq(variable), eq(clazz));
                    lenient().doReturn(dummyValue).when(execution).getVariableLocal(eq(variable), eq(clazz));
                });

        //Extra mocks (ex: read)
        initExtraMocks(execution);

        //Call method under test
        assertDoesNotThrow(() -> getTaskUnderTest().execute(execution));
    }

    @Test
    default void execute_should_throw_FlowableIllegalArgumentException_when_variables_wrongType() {
        Map<String, Class> typeByVariable = getVariablesAndTypes();
        //No variables==> No test
        if(typeByVariable.isEmpty()){
            assertTrue(true);
            ilogger.info("No variable configured as mandatory for this delegate");
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
                    var dummyValue = getDefaultValue(clazz);
                    lenient().doReturn(dummyValue).when(execution).getVariable(eq(variable), eq(clazzOriginal));
                    lenient().doReturn(dummyValue).when(execution).getVariableLocal(eq(variable), eq(clazzOriginal));
                });
        //Run the test
        //Verify
        assertThrows(VariableClassCastException.class, () -> getTaskUnderTest().execute(execution));
    }


    @Test
    default void execute_should_throw_FlowableIllegalArgumentException_when_variables_notDefined(){
        Map<String, Class> typeByVariable = getVariablesAndTypes();
        //No variables==> No test
        if(typeByVariable.isEmpty()){
            ilogger.info("No variable configured as mandatory for this delegate");
            assertTrue(true);
            return;
        }

        //Precondition
        DelegateExecution execution = mock(DelegateExecution.class);
        lenient().when(execution.getProcessInstanceId()).thenReturn(dumyId);
//
        String[] variables=getVariablesAndTypes().keySet().toArray(new String[0]);
        combinations(variables)
                .forEach(variablesSubset -> {
                    ilogger.info("subset of initialized variables: "+variablesSubset);
                    //Initailize the mocked variables
                    Set<String> intializedVars=new HashSet<>();

                    //The one not defined ==> Throw
                    for (String variable :variablesSubset) {
                        Class<?> clazz = typeByVariable.get(variable);
                        lenient().doThrow(new FlowableIllegalArgumentException("Variable "+variable+" not exists")).when(execution).getVariable(eq(variable), eq(clazz));
                        lenient().doThrow(new FlowableIllegalArgumentException("Variable "+variable+" not exists")).when(execution).getVariableLocal(eq(variable), eq(clazz));
                        ilogger.info("mock getVariable Throw : variable="+variable+" - class="+clazz);
                        intializedVars.add(variable);
                    }
                    //The others
                    getVariablesAndTypes().entrySet()
                            .stream().filter(entry -> !intializedVars.contains(entry.getKey()))
                            .forEach(entry -> {
                                String variable=entry.getKey();
                                Class<?> clazz = typeByVariable.get(variable);
                                var dummyValue = getDefaultValue(clazz);
                                lenient().doReturn(dummyValue).when(execution).getVariable(eq(variable), eq(clazz));
                                lenient().doReturn(dummyValue).when(execution).getVariableLocal(eq(variable), eq(clazz));
                                ilogger.info("mock getVariable VALUE: variable="+variable+" - class="+clazz);
                            });

                    //Run the test for this subset of initialized variables
                    //Verify
                    FlowableIllegalArgumentException exception = assertThrows(FlowableIllegalArgumentException.class, () -> getTaskUnderTest().execute(execution));
                    assertThat(exception.getMessage(), containsString(" not exists"));
                    // Reset the mock (this clears all stubbings and invocations)
                    Mockito.reset(execution);
                } );
    }



    default DelegateExecution createMockedExecution(){
        DelegateExecution execution = mock(DelegateExecution.class);
        lenient().doReturn(dumyId).when(execution).getProcessInstanceId();
        return execution;
    }

    //Init an object of  clazz type with default value
    default <T> T getDefaultValue(Class<T> clazz){
        if(clazz.equals(List.class)){
            return (T) new ArrayList<>();
        }
        else if(clazz.equals(Map.class)){
            return (T) new HashMap<>();
        }
        else if(clazz.equals(Set.class)){
            return (T) new HashSet<>();
        }
        else if(clazz.equals(JsonNode.class)){
            return (T) JsonNodeFactory.instance.objectNode();
        }
        else if(clazz.equals(String.class)) {
            return (T) "1";
        }
        else if(clazz.equals(Long.class)) {
            return (T) Long.valueOf(1);
        }
        else if(clazz.equals(Integer.class)) {
            return (T) Integer.valueOf(1);
        }
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    //Generate a Stream of all the combinaisons of elements of the array including the full list (but no empty set)
    private static <T> Stream<List<T>> combinations(T[] arr) {
        final long N = (long) Math.pow(2, arr.length);
        return StreamSupport.stream(new Spliterators.AbstractSpliterator<List<T>>(N, Spliterator.SIZED) {
            long i = 1;
            @Override
            public boolean tryAdvance(Consumer<? super List<T>> action) {
                if(i < N) {
                    List<T> out = new ArrayList<>(Long.bitCount(i) );
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
                    // Ensure that the full combination of all elements is returned at the end
                    List<T> allElements = Arrays.asList(arr);
                    action.accept(allElements); // Return the full set
                    return false;
                }
            }
        }, false);
    }



}
