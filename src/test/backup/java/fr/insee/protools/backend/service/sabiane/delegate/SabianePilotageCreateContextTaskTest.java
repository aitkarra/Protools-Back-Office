package fr.insee.protools.backend.service.sabiane.delegate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.ProtoolsTestUtils;
import fr.insee.protools.backend.service.context.ContextService;
import fr.insee.protools.backend.service.context.exception.BadContextIncorrectBPMNError;
import fr.insee.protools.backend.service.sabiane.pilotage.SabianePilotageService;
import fr.insee.protools.backend.dto.sabiane.pilotage.CampaignContextDto;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FlowableTest
class SabianePilotageCreateContextTaskTest extends TestWithContext {
    final static String ressourceFolder = ClassUtils.convertClassNameToResourcePath(SabianePilotageCreateContextTaskTest.class.getPackageName());
    final static String sabiane_context_json = ressourceFolder+"/protools-contexte-sabiane.json";
    final static String sabiane_context_incorrect_json = ressourceFolder+"/protools-contexte-sabiane-incorrect.json";

    @Mock SabianePilotageService platinePilotageService;
    @Spy ObjectMapper objectMapper;

    @InjectMocks
    SabianePilotageCreateContextTask sabianePilotageTask;

    String dumyId="ID1";

    @Test
    void execute_should_throwError_when_null_context(){
        assertThat_delegate_throwError_when_null_context(sabianePilotageTask);
    }

    @Test
    void execute_should_throw_BadContextIncorrectException_when_noContext() {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getProcessInstanceId()).thenReturn(dumyId);

        //Execute the unit under test
        assertThrows(BadContextIncorrectBPMNError.class,() -> sabianePilotageTask.execute(execution));
    }

    @Test
    void execute_should_work_when_contextOK() {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getProcessInstanceId()).thenReturn(dumyId);
        initContexteMockWithFile(sabiane_context_json);

        //Execute the unit under test
        sabianePilotageTask.execute(execution);

        //Post conditions : Exactly one call to post platinePilotageService.putMetadata
        //                  it is one call per partition defined in context
//        verify(platinePilotageService,times(1)).putMetadata(eq(partitionId), notNull());


        //Verify postCampaign
        ArgumentCaptor<CampaignContextDto> acMetadataDto = ArgumentCaptor.forClass(CampaignContextDto.class);
        //Exactly one Call to the postCampaign method
        verify(platinePilotageService,times(1)).postCampaign(acMetadataDto.capture());
        List<CampaignContextDto> allValues = acMetadataDto.getAllValues();
        assertEquals(1, allValues.size(),"We should have exactly one call to postCampaign");

        //Simple (redundant) verification of the campaignId
        assertEquals("MBG2022X01",allValues.get(0).getCampaign(),"Erreur with generated campaign id");
        //Verification that the sent json is exactly what we expected
        CampaignContextDto expectedCampaignDto = ProtoolsTestUtils.asObject(ressourceFolder + "/pilotage_expected_post_campaign.json", CampaignContextDto.class);
        assertEquals(expectedCampaignDto,allValues.get(0));

    }

}
