package fr.insee.protools.backend.service.rem;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.dto.era.CensusJsonDto;
import fr.insee.protools.backend.dto.rem.REMSurveyUnitDto;
import fr.insee.protools.backend.dto.rem.SuIdMappingJson;
import fr.insee.protools.backend.webclient.WebClientHelper;
import fr.insee.protools.backend.webclient.exception.runtime.WebClient4xxBPMNError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

import static fr.insee.protools.backend.webclient.configuration.ApiConfigProperties.KNOWN_API.KNOWN_API_REM;

@Service
@Slf4j
@RequiredArgsConstructor
public class RemService {

    private final WebClientHelper webClientHelper;

    public Long[] getPartitionSuIds(Long partitionId) {
        log.debug("getPartitionSuIds - partitionId={} ",partitionId);
        try {
            var response = webClientHelper.getWebClient(KNOWN_API_REM)
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/survey-units/partitions/{partitionId}/ids")
                            .build(partitionId))
                    .retrieve()
                    .bodyToMono(Long[].class)
                    .block();
            log.trace("partitionId={} - response={} ", partitionId, response);
            return response;
        }
        catch (WebClient4xxBPMNError e){
            if(e.getHttpStatusCodeError().equals(HttpStatus.NOT_FOUND)){
                String msg=
                        "Error 404/NOT_FOUND during get partition on REM with partitionId="+partitionId
                                + " - msg="+e.getMessage();
                log.error(msg);
                throw new WebClient4xxBPMNError(msg,e.getHttpStatusCodeError());
            }
            //Currently no remediation so just rethrow
            throw e;
        }
    }


    public JsonNode[] getPartitionAllSU(Long partitionId) {
        log.error("getPartitionAllSU - partitionId={} ",partitionId);
        try {
            var response = webClientHelper.getWebClient(KNOWN_API_REM)
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/survey-units/partitions/{partitionId}")
                            .queryParam("withExternals", true)
                            .build(partitionId))
                    //.uri("/starter/ues")
                    .retrieve()
                    .bodyToMono(JsonNode[].class)
                    .block();
            log.error("partitionId={} - response.length={} ", partitionId, response.length);
            return response;
        }
        catch (WebClient4xxBPMNError e){
            if(e.getHttpStatusCodeError().equals(HttpStatus.NOT_FOUND)){
                String msg=
                        "Error 404/NOT_FOUND during get SU on REM with partitionId="+partitionId
                                + " - msg="+e.getMessage();
                log.error(msg);
                throw new WebClient4xxBPMNError(msg,e.getHttpStatusCodeError());
            }
            //Currently no remediation so just rethrow
            throw e;
        }
    }

    public REMSurveyUnitDto getSurveyUnit(Long surveyUnitId ) {
        log.debug("getSurveyUnit - surveyUnitId ={}",surveyUnitId );
        try {
            var response = webClientHelper.getWebClient(KNOWN_API_REM)
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/survey-units/{surveyUnitId}")
                            .queryParam("withExternals", true)
                            .build(surveyUnitId))
                    .retrieve()
                    .bodyToMono(REMSurveyUnitDto.class)
                    .block();
            log.trace("surveyUnitId={} - response={} ", surveyUnitId, response);
            return response;
        }
        catch (WebClient4xxBPMNError e){
            if(e.getHttpStatusCodeError().equals(HttpStatus.NOT_FOUND)){
                String msg=
                        "Error 404/NOT_FOUND during get SU on REM with surveyUnitId="+surveyUnitId
                                + " - msg="+e.getMessage();
                log.error(msg);
                throw new WebClient4xxBPMNError(msg,e.getHttpStatusCodeError());
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
            var response = webClientHelper.getWebClient(KNOWN_API_REM)
                    .post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/survey-units/households/partitions/{partitionId}/census-upload")
                            .build(partitionId))
                    .bodyValue(values)
                    .retrieve()
                    .bodyToMono(SuIdMappingJson.class)
                    .block();
            log.trace("writeERASUList - partitionId={} - response={} ", partitionId, response);
            return response;
        } catch (WebClient4xxBPMNError e) {
            if (e.getHttpStatusCodeError().equals(HttpStatus.NOT_FOUND)) {
                String msg =
                        "Error 404/NOT_FOUND during REM post census-upload partitionId=" + partitionId
                                + " (check that the partition exists in REM) - msg=" + e.getMessage();
                log.error(msg);
                throw new WebClient4xxBPMNError(msg, e.getHttpStatusCodeError());
            }
            //Currently no remediation so just rethrow
            throw e;
        }
    }
}
