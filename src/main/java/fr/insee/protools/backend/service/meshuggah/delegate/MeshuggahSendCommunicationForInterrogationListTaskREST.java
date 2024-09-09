package fr.insee.protools.backend.service.meshuggah.delegate;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.dto.ContexteProcessus;
import fr.insee.protools.backend.service.meshuggah.MeshuggahService;
import fr.insee.protools.backend.service.utils.FlowableVariableUtils;
import fr.insee.protools.backend.service.utils.delegate.DefaultCallServiceInterroListTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;

import java.util.List;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_CURRENT_COMMUNICATION_ID;

@Slf4j
@RequiredArgsConstructor
public class MeshuggahSendCommunicationForInterrogationListTaskREST extends DefaultCallServiceInterroListTask {

    private final MeshuggahService meshuggahService;


    @Override
    protected void serviceAction(ContexteProcessus context, List<JsonNode> list, String... params) {
        if(params.length!=1){
            log.error("Wrong number of paramaters : {} - expected 1",params.length);
        }
        String currentCommunicationId=params[0];
        meshuggahService.postCommunicationRequest(context.getId().toString(),currentCommunicationId,list);
    }

    @Override
    protected void callService(DelegateExecution execution, ContexteProcessus context, List<JsonNode> list) {
        String currentCommunicationId= FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_CURRENT_COMMUNICATION_ID, String.class);
        serviceAction(context, list,currentCommunicationId);
    }
}

