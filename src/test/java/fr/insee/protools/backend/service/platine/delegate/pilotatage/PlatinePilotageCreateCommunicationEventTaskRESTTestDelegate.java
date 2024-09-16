package fr.insee.protools.backend.service.platine.delegate.pilotatage;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.dto.platine.pilotage.PlatinePilotageCommunicationEventDto;
import fr.insee.protools.backend.exception.ProtoolsProcessFlowBPMNError;
import fr.insee.protools.backend.service.platine.service.PlatinePilotageService;
import fr.insee.protools.backend.service.utils.delegate.IDelegateWithVariables;
import fr.insee.protools.backend.utils.data.InterroExamples;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class PlatinePilotageCreateCommunicationEventTaskRESTTestDelegate implements IDelegateWithVariables {

    @Mock
    PlatinePilotageService platinePilotageService;

    @InjectMocks
    PlatinePilotageCreateCommunicationEventTaskREST task;


    @Override
    public JavaDelegate getTaskUnderTest() {
        return task;
    }

    @Override
    public Map<String, Class> getVariablesAndTypes() {
        return Map.of(
                VARNAME_CURRENT_COMMUNICATION_ID, String.class,
                VARNAME_CURRENT_PARTITION_ID, String.class,
                VARNAME_COMMUNICATION_REQUEST_ID_FOR_INTERRO_ID_MAP, Map.class
        );
    }

    static Stream<Arguments> initExecuteParametersWarn() {
        return Stream.of(
                Arguments.of(List.of(InterroExamples.generateEmptyInterro().interro()), Map.of(), "differentComId")//The interro is unknown
        );
        /*
        Logger fooLogger = (Logger) LoggerFactory.getLogger(PlatinePilotageCreateCommunicationEventTaskREST.class);
        // create and start a ListAppender
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
                // call method under test
        Foo foo = new Foo();
        foo.doThat();

        // JUnit assertions
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("start", logsList.get(0)
                                      .getMessage());
        assertEquals(Level.INFO, logsList.get(0)
                                         .getLevel());

        assertEquals("finish", logsList.get(1)
                                       .getMessage());
        assertEquals(Level.INFO, logsList.get(1)
                                         .getLevel());
         */
    }

    static Stream<Arguments> initExecuteParametersOK() {
        return Stream.of(
                Arguments.of(Map.of(), "comId", Boolean.FALSE),
                Arguments.of(Map.of("interroId1", "commReqId1"), "xxx", Boolean.TRUE),
                Arguments.of(Map.of("interroId1", "commReqId1", "interroId2", "commReqId2", "interroId3", "commReqId3"), "xxx", Boolean.TRUE)
        );
    }

    @ParameterizedTest
    @MethodSource("initExecuteParametersOK")
    @DisplayName("Test execute method - should work and make correct call to service")
    void execute_should_work_whenEverythingOk(Map<String, String> commIdByInterro, String communcationId, Boolean expectedCallToService) {
        //Prepare
        DelegateExecution execution = createMockedExecution();
        doReturn(communcationId).when(execution).getVariable(VARNAME_CURRENT_COMMUNICATION_ID, String.class);
        doReturn(dumyId).when(execution).getVariable(VARNAME_CURRENT_PARTITION_ID, String.class);
        doReturn(commIdByInterro).when(execution).getVariable(VARNAME_COMMUNICATION_REQUEST_ID_FOR_INTERRO_ID_MAP, Map.class);

        //Execute method under test
        task.execute(execution);

        //verify

        //Case where the service is not supposed to be called
        if (!expectedCallToService) {
            verify(platinePilotageService, never()).postCommunicationEvents(any());
            return;
        }


        final ArgumentCaptor<List<PlatinePilotageCommunicationEventDto>> listCaptor
                = ArgumentCaptor.forClass((Class) List.class);

        verify(platinePilotageService, times(1)).postCommunicationEvents(listCaptor.capture());
        List<List<PlatinePilotageCommunicationEventDto>> allValues = listCaptor.getAllValues();
        assertEquals(1, allValues.size(), "We should have exactly one value");

        List<PlatinePilotageCommunicationEventDto> actualArgument = listCaptor.getValue();
        assertEquals(commIdByInterro.size(), actualArgument.size(), "The passed list of events has an incorrect size");

        //check that we passed the correct items
        Set<String> requiredInterroIds = new HashSet<>(commIdByInterro.keySet());
        for (var comEventDto : actualArgument) {
            String idInterro = comEventDto.getInterrogationId();
            if (requiredInterroIds.contains(idInterro)) {
                //ComRequestId is correct
                String expectedCommId = commIdByInterro.get(idInterro);
                String actualCommId = comEventDto.getCommunicationRequestId();
                assertEquals(expectedCommId, actualCommId,
                        "Communcation Request ID is not the one expected for idInterro=" + idInterro + " - ");

                assertEquals(communcationId,comEventDto.getCommuncationId());
                //CommId is
                requiredInterroIds.remove(idInterro); //this id has correctly been sent
            }
        }
    }

    @Test
    @DisplayName("Test execute method - should throw ProtoolsProcessFlowBPMNError when communication Id is null")
    public void execute_should_throw_when_varCommId_is_blank() {
        String commId="";
        //Prepare
        DelegateExecution execution = createMockedExecution();
        doReturn(commId).when(execution).getVariable(VARNAME_CURRENT_COMMUNICATION_ID, String.class);
        doReturn(dumyId).when(execution).getVariable(VARNAME_CURRENT_PARTITION_ID, String.class);
        doReturn(Map.of("interroId1", "commReqId1")).when(execution).getVariable(VARNAME_COMMUNICATION_REQUEST_ID_FOR_INTERRO_ID_MAP, Map.class);

        //Execute method under test
        assertThrows(ProtoolsProcessFlowBPMNError.class, () -> task.execute(execution));
    }
}