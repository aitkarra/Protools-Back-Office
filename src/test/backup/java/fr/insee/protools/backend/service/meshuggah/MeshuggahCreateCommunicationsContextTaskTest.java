package fr.insee.protools.backend.service.meshuggah;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.ProtoolsTestUtils;
import fr.insee.protools.backend.deprecated.service.meshuggah.MeshuggahCreateCommunicationsContextTask;
import fr.insee.protools.backend.deprecated.service.meshuggah.MeshuggahService;
import fr.insee.protools.backend.service.context.exception.BadContextIncorrectBPMNError;
import fr.insee.protools.backend.dto.meshuggah.MeshuggahComDetails;
import fr.insee.protools.backend.service.utils.TestWithContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;


class MeshuggahCreateCommunicationsContextTaskTest extends TestWithContext {

    @Mock
    MeshuggahService meshuggahService;
    @InjectMocks
    MeshuggahCreateCommunicationsContextTask meshuggahTask;

    //To be able to run tests with differents protools contexts
    private static Stream<Arguments> contextErrorArguments() {
        return Stream.of(
                Arguments.of(MeshuggahCtxExamples.ctx_ERROR_2partitions_2com_1_typo_phase),
                Arguments.of(MeshuggahCtxExamples.ctx_ERROR_no_part),
                Arguments.of(MeshuggahCtxExamples.ctx_ERROR_no_com),
                Arguments.of(MeshuggahCtxExamples.ctx_ERROR_no_moyen),
                Arguments.of(MeshuggahCtxExamples.ctx_ERROR_typo_phase),
                Arguments.of(MeshuggahCtxExamples.ctx_ERROR_1partitions_2com_1_typo_moyen),
                Arguments.of(MeshuggahCtxExamples.ctx_ERROR_typo_moyen));
    }

    @Test
    void execute_should_throwError_when_null_context(){
        assertThat_delegate_throwError_when_null_context(meshuggahTask);
    }

    @ParameterizedTest
    @MethodSource("contextErrorArguments")
    @DisplayName("Test execute method - should throw if Context is not correct")
    void execute_should_throw_BadContext_when_contextIncorrect(String context_json) throws JsonProcessingException {
        //Precondition
        DelegateExecution execution = mock(DelegateExecution.class);
        lenient().when(execution.getProcessInstanceId()).thenReturn(dumyId);

        //Precondition
        ProtoolsTestUtils.initContexteMockFromString(protoolsContext, context_json);
        //Run test
        assertThrows(BadContextIncorrectBPMNError.class, () -> meshuggahTask.execute(execution));
        Mockito.reset(protoolsContext);
    }


    @Test
    @DisplayName("Test execute method - should work and make correct call to service when context has one partition")
    void execute_should_work_when_ctx_1_part_ouverture_courrier() {
        //Precondition
        DelegateExecution execution = mock(DelegateExecution.class);
        lenient().when(execution.getProcessInstanceId()).thenReturn(dumyId);
        initContexteMockWithString(MeshuggahCtxExamples.ctx_OK_1part_ouverture_courrier);

        //Run method under test
        assertDoesNotThrow(() -> meshuggahTask.execute(execution));

        //Post conditions
        MeshuggahComDetails expectedDetails = MeshuggahComDetails.builder()
                .campaignId("AAC2023A00")
                .phase("OUVERTURE")
                .medium("COURRIER")
                .mode("WEB")
                .partitioningId(MeshuggahCtxExamples.ctx_partition1.toString())
                .avecQuestionnaire(false)
                .protocol("DEFAULT")
                .operation("RELANCE_LIBRE")
                .build();
        ArgumentCaptor<MeshuggahComDetails> acDetails = ArgumentCaptor.forClass(MeshuggahComDetails.class);
        ArgumentCaptor<JsonNode> acBody = ArgumentCaptor.forClass(JsonNode.class);
        verify(meshuggahService,times(1)).postCreateCommunication(acDetails.capture(),acBody.capture());
        MeshuggahComDetails details = acDetails.getValue();
        assertEquals(expectedDetails,details);


        JsonNode actualBody = acBody.getValue();
        assertEquals(MeshuggahCtxExamples.ctx_complementConnexion,actualBody.path("Enq_ComplementConnexion").asText());
        assertEquals(MeshuggahCtxExamples.ctx_logoPrestataire,actualBody.path("Enq_LogoPrestataire").asText());
        assertEquals(MeshuggahCtxExamples.ctx_mailResponsableOperationnel,actualBody.path("Enq_MailRespOperationnel").asText());
        assertEquals(MeshuggahCtxExamples.ctx_prestataire,actualBody.path("Enq_Prestataire").asBoolean());
        assertEquals(MeshuggahCtxExamples.ctx_relanceLibreMailParagraphe1,actualBody.path("Enq_RelanceLibreMailParagraphe1").asText());
        assertEquals(MeshuggahCtxExamples.ctx_relanceLibreMailParagraphe2,actualBody.path("Enq_RelanceLibreMailParagraphe2").asText());
        assertEquals(MeshuggahCtxExamples.ctx_relanceLibreMailParagraphe3,actualBody.path("Enq_RelanceLibreMailParagraphe3").asText());
        assertEquals(MeshuggahCtxExamples.ctx_relanceLibreMailParagraphe4,actualBody.path("Enq_RelanceLibreMailParagraphe4").asText());
        assertEquals(MeshuggahCtxExamples.ctx_responsableOperationnel,actualBody.path("Enq_RespOperationnel").asText());
        assertEquals(MeshuggahCtxExamples.ctx_responsableTraitement,actualBody.path("Enq_RespTraitement").asText());
        assertEquals(MeshuggahCtxExamples.ctx_serviceCollecteurSignataireFonction,actualBody.path("Enq_ServiceCollecteurSignataireFonction").asText());
        assertEquals(MeshuggahCtxExamples.ctx_serviceCollecteurSignataireNom,actualBody.path("Enq_ServiceCollecteurSignataireNom").asText());
        assertEquals(MeshuggahCtxExamples.ctx_themeMieuxConnaitreMail,actualBody.path("Enq_ThemeMieuxConnaitreMail").asText());
        assertEquals(MeshuggahCtxExamples.ctx_urlEnquete,actualBody.path("Enq_UrlEnquete").asText());
        assertEquals(MeshuggahCtxExamples.ctx_boiteRetour,actualBody.path("Mail_BoiteRetour").asText());
        assertEquals(MeshuggahCtxExamples.ctx_objetMail,actualBody.path("Mail_Objet").asText());
    }


    @Test
    @DisplayName("Test execute method - should work and make correct call to service when context has one partition")
    void execute_should_work_when_ctx_2_part_ouverture_courrier_relance_mail() {
        //Precondition
        DelegateExecution execution = mock(DelegateExecution.class);
        lenient().when(execution.getProcessInstanceId()).thenReturn(dumyId);
        initContexteMockWithString(MeshuggahCtxExamples.ctx_OK_2partitions_2com_ouverture_relance_ok);

        //Run method under test
        assertDoesNotThrow(() -> meshuggahTask.execute(execution));

        //Post conditions
        MeshuggahComDetails expectedDetailsOuverture = MeshuggahComDetails.builder()
                .campaignId("AAC2023A00")
                .phase("OUVERTURE")
                .medium("COURRIER")
                .mode("WEB")
                .partitioningId("to be set")
                .avecQuestionnaire(false)
                .protocol("DEFAULT")
                .operation("RELANCE_LIBRE")
                .build();
        MeshuggahComDetails expectedDetailsRelance = MeshuggahComDetails.builder()
                .campaignId("AAC2023A00")
                .phase("RELANCE")
                .medium("EMAIL")
                .mode("WEB")
                .partitioningId("to be set")
                .avecQuestionnaire(false)
                .protocol("DEFAULT")
                .operation("RELANCE_LIBRE")
                .build();
        ArgumentCaptor<MeshuggahComDetails> acDetails = ArgumentCaptor.forClass(MeshuggahComDetails.class);
        ArgumentCaptor<JsonNode> acBody = ArgumentCaptor.forClass(JsonNode.class);
        verify(meshuggahService,times(4)).postCreateCommunication(acDetails.capture(),acBody.capture());

        List<MeshuggahComDetails> actualDetailsList = acDetails.getAllValues();
        List<JsonNode> actualBodiesList = acBody.getAllValues();

        int nbOuverturePart1=0;
        int nbOuverturePart2=0;
        int nbRelancePart1=0;
        int nbRelancePart2=0;
        for(int i=0; i<4;i++){
            MeshuggahComDetails actualDetails = actualDetailsList.get(i);

            if(actualDetails.getPartitioningId().equals(MeshuggahCtxExamples.ctx_partition1.toString())){
                if(actualDetails.getPhase().equals("OUVERTURE")){
                    expectedDetailsOuverture.setPartitioningId(MeshuggahCtxExamples.ctx_partition1.toString());
                    assertEquals(expectedDetailsOuverture,actualDetails);
                    nbOuverturePart1++;
                }
                else if(actualDetails.getPhase().equals("RELANCE")){
                    expectedDetailsRelance.setPartitioningId(MeshuggahCtxExamples.ctx_partition1.toString());
                    assertEquals(expectedDetailsRelance,actualDetails);
                    nbRelancePart1++;
                }
            }
            else if(actualDetails.getPartitioningId().equals(MeshuggahCtxExamples.ctx_partition2.toString())){
                if(actualDetails.getPhase().equals("OUVERTURE")){
                    expectedDetailsOuverture.setPartitioningId(MeshuggahCtxExamples.ctx_partition2.toString());
                    assertEquals(expectedDetailsOuverture,actualDetails);
                    nbOuverturePart2++;
                }
                else if(actualDetails.getPhase().equals("RELANCE")){
                    expectedDetailsRelance.setPartitioningId(MeshuggahCtxExamples.ctx_partition2.toString());
                    assertEquals(expectedDetailsRelance,actualDetails);
                    nbRelancePart2++;
                }
            }
            else {
                fail("Incorect test definition (nb partition)");
            }


            JsonNode actualBody = actualBodiesList.get(i);
            assertEquals(MeshuggahCtxExamples.ctx_complementConnexion,actualBody.path("Enq_ComplementConnexion").asText());
            assertEquals(MeshuggahCtxExamples.ctx_logoPrestataire,actualBody.path("Enq_LogoPrestataire").asText());
            assertEquals(MeshuggahCtxExamples.ctx_mailResponsableOperationnel,actualBody.path("Enq_MailRespOperationnel").asText());
            assertEquals(MeshuggahCtxExamples.ctx_prestataire,actualBody.path("Enq_Prestataire").asBoolean());

            //we have various values for this fiels
            if(actualDetails.getPartitioningId().equals(MeshuggahCtxExamples.ctx_partition2.toString())){
               if(actualDetails.getPhase().equals("OUVERTURE")) {
                   assertEquals(MeshuggahCtxExamples.ctx_relanceLibreMailParagraphe1_partition2_com1, actualBody.path("Enq_RelanceLibreMailParagraphe1").asText());
               }
                else if(actualDetails.getPhase().equals("RELANCE")) {
                    assertEquals(MeshuggahCtxExamples.ctx_relanceLibreMailParagraphe1_partition2_com2, actualBody.path("Enq_RelanceLibreMailParagraphe1").asText());
                }
                else{
                   fail("Incorect test definition");
                }
            }
            else {
                assertEquals(MeshuggahCtxExamples.ctx_relanceLibreMailParagraphe1, actualBody.path("Enq_RelanceLibreMailParagraphe1").asText());
            }
            assertEquals(MeshuggahCtxExamples.ctx_relanceLibreMailParagraphe2,actualBody.path("Enq_RelanceLibreMailParagraphe2").asText());
            assertEquals(MeshuggahCtxExamples.ctx_relanceLibreMailParagraphe3,actualBody.path("Enq_RelanceLibreMailParagraphe3").asText());
            assertEquals(MeshuggahCtxExamples.ctx_relanceLibreMailParagraphe4,actualBody.path("Enq_RelanceLibreMailParagraphe4").asText());
            assertEquals(MeshuggahCtxExamples.ctx_responsableOperationnel,actualBody.path("Enq_RespOperationnel").asText());
            assertEquals(MeshuggahCtxExamples.ctx_responsableTraitement,actualBody.path("Enq_RespTraitement").asText());
            assertEquals(MeshuggahCtxExamples.ctx_serviceCollecteurSignataireFonction,actualBody.path("Enq_ServiceCollecteurSignataireFonction").asText());
            assertEquals(MeshuggahCtxExamples.ctx_serviceCollecteurSignataireNom,actualBody.path("Enq_ServiceCollecteurSignataireNom").asText());
            assertEquals(MeshuggahCtxExamples.ctx_themeMieuxConnaitreMail,actualBody.path("Enq_ThemeMieuxConnaitreMail").asText());
            assertEquals(MeshuggahCtxExamples.ctx_urlEnquete,actualBody.path("Enq_UrlEnquete").asText());
            assertEquals(MeshuggahCtxExamples.ctx_boiteRetour,actualBody.path("Mail_BoiteRetour").asText());
            assertEquals(MeshuggahCtxExamples.ctx_objetMail,actualBody.path("Mail_Objet").asText());
        }
        assertEquals(1,nbOuverturePart1,"Wrong number of opening for partition 1");
        assertEquals(1,nbOuverturePart2,"Wrong number of opening for partition 2");
        assertEquals(1,nbRelancePart1,"Wrong number of relance for partition 1");
        assertEquals(1,nbRelancePart2,"Wrong number of relance for partition 2");

    }

}