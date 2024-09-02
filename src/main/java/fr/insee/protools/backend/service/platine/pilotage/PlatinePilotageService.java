package fr.insee.protools.backend.service.platine.pilotage;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.dto.platine.pilotage.PlatinePilotageEligibleDto;
import fr.insee.protools.backend.dto.platine.pilotage.contact.PlatineContactDto;
import fr.insee.protools.backend.dto.platine.pilotage.query.QuestioningWebclientDto;
import fr.insee.protools.backend.dto.platine.pilotage.v2.PlatinePilotageCommunicationEventDto;
import fr.insee.protools.backend.httpclients.exception.runtime.HttpClient4xxBPMNError;
import fr.insee.protools.backend.httpclients.pagination.PageResponse;
import fr.insee.protools.backend.httpclients.restclient.RestClientHelper;
import fr.insee.protools.backend.service.platine.pilotage.metadata.MetadataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static fr.insee.protools.backend.httpclients.configuration.ApiConfigProperties.KNOWN_API.KNOWN_API_REM;
import static fr.insee.protools.backend.httpclients.webclient.WebClientHelper.logJson;
import static fr.insee.protools.backend.httpclients.configuration.ApiConfigProperties.KNOWN_API.KNOWN_API_PLATINE_PILOTAGE;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlatinePilotageService {

    private final RestClientHelper restClientHelper;

    @Value("${fr.insee.protools.api.platine-pilotage.interrogation.page.size:5000}")
    private int pageSizeGetInterro;

    public void putMetadata(String partitionId , MetadataDto dto) {
        log.debug("putMetadata : partitionId={} - dto.su.id={} ",partitionId,dto.getSurveyDto().getId());
        logJson(String.format("putMetadata - partitionId=%s : ",partitionId),dto, log,Level.DEBUG);
        var response = restClientHelper.getRestClient(KNOWN_API_PLATINE_PILOTAGE)
                .put()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/metadata/{id}")
                        .build(partitionId))
                .body(dto)
                .retrieve()
                .body(String.class);
        log.trace("putMetadata : partitionId={} - response={} ",partitionId,response);
    }

    public void putQuestionings(QuestioningWebclientDto dto) {
        log.debug("putQuestionings: idPartitioning={} - idSu={}",dto.getIdPartitioning(),dto.getSurveyUnit().getIdSu());
        logJson("putMetadata ",dto,log,Level.TRACE);
        var response = restClientHelper.getRestClient(KNOWN_API_PLATINE_PILOTAGE)
                .put()
                .uri("/api/questionings")
                .body(dto)
                .retrieve()
                .body(String.class);
        log.trace("putQuestionings - response={} ",response);
    }

    public PlatineContactDto getSUMainContact(Long idSU, String platinePartitionId){
        log.debug("getSUMainContact: platinePartitionId={} - idSu={}",platinePartitionId,idSU);
        PlatineContactDto response = restClientHelper.getRestClient(KNOWN_API_PLATINE_PILOTAGE)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/main-contact")
                        .queryParam("partitioning", platinePartitionId)
                        .queryParam("survey-unit", idSU)
                        .build())
                .retrieve()
                .body(PlatineContactDto.class);
        logJson("getSUMainContact response : ",response,log,Level.TRACE);
        return response;
    }

    public Boolean isToFollowUp(Long idSU, String platinePartitionId){
        log.debug("isToFollowUp: platinePartitionId={} - idSu={}",platinePartitionId,idSU);
        PlatinePilotageEligibleDto response = restClientHelper.getRestClient(KNOWN_API_PLATINE_PILOTAGE)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/partitionings/{idPartitioning}/survey-units/{idSu}/follow-up")
                        .build(platinePartitionId,idSU))
                .retrieve()
                .body(PlatinePilotageEligibleDto.class);
        Boolean result =  Boolean.valueOf(response.getEligible());
        logJson("isToFollowUp: result="+result+" -  response : ",response,log,Level.TRACE);
        return  result;
    }

    //V2
    public void putQuestionings(String campaignId, List<JsonNode> interrogations) {
        log.trace("putQuestionings: campaignId={}",campaignId);
        logJson("putMetadata ",interrogations,log,Level.TRACE);
        var response = restClientHelper.getRestClient(KNOWN_API_PLATINE_PILOTAGE)
                .put()
                .uri("/api/questionings")
                .body(interrogations)
                .retrieve()
                .body(String.class);
        log.trace("putQuestionings: campaignId={} - response={} ",campaignId,response);
    }

    public void postContext(String campaignId,JsonNode contextRootNode) {
        log.trace("postContext: campaignId={}",campaignId);
        var response = restClientHelper.getRestClient(KNOWN_API_PLATINE_PILOTAGE)
                .post()
                .uri("/context")
                .body(contextRootNode)
                .retrieve()
                .body(String.class);
        log.trace("postContext: campaignId={} - response={} ",campaignId,response);
    }

    public void postCommunicationEvent(List<PlatinePilotageCommunicationEventDto> platinePilotageCommunicationEventList) {
        log.trace("postCommunicationEvent: ");
        logJson("postCommunicationEvent ",platinePilotageCommunicationEventList,log,Level.TRACE);

        var response = restClientHelper.getRestClient(KNOWN_API_PLATINE_PILOTAGE)
                .post()
                .uri("/interrogations/communication-events")
                .body(platinePilotageCommunicationEventList)
                .retrieve()
                .body(String.class);
        log.trace("postCommunicationEvent: response={} ",response);
    }

    public PageResponse<JsonNode> getInterrogationToFollowUpPaginated(Long partitionId, long page, Optional<Boolean>  isToFollowUp) {
        log.debug("partitionId={} - page={} - pageSizeGetInterro={} - hasAccount={}",partitionId,page,pageSizeGetInterro,isToFollowUp);
        ParameterizedTypeReference<PageResponse<JsonNode>> typeReference = new ParameterizedTypeReference<>() { };
        try {
            PageResponse<JsonNode> response = restClientHelper.getRestClient(KNOWN_API_PLATINE_PILOTAGE)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("interrogations")
                        .queryParam("page", page)
                        .queryParam("size", pageSizeGetInterro)
                        .queryParam("partition_id", partitionId)
                        .queryParamIfPresent("follow-up", isToFollowUp)
                        .build(partitionId))
                .retrieve()
                .body(typeReference);
            log.trace("partitionId={} - page={} - pageSizeGetInterro={} - response={} ", partitionId,page,pageSizeGetInterro, response.getContent().size());
            return response;
        }
        catch (HttpClient4xxBPMNError e){
            if(e.getHttpStatusCodeError().equals(HttpStatus.NOT_FOUND)){
                String msg=
                        "Error 404/NOT_FOUND during Platine Pilotage getInterrogationToFollowUpPaginated with partitionId="+partitionId
                                + " - msg="+e.getMessage();
                log.error(msg);
                throw new HttpClient4xxBPMNError(msg,e.getHttpStatusCodeError());
            }
            //Currently no remediation so just rethrow
            throw e;
        }
    }
}
