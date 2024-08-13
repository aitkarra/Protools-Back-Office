package fr.insee.protools.backend.service.rem.delegate;

import fr.insee.protools.backend.service.DelegateContextVerifier;
import fr.insee.protools.backend.service.rem.RemService;
import fr.insee.protools.backend.service.utils.FlowableVariableUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.List;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_CURRENT_PARTITION_ID;
import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_REM_INTERRO_ID_LIST;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemGetPartitionListOfSuIdTask implements JavaDelegate, DelegateContextVerifier {

    private final RemService remService;

    @Override
    public void execute(DelegateExecution execution) {
        Long currentPartitionId = FlowableVariableUtils.getVariableOrThrow(execution,VARNAME_CURRENT_PARTITION_ID, Long.class);
        //No need protools context ==> no checkContextOrThrow
        log.info("ProcessInstanceId={} - partition={} begin",execution.getProcessInstanceId(),currentPartitionId);

        Long[] partitionSUIds = remService.getSampleSuIds(currentPartitionId);
        List<Long> remSuIdList = List.of(partitionSUIds);
        execution.getParent().setVariableLocal(VARNAME_REM_INTERRO_ID_LIST, remSuIdList);
        log.debug("ProcessInstanceId={} -  partition={} - remSuIdList={} end",execution.getProcessInstanceId(),currentPartitionId,remSuIdList);
    }
}