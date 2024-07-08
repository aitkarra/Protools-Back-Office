package fr.insee.protools.backend.service.questionnaire_model;

import fr.insee.protools.backend.service.context.exception.BadContextIncorrectBPMNError;
import fr.insee.protools.backend.webclient.WebClientHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * This service retrieves the nomenclatures from a static website based on a property uri
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QuestionnaireModelFromStaticWebsiteServiceImpl implements QuestionnaireModelService {

    //TODO : expose mandatory configuration?
    private final WebClientHelper webClientHelper;

    @Value("${fr.insee.protools.questionnaire.model.uri}")
    private String questionnaireModelUri;


    @Override
    public String getQuestionnaireModel(String questionnaireModelId, String folderPath){
        log.info("Get Questionnaire Model Value for questionnaireModelId={}", questionnaireModelId);
        String uri;
        String fullPath=questionnaireModelUri+"/" +folderPath+ "/"+questionnaireModelId + ".json";
        try {
            uri = new URI(fullPath).normalize().toString();
        } catch (URISyntaxException e) {
            throw new BadContextIncorrectBPMNError(String.format("questionnaireModelId=[%s] - folderPath=[%s] : fullPath=[%s] cannot be parsed: Error=[%s]"
                    ,questionnaireModelId,folderPath,fullPath, e.getMessage()));
        }
        return
                webClientHelper.getWebClientForFile()
                        .get()
                        .uri(uri)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
    }
}
