package fr.insee.protools.backend.service.rem.delegate;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.service.rem.RemServiceImpl;
import fr.insee.protools.backend.service.utils.delegate.IDelegateWithVariables;
import org.assertj.core.api.Assertions;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
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
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemPutContactPlatineTaskRESTTest implements IDelegateWithVariables {

    private static ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    RemServiceImpl remService;
    @InjectMocks
    RemPutContactPlatineTaskREST task;

    static Stream<Arguments> initExecuteParametersOK() {
        List<JsonNode> list1node = List.of(objectMapper.createObjectNode().put("id", 1).put("id", "toto"));
        List<JsonNode> list2node = List.of(
                objectMapper.createObjectNode().put("id", 1).put("id", "toto"),
                objectMapper.createObjectNode().put("identifier", 1).put("tata", "toto"));

        return Stream.of(
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
        assertThat(actualArgument,
                IsIterableContainingInAnyOrder.containsInAnyOrder(contacts.toArray(new Object[0])));
    }

    @Test
    @DisplayName("Test execute method - should write a log and not call service when List of contacts is empty ")
    public void execute_should_writeLog_and_notCallService_when_interroListIsEmpty() {
        //Prepare
        DelegateExecution execution = createMockedExecution();
        doReturn(List.of()).when(execution).getVariable(VARNAME_PLATINE_CONTACT_LIST, List.class);

        // create and start a ListAppender to capture logs
        Logger fooLogger = (Logger) LoggerFactory.getLogger(task.getClass());
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        fooLogger.addAppender(listAppender);
        listAppender.start();

        //Execute method under test
        task.execute(execution);

        //Verify
        List<ILoggingEvent> logsList = listAppender.list;
        Assertions.assertThat(logsList.get(0).getFormattedMessage())
                .contains("begin")
                .contains(execution.getProcessInstanceId());

        assertEquals(Level.INFO, logsList.get(1)
                .getLevel());
        Assertions.assertThat(logsList.get(1).getFormattedMessage())
                .contains("end",execution.getProcessInstanceId(),"Nothing to do");

        verify(remService,never()).putContactsPlatine(any());

    }
}