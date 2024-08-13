package fr.insee.protools.backend.service.platine.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.service.exception.IncorrectSUBPMNError;
import fr.insee.protools.backend.dto.rem.REMSurveyUnitDto;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

public class PlatineHelper {

    private static final ObjectMapper objectMapper = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES,false);

    //In platine pilotage, partition ID must start by the campaignId. We decide to follow it with the configured REM partitionID
    public static String computePilotagePartitionID(String campaignId, Long partitionId){
        if(campaignId==null||partitionId==null){
            return null;
        }
        return campaignId+partitionId;
    }

    public static REMSurveyUnitDto parseRemSUNode(ObjectMapper objectMapper, JsonNode remSUNode){
        REMSurveyUnitDto remSurveyUnitDto;
        try {
            remSurveyUnitDto = objectMapper.treeToValue(remSUNode, REMSurveyUnitDto.class);
        } catch (JsonProcessingException e) {
            throw new IncorrectSUBPMNError("Error while parsing the json retrieved from REM  "  ,remSUNode, e);
        }

        if(remSurveyUnitDto.getRepositoryId()==null){
            throw new IncorrectSUBPMNError("Error json retrieved from REM has no repositoryId " , remSUNode);
        }
        return remSurveyUnitDto;
    }


    public static REMSurveyUnitDto parseRemSUNode(JsonNode remSUNode){
        return parseRemSUNode(objectMapper,remSUNode);
    }

    private PlatineHelper(){}
}
