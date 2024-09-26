package fr.insee.protools.backend.service.platine.delegate.pilotatage;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.restclient.pagination.PageResponse;
import fr.insee.protools.backend.service.DelegateContextVerifier;
import fr.insee.protools.backend.service.platine.service.IPlatinePilotageService;
import fr.insee.protools.backend.service.utils.FlowableVariableUtils;
import fr.insee.protools.backend.service.utils.delegate.PaginationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.*;


@Slf4j
@Component
@RequiredArgsConstructor
public class PlatinePilotageGetListOfInterroToFollowUpPaginatedTaskREST implements JavaDelegate, DelegateContextVerifier, PaginationHelper {

    private final IPlatinePilotageService pilotageService;

    protected PageResponse readFunction(Integer pageToRead, Object... objects) {
        String partitionId = (String) objects[0];
        return pilotageService.getInterrogationToFollowUpPaginated(partitionId, pageToRead, Optional.of(Boolean.TRUE));
    }

    @Override
    public void execute(DelegateExecution execution) {
        String currentPartitionId = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_CURRENT_PARTITION_ID, String.class);
        getAndTreat(execution, VARNAME_INTERRO_LIST_PAGEABLE_CURRENT_PAGE, VARNAME_INTERRO_LIST_PAGEABLE_IS_LAST_PAGE, this::readFunction, currentPartitionId);
    }

    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public Map<String, Object> treatPage(DelegateExecution execution, List<JsonNode> contentList) {
        Map<String, Object> variables = new HashMap<>();
        variables.put(VARNAME_REM_INTERRO_LIST, contentList);
        return variables;
    }
}