package fr.insee.protools.backend.service.rem.delegate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.shaded.gson.JsonObject;
import fr.insee.protools.backend.exception.ProtoolsProcessFlowBPMNError;
import fr.insee.protools.backend.service.rem.RemService;
import fr.insee.protools.backend.service.utils.delegate.IDelegateWithVariables;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Stream;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemPutContactPlatineTaskRESTTest implements IDelegateWithVariables {

    @Mock
    RemService remService;

    @InjectMocks
    RemPutContactPlatineTaskREST task;

    private static ObjectMapper objectMapper=new ObjectMapper();
    static Stream<Arguments> initExecuteParametersOK() {
        List<JsonNode> list1node = List.of(objectMapper.createObjectNode().put("id",1).put("id","toto"));
        List<JsonNode> list2node = List.of(
                objectMapper.createObjectNode().put("id",1).put("id","toto"),
                objectMapper.createObjectNode().put("identifier",1).put("tata","toto"));

        return Stream.of(
                Arguments.of(List.of()),
                Arguments.of(list1node),
                Arguments.of(list2node));
    }

    @Override
    public JavaDelegate getTaskUnderTest() {
        return task;
    }

    @Override
    public Map<String, Class> getVariablesAndTypes() {
        return Map.of(VARNAME_PLATINE_CONTACT_LIST, List.class);
    }

    @ParameterizedTest
    @MethodSource("initExecuteParametersOK")
    @DisplayName("Test execute method - should work and make correct call to service")
    void execute_should_work_whenEverythingOk(List<JsonNode> contacts) {
        //Prepare
        DelegateExecution execution = createMockedExecution();
        doReturn(contacts).when(execution).getVariable(VARNAME_PLATINE_CONTACT_LIST, List.class);

        //Execute method under test
        task.execute(execution);

        //verify
        final ArgumentCaptor<List<JsonNode>> listCaptor = ArgumentCaptor.forClass((Class) List.class);

        verify(remService, times(1)).putContactsPlatine(listCaptor.capture());
        List<List<JsonNode>> allValues = listCaptor.getAllValues();
        assertEquals(1, allValues.size(), "We should have exactly one value");

        List<JsonNode> actualArgument = listCaptor.getValue();
        assertEquals(contacts.size(), actualArgument.size(), "The passed list of contacts has an incorrect size : Exepected " + contacts.size() + " - actual:" + actualArgument.size());

        //check that we passed the correct items
        if (contacts.isEmpty()) {
            assertThat("If no contacts (empty List) ==> should pass an empty list", actualArgument.isEmpty());
        }
        else
        {
            assertThat(actualArgument,
                    IsIterableContainingInAnyOrder.containsInAnyOrder(contacts.toArray(new Object[0])));
        }
    }

    @Test
    @DisplayName("Test execute method - should throw ProtoolsProcessFlowBPMNError when communication Id is null")
    public void execute_should_throw_when_varCommId_is_blank() {
        String commId = "";
        //Prepare
        DelegateExecution execution = createMockedExecution();
        doReturn(commId).when(execution).getVariable(VARNAME_CURRENT_COMMUNICATION_ID, String.class);
        doReturn(dumyId).when(execution).getVariable(VARNAME_CURRENT_PARTITION_ID, String.class);
        doReturn(Map.of("interroId1", "commReqId1")).when(execution).getVariable(VARNAME_COMMUNICATION_REQUEST_ID_FOR_INTERRO_ID_MAP, Map.class);

        //Execute method under test
        assertThrows(ProtoolsProcessFlowBPMNError.class, () -> task.execute(execution));
    }
}