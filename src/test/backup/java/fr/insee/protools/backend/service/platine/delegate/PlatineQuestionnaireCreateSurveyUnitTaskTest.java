package fr.insee.protools.backend.service.platine.delegate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.insee.protools.backend.ProtoolsTestUtils;
import fr.insee.protools.backend.deprecated.service.platine.delegate.PlatineQuestionnaireCreateSurveyUnitTask;
import fr.insee.protools.backend.dto.platine_sabiane_questionnaire.surveyunit.SurveyUnitResponseDto;
import fr.insee.protools.backend.service.context.exception.BadContextIncorrectBPMNError;
import fr.insee.protools.backend.service.exception.IncorrectSUBPMNError;
import fr.insee.protools.backend.deprecated.service.platine.questionnaire.PlatineQuestionnaireService;
import fr.insee.protools.backend.service.utils.TestWithContext;
import fr.insee.protools.backend.service.utils.data.RemSUData;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.util.ClassUtils;

import java.util.stream.Stream;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PlatineQuestionnaireCreateSurveyUnitTaskTest extends TestWithContext {

    @Mock
    PlatineQuestionnaireService platineQuestionnaireService;

    @InjectMocks
    PlatineQuestionnaireCreateSurveyUnitTask platineQuestionnaireCreateSurveyUnitTask;



    final static String ressourceFolder = ClassUtils.convertClassNameToResourcePath(PlatineQuestionnaireCreateSurveyUnitTask.class.getPackageName());
    final static String platine_context_json = ressourceFolder+"/protools-contexte-platine-individu.json";
    final static String platine_context_logement_json = ressourceFolder+"/protools-contexte-platine-individu.json";

    //To be able to run tests with differents protools contexts
    private static Stream<Arguments> protoolsContextArguments() {
        return Stream.of(
                Arguments.of(platine_context_json),
                Arguments.of(platine_context_logement_json)
        );
    }

    @Test
    void execute_should_throwError_when_null_context(){
        assertThat_delegate_throwError_when_null_context(platineQuestionnaireCreateSurveyUnitTask);
    }
    @Test
    void execute_should_throw_BadContextIncorrectException_when_noContext() {
        DelegateExecution execution=createMockedExecution();
        //Execute the unit under test
        assertThrows(BadContextIncorrectBPMNError.class,() -> platineQuestionnaireCreateSurveyUnitTask.execute(execution));
    }
    @ParameterizedTest
    @MethodSource("protoolsContextArguments")
    void execute_should_work_when_ContextOK(String context_json) throws JsonProcessingException {
        //Prepare
        DelegateExecution execution=createMockedExecution();
        JsonNode contextRootNode = initContexteMockWithFile(context_json);
        JsonNode remSU = ProtoolsTestUtils.asJsonNode(RemSUData.rem_su_3personnes);
        String idPartition="1";
        lenient().doReturn(idPartition).when(execution).getVariable(VARNAME_CURRENT_PARTITION_ID,String.class);
        lenient().doReturn(remSU).when(execution).getVariable(VARNAME_REM_INTERROGATION,JsonNode.class);
        lenient().doReturn("TOTO").when(execution).getVariable(VARNAME_DIRECTORYACCESS_ID_CONTACT,String.class);

        //Execute the unit under test
        platineQuestionnaireCreateSurveyUnitTask.execute(execution);

        //Post Conditions
        ObjectMapper objectMapper = new ObjectMapper();
        ArgumentCaptor<SurveyUnitResponseDto> acSUResDTO = ArgumentCaptor.forClass(SurveyUnitResponseDto.class);
        verify(platineQuestionnaireService,times(1)).postSurveyUnit(acSUResDTO.capture(),any());
        SurveyUnitResponseDto valueParam = acSUResDTO.getValue();
        assertEquals("3043280", valueParam.getId(),"Wrong ID SU (repository id)");
        assertEquals("tic2023a00_web", valueParam.getQuestionnaireId(),"Wrong questionnaireId");
        assertEquals(objectMapper.readTree("[{\"name\":\"whoAnswers1\",\"value\":\"Qui doit répondre paragraphe 1\"}," +
                        "{\"name\":\"whoAnswers2\",\"value\":\"Qui doit répondre paragraphe 2\"}," +
                        "{\"name\":\"whoAnswers3\",\"value\":\"Qui doit répondre paragraphe 3\"}]")
                , valueParam.getPersonalization(),"Wrong questionnaireId");
        assertEquals(objectMapper.readTree("{\"EXTERNAL\":{\"ADMINISTRATION1\":\"Insee\",\"ADMINISTRATION2\":\"Patate\"}}"),
                valueParam.getData(),"Wrong data (external)");
        assertEquals(objectMapper.createObjectNode(), valueParam.getComment(),"No comment expected");
        //assertEquals(objectMapper.createObjectNode(), valueParam.getStateData(),"No stateData expected");
    }

    @Test
    void execute_should_throw_IncorrectSUException_when_wrongSU() {
        DelegateExecution execution=createMockedExecution();
        initContexteMockWithFile(platine_context_json);
        JsonNode remSU = ProtoolsTestUtils.asJsonNode(RemSUData.rem_su_3personnes);
        //Break this node
        ((ObjectNode) remSU).remove("repositoryId");
        String idPartition="1";
        lenient().doReturn(idPartition).when(execution).getVariable(VARNAME_CURRENT_PARTITION_ID,String.class);
        lenient().doReturn(remSU).when(execution).getVariable(VARNAME_REM_INTERROGATION,JsonNode.class);
        lenient().doReturn("TOTO").when(execution).getVariable(VARNAME_DIRECTORYACCESS_ID_CONTACT,String.class);


        //Execute the unit under test
        assertThrows(IncorrectSUBPMNError.class,() -> platineQuestionnaireCreateSurveyUnitTask.execute(execution));
    }



}