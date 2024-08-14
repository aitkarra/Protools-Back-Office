package fr.insee.protools.backend.service.platine.questionnaire;

import fr.insee.protools.backend.httpclients.restclient.RestClientHelper;
import fr.insee.protools.backend.service.common.platine_sabiane.QuestionnairePlatineSabianeService;
import fr.insee.protools.backend.httpclients.webclient.WebClientHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import static fr.insee.protools.backend.httpclients.configuration.ApiConfigProperties.KNOWN_API.KNOWN_API_PLATINE_QUESTIONNAIRE;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlatineQuestionnaireService implements QuestionnairePlatineSabianeService {

    private final RestClientHelper restClientHelper;

    @Override
    public RestClient restClient() {
        return restClientHelper.getRestClient(KNOWN_API_PLATINE_QUESTIONNAIRE);
    }

    @Override
    public Logger getLogger() {
        return log;
    }
}
