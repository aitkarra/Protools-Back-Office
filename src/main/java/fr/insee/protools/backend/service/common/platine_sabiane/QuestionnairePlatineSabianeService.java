package fr.insee.protools.backend.service.common.platine_sabiane;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.dto.platine_sabiane_questionnaire.NomenclatureDto;
import fr.insee.protools.backend.dto.platine_sabiane_questionnaire.QuestionnaireModelCreateDto;
import fr.insee.protools.backend.dto.platine_sabiane_questionnaire.campaign.CampaignDto;
import fr.insee.protools.backend.dto.platine_sabiane_questionnaire.surveyunit.SurveyUnitResponseDto;
import fr.insee.protools.backend.webclient.WebClientHelper;
import fr.insee.protools.backend.webclient.exception.runtime.WebClient4xxBPMNError;
import fr.insee.protools.backend.webclient.exception.runtime.WebClient5xxBPMNError;
import fr.insee.protools.backend.webclient.exception.runtime.WebClientNullReturnBPMNError;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public interface QuestionnairePlatineSabianeService {

    //Internal methods
    WebClient webClient();
    org.slf4j.Logger getLogger();

    /** Create a new nomenclature **/
    default void postNomenclature(String nomenclatureId,  String nomenclatureLabel , JsonNode nomenclatureValue) {
        NomenclatureDto dto = new NomenclatureDto(nomenclatureId,nomenclatureLabel,nomenclatureValue);
        NomenclatureDto response =
                webClient()
                        .post()
                        .uri("/api/nomenclature")
                        .bodyValue(dto)
                        .retrieve()
                        .bodyToMono(NomenclatureDto.class)
                        .block();
        getLogger().info("postNomenclature: nomenclatureId={} - response={}",nomenclatureId,response);
        //TODO:  gestion des erreurs (ex: 403...)
    }

    /** Create a new questionnaireModel **/
    default void postQuestionnaireModel(String questionnaireId, String questionnaireLabel, JsonNode questionnaireValue, Set<String> requiredNomenclatures) {
        QuestionnaireModelCreateDto dto =
                new QuestionnaireModelCreateDto(questionnaireId,questionnaireLabel,questionnaireValue,requiredNomenclatures);

        QuestionnaireModelCreateDto response =
                webClient()
                        .post()
                        .uri("/api/questionnaire-models")
                        .bodyValue(dto)
                        .retrieve()
                        .bodyToMono(QuestionnaireModelCreateDto.class)
                        .block();
        getLogger().info("postQuestionnaireModel: questionnaireId={} - response={}",questionnaireId,response);
        //TODO:  gestion des erreurs (ex: 403...)

    }

    /** Get the list of existing nomenclatures */
    default Set<String> getNomenclaturesId() {
        List<String> response = webClient()
                .get()
                .uri("/api/nomenclatures")
                .retrieve()
                .bodyToMono(List.class)
                .block();
        getLogger().info("getNomenclaturesId: response= {}",response);
        return (response==null)?new HashSet<>():response.stream().collect(Collectors.toSet());
    }

    /** Checks if the questionnaireModel exists **/
    default boolean questionnaireModelExists(String idQuestionnaireModel) {
        getLogger().info("questionnaireModelExists: idQuestionnaireModel={} ",idQuestionnaireModel);
        boolean modelExists = false;
        try{
            var response = webClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/questionnaire/{id}")
                            .build(idQuestionnaireModel))
                    .retrieve().toBodilessEntity().block();
            if(response==null) {
                throw new WebClientNullReturnBPMNError("Error while checking if questionnaireModel exists - null result");
            }

            if(response.getStatusCode().is2xxSuccessful()) {
                modelExists = true;
            }
            else if(response.getStatusCode().is4xxClientError()){
                if(response.getStatusCode()==HttpStatus.NOT_FOUND){
                    modelExists=false;
                }
                else{
                    throw new WebClient4xxBPMNError("Error while checking if questionnaireModel exists ", response.getStatusCode());
                }
            }
            else{
                throw new WebClient5xxBPMNError("Error while checking if questionnaireModel exists ");
            }
            getLogger().debug("response code={}",response.getStatusCode());
        }
        catch (WebClient4xxBPMNError e){
            if(e.getHttpStatusCodeError().equals(HttpStatus.NOT_FOUND)){
                modelExists=false;
            }
            else {
                throw e;
            }
        }
        getLogger().info("questionnaireModelExists: idQuestionnaireModel={} - modelExists={}",idQuestionnaireModel,modelExists);
        return modelExists;
    }

    /** Create the campaign **/
    default void postCampaign(CampaignDto campaignDto) {
        WebClientHelper.logJson("postCampaign: ", campaignDto,getLogger(), Level.DEBUG);
        //Http Status Codes : https://github.com/InseeFr/Queen-Back-Office/blob/3.5.36-rc/src/main/java/fr/insee/queen/api/controller/CampaignController.java
        // HttpStatus.BAD_REQUEST(400) if campaign already exists
        // HttpStatus.FORBIDDEN (403) if the questionnaire does not exist or is already associated (Request to change it to 409)
        // WARNING : 403 will also be returned if user does not have an authorized role
        try {
            var response = webClient()
                    .post()
                    .uri("/api/campaigns")
                    .bodyValue(campaignDto)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            getLogger().info("postCampaign: idCampaign={} -  response={} ", campaignDto.getId(), response);
        }
        catch (WebClient4xxBPMNError e){
            if(e.getHttpStatusCodeError().equals(HttpStatus.FORBIDDEN)){
                String msg=
                        "Error 403/FORBIDEN during Questionnaire postCampaign."
                                + " It can be caused by a missing permission or if a questionnaire model"
                                + " "+campaignDto.getQuestionnaireIds()
                                +" is already assigned to another campaign."
                                + " msg="+e.getMessage();
                getLogger().error(msg);
                throw new WebClient4xxBPMNError(msg,e.getHttpStatusCodeError());
            }
            else if(e.getHttpStatusCodeError().equals(HttpStatus.BAD_REQUEST)){
                String msg="Error 400/BAD_REQUEST during Questionnaire postCampaign."
                                + " One possible cause is that the campaign already exists "
                                + " msg="+e.getMessage();
                getLogger().error(msg);
                throw new WebClient4xxBPMNError(msg,e.getHttpStatusCodeError());
            }
            //Currently no remediation so just rethrow
            throw e;
        }
    }

    /** Create a survey Unit **/
    default void postSurveyUnit(SurveyUnitResponseDto suDto, String idCampaign) {
        WebClientHelper.logJson("postSurveyUnit: idCampaign="+idCampaign, suDto,getLogger(),Level.DEBUG);
        try {
            //TODO : delete this log
            getLogger().debug("postSurveyUnit: BEFORE - idCampaign={} - idSu={}", idCampaign,suDto.getId());
            var response = webClient()
                    .post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/campaign/{id}/survey-unit")
                            .build(idCampaign))
                    .bodyValue(suDto)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            getLogger().info("postSurveyUnit: AFTER - idCampaign={} - idSu={} - response={} ", idCampaign,suDto.getId(), response);
        }
        catch (WebClient4xxBPMNError e){
            if(e.getHttpStatusCodeError().equals(HttpStatus.BAD_REQUEST)){
                String msg="Error 400/BAD_REQUEST during Questionnaire postSurveyUnit."
                        + " msg="+e.getMessage();
                getLogger().error(msg);
                throw new WebClient4xxBPMNError(msg,e.getHttpStatusCodeError());
            }
            //Currently no remediation so just rethrow
            throw e;
        }
    }
}
