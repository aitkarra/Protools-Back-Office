package fr.insee.protools.backend.service.sabiane;

import fr.insee.protools.backend.service.exception.IncorrectSUBPMNError;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SabianeIdHelperTest {

    @ParameterizedTest
    @CsvSource(
            {
                    "1,2,1P2",
                    "A,B,APB",
                    "99999999,aJJTJJTTJPPPP,99999999PaJJTJJTTJPPPP"
            })
    void computeSabianeID_should_work_when_inputsAreCorrects(String partitionId, String remRepositoryID, String expectedResult) {
        assertThat(SabianeIdHelper.computeSabianeID(partitionId,remRepositoryID)).isEqualTo(expectedResult);
    }

    @Test
    void computeSabianeID_should_throw_when_inputsAreInCorrects() {
        assertThrows(IncorrectSUBPMNError.class,() -> SabianeIdHelper.computeSabianeID("99",null));
        assertThrows(IncorrectSUBPMNError.class,() -> SabianeIdHelper.computeSabianeID(null,"99"));
        assertThrows(IncorrectSUBPMNError.class,() -> SabianeIdHelper.computeSabianeID(null,null));
    }

}