package fr.insee.protools.backend.service.era;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.service.DelegateContextVerifier;
import fr.insee.protools.backend.service.context.ContextService;
import fr.insee.protools.backend.service.era.dto.CensusJsonDto;
import fr.insee.protools.backend.service.era.dto.GenderType;
import fr.insee.protools.backend.service.utils.FlowableVariableUtils;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.*;
import static fr.insee.protools.backend.service.context.ContextConstants.CTX_PARTITIONS;
import static fr.insee.protools.backend.service.context.ContextConstants.CTX_PARTITION_ERA_SEXE;
import static fr.insee.protools.backend.service.utils.ContextUtils.getCurrentPartitionNode;

@Component
@Slf4j
public class EraGetSUForPeriodAndGenderTask implements JavaDelegate, DelegateContextVerifier {

    @Autowired ContextService protoolsContext;
    @Autowired EraService eraService;
    @Autowired ContextService contextService;

    @Override
    public void execute(DelegateExecution execution) {
        LocalDate startDate = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_ERA_QUERY_START_DATE, LocalDate.class);
        LocalDate endDate = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_ERA_QUERY_END_DATE, LocalDate.class);
        Long currentPartitionId = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_CURRENT_PARTITION_ID, Long.class);

        //Get current partition from contexte to get it's defined sexe
        JsonNode contextRootNode = protoolsContext.getContextByProcessInstance(execution.getProcessInstanceId());
        checkContextOrThrow(log, execution.getProcessInstanceId(), contextRootNode);

        JsonNode currentPartitionNode = getCurrentPartitionNode(contextRootNode, currentPartitionId);
        GenderType sexe = GenderType.fromLabel(currentPartitionNode.path(CTX_PARTITION_ERA_SEXE).asText());

        List<CensusJsonDto> response = eraService.getSUForPeriodAndSex(startDate, endDate, sexe);
        execution.setVariableLocal(VARNAME_ERA_RESPONSE, response);
    }

    @Override
    public Set<String> getContextErrors(JsonNode contextRootNode) {
        if (contextRootNode == null) {
            return Set.of("Context is missing");
        }
        Set<String> results = new HashSet<>();
        Set<String> requiredNodes =
                Set.of(
                        //Global & Campaign
                        CTX_PARTITIONS
                );
        Set<String> requiredPartition =
                Set.of(CTX_PARTITION_ERA_SEXE);
        results.addAll(DelegateContextVerifier.computeMissingChildrenMessages(requiredNodes, contextRootNode, getClass()));
        var partitionIterator = contextRootNode.path(CTX_PARTITIONS).elements();

        //Partitions
        while (partitionIterator.hasNext()) {
            var partitionNode = partitionIterator.next();
            var missingChildren = DelegateContextVerifier.computeMissingChildrenMessages(requiredPartition, partitionNode, getClass());
            if (!missingChildren.isEmpty()) {
                results.addAll(missingChildren);
                continue;
            }

            //We will use the custom label
            String genderType = partitionNode.path(CTX_PARTITION_ERA_SEXE).asText();
            try {
                GenderType.fromLabel(genderType);
            } catch (IllegalStateException i) {
                results.add(DelegateContextVerifier.computeIncorrectEnumMessage(CTX_PARTITION_ERA_SEXE, genderType, GenderType.getAllValidLabels(), getClass()));
            }
        }
        return results;
    }
}