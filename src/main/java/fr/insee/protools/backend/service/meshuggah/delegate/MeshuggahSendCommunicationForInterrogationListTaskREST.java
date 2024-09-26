package fr.insee.protools.backend.service.meshuggah.delegate;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.dto.ContexteProcessus;
import fr.insee.protools.backend.service.context.IContextService;
import fr.insee.protools.backend.service.meshuggah.IMeshuggahService;
import fr.insee.protools.backend.service.utils.FlowableVariableUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.List;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_CURRENT_COMMUNICATION_ID;
import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_REM_INTERRO_LIST;

@Component
@RequiredArgsConstructor
@Slf4j
public class MeshuggahSendCommunicationForInterrogationListTaskREST implements JavaDelegate {

    private final IMeshuggahService meshuggahService;
    private final IContextService protoolsContext;

    @Override
    public void execute(DelegateExecution execution) {
        ContexteProcessus context = protoolsContext.getContextDtoByProcessInstance(execution.getProcessInstanceId());
        List<JsonNode> interroList = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_REM_INTERRO_LIST, List.class);
        String currentCommunicationId = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_CURRENT_COMMUNICATION_ID, String.class);

        log.info("ProcessInstanceId={} - currentCommunicationId={} - begin",
                execution.getProcessInstanceId(), currentCommunicationId);

        meshuggahService.postCommunicationRequest(String.valueOf(context.getId()), currentCommunicationId, interroList);

        log.info("ProcessInstanceId={} - currentCommunicationId={} - end",
                execution.getProcessInstanceId(), currentCommunicationId);
    }
}
