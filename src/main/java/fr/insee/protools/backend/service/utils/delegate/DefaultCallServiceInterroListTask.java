package fr.insee.protools.backend.service.utils.delegate;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.dto.ContexteProcessus;
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


@Slf4j
@Component
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public abstract class DefaultCallServiceInterroListTask implements JavaDelegate, DelegateContextVerifier {

    private final ContextService protoolsContext;

    // Abstract method for the specific service action
    protected abstract void serviceAction(ContexteProcessus context,List<JsonNode> list,String ... params);
    protected void callService(DelegateExecution execution, ContexteProcessus context, List<JsonNode> list){
        serviceAction(context,list);
    }

    @Override
    public void execute(DelegateExecution execution) {
        ContexteProcessus context = protoolsContext.getContextDtoByProcessInstance(execution.getProcessInstanceId());
        checkContextOrThrow(log,execution.getProcessInstanceId(), context);

        String currentPartitionId = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_CURRENT_PARTITION_ID, String.class);
        List<ProtoolsInterrogationDto> protoolsInterrogationList = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_REM_INTERRO_LIST, List.class);

        List<JsonNode> interrogationList = protoolsInterrogationList.stream()
                .map(protoolsInterrogationDto -> {
                    log.trace("ProcessInstanceId={}  - campainId={} - currentPartitionId={} - idInterrogation={}",
                            execution.getProcessInstanceId(),context.getId(),currentPartitionId,protoolsInterrogationDto.getIdInterrogation());
                    return protoolsInterrogationDto.getRemInterrogation();
                })
                .toList();
        callService(execution,context,interrogationList);
    }
}
