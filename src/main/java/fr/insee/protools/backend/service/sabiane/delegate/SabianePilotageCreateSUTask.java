package fr.insee.protools.backend.service.sabiane.delegate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.dto.rem.*;
import fr.insee.protools.backend.dto.sabiane.pilotage.*;
import fr.insee.protools.backend.service.DelegateContextVerifier;
import fr.insee.protools.backend.service.context.ContextService;
import fr.insee.protools.backend.service.context.enums.PartitionTypeEchantillon;
import fr.insee.protools.backend.service.exception.IncorrectSUBPMNError;
import fr.insee.protools.backend.service.platine.utils.PlatineHelper;
import fr.insee.protools.backend.service.rem.RemDtoUtils;
import fr.insee.protools.backend.service.sabiane.SabianeIdHelper;
import fr.insee.protools.backend.service.sabiane.pilotage.SabianePilotageService;
import fr.insee.protools.backend.service.utils.FlowableVariableUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_CURRENT_PARTITION_ID;
import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_REM_INTERROGATION;
import static fr.insee.protools.backend.service.context.ContextConstants.*;
import static fr.insee.protools.backend.service.utils.ContextUtils.getCurrentPartitionNode;

@Slf4j
@Component
@RequiredArgsConstructor
public class SabianePilotageCreateSUTask implements JavaDelegate, DelegateContextVerifier {
    
    private final ContextService protoolsContext;
    private final SabianePilotageService sabianePilotageService;
    
    private static final DateTimeFormatter birthdateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId parisTimezone = ZoneId.of("Europe/Paris");
    private static final ObjectMapper objectMapper = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false).configure(FAIL_ON_MISSING_CREATOR_PROPERTIES, true);

    @Override
    public void execute(DelegateExecution execution) {
        JsonNode contextRootNode = protoolsContext.getContextByProcessInstance(execution.getProcessInstanceId());
        checkContextOrThrow(log, execution.getProcessInstanceId(), contextRootNode);

        Long currentPartitionId = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_CURRENT_PARTITION_ID, Long.class);
        JsonNode remSUNode = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_REM_INTERROGATION, JsonNode.class);
        JsonNode currentPartitionNode = getCurrentPartitionNode(contextRootNode, currentPartitionId);

        Boolean priority = currentPartitionNode.path(CTX_PARTITION_PRIORITAIRE).asBoolean();
        boolean isLogement = currentPartitionNode.path(CTX_PARTITION_TYPE_ECHANTILLON).textValue().equalsIgnoreCase(PartitionTypeEchantillon.LOGEMENT.getAsString());

        //Create the DTO object
        SurveyUnitContextDto dto = createSabianeSUContextDto(contextRootNode, currentPartitionId, remSUNode, isLogement, priority);

        log.info("ProcessInstanceId={} - currentPartitionId={} - remSU.id={}", execution.getProcessInstanceId(), currentPartitionId, dto.getId());

        //Call service
        sabianePilotageService.postSurveyUnits(List.of(dto));

        log.debug("ProcessInstanceId={}  end", execution.getProcessInstanceId());
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
                        CTX_CAMPAGNE_ID, CTX_PARTITIONS
                );
        Set<String> requiredPartition =
                Set.of(CTX_PARTITION_ID, CTX_PARTITION_TYPE_ECHANTILLON,CTX_PARTITION_PRIORITAIRE);

        results.addAll(DelegateContextVerifier.computeMissingChildrenMessages(requiredNodes,contextRootNode,getClass()));

        //Partitions
        if(!contextRootNode.path(CTX_PARTITIONS).isArray() || contextRootNode.path(CTX_PARTITIONS).isEmpty()){
            results.add(DelegateContextVerifier.computeIncorrectMessage(CTX_PARTITIONS," should be a non empty array", getClass()));
        }
        else{
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
        }

        return results;
    }

    /**
     * Compute a timestamp considering that {@code remBirthdate} is localized in
     * "Europe/Paris" timezone and is at the start of day
     * @param remBirthdate A birthdate. Can be of the form
     *      <ul>
     *          <li>yyyyMMdd - ex: 20110823</li>
     *          <li>yyyyMM then assume dd is 01 - ex 201108 ==> 20110801</li>
     *          <li>yyyy then assume MM is 01 and dd is 01 - ex: 2011 ==> 20110101</li>
     *      </ul>
     * @return the unix timestamp (number of seconds since epoch) corresponding to this birthdate
     */
    protected static Long computeSabianeBirthDateFromRem(String remBirthdate) {
        if(StringUtils.isBlank(remBirthdate))
            return null;

        String adjudstedBirthDate = remBirthdate.trim();
        int len = adjudstedBirthDate.length();
        switch (len) {
            case 4:
                adjudstedBirthDate += "0101";//If 4 char (ex: 2009) consider 1st junary
                break;
            case 6:
                adjudstedBirthDate += "01";//If 6 char (ex: 200912) : consider 1st day of month
                break;
            default:
        }
        try {
            LocalDate localDate = LocalDate.parse(adjudstedBirthDate, birthdateFormatter);
            ZonedDateTime zonedDateTime = localDate.atStartOfDay(parisTimezone);
            return zonedDateTime.toInstant().getEpochSecond();
        } catch (DateTimeParseException e) {
            throw new IncorrectSUBPMNError("Error while parsing the json retrieved from REM : the birthdate" + remBirthdate + " is incorrect.");
        }
    }

    private static SabianePhoneNumberDto createSabianePhoneNumberFromREM(PhoneNumberDto remPhone, Source sabianeSource) {
        return new SabianePhoneNumberDto(sabianeSource, remPhone.getFavorite(), remPhone.getNumber());
    }

    protected static List<SabianePhoneNumberDto> createSabianePhoneList(List<PhoneNumberDto> remPhones) {
        List<SabianePhoneNumberDto> res = new ArrayList<>(2);
        //Get the favorites numbers (excluding interviewers ones)
        List<PhoneNumberDto> favoritesNumeros =
                remPhones.stream()
                        .filter(e -> !REMPhoneSource.INTERVIEWER.equals(e.getSource()))
                        .filter(e->Boolean.TRUE.equals(e.getFavorite()))
                        .toList();

        //INTERVIEWER : Keep all phones numbers
        remPhones.stream()
                .filter(e -> REMPhoneSource.INTERVIEWER.equals(e.getSource()))
                .forEach(phoneNumberDto ->  res.add(createSabianePhoneNumberFromREM(phoneNumberDto, Source.INTERVIEWER)));


        List<PhoneNumberDto> nonInterviewerNonFavoriteByPriority =
                remPhones.stream()
                        .filter(e->Boolean.FALSE.equals(e.getFavorite()))
                        //Discard interviewer as they are handled just before
                        .filter(phoneNumberDto -> !(phoneNumberDto.getSource().equals(REMPhoneSource.INTERVIEWER)))
                        .sorted(Comparator.comparingInt(obj -> {
                            //Selection priority order INITIAL>DIRECTORY>OTHER
                            switch (obj.getSource()) {
                                case INITIAL:
                                    return 0;
                                case DIRECTORY:
                                    return 1;
                                case OTHER:
                                    return 2;
                                default:
                                    return 3; // Handle other cases if necessary
                            }
                        }))
                        .collect(Collectors.toCollection(ArrayList::new));//Must be mutable

        switch (favoritesNumeros.size()) {
            case 0://no favorite number : FILL FISCAL and DIRECTORY using priority (initial, directory, other)
                if (!nonInterviewerNonFavoriteByPriority.isEmpty()) {
                    PhoneNumberDto remInitialNonFavorite = nonInterviewerNonFavoriteByPriority.remove(0);
                    res.add(createSabianePhoneNumberFromREM(remInitialNonFavorite, Source.FISCAL));
                }
                if (!nonInterviewerNonFavoriteByPriority.isEmpty()) {
                    PhoneNumberDto remInitialNonFavorite = nonInterviewerNonFavoriteByPriority.remove(0);
                    res.add(createSabianePhoneNumberFromREM(remInitialNonFavorite, Source.DIRECTORY));
                }
                break;
            case 1:
                res.add(createSabianePhoneNumberFromREM(favoritesNumeros.get(0), Source.FISCAL));
                if (!nonInterviewerNonFavoriteByPriority.isEmpty()) {
                    PhoneNumberDto remInitialNonFavorite = nonInterviewerNonFavoriteByPriority.remove(0);
                    res.add(createSabianePhoneNumberFromREM(remInitialNonFavorite, Source.DIRECTORY));
                }
                break;
            case 2:
                res.add(createSabianePhoneNumberFromREM(favoritesNumeros.get(0), Source.FISCAL));
                res.add(createSabianePhoneNumberFromREM(favoritesNumeros.get(1), Source.DIRECTORY));
                break;
            default: //More than 2 favorite numbers
                throw new IncorrectSUBPMNError("Error while parsing the json retrieved from REM : more than 2 favorite phone number");
        }
        return res;
    }

    private static SabianePersonDto createSabianePersonFromRemPerson(PersonDto remPerson, Boolean isPrivileged) {
        return SabianePersonDto.builder()
                .title(convertREMGenderToSabianeCivilityTitle(remPerson.getGender()))
                .firstName(remPerson.getFirstName())
                .lastName(remPerson.getLastName())
                .email(remPerson.getEmails().stream().findFirst().map(EmailDto::getMailAddress).orElse(""))
                .birthdate(computeSabianeBirthDateFromRem(remPerson.getDateOfBirth()))
                .favoriteEmail(Boolean.FALSE).privileged(isPrivileged)
                .phoneNumbers(createSabianePhoneList(remPerson.getPhoneNumbers()))
                .build();
    }

    protected static SurveyUnitContextDto createSabianeSUContextDto(JsonNode contextRootNode, Long currentPartitionId, JsonNode remSUNode, Boolean isLogement, Boolean priority) {
        REMSurveyUnitDto remSurveyUnitDto = PlatineHelper.parseRemSUNode(objectMapper,  remSUNode);
        Pair<PersonDto, Optional<PersonDto>> mainAndSecondaryPerson = RemDtoUtils.findContactAndSecondary(remSUNode, remSurveyUnitDto, isLogement);
        String id = SabianeIdHelper.computeSabianeID(currentPartitionId.toString(), remSurveyUnitDto.getRepositoryId().toString());

        List<SabianePersonDto> sabianePersons = new ArrayList<>(2);
        sabianePersons.add(createSabianePersonFromRemPerson(mainAndSecondaryPerson.getLeft(), true)); //first is privileged
        mainAndSecondaryPerson.getRight().ifPresent(personDto -> sabianePersons.add(createSabianePersonFromRemPerson(personDto, false)));

        //Organisation Unit (probably useless in production)
        Optional<String> poleGestionOpale = RemDtoUtils.searchAdditionalInformation(RemDtoUtils.REM_ADDITIONALINFOS_POLE_GESTION_OPALE,remSUNode);

        String noGrap = (remSurveyUnitDto.getOtherIdentifier()==null)?null:remSurveyUnitDto.getOtherIdentifier().getNograp();
        return SurveyUnitContextDto.builder()
                .id(id)
                .persons(sabianePersons)
                .address(computeSabianeAdress(remSurveyUnitDto.getAddress(), mainAndSecondaryPerson.getLeft()))
                .organizationUnitId(poleGestionOpale.orElse(null))
                .priority(priority)
                .campaign(contextRootNode.path(CTX_CAMPAGNE_ID).textValue())
                .sampleIdentifiers(
                        SampleIdentifiersDto.builder()
                                .autre("0")
                                .bs(0)
                                .ec("0")
                                .le(0)
                                .nograp(noGrap).noi(0).nole(0).nolog(0).numfa(0).rges(0).ssech(currentPartitionId).build())
                .states(List.of(StateDto.builder().date(Instant.now().toEpochMilli()).type(StateType.NVM).build()))
                .build();
    }

    /**
     * @param str the string to parse
     * @return l2 and l3 part of an address by splitting {@code str} at 38 char. If {@code str} is null or empty
     *          return a pair of empty strings
     */
    protected static Pair<String, String> computeL2L3(String str) {
        if (str == null || str.isEmpty() || str.isBlank()) {
            return Pair.of(StringUtils.EMPTY, StringUtils.EMPTY);
        }
        if (str.length() <= 38) {
            return Pair.of(str, StringUtils.EMPTY);
        }
        return Pair.of(str.substring(0, 38), str.substring(38));
    }

    /**
     * @param remGender
     * @return A {@code SabianeTitle} for the passed string (supposedly a rem gender). MISTER if value is 1. MISS in every other case
     */
    protected static SabianeTitle convertREMGenderToSabianeCivilityTitle(String remGender) {
        if(remGender == null){
            return SabianeTitle.MISS;
        }
        return switch (remGender) {
            //TODO: enable when pass to java 19 (preview in 17)
            //case null:
            //    yield SabianeTitle.MISS;
            case "1":
                yield SabianeTitle.MISTER;
            default:
                yield SabianeTitle.MISS;
        };
    }

    protected static AddressDto computeSabianeAdress(REMAddressDto remAddressDto, PersonDto contact) {

        //l1 : “M” si REM.person.gender = "1" ; “MME” sinon   +    “firstName” +  “lastName” (pour la personne mise en privileged)
        StringBuilder l1Builder = new StringBuilder(convertREMGenderToSabianeCivilityTitle(contact.getGender()).getFrenchCivility());
        l1Builder.append(org.apache.commons.lang3.StringUtils.SPACE);
        if(!StringUtils.isBlank(contact.getFirstName())){
            l1Builder.append(contact.getFirstName());
            l1Builder.append(org.apache.commons.lang3.StringUtils.SPACE);
        }
        if(!StringUtils.isBlank(contact.getLastName())){
            l1Builder.append(contact.getLastName());
        }
        String l1=l1Builder.toString().trim();

        //l2 : partie de “REM.address.addressSupplement” <=38 caractères
        //l3 : partie de “REM.address.addressSupplement” > 38 caractères
        Pair<String, String> l2L3 = computeL2L3(remAddressDto.getAddressSupplement());
        String l2 = l2L3.getLeft();
        String l3 = l2L3.getRight();

        //l4: “REM.address.streetNumber” + “REM.address.repetitionIndex” + “REM.address.streetType” + “REM.address.streetName”
        StringBuilder l4Builder = new StringBuilder();
        if(!StringUtils.isBlank(remAddressDto.getStreetNumber())){
            l4Builder.append(remAddressDto.getStreetNumber());
            l4Builder.append(org.apache.commons.lang3.StringUtils.SPACE);
        }
        if(!StringUtils.isBlank(remAddressDto.getRepetitionIndex())){
            l4Builder.append(remAddressDto.getRepetitionIndex());
            l4Builder.append(org.apache.commons.lang3.StringUtils.SPACE);
        }
        if(!StringUtils.isBlank(remAddressDto.getStreetType())){
            l4Builder.append(remAddressDto.getStreetType());
            l4Builder.append(org.apache.commons.lang3.StringUtils.SPACE);
        }
        if(!StringUtils.isBlank(remAddressDto.getStreetName())){
            l4Builder.append(remAddressDto.getStreetName());
        }
        String l4 = l4Builder.toString().trim();

        //l5: “REM.address.specialDistribution”
        String l5 = (StringUtils.isBlank(remAddressDto.getSpecialDistribution()) ? "" : remAddressDto.getSpecialDistribution());

        //l6: “REM.address.zipCode” + “REM.address.cityName”
        StringBuilder l6Builder = new StringBuilder();
        if(!StringUtils.isBlank(remAddressDto.getZipCode())){
            l6Builder.append(remAddressDto.getZipCode());
            l6Builder.append(org.apache.commons.lang3.StringUtils.SPACE);
        }
        if(!StringUtils.isBlank(remAddressDto.getCityName())){
            l6Builder.append(remAddressDto.getCityName());
        }
        String l6 = l6Builder.toString().trim();

        //l7: REM.countryName  ou “FRANCE” si null
        String l7 = (StringUtils.isBlank(remAddressDto.getCountryName()) ? "FRANCE" : remAddressDto.getCountryName());

        if(remAddressDto.getLocationHelp()!=null) {
            return AddressDto.builder().elevator(remAddressDto.getLocationHelp().getElevator()).building(remAddressDto.getLocationHelp().getBuilding())
                    .floor(remAddressDto.getLocationHelp().getFloor()).door(remAddressDto.getLocationHelp().getDoor()).staircase(remAddressDto.getLocationHelp().getStaircase())
                    .cityPriorityDistrict(remAddressDto.getLocationHelp().getCityPriorityDistrict())
                    .l1(l1).l2(l2).l3(l3).l4(l4).l5(l5).l6(l6).l7(l7).build();
        }
        else{
            return AddressDto.builder().l1(l1).l2(l2).l3(l3).l4(l4).l5(l5).l6(l6).l7(l7).build();
        }
    }
}
