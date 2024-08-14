package fr.insee.protools.backend.service.rem.delegate.v2;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.dto.internal.ProtoolsInterrogationDto;
import fr.insee.protools.backend.httpclients.pagination.PageResponse;
import fr.insee.protools.backend.service.DelegateContextVerifier;
import fr.insee.protools.backend.service.rem.RemService;
import fr.insee.protools.backend.service.utils.FlowableVariableUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.*;

@Slf4j
public abstract class RemGetPartitionListOfInterroDefaultPaginated implements JavaDelegate, DelegateContextVerifier, PaginationHelper {

    @Override
    public Map<String, Object> treatPage(DelegateExecution execution, List<JsonNode> contentList) {

        String jsonKeyId = "id";
        List<ProtoolsInterrogationDto> protoolsInterrogationDtos = contentList.stream()
                //TODO: remove as it should not occurs?
                .filter(jsonNode -> {
                    boolean hasId = jsonNode.has(jsonKeyId) && !jsonNode.get(jsonKeyId).isNull();
                    if (!hasId) {
                        log.warn("Skipping interrogation without id: {}", jsonNode);
                    }
                    return hasId;
                }).map(jsonNode -> ProtoolsInterrogationDto.builder().idInterrogation(jsonNode.get(jsonKeyId).asText()).remInterrogation(jsonNode).build()).toList();

        Map<String, Object> variables = Map.of(VARNAME_REM_PROTOOLS_INTERRO_LIST, protoolsInterrogationDtos);
        return variables;
    }
}