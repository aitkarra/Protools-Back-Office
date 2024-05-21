package fr.insee.protools.backend.service.platine.delegate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.dto.platine.pilotage.PlatineAddressDto;
import fr.insee.protools.backend.dto.platine.pilotage.PlatinePilotageGenderType;
import fr.insee.protools.backend.dto.platine.pilotage.query.ContactAccreditationDto;
import fr.insee.protools.backend.dto.platine.pilotage.query.QuestioningWebclientDto;
import fr.insee.protools.backend.dto.platine.pilotage.questioning.PlatineQuestioningSurveyUnitDto;
import fr.insee.protools.backend.dto.rem.*;
import fr.insee.protools.backend.service.DelegateContextVerifier;
import fr.insee.protools.backend.service.context.ContextService;
import fr.insee.protools.backend.service.context.enums.CampaignContextEnum;
import fr.insee.protools.backend.service.context.enums.PartitionTypeEchantillon;
import fr.insee.protools.backend.service.platine.pilotage.PlatinePilotageService;
import fr.insee.protools.backend.service.platine.utils.PlatineHelper;
import fr.insee.protools.backend.service.rem.RemDtoUtils;
import fr.insee.protools.backend.service.utils.FlowableVariableUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static fr.insee.protools.backend.service.FlowableVariableNameConstants.*;
import static fr.insee.protools.backend.service.context.ContextConstants.*;
import static fr.insee.protools.backend.service.utils.ContextUtils.getCurrentPartitionNode;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlatinePilotageCreateSurveyUnitTask implements JavaDelegate, DelegateContextVerifier {

    private static final ObjectMapper objectMapper = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES,false);

    private final ContextService protoolsContext;
    private final PlatinePilotageService platinePilotageService;

    @Override
        public void execute(DelegateExecution execution) {
            JsonNode contextRootNode = protoolsContext.getContextByProcessInstance(execution.getProcessInstanceId());
            checkContextOrThrow(log,execution.getProcessInstanceId(), contextRootNode);
            String campainId = contextRootNode.path(CTX_CAMPAGNE_ID).asText();
            Long currentPartitionId = FlowableVariableUtils.getVariableOrThrow(execution,VARNAME_CURRENT_PARTITION_ID, Long.class);
            JsonNode remSUNode = FlowableVariableUtils.getVariableOrThrow(execution,VARNAME_REM_SURVEY_UNIT, JsonNode.class);
            String idInternaute = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_DIRECTORYACCESS_ID_CONTACT, String.class);
            JsonNode currentPartitionNode = getCurrentPartitionNode(contextRootNode, currentPartitionId);

            //Create the platine DTO object
            QuestioningWebclientDto dto = computeQuestioningWebclientDto(remSUNode,currentPartitionNode,idInternaute,campainId);

            log.info("ProcessInstanceId={}  campainId={} - currentPartitionId={} - remSUNode.id={} ",
                    execution.getProcessInstanceId(), campainId,currentPartitionId,dto.getSurveyUnit().getIdSu());
            //Call service
            platinePilotageService.putQuestionings(dto);
    }

    private static QuestioningWebclientDto computeQuestioningWebclientDto(JsonNode remSUNode, JsonNode currentPartitionNode,String idInternaute, String idCampagne) {
        REMSurveyUnitDto remSurveyUnitDto=PlatineHelper.parseRemSUNode(objectMapper,VARNAME_REM_SURVEY_UNIT,remSUNode);
        boolean isLogement = currentPartitionNode.path(CTX_PARTITION_TYPE_ECHANTILLON).textValue().equalsIgnoreCase(PartitionTypeEchantillon.LOGEMENT.getAsString());
        PersonDto contact = RemDtoUtils.findContact(remSUNode, remSurveyUnitDto, isLogement);

        PlatineAddressDto platineAdress = computePlatineAdress(remSurveyUnitDto.getAddress());
        ContactAccreditationDto platineContact = convertREMPersonToPlatineContact(idInternaute, platineAdress, contact);

        return
                QuestioningWebclientDto.builder()
                        .idPartitioning(PlatineHelper.computePilotagePartitionID(idCampagne,currentPartitionNode.path(CTX_PARTITION_ID).asLong()))
                        .modelName(currentPartitionNode.path(CTX_PARTITION_QUESTIONNAIRE_MODEL).asText())
                        .surveyUnit(computeSurveyUnitDto(remSurveyUnitDto))
                        .contacts(List.of(platineContact))
                        .build();
    }



    private static PlatineQuestioningSurveyUnitDto computeSurveyUnitDto(REMSurveyUnitDto remSurveyUnitDto) {

        return PlatineQuestioningSurveyUnitDto.builder()
                .idSu(remSurveyUnitDto.getRepositoryId().toString())
                .identificationCode("")
                .identificationName("")
                .address(computePlatineAdress(remSurveyUnitDto.getAddress()))
                .build();
    }

    @Override
    public Set<String> getContextErrors(JsonNode contextRootNode) {
        if(contextRootNode==null){
            return Set.of("Context is missing");
        }
        Set<String> results=new HashSet<>();
        Set<String> requiredNodes =
                Set.of(
                        //Global & Campaign
                        CTX_CAMPAGNE_ID,CTX_CAMPAGNE_CONTEXTE, CTX_PARTITIONS
                );
        Set<String> requiredPartition =
                Set.of(CTX_PARTITION_ID,CTX_PARTITION_QUESTIONNAIRE_MODEL, CTX_PARTITION_TYPE_ECHANTILLON);


        results.addAll(DelegateContextVerifier.computeMissingChildrenMessages(requiredNodes,contextRootNode,getClass()));
        if (!contextRootNode.path(CTX_CAMPAGNE_CONTEXTE).asText().equalsIgnoreCase(CampaignContextEnum.HOUSEHOLD.getAsString())) {
            results.add(DelegateContextVerifier.computeIncorrectMessage(CTX_CAMPAGNE_CONTEXTE,"contexte for platine can only be "+CampaignContextEnum.HOUSEHOLD,getClass()));
        }

        //Maybe one day we will have partitions for platine and partitions for sabiane and we will only validate the platine ones
        //Partitions
        var partitionIterator =contextRootNode.path(CTX_PARTITIONS).elements();
        while (partitionIterator.hasNext()) {
            var partitionNode = partitionIterator.next();
            results.addAll(DelegateContextVerifier.computeMissingChildrenMessages(requiredPartition,partitionNode,getClass()));

            //Check value of Enum
            String enumVal = partitionNode.path(CTX_PARTITION_TYPE_ECHANTILLON).asText();
            if(! EnumUtils.isValidEnumIgnoreCase(PartitionTypeEchantillon.class, enumVal)){
                results.add(DelegateContextVerifier.computeIncorrectEnumMessage(CTX_PARTITION_TYPE_ECHANTILLON,enumVal,Arrays.toString(PartitionTypeEchantillon.values()),getClass()));
            }
        }
        return results;
    }

    /**
     * Compute the lastname to send to platine.
     * @param lastname from rem
     * @param birthname from rem
     * @return the computed platine last name
     */
    protected static String getPlatineLastname(String lastname, String birthname){
        if(lastname!=null)
            return lastname;
        else if(birthname!=null)
            return birthname;
        return "";
    }

    private static ContactAccreditationDto convertREMPersonToPlatineContact(String idInternaute, PlatineAddressDto platineAdress, PersonDto remPersonDto) {
        //If lastname is null then we use birthname
        String platineName = getPlatineLastname(remPersonDto.getLastName(),remPersonDto.getBirthName());
        return ContactAccreditationDto.builder()
                .identifier(idInternaute)
                .externalId(remPersonDto.getExternalId())
                .function("")
                .lastName(platineName)
                .firstName(remPersonDto.getFirstName())
                .isMain(true)
                .civility(convertREMGenderToPlatineCivility(remPersonDto.getGender()).getLabel())
                //Get favorite phone/mail ; if no favorite get the first of the list ; else empty
                .email(remPersonDto.getEmails().stream()
                        .filter(EmailDto::getFavorite)
                        .findFirst()
                        .orElse(remPersonDto.getEmails().stream().findFirst().orElse(new EmailDto()))
                        .getMailAddress())

                .phone(
                        remPersonDto.getPhoneNumbers().stream()
                                .filter(PhoneNumberDto::getFavorite)
                                .findFirst()
                                .orElse(remPersonDto.getPhoneNumbers().stream().findFirst().orElse(new PhoneNumberDto()))
                                .getNumber())
                .address(platineAdress)
                .build();
    }

    static PlatinePilotageGenderType convertREMGenderToPlatineCivility(String remGender) {
        if(remGender==null)
            return PlatinePilotageGenderType.Undefined;
        return switch (remGender) {
            case "2":
                yield PlatinePilotageGenderType.Female;
            case "1" :
                yield PlatinePilotageGenderType.Male;
            default:
                yield PlatinePilotageGenderType.Undefined;
        };
    }

    private static PlatineAddressDto computePlatineAdress(REMAddressDto remAddressDto) {
        //The REM  REMAddressDto and Platine PlatineQuestioningSurveyUnitDto are identical
        //I'll use the copyProperty to recopy identical members
        var platineAdresse = BeanUtils.instantiateClass(PlatineAddressDto.class);
        BeanUtils.copyProperties(remAddressDto, platineAdresse);
        return platineAdresse;
    }

}
