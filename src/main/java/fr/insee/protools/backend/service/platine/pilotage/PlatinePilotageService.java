package fr.insee.protools.backend.service.platine.pilotage;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.dto.platine.pilotage.PlatinePilotageEligibleDto;
import fr.insee.protools.backend.dto.platine.pilotage.contact.PlatineContactDto;
import fr.insee.protools.backend.dto.platine.pilotage.query.QuestioningWebclientDto;
import fr.insee.protools.backend.service.platine.pilotage.metadata.MetadataDto;
import fr.insee.protools.backend.httpclients.webclient.WebClientHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.springframework.stereotype.Service;

import java.util.List;

import static fr.insee.protools.backend.httpclients.webclient.WebClientHelper.logJson;
import static fr.insee.protools.backend.httpclients.configuration.ApiConfigProperties.KNOWN_API.KNOWN_API_PLATINE_PILOTAGE;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlatinePilotageService {

    private final WebClientHelper webClientHelper;
    public void putMetadata(String partitionId , MetadataDto dto) {
        log.debug("putMetadata : partitionId={} - dto.su.id={} ",partitionId,dto.getSurveyDto().getId());
        logJson(String.format("putMetadata - partitionId=%s : ",partitionId),dto, log,Level.DEBUG);
        var response = webClientHelper.getWebClient(KNOWN_API_PLATINE_PILOTAGE)
                .put()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/metadata/{id}")
                        .build(partitionId))
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        log.trace("putMetadata : partitionId={} - response={} ",partitionId,response);
    }

    public void putQuestionings(QuestioningWebclientDto dto) {
        log.debug("putQuestionings: idPartitioning={} - idSu={}",dto.getIdPartitioning(),dto.getSurveyUnit().getIdSu());
        logJson("putMetadata ",dto,log,Level.TRACE);
        var response = webClientHelper.getWebClient(KNOWN_API_PLATINE_PILOTAGE)
                .put()
                .uri("/api/questionings")
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        log.trace("putQuestionings - response={} ",response);
    }

    public PlatineContactDto getSUMainContact(Long idSU, String platinePartitionId){
        log.debug("getSUMainContact: platinePartitionId={} - idSu={}",platinePartitionId,idSU);
        PlatineContactDto response = webClientHelper.getWebClient(KNOWN_API_PLATINE_PILOTAGE)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/main-contact")
                        .queryParam("partitioning", platinePartitionId)
                        .queryParam("survey-unit", idSU)
                        .build())
                .retrieve()
                .bodyToMono(PlatineContactDto.class)
                .block();
        logJson("getSUMainContact response : ",response,log,Level.TRACE);
        return response;
    }

    public Boolean isToFollowUp(Long idSU, String platinePartitionId){
        log.debug("isToFollowUp: platinePartitionId={} - idSu={}",platinePartitionId,idSU);
        PlatinePilotageEligibleDto response = webClientHelper.getWebClient(KNOWN_API_PLATINE_PILOTAGE)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/partitionings/{idPartitioning}/survey-units/{idSu}/follow-up")
                        .build(platinePartitionId,idSU))
                .retrieve()
                .bodyToMono(PlatinePilotageEligibleDto.class)
                .block();
        Boolean result =  Boolean.valueOf(response.getEligible());
        logJson("isToFollowUp: result="+result+" -  response : ",response,log,Level.TRACE);
        return  result;
    }

    //V2
    public void putQuestionings(String campaignId, List<JsonNode> interrogations) {
        log.trace("putQuestionings: campaignId={}",campaignId);
        logJson("putMetadata ",interrogations,log,Level.TRACE);
        var response = webClientHelper.getWebClient(KNOWN_API_PLATINE_PILOTAGE)
                .put()
                .uri("/api/questionings")
                .bodyValue(interrogations)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        log.trace("putQuestionings: campaignId={} - response={} ",campaignId,response);
    }

    public void postCreateCampaign(String campaignId,JsonNode contextRootNode) {
        log.trace("postCreateCampaign: campaignId={}",campaignId);
        var response = webClientHelper.getWebClient(KNOWN_API_PLATINE_PILOTAGE)
                .post()
                .uri("/api/campaign")
                .bodyValue(contextRootNode)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        log.trace("postCreateCampaign: campaignId={} - response={} ",campaignId,response);
    }
}
