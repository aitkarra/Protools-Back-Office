package fr.insee.protools.backend.service.sabiane.delegate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.ProtoolsTestUtils;
import fr.insee.protools.backend.service.context.ContextConstants;
import fr.insee.protools.backend.service.context.exception.BadContextIncorrectBPMNError;
import fr.insee.protools.backend.deprecated.service.nomenclature.NomenclatureService;
import fr.insee.protools.backend.deprecated.service.questionnaire_model.QuestionnaireModelService;
import fr.insee.protools.backend.service.sabiane.questionnaire.SabianeQuestionnaireService;
import fr.insee.protools.backend.service.utils.TestWithContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.test.FlowableTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fr.insee.protools.backend.service.context.ContextConstants.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@FlowableTest
public
class SabianeQuestionnaireCreateContextTaskTest extends TestWithContext {


    final static String ressourceFolder = ClassUtils.convertClassNameToResourcePath(SabianeQuestionnaireCreateContextTaskTest.class.getPackageName());
    final static String sabiane_context_json = ressourceFolder+"/protools-contexte-sabiane.json";
    final static String sabiane_context_incorrect_json = ressourceFolder+"/protools-contexte-sabiane-incorrect.json";

    @Mock SabianeQuestionnaireService sabianeQuestionnaireService;
    @Mock QuestionnaireModelService questionnaireModelService;
    @Mock NomenclatureService nomenclatureService;
    @Spy ObjectMapper objectMapper;

    @InjectMocks
    SabianeQuestionnaireCreateContextTask sabianeQuestTask;

    private final String questionnaireContent1 ="{\"id\": \"TOTO\" , \"toto\": 55 }";

    @Test
    void execute_should_throwError_when_null_context(){
        assertThat_delegate_throwError_when_null_context(sabianeQuestTask);
    }

    @Test
    void execute_should_throwBadContextIncorrect_when_contextIsKO() {
        // preconditions
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getProcessInstanceId()).thenReturn("1");
        initContexteMockWithFile(sabiane_context_incorrect_json);

        //Execute the unit under test
        assertThrows(BadContextIncorrectBPMNError.class, () -> sabianeQuestTask.execute(execution));

        // postconditions
        //assertThat(execution.getVariable("myVariable")).isEqualTo("myValue");

    }

    /*@Test
    void initQuestionnaireModels_should_produce_correct_json(){
        sabianeQuestTask.
    }*/

    @Test
    void execute_should_work_and_make_correct_calls() throws IOException {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getProcessInstanceId()).thenReturn("1");
        JsonNode contextRootNode = initContexteMockWithFile(sabiane_context_json);
        assertEquals(2,contextRootNode.path(ContextConstants.CTX_QUESTIONNAIRE_MODELS).size(), "Context error : expected exactly 2 questionnaire model");

        //Prepare the list of existing nomenclatures ("L_NATIONETR-1-1-0" does not exists yet)
        when(sabianeQuestionnaireService.getNomenclaturesId()).thenReturn(Set.of("L_DEPNAIS-1-1-0","L_PAYSNAIS-1-1-0","L_COMMUNEPASSEE-1-1-0"));
        //Mock the read nomenclature
        String nomenclatureContent="{\"id\": \"nomenclatureContent\"}";

        when(nomenclatureService.getNomenclatureContent("L_NATIONETR-1-1-0","NATIONETR")).thenReturn(nomenclatureContent);
        //Mock questionnaire
        when(sabianeQuestionnaireService.questionnaireModelExists(anyString())).thenReturn(false);
        when(questionnaireModelService.getQuestionnaireModel(anyString(), anyString())).thenReturn(questionnaireContent1);


        //Execute the unit under test
        assertThatCode(() -> sabianeQuestTask.execute(execution)).doesNotThrowAnyException();

        //Verifications on nomenclatures
        verify(sabianeQuestionnaireService,atLeastOnce()).getNomenclaturesId();
        verify(sabianeQuestionnaireService).postNomenclature(
                "L_NATIONETR-1-1-0",
                "liste des nationalités",
                objectMapper.readTree(nomenclatureContent));

        //Verifications on questionnaires (2 questionnaires models to create)
        verify(sabianeQuestionnaireService,times(2)).postQuestionnaireModel(any(),any(),any(),any());
        for(int i =0; i<2; i++){
            String idQuestionnaireModel = contextRootNode.path(ContextConstants.CTX_QUESTIONNAIRE_MODELS).get(i).path(CTX_QUESTIONNAIRE_MODEL_ID).asText();
            String labelQuestionnaireModel = contextRootNode.path(ContextConstants.CTX_QUESTIONNAIRE_MODELS).get(i).path(CTX_QUESTIONNAIRE_MODEL_LABEL).asText();
            String repertoireQuestionnaireModel = contextRootNode.path(ContextConstants.CTX_QUESTIONNAIRE_MODELS).get(i).path(CTX_QUESTIONNAIRE_MODEL_CHEMIN_REPERTOIRE).asText();
            Set<String> nomenclatures = new HashSet<>();
            var iter=contextRootNode.path(ContextConstants.CTX_QUESTIONNAIRE_MODELS).get(i).path(CTX_QUESTIONNAIRE_MODEL_REQUIRED_NOMENCLATURES).elements();
            while(iter.hasNext()){
                var nomenclature=iter.next().asText();
                nomenclatures.add(nomenclature);
            }

            verify(sabianeQuestionnaireService,times(1)).questionnaireModelExists(idQuestionnaireModel);
            verify(questionnaireModelService,times(1)).getQuestionnaireModel(idQuestionnaireModel,repertoireQuestionnaireModel);
            verify(sabianeQuestionnaireService).postQuestionnaireModel(idQuestionnaireModel,labelQuestionnaireModel,
                    objectMapper.readTree(questionnaireContent1),nomenclatures);
        }

        //Verify postCampaign
        ArgumentCaptor<JsonNode> acContext = ArgumentCaptor.forClass(JsonNode.class);
        ArgumentCaptor<String> acCampaignId = ArgumentCaptor.forClass(String.class);
        verify(sabianeQuestionnaireService,times(1)).postContext(acCampaignId.capture(),acContext.capture());

        List<JsonNode> allCtxValues = acContext.getAllValues();
        assertEquals(1, allCtxValues.size(),"We should have exactly one campaign");

        JsonNode expectedCtx = ProtoolsTestUtils.asJsonNode(sabiane_context_json);
        assertEquals(expectedCtx,allCtxValues.get(0),"Error with the passed json context");
    }
}
