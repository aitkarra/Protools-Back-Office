package fr.insee.protools.backend.service.platine.questionnaire;

import fr.insee.protools.backend.service.common.platine_sabiane.QuestionnairePlatineSabianeService;
import fr.insee.protools.backend.httpclients.webclient.WebClientHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import static fr.insee.protools.backend.httpclients.configuration.ApiConfigProperties.KNOWN_API.KNOWN_API_PLATINE_QUESTIONNAIRE;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlatineQuestionnaireService implements QuestionnairePlatineSabianeService {

    private final WebClientHelper webClientHelper;

    @Override
    public WebClient webClient() {
        return webClientHelper.getWebClient(KNOWN_API_PLATINE_QUESTIONNAIRE);
    }

    @Override
    public Logger getLogger() {
        return log;
    }
}
