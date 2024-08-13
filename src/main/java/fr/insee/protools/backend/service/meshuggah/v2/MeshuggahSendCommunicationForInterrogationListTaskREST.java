package fr.insee.protools.backend.service.meshuggah.v2;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.service.meshuggah.MeshuggahService;
import fr.insee.protools.backend.service.utils.FlowableVariableUtils;
import fr.insee.protools.backend.service.utils.delegate.DefaultCallServiceInterroListTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class MeshuggahSendCommunicationForInterrogationListTaskREST extends DefaultCallServiceInterroListTask {

    private final MeshuggahService meshuggahService;


    @Override
    protected void serviceAction(String campaignId, List<JsonNode> list, String... params) {
        if(params.length!=1){
            log.error("Wrong number of paramaters : {} - expected 1",params.length);
        }
        String mode=params[0];
        meshuggahService.sendCommunications(campaignId,mode,list);
    }

    @Override
    protected void callService(DelegateExecution execution, JsonNode contextRootNode, String campaignId, List<JsonNode> list) {
        //TODO : communicationMode
        String communicationMode = FlowableVariableUtils.getVariableOrThrow(execution,"communicationMode", String.class);
        serviceAction(campaignId, list,communicationMode);
    }
}

