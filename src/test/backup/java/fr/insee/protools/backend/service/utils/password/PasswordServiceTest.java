package fr.insee.protools.backend.service.utils.password;

import org.junit.jupiter.api.Test;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.RuleResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest(classes = PasswordServiceImpl.class)
@TestPropertySource(properties = {
        "fr.insee.protools.password.create.withDigits = true",
        "fr.insee.protools.password.create.withUpperCase = true",
        "fr.insee.protools.password.create.withLowerCase = true",
        "fr.insee.protools.password.create.withSpecial = true",
})
class PasswordServiceTest {

    @Autowired private PasswordService passwordService;

    @Test
    void testGeneratePassword() {
        //Prepare
        var rules = ((PasswordServiceImpl)passwordService).generateRandomPasswordCharacterRules(true,true,true,true);
        PasswordValidator passwordValidator = new PasswordValidator(rules);

        //Generate and validate X1000
        for (int i = 0; i < 1000 ; i++) {
            for(int length=8; length < 19; length ++) { // and for various sizes
                //Execute method under test
                String generatePassword = passwordService.generatePassword(length);

                //Post condition
                PasswordData passwordData = new PasswordData(generatePassword);
                RuleResult validate = passwordValidator.validate(passwordData);

                assertTrue(validate.isValid(), "Generated password meets the rules : " + generatePassword);
                assertEquals(length, generatePassword.length(), "Generated password length should meets the confif : " + generatePassword);
            }
        }

    }
}
