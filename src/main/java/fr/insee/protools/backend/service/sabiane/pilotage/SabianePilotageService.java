package fr.insee.protools.backend.service.sabiane.pilotage;

import fr.insee.protools.backend.dto.sabiane.pilotage.CampaignContextDto;
import fr.insee.protools.backend.dto.sabiane.pilotage.SurveyUnitContextDto;
import fr.insee.protools.backend.httpclients.webclient.WebClientHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.springframework.stereotype.Service;

import java.util.List;

import static fr.insee.protools.backend.httpclients.configuration.ApiConfigProperties.KNOWN_API.KNOWN_API_SABIANE_PILOTAGE;


@Service
@Slf4j
@RequiredArgsConstructor
public class SabianePilotageService {

    private final WebClientHelper webClientHelper;

    public void postCampaign(CampaignContextDto campaignContextDto) {
        WebClientHelper.logJson("postCampaign: ",campaignContextDto, log,Level.DEBUG);

        //TODO: This call returns that if campaign already exists :   statusCode=400 BAD_REQUEST - contentType=Optional[text/plain;charset=UTF-8] - Campaign with id 'MBG2022X01' already exists
        var response = webClientHelper.getWebClient(KNOWN_API_SABIANE_PILOTAGE)
                .post()
                .uri("/api/campaign")
                .bodyValue(campaignContextDto)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        log.trace("postCampaign: campaign={} - response={} ", campaignContextDto.getCampaign(), response);
    }
    public void postSurveyUnits(List<SurveyUnitContextDto> values) {
            log.debug("postSurveyUnits - values.size={}", values == null ? 0 : values.size());
            if (values == null) {
                log.debug("postSurveyUnits - values==null ==> Nothing to do");
                return;
            }
            var response = webClientHelper.getWebClient(KNOWN_API_SABIANE_PILOTAGE)
                    .post()
                    .uri("/api/survey-units")
                    .bodyValue(values)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.trace("postSurveyUnits - response={} ", response);
    }
}


