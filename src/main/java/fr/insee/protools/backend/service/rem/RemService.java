package fr.insee.protools.backend.service.rem;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.dto.era.CensusJsonDto;
import fr.insee.protools.backend.dto.rem.REMSurveyUnitDto;
import fr.insee.protools.backend.dto.rem.SuIdMappingJson;
import fr.insee.protools.backend.httpclients.restclient.RestClientHelper;
import fr.insee.protools.backend.httpclients.webclient.WebClientHelper;
import fr.insee.protools.backend.httpclients.exception.runtime.HttpClient4xxBPMNError;
import fr.insee.protools.backend.httpclients.pagination.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static fr.insee.protools.backend.httpclients.configuration.ApiConfigProperties.KNOWN_API.KNOWN_API_REM;

@Service
@Slf4j
@RequiredArgsConstructor
public class RemService {

    private final RestClientHelper restClientHelper;
    @Value("${fr.insee.protools.api.rem.interrogation.page.size:5000}")
    private int pageSizeGetInterro;

    public Long[] getSampleSuIds(Long partitionId) {
        log.debug("getSampleSuIds - partitionId={} ",partitionId);
        try {
            Long[] response = restClientHelper.getRestClient(KNOWN_API_REM)
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/survey-units/partitions/{partitionId}/ids")
                            .build(partitionId))
                    .retrieve()
                    .body(Long[].class);
            log.trace("partitionIds={} - response={} ", partitionId, response);
            return response;
        }
        catch (HttpClient4xxBPMNError e){
            if(e.getHttpStatusCodeError().equals(HttpStatus.NOT_FOUND)){
                String msg=
                        "Error 404/NOT_FOUND during get sample on REM with partitionId="+partitionId
                                + " - msg="+e.getMessage();
                log.error(msg);
                throw new HttpClient4xxBPMNError(msg,e.getHttpStatusCodeError());
            }
            //Currently no remediation so just rethrow
            throw e;
        }
    }

    public REMSurveyUnitDto getSurveyUnit(Long surveyUnitId ) {
        log.debug("getSurveyUnit - surveyUnitId ={}",surveyUnitId );
        try {
            var response = restClientHelper.getRestClient(KNOWN_API_REM)
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/survey-units/{surveyUnitId}")
                            .queryParam("withExternals", true)
                            .build(surveyUnitId))
                    .retrieve()
                    .body(REMSurveyUnitDto.class);
            log.trace("surveyUnitId={} - response={} ", surveyUnitId, response);
            return response;
        }
        catch (HttpClient4xxBPMNError e){
            if(e.getHttpStatusCodeError().equals(HttpStatus.NOT_FOUND)){
                String msg=
                        "Error 404/NOT_FOUND during get SU on REM with surveyUnitId="+surveyUnitId
                                + " - msg="+e.getMessage();
                log.error(msg);
                throw new HttpClient4xxBPMNError(msg,e.getHttpStatusCodeError());
            }
            //Currently no remediation so just rethrow
            throw e;
        }
    }

    /**
     * @param partitionId
     * @param values
     * @return return null if values is null ; else return the result of the api call
     */
    public SuIdMappingJson writeERASUList(long partitionId, List<CensusJsonDto> values) {
        log.debug("writeERASUList - partitionId={}  - values.size={}", partitionId, values == null ? 0 : values.size());
        if (values == null) {
            log.debug("writeERASUList - partitionId={}  - values==null ==> Nothing to do");
            return null;
        }
        try {
            var response = restClientHelper.getRestClient(KNOWN_API_REM)
                    .post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/survey-units/households/partitions/{partitionId}/census-upload")
                            .build(partitionId))
                    .body(values)
                    .retrieve()
                    .body(SuIdMappingJson.class);
            log.trace("writeERASUList - partitionId={} - response={} ", partitionId, response);
            return response;
        } catch (HttpClient4xxBPMNError e) {
            if (e.getHttpStatusCodeError().equals(HttpStatus.NOT_FOUND)) {
                String msg =
                        "Error 404/NOT_FOUND during REM post census-upload partitionId=" + partitionId
                                + " (check that the partition exists in REM) - msg=" + e.getMessage();
                log.error(msg);
                throw new HttpClient4xxBPMNError(msg, e.getHttpStatusCodeError());
            }
            //Currently no remediation so just rethrow
            throw e;
        }
    }


    public PageResponse<JsonNode> getPartitionAllInterroPaginated(Long partitionId, long page, Boolean hasAccount) {
        log.debug("partitionId={} - page={} - pageSizeGetInterro={} - hasAccount={}",partitionId,page,pageSizeGetInterro,hasAccount);
        ParameterizedTypeReference<PageResponse<JsonNode>> typeReference = new ParameterizedTypeReference<>() { };
        try {
            PageResponse<JsonNode> response;
            if(hasAccount!=null) {
                response = restClientHelper.getRestClient(KNOWN_API_REM)
                        .get()
                        .uri(uriBuilder -> uriBuilder
                                .path("interrogations/ids")
                                .queryParam("page", page)
                                .queryParam("size", pageSizeGetInterro)
                                .queryParam("partition_id", partitionId)
                                .queryParam("hasAccount", false)
                                .build(partitionId))
                        .retrieve()
                        .body(typeReference);
            }
            else {
                response = restClientHelper.getRestClient(KNOWN_API_REM)
                        .get()
                        .uri(uriBuilder -> uriBuilder
                                .path("interrogations/ids")
                                .queryParam("page", page)
                                .queryParam("size", pageSizeGetInterro)
                                .queryParam("partition_id", partitionId)
                                .build(partitionId))
                        .retrieve()
                        .body(typeReference);
            }
            log.trace("partitionId={} - page={} - pageSizeGetInterro={} - response={} ", partitionId,page,pageSizeGetInterro, response.getContent().size());
            return response;
        }
        catch (HttpClient4xxBPMNError e){
            if(e.getHttpStatusCodeError().equals(HttpStatus.NOT_FOUND)){
                String msg=
                        "Error 404/NOT_FOUND during get sample on REM with partitionId="+partitionId
                                + " - msg="+e.getMessage();
                log.error(msg);
                throw new HttpClient4xxBPMNError(msg,e.getHttpStatusCodeError());
            }
            //Currently no remediation so just rethrow
            throw e;
        }
    }

    //V2

    public Long[] getInterrogationIdsWithoutAccountForLot(Long lotId) {
        log.debug("getSampleSuIds - lotId={} ",lotId);
        try {
            Long[] response = restClientHelper.getRestClient(KNOWN_API_REM)
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/interrogations/lots/{lotId}/ids")
                            .build(lotId))
                    .retrieve()
                    .body(Long[].class);
            log.trace("partitionIds={} - response={} ", lotId, response);
            return response;
        }
        catch (HttpClient4xxBPMNError e){
            if(e.getHttpStatusCodeError().equals(HttpStatus.NOT_FOUND)){
                String msg=
                        "Error 404/NOT_FOUND during get lot  interrogations ids on REM with lot="+lotId
                                + " - msg="+e.getMessage();
                log.error(msg);
                throw new HttpClient4xxBPMNError(msg,e.getHttpStatusCodeError());
            }
            //Currently no remediation so just rethrow
            throw e;
        }
    }

    public void patchInterrogationsSetAccounts(Map<Long, String> userByInterroId) {
        log.debug("patchInterrogationsSetAccounts - userByInterroId.size={}", userByInterroId.size());
        var response = restClientHelper.getRestClient(KNOWN_API_REM)
                .post()
                .uri("/interrogations/updateAccounts")
                .body(userByInterroId)
                .retrieve()
                .body(SuIdMappingJson.class);
        log.trace("patchInterrogationsSetAccounts - response={} ", response);
    }
}
