package fr.insee.protools.backend.service.platine.delegate.v2;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.dto.internal.ProtoolsInterrogationDto;
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

import java.util.List;
import java.util.Map;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.*;

@Slf4j
@RequiredArgsConstructor
@Component
public class PlatinePilotageCreateCommunicationEventREST implements JavaDelegate, DelegateContextVerifier {

    private final PlatinePilotageService platinePilotageService;

    @Override
    public void execute(DelegateExecution execution) {
        log.info("ProcessInstanceId={}  begin",execution.getProcessInstanceId());
        List<JsonNode> contentList = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_REM_INTERRO_LIST, List.class);
        Map<String,String> communicationRequestIdByInterroIdMap = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_COMMUNICATION_REQUEST_ID_FOR_INTERRO_ID_MAP, Map.class);
        String currentCommunicationId= FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_CURRENT_COMMUNICATION_ID, String.class);

        //TODO: put it at a single place
        String jsonKeyId = "id";
        String state="XXXXX";

        List<PlatinePilotageCommunicationEventDto> platinePilotageCommunicationEventList = contentList.stream()
            .map(jsonNode -> {
                String interroId = jsonNode.path(jsonKeyId).asText();
                String communicationRequestId=communicationRequestIdByInterroIdMap.get(interroId);
                return PlatinePilotageCommunicationEventDto.builder().communcationId(currentCommunicationId).communicationRequestId(communicationRequestId).interrogationId(interroId).state(state).build();
            }).toList();

        platinePilotageService.postCommunicationEvent(platinePilotageCommunicationEventList);

        log.info("ProcessInstanceId={}  end",execution.getProcessInstanceId());

    }
}