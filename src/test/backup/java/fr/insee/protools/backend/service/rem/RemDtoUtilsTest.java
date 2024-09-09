package fr.insee.protools.backend.service.rem;

import fr.insee.protools.backend.service.exception.IncorrectSUBPMNError;
import fr.insee.protools.backend.dto.rem_tmp.PersonDto;
import fr.insee.protools.backend.dto.rem_tmp.REMSurveyUnitDto;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RemDtoUtilsTest {

    static PersonDto person1_main_surveyed = PersonDto.builder().index(1).main(Boolean.TRUE).surveyed(Boolean.TRUE).coDeclarant(Boolean.FALSE).build();
    static PersonDto person2_main = PersonDto.builder().index(2).main(Boolean.TRUE).surveyed(Boolean.FALSE).coDeclarant(Boolean.FALSE).build();
    static PersonDto person3_surveyed = PersonDto.builder().index(3).main(Boolean.FALSE).surveyed(Boolean.TRUE).coDeclarant(Boolean.FALSE).build();

    static PersonDto person4_main_surveyed_codeclarant = PersonDto.builder().index(4).main(Boolean.TRUE).surveyed(Boolean.TRUE).coDeclarant(Boolean.TRUE).build();
    static PersonDto person5_main_codeclarant = PersonDto.builder().index(5).main(Boolean.TRUE).surveyed(Boolean.FALSE).coDeclarant(Boolean.TRUE).build();
    static PersonDto person6_surveyed_codeclarant = PersonDto.builder().index(6).main(Boolean.FALSE).surveyed(Boolean.TRUE).coDeclarant(Boolean.TRUE).build();

    static PersonDto person7_main_surveyed_codeclarantNull = PersonDto.builder().index(7).main(Boolean.TRUE).surveyed(Boolean.TRUE).coDeclarant(null).build();
    static PersonDto person8 = PersonDto.builder().index(8).main(Boolean.FALSE).surveyed(Boolean.FALSE).coDeclarant(Boolean.FALSE).build();
    static PersonDto person9 = PersonDto.builder().index(9).main(Boolean.FALSE).surveyed(Boolean.FALSE).coDeclarant(Boolean.FALSE).build();
    static PersonDto person10_codeclarant = PersonDto.builder().index(10).main(Boolean.FALSE).surveyed(Boolean.FALSE).coDeclarant(Boolean.TRUE).build();


    private static Stream<Arguments> findContactIndividuArgument() {
        return Stream.of(
                //Run method under test : 1 person surveyed
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person3_surveyed)).build(),3),
                //Run method under test : 2 person including 1 surveyed
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person3_surveyed, person2_main)).build(),3),
                //Run method under test : 2 person including 1 surveyed (swapped)
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person2_main, person3_surveyed)).build(),3),
                //Run method under test : 3 person including 1 surveyed (reswap)
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person8, person3_surveyed, person9)).build(),3),
                //Run method under test : 3 person including 1 surveyed (swapped 1)
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person3_surveyed, person9, person8)).build(),3),
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person9, person8, person3_surveyed)).build(),3),
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person9, person8, person8,person8,person8,person8,person8,person8,person8,person8,person3_surveyed)).build(),3)

        );
    }

    private static Stream<Arguments> findContactIndividuArgument_noSurveyed() {
        return Stream.of(
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person2_main)).build()),
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person2_main, person5_main_codeclarant)).build()),
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person2_main, person5_main_codeclarant,person9,person8)).build())
        );
    }

   @ParameterizedTest
    @MethodSource("findContactIndividuArgument_noSurveyed")
    void findContactIndividu_shouldThrow_whenNoSurved(REMSurveyUnitDto su) {
        assertThrows(IncorrectSUBPMNError.class, () -> RemDtoUtils.findContact(null, su, false));
    }


    @ParameterizedTest
    @MethodSource("findContactIndividuArgument")
    void findContact_shouldWork_for_individu(REMSurveyUnitDto su, int expectedContactIndex) {
            PersonDto res = RemDtoUtils.findContact(null, su, false);
            //Check
            assertEquals(res.getIndex(), expectedContactIndex);
    }



    private static Stream<Arguments> findContactLogementArgument() {
        return Stream.of(
                //Run method under test : 1 person main
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person2_main)).build(),2),
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person5_main_codeclarant)).build(),5),
                //Run method under test : 2 person including 1 main
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person2_main, person3_surveyed)).build(),2),
                //Run method under test : 2 person including 1 main (swapped)
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person3_surveyed, person2_main)).build(),2),
                //Run method under test : 3 person including 1 main
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person8, person2_main, person9)).build(),2),
                //Run method under test : 3 person including 1 surveyed (swapped 1)
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person2_main, person9, person8)).build(),2),
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person9, person8, person2_main)).build(),2),
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person9, person8, person8,person8,person8,person8,person8,person8,person8,person8,person5_main_codeclarant)).build(),5),
                //2 mains
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person9, person8, person8,person8,person8,person8,person8,person8,person8,person8,person5_main_codeclarant,person2_main)).build(),5)

        );
    }

    private static Stream<Arguments> findContactLogementArgument_noMain() {
        return Stream.of(
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person3_surveyed)).build()),
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person3_surveyed, person9)).build()),
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person3_surveyed,person9,person8)).build())

        );
    }

    @ParameterizedTest
    @MethodSource("findContactLogementArgument_noMain")
    void findContact_shouldThrow_whenNoSurved_for_logment(REMSurveyUnitDto su) {
        assertThrows(IncorrectSUBPMNError.class, () -> RemDtoUtils.findContact(null, su, true));
    }

    @ParameterizedTest
    @MethodSource("findContactLogementArgument")
    void findContactAndSecondary_shouldWork_for_logement(REMSurveyUnitDto su, int expectedContactIndex) {
        PersonDto res = RemDtoUtils.findContact(null, su, true);
        //Check
        assertEquals(res.getIndex(), expectedContactIndex);
    }


    private static Stream<Arguments> findContactAndSecondaryIndividuArgument() {
        return Stream.of(
                //Run method under test : 1 person surveyed
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person3_surveyed)).build(),3,-1),
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person1_main_surveyed)).build(),1,-1),
                //2 Persons ; 1 Main/Surveyed - 1 N/A
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person1_main_surveyed,person8)).build(),1,-1),
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person8,person1_main_surveyed)).build(),1,-1),
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person3_surveyed,person8)).build(),3,-1),

                //2 or 3 Persons : 1 Main/Surveyed - 1 Declarant
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person1_main_surveyed,person10_codeclarant)).build(),1,10),
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person1_main_surveyed,person10_codeclarant,person9)).build(),1,10),
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person8,person1_main_surveyed,person10_codeclarant,person9)).build(),1,10),
                //2 or 3 Persons : 1 Surveyed  (not main) - 1 Main
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person3_surveyed,person2_main)).build(),3,2),
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person3_surveyed,person5_main_codeclarant)).build(),3,5),
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person8,person3_surveyed,person5_main_codeclarant)).build(),3,5),
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person8,person3_surveyed,person5_main_codeclarant,person9)).build(),3,5)
                );
    }
    @ParameterizedTest
    @MethodSource("findContactAndSecondaryIndividuArgument")
    void findContactAndSecondary_shouldWork_for_individu(REMSurveyUnitDto su, int expectedFirstContactIndex, int expectedSecondContact) {
        Pair<PersonDto, Optional<PersonDto>> res = RemDtoUtils.findContactAndSecondary(null, su, false);
        //Check
        assertEquals(res.getLeft().getIndex(), expectedFirstContactIndex);
        if(expectedSecondContact==-1){
            assertThat(res.getRight()).isEmpty();
        }
        else{
            assertThat(res.getRight().get().getIndex()).isEqualTo(expectedSecondContact);
        }
    }



    private static Stream<Arguments> findContactAndSecondaryLogementArgument() {
        return Stream.of(
                //Run method under test : 1 person surveyed
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person2_main)).build(),2,-1),
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person1_main_surveyed)).build(),1,-1),
                //2/3 Persons ; 1 Main  - 1 N/A
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person1_main_surveyed,person8)).build(),1,-1),
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person8,person1_main_surveyed)).build(),1,-1),
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person2_main,person8)).build(),2,-1),
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person9,person2_main,person8)).build(),2,-1),

                //2 or 3 Persons : 1 Main - 1 Declarant
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person1_main_surveyed,person10_codeclarant)).build(),1,10),
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person1_main_surveyed,person10_codeclarant,person9)).build(),1,10),
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person8,person1_main_surveyed,person10_codeclarant,person9)).build(),1,10),
                Arguments.of(REMSurveyUnitDto.builder().persons(List.of(person8,person7_main_surveyed_codeclarantNull,person10_codeclarant,person9)).build(),7,10)
     );
    }
    @ParameterizedTest
    @MethodSource("findContactAndSecondaryLogementArgument")
    void findContactAndSecondary_shouldWork_for_Logement(REMSurveyUnitDto su, int expectedFirstContactIndex, int expectedSecondContact) {
        Pair<PersonDto, Optional<PersonDto>> res = RemDtoUtils.findContactAndSecondary(null, su, true);
        //Check
        assertEquals(res.getLeft().getIndex(), expectedFirstContactIndex);
        if(expectedSecondContact==-1){
            assertThat(res.getRight()).isEmpty();
        }
        else{
            assertThat(res.getRight().get().getIndex()).isEqualTo(expectedSecondContact);
        }
    }
}