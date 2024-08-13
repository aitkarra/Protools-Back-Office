package fr.insee.protools.backend.service.platine.delegate.v2;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.service.platine.questionnaire.PlatineQuestionnaireService;
import fr.insee.protools.backend.service.utils.delegate.DefaultCallServiceInterroListTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlatineQuestionnaireCreateInterrogationListTaskREST extends DefaultCallServiceInterroListTask {

    private final PlatineQuestionnaireService platineQuestionnaireService;

    @Override
    protected void serviceAction(String campaignId,List<JsonNode> list, String... params) {
        platineQuestionnaireService.postSurveyUnits(campaignId,list);
    }

}
