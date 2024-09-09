package fr.insee.protools.backend.service.platine.delegate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.deprecated.service.platine.delegate.PlatinePilotageGetSUContactTask;
import fr.insee.protools.backend.service.context.exception.BadContextIncorrectBPMNError;
import fr.insee.protools.backend.deprecated.service.platine.pilotage.PlatinePilotageService;
import fr.insee.protools.backend.dto.platine.pilotage.contact.PlatineContactDto;
import fr.insee.protools.backend.deprecated.service.platine.utils.PlatineHelper;
import fr.insee.protools.backend.service.rem.delegate.ExtractContactIdentifierFromREMSUTask;
import fr.insee.protools.backend.service.utils.TestWithContext;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.util.ClassUtils;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.*;
import static fr.insee.protools.backend.service.context.ContextConstants.CTX_CAMPAGNE_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PlatinePilotageGetSUContactTaskTest extends TestWithContext {
    final static String ressourceFolder = ClassUtils.convertClassNameToResourcePath(PlatinePilotageGetSUContactTaskTest.class.getPackageName());
    final static String platine_context_json = ressourceFolder + "/protools-contexte-platine-individu.json";
    final static String platine_context_incorrect_json = ressourceFolder + "/protools-contexte-platine-incorrect-no-campaign-id.json";

    @Mock PlatinePilotageService platinePilotageService;
    @InjectMocks
    PlatinePilotageGetSUContactTask platinePilotageTask;

    @Test
    void execute_should_throwError_when_null_context(){
        assertThat_delegate_throwError_when_null_context(platinePilotageTask);
    }

    @Test
    void execute_should_throw_BadContextIncorrectException_when_noContext() {
        DelegateExecution execution = createMockedExecution();
        //Execute the unit under test
        assertThrows(BadContextIncorrectBPMNError.class, () -> platinePilotageTask.execute(execution));
    }

    @Test
    void execute_should_throw_BadContextIncorrectException_when_CampaignId_missing() {
        DelegateExecution execution = createMockedExecution();
        initContexteMockWithFile(platine_context_incorrect_json);

        //Execute the unit under test
        assertThrows(BadContextIncorrectBPMNError.class, () -> platinePilotageTask.execute(execution));
    }

    @Test
    @DisplayName("PlatinePilotageGetSUContactTaskTest should throw an exception when the VARNAME_REM_SURVEY_UNIT_IDENTIFIER variable is missing or has wrong type")
    void execute_should_throw_when_variable1_missing() {
        //Prepare
        DelegateExecution execution = createMockedExecution();
        ExtractContactIdentifierFromREMSUTask extractTask = new ExtractContactIdentifierFromREMSUTask();
        //set the 2nd variable correctly
        lenient().doReturn("999").when(execution).getVariable(VARNAME_CURRENT_PARTITION_ID, String.class);


        //No variable set ==> Error
        assertThrows(FlowableIllegalArgumentException.class, () -> extractTask.execute(execution));

        //Wrong Type ==> Error
        lenient().doReturn(Boolean.FALSE).when(execution).getVariable(VARNAME_REM_SURVEY_UNIT_IDENTIFIER, Boolean.class);
        assertThrows(FlowableIllegalArgumentException.class, () -> extractTask.execute(execution));
    }

    @Test
    @DisplayName("PlatinePilotageGetSUContactTaskTest should throw an exception when the VARNAME_CURRENT_PARTITION_ID variable is missing or has wrong type")
    void execute_should_throw_when_variable2_missing() {
        //Prepare
        DelegateExecution execution = createMockedExecution();
        ExtractContactIdentifierFromREMSUTask extractTask = new ExtractContactIdentifierFromREMSUTask();
        //set the 2nd variable correctly
        lenient().doReturn(12L).when(execution).getVariable(VARNAME_REM_SURVEY_UNIT_IDENTIFIER, Long.class);


        //No variable set ==> Error
        assertThrows(FlowableIllegalArgumentException.class, () -> extractTask.execute(execution));

        //Wrong Type ==> Error
        lenient().doReturn(Boolean.FALSE).when(execution).getVariable(VARNAME_CURRENT_PARTITION_ID, Boolean.class);
        assertThrows(FlowableIllegalArgumentException.class, () -> extractTask.execute(execution));
    }

    @Test
    @DisplayName("PlatinePilotageGetSUContactTaskTest should throw an exception when the VARNAME_CURRENT_PARTITION_ID and VARNAME_CURRENT_PARTITION_ID variable are missing or have wrong type")
    void execute_should_throw_when_variable1and2_missing() {
        //Prepare
        DelegateExecution execution = createMockedExecution();
        ExtractContactIdentifierFromREMSUTask extractTask = new ExtractContactIdentifierFromREMSUTask();

        //No variable set ==> Error
        assertThrows(FlowableIllegalArgumentException.class, () -> extractTask.execute(execution));

        //Wrong Type ==> Error
        lenient().doReturn(Boolean.FALSE).when(execution).getVariable(VARNAME_CURRENT_PARTITION_ID, Boolean.class);
        lenient().doReturn(Boolean.TRUE).when(execution).getVariable(VARNAME_REM_SURVEY_UNIT_IDENTIFIER, Boolean.class);


        assertThrows(FlowableIllegalArgumentException.class, () -> extractTask.execute(execution));
    }


    @Test
    void execute_should_work_when_context_and_variables_OK() throws JsonProcessingException {
        DelegateExecution execution = createMockedExecution();
        JsonNode contextRootNode = initContexteMockWithFile(platine_context_json);
        //Prepare parent
        DelegateExecution executionParent = mock(DelegateExecution.class);
        doReturn(executionParent).when(execution).getParent();
        //Process variables
        Long suId = 20231110L;
        String currentPartitionID = "120883";
        lenient().doReturn(currentPartitionID).when(execution).getVariable(VARNAME_CURRENT_PARTITION_ID, String.class);
        lenient().doReturn(suId).when(execution).getVariable(VARNAME_REM_SURVEY_UNIT_IDENTIFIER, Long.class);

        //Prepare service response from platine
        PlatineContactDto contactDto = PlatineContactDto.builder().identifier("55").build();
        doReturn(contactDto).when(platinePilotageService).getSUMainContact(any(),any());


        //Execute the unit under test
        assertDoesNotThrow(() -> platinePilotageTask.execute(execution));

        //Post conditions :
        // 1 : Exactly one call to post platinePilotageService.getSUMainContact with correct parameters
        String platinePartitionId = PlatineHelper.computePilotagePartitionID(contextRootNode.path(CTX_CAMPAGNE_ID).asText(), currentPartitionID);
        verify(platinePilotageService, times(1)).getSUMainContact(suId, platinePartitionId);
        //2 : VARNAME_PLATINE_CONTACT has been set to correct value
        ArgumentCaptor<JsonNode> acVariableValue = ArgumentCaptor.forClass(JsonNode.class);
        ArgumentCaptor<String> acVariableKey = ArgumentCaptor.forClass(String.class);

        verify(executionParent, times(1)).setVariableLocal(acVariableKey.capture(),acVariableValue.capture());
        assertEquals(VARNAME_PLATINE_CONTACT, acVariableKey.getValue(),"variable VARNAME_PLATINE_CONTACT not set");
        PlatineContactDto argumentDto = (new ObjectMapper()).treeToValue(acVariableValue.getValue(),PlatineContactDto.class);
        assertEquals("55", argumentDto.getIdentifier(),"Bad Object retrieved");

    }

}