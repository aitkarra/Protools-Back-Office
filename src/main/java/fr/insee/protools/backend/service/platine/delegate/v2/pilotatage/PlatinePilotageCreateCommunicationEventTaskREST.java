package fr.insee.protools.backend.service.platine.delegate.v2.pilotatage;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.dto.platine.pilotage.v2.PlatinePilotageCommunicationEventDto;
import fr.insee.protools.backend.service.DelegateContextVerifier;
import fr.insee.protools.backend.service.context.ContextService;
import fr.insee.protools.backend.service.platine.pilotage.PlatinePilotageService;
import fr.insee.protools.backend.service.utils.FlowableVariableUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import javax.naming.Context;
import java.util.List;
import java.util.Map;

import static fr.insee.protools.backend.dto.platine.pilotage.v2.PlatinePilotageCommunicationEventType.COMMUNICATION_STATE_SENT;
import static fr.insee.protools.backend.service.FlowableVariableNameConstants.*;
import static fr.insee.protools.backend.service.utils.ContextUtils.getPartitionNodeIfExists;

@Slf4j
@RequiredArgsConstructor
@Component
public class PlatinePilotageCreateCommunicationEventTaskREST implements JavaDelegate, DelegateContextVerifier {

    private final PlatinePilotageService platinePilotageService;
    private final ContextService protoolsContext;

    @Override
    public void execute(DelegateExecution execution) {
        String currentCommunicationId= FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_CURRENT_COMMUNICATION_ID, String.class);
        String currentPartitionId = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_CURRENT_PARTITION_ID, String.class);

        log.info("ProcessInstanceId={}  currentPartitionId={} - currentCommunicationId{} - begin",
                execution.getProcessInstanceId(),currentPartitionId, currentCommunicationId);
        List<JsonNode> contentList = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_REM_INTERRO_LIST, List.class);
        Map<String,String> communicationRequestIdByInterroIdMap = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_COMMUNICATION_REQUEST_ID_FOR_INTERRO_ID_MAP, Map.class);

          //TODO: put it at a single place ==> Maybe an helper to get the Id of an interro or of any json?
        String jsonKeyId = "id";

        List<PlatinePilotageCommunicationEventDto> platinePilotageCommunicationEventList = contentList.stream()
            .map(jsonNode -> {
                String interroId = jsonNode.path(jsonKeyId).asText();
                String communicationRequestId=communicationRequestIdByInterroIdMap.get(interroId);
                return PlatinePilotageCommunicationEventDto.builder()
                        .communcationId(currentCommunicationId)
                        .communicationRequestId(communicationRequestId)
                        .interrogationId(interroId).state(COMMUNICATION_STATE_SENT).build();
            }).toList();

        platinePilotageService.postCommunicationEvent(platinePilotageCommunicationEventList);

        log.info("ProcessInstanceId={}  end",execution.getProcessInstanceId());

    }
}