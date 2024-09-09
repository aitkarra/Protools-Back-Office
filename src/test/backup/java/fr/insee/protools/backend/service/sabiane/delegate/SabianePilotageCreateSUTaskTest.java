package fr.insee.protools.backend.service.sabiane.delegate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.insee.protools.backend.ProtoolsTestUtils;
import fr.insee.protools.backend.dto.rem_tmp.*;
import fr.insee.protools.backend.dto.sabiane.pilotage.*;
import fr.insee.protools.backend.service.context.exception.BadContextIncorrectBPMNError;
import fr.insee.protools.backend.service.exception.IncorrectSUBPMNError;
import fr.insee.protools.backend.service.sabiane.SabianeIdHelper;
import fr.insee.protools.backend.service.sabiane.pilotage.SabianePilotageService;
import fr.insee.protools.backend.service.utils.TestWithContext;
import fr.insee.protools.backend.service.utils.data.CtxExamples;
import fr.insee.protools.backend.service.utils.data.RemSUData;
import org.apache.commons.lang3.tuple.Pair;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.delegate.DelegateExecution;
import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_CURRENT_PARTITION_ID;
import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_REM_INTERROGATION;
import static fr.insee.protools.backend.service.utils.FlowableVariableUtils.getMissingVariableMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SabianePilotageCreateSUTaskTest extends TestWithContext {

    static final String ctx_partition1 = "99";
    static final String minimal_ctx_ok  =
            """   
                    {    "id": "TEST_ID" , "partitions": [{ "id":99, "typeEchantillon": "logement" , "prioritaire": "false"}] }
            """;

    @Mock SabianePilotageService sabianePilotageService;

    @InjectMocks
    SabianePilotageCreateSUTask sabianePilotageTask;

    @Test
    void execute_should_throwError_when_null_context(){
        assertThat_delegate_throwError_when_null_context(sabianePilotageTask);
    }

    @Test
    @DisplayName("convertREMGenderToSabianeCivilityTitle should return MISS when param '2' ; 'MISTER' when param is '1' and throw in other cases")
    void convertREMGenderToSabianeCivilityTitle_should_ReturnCorrectValues() {
        assertThat(SabianePilotageCreateSUTask.convertREMGenderToSabianeCivilityTitle("1")).isEqualTo(SabianeTitle.MISTER);
        assertThat(SabianePilotageCreateSUTask.convertREMGenderToSabianeCivilityTitle("1").getSabianeTitle()).isEqualTo("Mister");
        assertThat(SabianePilotageCreateSUTask.convertREMGenderToSabianeCivilityTitle("1").getFrenchCivility()).isEqualTo("M");

        assertThat(SabianePilotageCreateSUTask.convertREMGenderToSabianeCivilityTitle("2")).isEqualTo(SabianeTitle.MISS);
        assertThat(SabianePilotageCreateSUTask.convertREMGenderToSabianeCivilityTitle("2").getSabianeTitle()).isEqualTo("MISS");
        assertThat(SabianePilotageCreateSUTask.convertREMGenderToSabianeCivilityTitle("2").getFrenchCivility()).isEqualTo("MME");

        assertThat(SabianePilotageCreateSUTask.convertREMGenderToSabianeCivilityTitle("2 ")).isEqualTo(SabianeTitle.MISS);
        assertThat(SabianePilotageCreateSUTask.convertREMGenderToSabianeCivilityTitle(" 2 ")).isEqualTo(SabianeTitle.MISS);
        assertThat(SabianePilotageCreateSUTask.convertREMGenderToSabianeCivilityTitle("3")).isEqualTo(SabianeTitle.MISS);
        assertThat(SabianePilotageCreateSUTask.convertREMGenderToSabianeCivilityTitle("23JZKEOSJF")).isEqualTo(SabianeTitle.MISS);
        assertThat(SabianePilotageCreateSUTask.convertREMGenderToSabianeCivilityTitle("0")).isEqualTo(SabianeTitle.MISS);
        assertThat(SabianePilotageCreateSUTask.convertREMGenderToSabianeCivilityTitle("-1")).isEqualTo(SabianeTitle.MISS);
        assertThat(SabianePilotageCreateSUTask.convertREMGenderToSabianeCivilityTitle("-2")).isEqualTo(SabianeTitle.MISS);
        assertThat(SabianePilotageCreateSUTask.convertREMGenderToSabianeCivilityTitle("& ukdslw,kvlk,l")).isEqualTo(SabianeTitle.MISS);
        assertThat(SabianePilotageCreateSUTask.convertREMGenderToSabianeCivilityTitle("")).isEqualTo(SabianeTitle.MISS);
        assertThat(SabianePilotageCreateSUTask.convertREMGenderToSabianeCivilityTitle(null)).isEqualTo(SabianeTitle.MISS);
    }

    @Test
    void createSabianePhoneList_should_ReturnCorrectValues_2favorites_initial() {
        //Prepare
        List<PhoneNumberDto> remPhones1 =
                List.of(
                        PhoneNumberDto.builder().source(REMPhoneSource.INITIAL).number("01").favorite(Boolean.TRUE).build(),
                        PhoneNumberDto.builder().source(REMPhoneSource.INITIAL).number("02").favorite(Boolean.TRUE).build(),
                        PhoneNumberDto.builder().source(REMPhoneSource.INITIAL).number("03").favorite(Boolean.FALSE).build(),
                        PhoneNumberDto.builder().source(REMPhoneSource.DIRECTORY).number("04").favorite(Boolean.FALSE).build(),
                        PhoneNumberDto.builder().source(REMPhoneSource.DIRECTORY).number("05").favorite(Boolean.FALSE).build(),
                        PhoneNumberDto.builder().source(REMPhoneSource.INTERVIEWER).number("06").favorite(Boolean.TRUE).build(),
                        PhoneNumberDto.builder().source(REMPhoneSource.INTERVIEWER).number("07").favorite(Boolean.FALSE).build(),
                        PhoneNumberDto.builder().source(REMPhoneSource.INTERVIEWER).number("08").favorite(Boolean.TRUE).build(),
                        PhoneNumberDto.builder().source(REMPhoneSource.OTHER).number("09").favorite(Boolean.FALSE).build());

        Set<SabianePhoneNumberDto> expectedSabiane1 =
                Set.of(
                        SabianePhoneNumberDto.builder().source(Source.FISCAL).number("01").favorite(Boolean.TRUE).build(),
                        SabianePhoneNumberDto.builder().source(Source.DIRECTORY).number("02").favorite(Boolean.TRUE).build(),
                        SabianePhoneNumberDto.builder().source(Source.INTERVIEWER).number("06").favorite(Boolean.TRUE).build(),
                        SabianePhoneNumberDto.builder().source(Source.INTERVIEWER).number("07").favorite(Boolean.FALSE).build(),
                        SabianePhoneNumberDto.builder().source(Source.INTERVIEWER).number("08").favorite(Boolean.TRUE).build()
                );
        //Execute the unit under test
        List<SabianePhoneNumberDto> res = SabianePilotageCreateSUTask.createSabianePhoneList(new ArrayList<>(remPhones1));
        //Post conditions
        assertEquals(expectedSabiane1.size(), res.size());
        assertTrue(res.containsAll(expectedSabiane1));

    }

    @Test
    void createSabianePhoneList_should_ReturnCorrectValues_noFavorite() {
        //Prepare
        List<PhoneNumberDto> remPhones1 =
                List.of(
                        PhoneNumberDto.builder().source(REMPhoneSource.INITIAL).number("01").favorite(Boolean.FALSE).build(),
                        PhoneNumberDto.builder().source(REMPhoneSource.INITIAL).number("02").favorite(Boolean.FALSE).build(),
                        PhoneNumberDto.builder().source(REMPhoneSource.INITIAL).number("03").favorite(Boolean.FALSE).build()
                );
        Set<SabianePhoneNumberDto> expectedSabiane1 =
                Set.of(
                        SabianePhoneNumberDto.builder().source(Source.FISCAL).number("01").favorite(Boolean.FALSE).build(),
                        SabianePhoneNumberDto.builder().source(Source.DIRECTORY).number("02").favorite(Boolean.FALSE).build()
                );
        //Execute the unit under test
        List<SabianePhoneNumberDto> res = SabianePilotageCreateSUTask.createSabianePhoneList(new ArrayList<>(remPhones1));
        //Post conditions
        assertEquals(expectedSabiane1.size(), res.size());
        assertTrue(res.containsAll(expectedSabiane1));
    }

    @Test
    void createSabianePhoneList_should_ReturnCorrectValues_1Favorite() {
        //Prepare
        List<PhoneNumberDto> remPhones1 =
                List.of(
                        PhoneNumberDto.builder().source(REMPhoneSource.INITIAL).number("01").favorite(Boolean.FALSE).build(),
                        PhoneNumberDto.builder().source(REMPhoneSource.INITIAL).number("02").favorite(Boolean.FALSE).build(),
                        PhoneNumberDto.builder().source(REMPhoneSource.INITIAL).number("03").favorite(Boolean.TRUE).build()
                );
        Set<SabianePhoneNumberDto> expectedSabiane1 =
                Set.of(
                        SabianePhoneNumberDto.builder().source(Source.FISCAL).number("03").favorite(Boolean.TRUE).build(),
                        SabianePhoneNumberDto.builder().source(Source.DIRECTORY).number("01").favorite(Boolean.FALSE).build()
                );
        //Execute the unit under test
        List<SabianePhoneNumberDto> res = SabianePilotageCreateSUTask.createSabianePhoneList(new ArrayList<>(remPhones1));
        //Post conditions
        assertEquals(expectedSabiane1.size(), res.size());
        assertTrue(res.containsAll(expectedSabiane1));
    }

    @Test
    void createSabianePhoneList_should_ReturnCorrectValues_1InterviewerFavorite() {
        //Prepare
        List<PhoneNumberDto> remPhones1 =
                List.of(
                        PhoneNumberDto.builder().source(REMPhoneSource.INITIAL).number("01").favorite(Boolean.FALSE).build(),
                        PhoneNumberDto.builder().source(REMPhoneSource.INITIAL).number("02").favorite(Boolean.FALSE).build(),
                        PhoneNumberDto.builder().source(REMPhoneSource.INITIAL).number("03").favorite(Boolean.FALSE).build(),
                        PhoneNumberDto.builder().source(REMPhoneSource.INTERVIEWER).number("04").favorite(Boolean.TRUE).build()
                );
        Set<SabianePhoneNumberDto> expectedSabiane1 =
                Set.of(
                        SabianePhoneNumberDto.builder().source(Source.FISCAL).number("01").favorite(Boolean.FALSE).build(),
                        SabianePhoneNumberDto.builder().source(Source.DIRECTORY).number("02").favorite(Boolean.FALSE).build(),
                        SabianePhoneNumberDto.builder().source(Source.INTERVIEWER).number("04").favorite(Boolean.TRUE).build()

                );
        //Execute the unit under test
        List<SabianePhoneNumberDto> res = SabianePilotageCreateSUTask.createSabianePhoneList(new ArrayList<>(remPhones1));
        assertEquals(expectedSabiane1.size(), res.size());
        assertTrue(res.containsAll(expectedSabiane1));
    }


    @Test
    void createSabianePhoneList_should_throw_moreThan2Favorites() {
        //Prepare
        List<PhoneNumberDto> remPhones1 =
                List.of(
                        PhoneNumberDto.builder().source(REMPhoneSource.INITIAL).number("01").favorite(Boolean.FALSE).build(),
                        PhoneNumberDto.builder().source(REMPhoneSource.INITIAL).number("02").favorite(Boolean.TRUE).build(),
                        PhoneNumberDto.builder().source(REMPhoneSource.INITIAL).number("03").favorite(Boolean.TRUE).build(),
                        PhoneNumberDto.builder().source(REMPhoneSource.INITIAL).number("04").favorite(Boolean.TRUE).build()
                );

        //Execute the unit under test
        assertThrows(IncorrectSUBPMNError.class, () ->SabianePilotageCreateSUTask.createSabianePhoneList(new ArrayList<>(remPhones1)));
    }

    @Test
    void createSabianePhoneList_should_throw_moreThan2Favorites_excludingInterviewers() {
        //Prepare
        List<PhoneNumberDto> remPhones1 =
                List.of(
                        PhoneNumberDto.builder().source(REMPhoneSource.INITIAL).number("01").favorite(Boolean.FALSE).build(),
                        PhoneNumberDto.builder().source(REMPhoneSource.INITIAL).number("02").favorite(Boolean.TRUE).build(),
                        PhoneNumberDto.builder().source(REMPhoneSource.INITIAL).number("03").favorite(Boolean.TRUE).build(),
                        PhoneNumberDto.builder().source(REMPhoneSource.INTERVIEWER).number("04").favorite(Boolean.TRUE).build()
                );

        //Execute the unit under test
        assertDoesNotThrow(() ->SabianePilotageCreateSUTask.createSabianePhoneList(new ArrayList<>(remPhones1)));
    }

    @ParameterizedTest
    @CsvSource(
            {       "20110808, 1312754400",
                    "20110801, 1312149600",
                    "201108, 1312149600",
                    "2000, 946681200",
                    ","
            })
    void computeSabianeBirthDateFromRem_should_ReturnCorrectValues(String remBirthdateInput, Long expectedOutputTimestampMs) {
        //Prepare
        //Call method under test
        Long result = SabianePilotageCreateSUTask.computeSabianeBirthDateFromRem(remBirthdateInput);
        // Post status
        assertEquals(expectedOutputTimestampMs, result, "The computed timestamp for the birthdate : " + remBirthdateInput + " is incorrect");
    }

    @ParameterizedTest
    @ValueSource(strings = {"20001", "anabab", "200099", "2011010111111",  "2", "200", "2011a", "1600010a", ",", "01122024"})
    // Les valeurs à tester
    void computeSabianeBirthDateFromRem_should_Throw_when_dateIsIncorrect(String inccorectDate) {
        assertThrows(IncorrectSUBPMNError.class, () -> SabianePilotageCreateSUTask.computeSabianeBirthDateFromRem(inccorectDate));
    }


    @ParameterizedTest
    @CsvSource(
            {"0123456789012345678901234567891234567abcd,0123456789012345678901234567891234567a,bcd",
                    "'','',''",
                    "'           ','',''",
                    "'                                                                                        ','',''",
                    "0123456789012345678901234567891234567, 0123456789012345678901234567891234567,''"
            })
    @DisplayName("computeL2L3 should cut the string at 38th caracter (included in L2)")
    void computeL2L3_should_ReturnCorrectValues(String input, String expectedL2, String expectedL3) {
        //Prepare
        //Call method under test
        Pair<String, String> result = SabianePilotageCreateSUTask.computeL2L3(input);
        // Post status
        assertEquals(Pair.of(expectedL2, expectedL3), result, "The computed L2 & L3 for input " + input + " is incorrect");
    }

    // Méthode statique fournissant des données de test
    static Stream<Arguments> providedREMAdressAndContactAndExpectedResults() {
        return Stream.of(
                Arguments.of(
                        REMAddressDto.builder().streetNumber("01").repetitionIndex("bis").streetType("rue")
                                .streetName("des lilas").specialDistribution("specialDistribution")
                                .zipCode("59000").cityName("Lille")/*.countryName("FRANCE")*/
                                .addressSupplement("SupplementAdresse")
                                .locationHelp(null).build()
                        , PersonDto.builder().gender("1").firstName("FirstName").lastName("LastName").build(),
                        AddressDto.builder()
                                .l1("M FirstName LastName")
                                .l2("SupplementAdresse").l3("")
                                .l4("01 bis rue des lilas")
                                .l5("specialDistribution")
                                .l6("59000 Lille")
                                .l7("FRANCE")
                                .build()
                ),

                Arguments.of(
                        REMAddressDto.builder().streetNumber("01").repetitionIndex("bis").streetType("rue")
                                .streetName("des lilas").specialDistribution("specialDistribution")
                                .zipCode("59000")/*.cityName("").countryName("FRANCE")*/
                                .addressSupplement("SupplementAdresse")
                                .locationHelp(null).build()
                        , PersonDto.builder().gender("1")/*.firstName("FirstName")*/.lastName("LastName").build(),
                        AddressDto.builder()
                                .l1("M LastName")
                                .l2("SupplementAdresse").l3("")
                                .l4("01 bis rue des lilas")
                                .l5("specialDistribution")
                                .l6("59000")
                                .l7("FRANCE")
                                .build()
                ),

                Arguments.of(
                        REMAddressDto.builder().streetNumber("01").repetitionIndex("bis").streetType("rue")
                                .streetName("des lilas").specialDistribution("specialDistribution")
                                /*.zipCode("59000")*/.cityName("TOTO")/*.countryName("FRANCE")*/
                                .addressSupplement("SupplementAdresse")
                                .locationHelp(null).build()
                        , PersonDto.builder().gender("1").firstName("FirstName")/*.lastName("LastName")*/.build(),
                        AddressDto.builder()
                                .l1("M FirstName")
                                .l2("SupplementAdresse").l3("")
                                .l4("01 bis rue des lilas")
                                .l5("specialDistribution")
                                .l6("TOTO")
                                .l7("FRANCE")
                                .build()
                ),
                Arguments.of(
                        REMAddressDto.builder().streetNumber("01").repetitionIndex("bis").streetType("rue")
                                .streetName("des lilas").specialDistribution("specialDistribution")
                                /*.zipCode("59000").cityName("TOTO").countryName("FRANCE")*/
                                .addressSupplement("SupplementAdresse")
                                .locationHelp(null).build()
                        , PersonDto.builder().gender("1")/*.firstName("FirstName").lastName("LastName")*/.build(),
                        AddressDto.builder()
                                .l1("M")
                                .l2("SupplementAdresse").l3("")
                                .l4("01 bis rue des lilas")
                                .l5("specialDistribution")
                                .l6("")
                                .l7("FRANCE")
                                .build()
                ),


                Arguments.of(
                        REMAddressDto.builder().streetNumber("1110")/*.repetitionIndex()*/.streetType("route")
                                .streetName("des Touches").specialDistribution("specialDistribution")
                                .zipCode("59000").cityName("Lille").countryName("FRANCE")
                                .addressSupplement("SupplementAdresse")
                                .locationHelp(null).build()
                        , PersonDto.builder().gender("XXKLXKEZSDKWLJDWSKKJ").firstName("FirstName").lastName("LastName").build(),
                        AddressDto.builder()
                                .l1("MME FirstName LastName")
                                .l2("SupplementAdresse").l3("")
                                .l4("1110 route des Touches")
                                .l5("specialDistribution")
                                .l6("59000 Lille")
                                .l7("FRANCE")
                                .build()
                ),

                Arguments.of(
                        REMAddressDto.builder()/*.streetNumber().repetitionIndex()*/.streetType("place")
                                .streetName("de la Poste").specialDistribution("specialDistribution")
                                .zipCode("59000").cityName("Lille").countryName("FRANCE")
                                .addressSupplement("SupplementAdresse")
                                .locationHelp(null).build()
                        , PersonDto.builder()/*.gender("2")*/.firstName("FirstName").lastName("LastName").build(),
                        AddressDto.builder()
                                .l1("MME FirstName LastName")
                                .l2("SupplementAdresse").l3("")
                                .l4("place de la Poste")
                                .l5("specialDistribution")
                                .l6("59000 Lille")
                                .l7("FRANCE")
                                .build()
                ),

                Arguments.of(
                        REMAddressDto.builder().streetNumber("").repetitionIndex("    ").streetType("place")
                                .streetName("de la Poste").specialDistribution("specialDistribution")
                                .zipCode("59000").cityName("Lille").countryName("                 ")
                                .addressSupplement("SupplementAdresse")
                                .locationHelp(null).build()
                        , PersonDto.builder().gender(null).firstName("FirstName").lastName("LastName").build(),
                        AddressDto.builder()
                                .l1("MME FirstName LastName")
                                .l2("SupplementAdresse").l3("")
                                .l4("place de la Poste")
                                .l5("specialDistribution")
                                .l6("59000 Lille")
                                .l7("FRANCE")
                                .build())

                ,

                Arguments.of(
                        REMAddressDto.builder().streetNumber("").repetitionIndex("    ").streetType("place")
                                .streetName("de la Poste").specialDistribution("specialDistribution")
                                .zipCode("59000").cityName("Lille").countryName("                 ")
                                .addressSupplement("SupplementAdresse")
                                .locationHelp(
                                        LocationHelpDto.builder()
                                                .building("building")
                                                .cityPriorityDistrict(Boolean.FALSE)
                                                .door("door")
                                                .elevator(Boolean.TRUE)
                                                .floor("floor")
                                                .build())
                                .build()
                        , PersonDto.builder().gender("  ").firstName("FirstName").lastName("LastName").build(),
                        AddressDto.builder()
                                .l1("MME FirstName LastName")
                                .l2("SupplementAdresse").l3("")
                                .l4("place de la Poste")
                                .l5("specialDistribution")
                                .l6("59000 Lille")
                                .l7("FRANCE")
                                .building("building")
                                .cityPriorityDistrict(Boolean.FALSE)
                                .door("door")
                                .elevator(Boolean.TRUE)
                                .floor("floor")
                                .build())
        );
    }

    @ParameterizedTest
    @MethodSource("providedREMAdressAndContactAndExpectedResults")
    void computeSabianeAdress_should_returnCorrectValues(REMAddressDto remAdress, PersonDto remContact, AddressDto expectedSabianeAdress) {
        //Prepare
        //Call method under test
        AddressDto sabianeAdress = SabianePilotageCreateSUTask.computeSabianeAdress(remAdress, remContact);
        //Post conditions
        assertEquals(expectedSabianeAdress, sabianeAdress);
    }

    @Test
    void createSabianePersonFromRemPerson_should_returnCorrectValues_for_individu() throws JsonProcessingException, JSONException {
        String ctx_ok =
"""   
        {    "id": "TEST_ID" }
""";

        String expectedJsonSabiane =
"""
{
    "organizationUnitId": "ID_POLE_GESTION_OPALE",
    "address": {
        "cityPriorityDistrict": false,
        "door": "door",
        "floor": "00/_",
        "l1": "MME firstName2 lastName2",
        "l2": "SupplementAdresse",
        "l3": "",
        "l4": "1 RUE DE L INSEE",
        "l5": "specialDistribution",
        "l6": "59000 Lille",
        "l7": "FRANCE"
    },
    "persons": [
        {
            "birthdate": 315615600,
            "email": "test2@gmail.com",
            "favoriteEmail": false,
            "firstName": "firstName2",
            "lastName": "lastName2",
            "phoneNumbers": [
                {
                    "favorite": false,
                    "number": "02",
                    "source": "FISCAL"
                }
            ],
            "privileged": true,
            "title": "MISS"
        }
    ],
    "sampleIdentifiers": {
        "autre": "0",
        "bs": 0,
        "ec": "0",
        "le": 0,
        "nograp": null,
        "noi": 0,
        "nole": 0,
        "nolog": 0,
        "numfa": 0,
        "rges": 0,
        "ssech": 99
    }
}
""";

        //Prepare
        JsonNode ctxNode = new ObjectMapper().readTree(ctx_ok);
        JsonNode remNode =  ProtoolsTestUtils.asObject(RemSUData.rem_su_test_selection_3personnes,JsonNode.class);


        //Run Methode under tests
        SurveyUnitContextDto sabianeDto = SabianePilotageCreateSUTask.createSabianeSUContextDto(ctxNode, ctx_partition1, remNode, false, false);

        //Verifications
        JsonNode sabianeResultNode = new ObjectMapper().valueToTree(sabianeDto);
        assertThat(sabianeResultNode.get("states").isArray()).isTrue();
        assertThat(sabianeResultNode.get("states").size()).isEqualTo(1);
        assertThat(sabianeResultNode.get("states").get(0).get("type").textValue()).isEqualTo(StateType.NVM.toString());
        //state datetime
        long statesTS = sabianeResultNode.get("states").get(0).get("date").longValue();
        long currentTs = Instant.now().toEpochMilli();
        long difference = currentTs - statesTS;
        assertThat(statesTS).isLessThanOrEqualTo(currentTs);
        assertThat(difference).isLessThanOrEqualTo(1 * 60 * 1000);//less than one minute old


        //Put static values in expected result
        JsonNode expectedSabianeNode = new ObjectMapper().readTree(expectedJsonSabiane);//Execute method under test
        ObjectNode objectNode = (ObjectNode) expectedSabianeNode;
        // Ajouter une nouvelle clé/valeur
        objectNode.put("priority", false);
        objectNode.put("campaign", "TEST_ID");
        String idSabiane = SabianeIdHelper.computeSabianeID(ctx_partition1.toString(), "3043280");
        objectNode.put("id", idSabiane);

        //We are sure of the state; we can just override the expected value with the actual one
        objectNode.put("states", sabianeResultNode.get("states"));
        // Convertir l'ObjectNode en JsonNode
        expectedSabianeNode = objectNode;
        JSONAssert.assertEquals(expectedSabianeNode.toString(), sabianeResultNode.toString(), false);
    }


    @Test
    void createSabianePersonFromRemPerson_should_returnCorrectValues_for_logement() throws JsonProcessingException, JSONException {
        String ctx_ok =
                """   
                        {    "id": "TEST_ID" }
                """;

        String expectedJsonSabiane =
                """
                {
                    "organizationUnitId": "ID_POLE_GESTION_OPALE",
                    "address": {
                        "cityPriorityDistrict": false,
                        "door": "door",
                        "floor": "00/_",
                        "l1": "MME firstName2 lastName2",
                        "l2": "SupplementAdresse",
                        "l3": "",
                        "l4": "1 RUE DE L INSEE",
                        "l5": "specialDistribution",
                        "l6": "59000 Lille",
                        "l7": "FRANCE"
                    },
                    "persons": [
                        {
                            "birthdate": 315615600,
                            "email": "test2@gmail.com",
                            "favoriteEmail": false,
                            "firstName": "firstName2",
                            "lastName": "lastName2",
                            "phoneNumbers": [
                                {
                                    "favorite": false,
                                    "number": "02",
                                    "source": "FISCAL"
                                }
                            ],
                            "privileged": true,
                            "title": "MISS"
                        }
                    ],
                    "sampleIdentifiers": {
                        "autre": "0",
                        "bs": 0,
                        "ec": "0",
                        "le": 0,
                        "nograp": null,
                        "noi": 0,
                        "nole": 0,
                        "nolog": 0,
                        "numfa": 0,
                        "rges": 0,
                        "ssech": 99
                    }
                }
                """;

        //Prepare
        JsonNode ctxNode = new ObjectMapper().readTree(ctx_ok);
        JsonNode remNode =  ProtoolsTestUtils.asObject(RemSUData.rem_su_test_selection_3personnes,JsonNode.class);

        //Run Methode under tests
        SurveyUnitContextDto sabianeDto = SabianePilotageCreateSUTask.createSabianeSUContextDto(ctxNode, ctx_partition1, remNode, true, false);

        //Verifications
        JsonNode sabianeResultNode = new ObjectMapper().valueToTree(sabianeDto);
        assertThat(sabianeResultNode.get("states").isArray()).isTrue();
        assertThat(sabianeResultNode.get("states").size()).isEqualTo(1);
        assertThat(sabianeResultNode.get("states").get(0).get("type").textValue()).isEqualTo(StateType.NVM.toString());
        //state datetime
        long statesTS = sabianeResultNode.get("states").get(0).get("date").longValue();
        long currentTs = Instant.now().toEpochMilli();
        long difference = currentTs - statesTS;
        assertThat(statesTS).isLessThanOrEqualTo(currentTs);
        assertThat(difference).isLessThanOrEqualTo(1 * 60 * 1000);//less than one minute old


        //Put static values in expected result
        JsonNode expectedSabianeNode = new ObjectMapper().readTree(expectedJsonSabiane);//Execute method under test
        ObjectNode objectNode = (ObjectNode) expectedSabianeNode;
        // Ajouter une nouvelle clé/valeur
        objectNode.put("priority", false);
        objectNode.put("campaign", "TEST_ID");
        String idSabiane = SabianeIdHelper.computeSabianeID(ctx_partition1.toString(), "3043280");
        objectNode.put("id", idSabiane);

        //We are sure of the state; we can just override the expected value with the actual one
        objectNode.put("states", sabianeResultNode.get("states"));
        // Convertir l'ObjectNode en JsonNode
        expectedSabianeNode = objectNode;
        JSONAssert.assertEquals(expectedSabianeNode.toString(), sabianeResultNode.toString(), false);
    }

    @Test
    @DisplayName("Test execute method - should throw if VARNAME_CURRENT_PARTITION_ID or VARNAME_REM_SURVEY_UNIT not initialized")
    void execute_should_throw_FlowableIllegalArgumentException_when_variables_notDefined() throws JsonProcessingException {
        //Precondition
        DelegateExecution execution = createMockedExecution();
        initContexteMockWithString(minimal_ctx_ok);

        //Execute the unit under test
        FlowableIllegalArgumentException exception = assertThrows(FlowableIllegalArgumentException.class, () -> sabianePilotageTask.execute(execution));
        //Post conditions
        assertThat(exception.getMessage()).isEqualTo(getMissingVariableMessage(VARNAME_CURRENT_PARTITION_ID));

        //Create First variable
        when(execution.getVariable(VARNAME_CURRENT_PARTITION_ID, String.class)).thenReturn("99");
        //Execute again
        exception = assertThrows(FlowableIllegalArgumentException.class, () -> sabianePilotageTask.execute(execution));
        //Check the error
        assertThat(exception.getMessage()).isEqualTo(getMissingVariableMessage(VARNAME_REM_INTERROGATION));

        //Create 2nd variable
        when(execution.getVariable(VARNAME_REM_INTERROGATION, JsonNode.class)).thenReturn(ProtoolsTestUtils.asJsonNode(RemSUData.rem_su_1personne));
        //Execute again
        assertDoesNotThrow(() -> sabianePilotageTask.execute(execution));
    }


    private static Stream<Arguments> contextErrorArguments() {
        return Stream.of(
                Arguments.of(CtxExamples.ctx_no_part),
                Arguments.of(CtxExamples.ctx_idCampagne_idPartition),
                Arguments.of(CtxExamples.ctx_idCampagne_1emptyPartition),
                Arguments.of(CtxExamples.ctx_idCampagne_idPartition_typeLogement),
                Arguments.of(CtxExamples.ctx_idCampagne_idPartition_typeIndividu)
        );
    }

    @ParameterizedTest
    @MethodSource("contextErrorArguments")
    @DisplayName("Test execute method - should throw if Context is not correct")
    void execute_should_throw_BadContext_when_contextIncorrect(String context_json) throws JsonProcessingException {
        //Precondition
        DelegateExecution execution = createMockedExecution();
        //Variables
        lenient().when(execution.getVariable(VARNAME_CURRENT_PARTITION_ID, String.class)).thenReturn(CtxExamples.ctx_partition1);
        lenient().when(execution.getVariable(VARNAME_REM_INTERROGATION, JsonNode.class)).thenReturn(ProtoolsTestUtils.asJsonNode(RemSUData.rem_su_1personne));
        //Ctx
        ProtoolsTestUtils.initContexteMockFromString(protoolsContext, context_json);

        //Run test
        assertThrows(BadContextIncorrectBPMNError.class, () -> sabianePilotageTask.execute(execution));
        Mockito.reset(protoolsContext);
    }


    static Stream<Arguments> initExecuteParameters() {
        return Stream.of(
                Arguments.of(
                        SabianeCtxExamples.ctx_ok_idCampagne_idPartition_typeLogement_prioritaire,
                        RemSUData.rem_su_1personne,
                        Boolean.TRUE),
                Arguments.of(
                        SabianeCtxExamples.ctx_ok_idCampagne_idPartition_typeLogement_nonPrioritaire,
                        RemSUData.rem_su_1personne,
                        Boolean.FALSE)
        );
    }

    @ParameterizedTest
    @MethodSource("initExecuteParameters")
    @DisplayName("Test execute method - should work and make correct call to service when context has one partition")
    void execute_should_work_when_ctx_1part_logement(String inputCtx, String inputRemSU, Boolean expectedPriority) {
        //Precondition
        DelegateExecution execution = mock(DelegateExecution.class);
        lenient().when(execution.getProcessInstanceId()).thenReturn(dumyId);
        lenient().when(execution.getVariable(VARNAME_CURRENT_PARTITION_ID, String.class)).thenReturn(CtxExamples.ctx_partition1);
        JsonNode remSU = ProtoolsTestUtils.asJsonNode(inputRemSU);
        lenient().when(execution.getVariable(VARNAME_REM_INTERROGATION, JsonNode.class)).thenReturn(remSU);
        //Ctx
        initContexteMockWithString(inputCtx);

        //Run method under test
        assertDoesNotThrow(() -> sabianePilotageTask.execute(execution));

        final ArgumentCaptor<List<SurveyUnitContextDto> > listCaptor
                = ArgumentCaptor.forClass((Class) List.class);
        verify(sabianePilotageService,times(1)).postSurveyUnits(listCaptor.capture());

        List<List<SurveyUnitContextDto>> captured = listCaptor.getAllValues();
        assertEquals(1,captured.size(),"We are supposed to call the method only one");
        List<SurveyUnitContextDto> actualListOfSU = captured.get(0);
        assertEquals(1,actualListOfSU.size(),"We are supposed to create SU one by one");
        SurveyUnitContextDto actualSU = actualListOfSU.get(0);

        assertEquals(1,actualSU.getPersons().size());
        //Prioritaire
        assertEquals(expectedPriority,actualSU.getPriority());


        assertEquals(remSU.path("persons").path(0).path("emails").path(0).path("mailAddress").asText(),actualSU.getPersons().get(0).getEmail());
        assertEquals(remSU.path("persons").path(0).path("firstName").asText(),actualSU.getPersons().get(0).getFirstName());
        Mockito.reset(protoolsContext);
    }

}