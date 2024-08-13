package fr.insee.protools.backend.service.meshuggah;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.dto.meshuggah.MeshuggahComDetails;
import fr.insee.protools.backend.httpclients.webclient.WebClientHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static fr.insee.protools.backend.httpclients.configuration.ApiConfigProperties.KNOWN_API.KNOWN_API_MESHUGGAH;

@Service
@Slf4j
@RequiredArgsConstructor
public class MeshuggahService {

    private final WebClientHelper webClientHelper;


    public void postCreateCommunication(MeshuggahComDetails meshuggahComDetails, JsonNode body) {
        log.debug("postCreateCommunication: meshuggahComDetails={}",meshuggahComDetails);
        String response = webClientHelper.getWebClient(KNOWN_API_MESHUGGAH)
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/communication/medium/{medium}/campagne/{campaignId}/partition/{partitioningId}/phase/{phase}/operation/{operation}/mode/{mode}/protocol/{protocol}")
                        .queryParam("avecQuestionnaire",meshuggahComDetails.isAvecQuestionnaire())
                        .build(
                                meshuggahComDetails.getMedium(),
                                meshuggahComDetails.getCampaignId(),
                                meshuggahComDetails.getPartitioningId(),
                                meshuggahComDetails.getPhase(),
                                meshuggahComDetails.getOperation(),
                                meshuggahComDetails.getMode(),
                                meshuggahComDetails.getProtocol()))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        log.debug("postCreateCommunication: meshuggahComDetails={} - response={}",meshuggahComDetails,response);

    }

    public void postSendCommunication(MeshuggahComDetails meshuggahComDetails, JsonNode body) {
        log.debug("postSendCommunication: meshuggahComDetails={}",meshuggahComDetails);
        String response = webClientHelper.getWebClient(KNOWN_API_MESHUGGAH)
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/communication-request/medium/{medium}/campagne/{campaignId}/partition/{partitioningId}/phase/{phase}/operation/{operation}/mode/{mode}/protocol/{protocol}")
                        .queryParam("avecQuestionnaire",meshuggahComDetails.isAvecQuestionnaire())
                        .build(
                                meshuggahComDetails.getMedium(),
                                meshuggahComDetails.getCampaignId(),
                                meshuggahComDetails.getPartitioningId(),
                                meshuggahComDetails.getPhase(),
                                meshuggahComDetails.getOperation(),
                                meshuggahComDetails.getMode(),
                                meshuggahComDetails.getProtocol()))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        log.debug("postSendCommunication: meshuggahComDetails={} - response={}",meshuggahComDetails,response);

    }


    //V2

    public void sendCommunications(String campaign, String mode, List<JsonNode> interrogations) {
        log.debug("postSendCommunication: campaign={} - mode={} - interrogations.size={}",campaign,mode,interrogations.size());
        String response = webClientHelper.getWebClient(KNOWN_API_MESHUGGAH)
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/campagne/{campaignId}/mode/{mode}")
                        .build(campaign,mode))
                .bodyValue(interrogations)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        log.debug("postSendCommunication: campaign={} - mode={} - response={}",campaign,mode,response);

    }

    }
