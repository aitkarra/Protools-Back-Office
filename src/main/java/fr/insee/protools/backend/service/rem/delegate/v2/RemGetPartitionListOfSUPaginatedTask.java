package fr.insee.protools.backend.service.rem.delegate.v2;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.dto.internal.ProtoolsInterrogationDto;
import fr.insee.protools.backend.service.DelegateContextVerifier;
import fr.insee.protools.backend.service.exception.PageableAPIBPMNError;
import fr.insee.protools.backend.service.rem.RemService;
import fr.insee.protools.backend.service.utils.FlowableVariableUtils;
import fr.insee.protools.backend.httpclients.pagination.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemGetPartitionListOfSUPaginatedTask implements JavaDelegate, DelegateContextVerifier {

    private final RemService remService;


    @Override
    public void execute(DelegateExecution execution) {
        Long currentPartitionId = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_CURRENT_PARTITION_ID, Long.class);
        log.info("ProcessInstanceId={} - partition={} begin", execution.getProcessInstanceId(), currentPartitionId);
        //No need protools context ==> no checkContextOrThrow

        Long currentPage = FlowableVariableUtils.getVariableOrNull(execution, VARNAME_REM_INTERRO_LIST_PAGEABLE_CURRENT_PAGE, Long.class);
        Boolean isLastPage = FlowableVariableUtils.getVariableOrNull(execution, VARNAME_REM_INTERRO_LIST_PAGEABLE_IS_LAST_PAGE, Boolean.class);
        if (currentPage == null) {
            currentPage = 0L;
        }
        else if(isLastPage == null) {
            isLastPage = Boolean.FALSE;
        }
        else {
            currentPage++;
        }

        if (Boolean.TRUE.equals(isLastPage)) {
            log.warn("ProcessInstanceId={} - partition={} : last page already reached", execution.getProcessInstanceId(), currentPartitionId);
        } else {
            PageResponse<JsonNode> pageResponse = remService.getPartitionAllSuPaginated(currentPartitionId, currentPage);
            int nbValRead = treatPage(execution, pageResponse, currentPage,
                    VARNAME_REM_INTERRO_LIST, VARNAME_REM_INTERRO_LIST_PAGEABLE_CURRENT_PAGE, VARNAME_REM_INTERRO_LIST_PAGEABLE_IS_LAST_PAGE)
                    .size();
            log.debug("ProcessInstanceId={} -  partition={} - nbValRead={} end", execution.getProcessInstanceId(), currentPartitionId, nbValRead);
        }
    }


    private static List<JsonNode> treatPage(DelegateExecution execution,
                                         PageResponse<JsonNode> pageResponse,
                                         Long expectedCurrentPage,
                                         String varname_list,
                                         String varname_current_page,
                                         String varname_is_last_page) {
        Boolean isLastPage = pageResponse.isLast();
        long responseCurrentPage = pageResponse.getNumber();
        if (responseCurrentPage != expectedCurrentPage) {
            throw new PageableAPIBPMNError(String.format("Error while reading SU from REM- expected page=%s got page=%s",
                    expectedCurrentPage, responseCurrentPage));
        }

        String jsonKeyId="id";

        List<JsonNode> contentList = pageResponse.getContent();
        List<ProtoolsInterrogationDto> protoolsInterrogationDtos = contentList.stream()
                .filter(jsonNode -> {
                    boolean hasId = jsonNode.has(jsonKeyId) && !jsonNode.get(jsonKeyId).isNull();
                    if (!hasId) {
                        log.warn("Skipping interrogation without id: {}", jsonNode);
                    }
                    return hasId;
                })
                .map(jsonNode -> ProtoolsInterrogationDto.builder()
                        .idInterrogation(jsonNode.get(jsonKeyId).asText())
                        .remInterrogation(jsonNode)
                        .build())
                .toList();

        Map<String, ? extends Object> variables = Map.of(
                varname_list, protoolsInterrogationDtos,
                varname_current_page, expectedCurrentPage,
                varname_is_last_page, isLastPage
        );
        execution.getParent().setVariablesLocal(variables);
        return contentList;
    }
}