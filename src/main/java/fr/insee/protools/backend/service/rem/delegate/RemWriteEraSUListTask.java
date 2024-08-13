package fr.insee.protools.backend.service.rem.delegate;

import fr.insee.protools.backend.dto.era.CensusJsonDto;
import fr.insee.protools.backend.dto.rem.SuIdMappingJson;
import fr.insee.protools.backend.dto.rem.SuIdMappingRecord;
import fr.insee.protools.backend.service.DelegateContextVerifier;
import fr.insee.protools.backend.service.exception.ProtoolsTaskBPMNError;
import fr.insee.protools.backend.service.rem.RemService;
import fr.insee.protools.backend.service.utils.FlowableVariableUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.List;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class RemWriteEraSUListTask implements JavaDelegate, DelegateContextVerifier {

    private final RemService remService;

    @Override
    public void execute(DelegateExecution execution) {

        //Get the list of SU previouly retrieved from ERA
        List<CensusJsonDto> eraSUList = FlowableVariableUtils.getVariableOrThrow(execution,VARNAME_ERA_RESPONSE, List.class);
        //Get current REM partition
        Long currentPartitionId = FlowableVariableUtils.getVariableOrThrow(execution,VARNAME_CURRENT_PARTITION_ID, Long.class);
        if(eraSUList==null||eraSUList.isEmpty()){
            log.info("ProcessInstanceId={} - currentPartitionId={} - variable {} is empty",execution.getProcessInstanceId(), currentPartitionId,VARNAME_ERA_RESPONSE);
            execution.getParent().setVariableLocal(VARNAME_REM_INTERRO_ID_LIST, List.of());
            return;
        }

        log.info("ProcessInstanceId={} - currentPartitionId={} - eraSUList.size={} begin",execution.getProcessInstanceId(), currentPartitionId,eraSUList.size());
        //Store SU in REM
        SuIdMappingJson remMapping = remService.writeERASUList(currentPartitionId, eraSUList);
        if(remMapping==null){
            log.error("ProcessInstanceId={} - currentPartitionId={}  remMapping is empty/null end",execution.getProcessInstanceId(), currentPartitionId);
            execution.getParent().setVariableLocal(VARNAME_REM_INTERRO_ID_LIST, List.of());
            throw new ProtoolsTaskBPMNError("Error while writing list of Era SU to REM : REM returned a null result ");
        }

        //STORE the list of REM identifier created
        List<Long> remSuIdList = remMapping.getData().stream().map(SuIdMappingRecord::repositoryId).toList();
        execution.getParent().setVariableLocal(VARNAME_REM_INTERRO_ID_LIST, remSuIdList);
        log.info("ProcessInstanceId={} - currentPartitionId={} - remSuIdList={}  end",execution.getProcessInstanceId(), currentPartitionId,remSuIdList);
    }
}
