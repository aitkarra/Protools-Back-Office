package fr.insee.protools.backend.service.utils.delegate;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.dto.internal.ProtoolsInterrogationDto;
import fr.insee.protools.backend.service.DelegateContextVerifier;
import fr.insee.protools.backend.service.context.ContextService;
import fr.insee.protools.backend.service.utils.FlowableVariableUtils;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.List;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_CURRENT_PARTITION_ID;
import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_REM_INTERRO_LIST;
import static fr.insee.protools.backend.service.context.ContextConstants.CTX_CAMPAGNE_ID;


@Slf4j
@Component
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public abstract class DefaultCallServiceInterroListTask implements JavaDelegate, DelegateContextVerifier {

    private final ContextService protoolsContext;

    // Abstract method for the specific service action
    protected abstract void serviceAction(String campaignId,List<JsonNode> list,String ... params);
    protected void callService(DelegateExecution execution, JsonNode contextRootNode, String campaignId,List<JsonNode> list){
        serviceAction(campaignId,list);
    }

    @Override
    public void execute(DelegateExecution execution) {
        JsonNode contextRootNode = protoolsContext.getContextByProcessInstance(execution.getProcessInstanceId());
        checkContextOrThrow(log,execution.getProcessInstanceId(), contextRootNode);

        String campainId = contextRootNode.path(CTX_CAMPAGNE_ID).asText();
        Long currentPartitionId = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_CURRENT_PARTITION_ID, Long.class);
        List<ProtoolsInterrogationDto> protoolsInterrogationList = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_REM_INTERRO_LIST, List.class);

        List<JsonNode> interrogationList = protoolsInterrogationList.stream()
                .map(protoolsInterrogationDto -> {
                    log.trace("ProcessInstanceId={}  - campainId={} - currentPartitionId={} - idInterrogation={}",
                            execution.getProcessInstanceId(),campainId,currentPartitionId,protoolsInterrogationDto.getIdInterrogation());
                    return protoolsInterrogationDto.getRemInterrogation();
                })
                .toList();
        callService(execution,contextRootNode,campainId,interrogationList);
    }
}
