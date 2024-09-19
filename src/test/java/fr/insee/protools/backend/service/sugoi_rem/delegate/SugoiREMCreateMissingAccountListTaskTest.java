package fr.insee.protools.backend.service.sugoi_rem.delegate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.dto.ContexteProcessus;
import fr.insee.protools.backend.dto.sugoi.Habilitation;
import fr.insee.protools.backend.dto.sugoi.User;
import fr.insee.protools.backend.service.rem.RemService;
import fr.insee.protools.backend.service.sugoi.SugoiService;
import fr.insee.protools.backend.service.utils.delegate.TestDelegateWithContext;
import fr.insee.protools.backend.service.utils.password.PasswordService;
import org.apache.commons.lang3.RandomStringUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_CURRENT_PARTITION_ID;
import static fr.insee.protools.backend.utils.data.CtxExamples.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SugoiREMCreateMissingAccountListTaskTest extends TestDelegateWithContext {

    @Mock
    RemService remService;

    @Mock
    SugoiService sugoiService;

    @Mock
    PasswordService passwordService;

    @InjectMocks
    SugoiREMCreateMissingAccountListTask task;

    @Override
    public JavaDelegate getTaskUnderTest() {
        return task;
    }

    ///provide ctx and expected password length and list of rem Ids for which we need to init a username/pwd
    private static Stream<Arguments> executeContextOKArgs() {
        return Stream.of("menage", "entreprise")
                .flatMap(contexte ->
                        IntStream.rangeClosed(0, 10)
                                .mapToObj(size -> Arguments.of(contexte,
                                        IntStream.rangeClosed(1, 1+10*size)
                                                .mapToObj(i -> "ID_" + i)
                                                .toList()
                                ))
                );
    }

    @ParameterizedTest
    @MethodSource("executeContextOKArgs")
    public void execute_should_work_whenEverythingOk(String context,List<String> remIdInterroWithoutAccount) {
        record MockData(String idInterro, String sugoiResponseUserName, User createdUser, String pwd) {
        }
        Habilitation PLATINE_HABILITATION = new Habilitation("platine", "repondant", null);
        User expectedCreateSugoiUserBody = User.builder().habilitations(List.of(PLATINE_HABILITATION)).build();
        String partitionId = UUID.randomUUID().toString();

        int tmpexpectedPwdSize=-1;
        String contexts_as_string;
        if(context.equals("menage"))
        {
            contexts_as_string=ctx_contexte_menage;
            tmpexpectedPwdSize=8;
        }
        else if(context.equals("entreprise")){
            contexts_as_string=ctx_contexte_entreprise;
            tmpexpectedPwdSize=12;
        }
        else {
            assertFalse(Boolean.TRUE,"Tests parameters are incorrects");
            return;
        }
        final int expectedPwdSize=tmpexpectedPwdSize;

        //Prepare
        DelegateExecution execution = createMockedExecution();
        JsonNode expectedContext = initContexteMockWithString(contexts_as_string);

        List<MockData> mockDataList = remIdInterroWithoutAccount.stream()
                .map(id -> {
                    String pwd = RandomStringUtils.randomAlphanumeric(expectedPwdSize);
                    String userName = "USER_" + RandomStringUtils.randomAlphanumeric(10);
                    User sugoiUserResponse = User.builder().username(userName).habilitations(List.of(PLATINE_HABILITATION)).build();
                    return new MockData(id, userName, sugoiUserResponse, pwd);
                })
                .toList();

        when(sugoiService.postCreateUser(any())).thenAnswer(AdditionalAnswers.returnsElementsOf(mockDataList.stream().map(MockData::createdUser).toList()));
        when(passwordService.generatePassword(expectedPwdSize)).thenAnswer(AdditionalAnswers.returnsElementsOf(mockDataList.stream().map(MockData::pwd).toList()));

        doReturn(partitionId).when(execution).getVariable(VARNAME_CURRENT_PARTITION_ID, String.class);
        doReturn(remIdInterroWithoutAccount).when(remService).getInterrogationIdsWithoutAccountForPartition(partitionId);

        //Call method under test
        task.execute(execution);

        //Verify
        verify(remService, times(1)).getInterrogationIdsWithoutAccountForPartition(eq(partitionId));
        verify(passwordService, times(remIdInterroWithoutAccount.size())).generatePassword(eq(expectedPwdSize));
        verify(sugoiService, times(remIdInterroWithoutAccount.size())).postCreateUser(eq(expectedCreateSugoiUserBody));

        // Verification that postInitPassword was called once for each element in mockDataList
        for (MockData mockData : mockDataList) {
            verify(sugoiService, times(1)).postInitPassword(mockData.sugoiResponseUserName(), mockData.pwd());
        }

        ArgumentCaptor<Map> patchArgs = ArgumentCaptor.forClass(Map.class);
        verify(remService, times(1)).patchInterrogationsSetAccounts(patchArgs.capture());
        var args = patchArgs.getValue();
        for (MockData mockData : mockDataList) {
            assertTrue(args.containsKey(mockData.idInterro), "Arguments passed to patchInterrogationsSetAccounts are incorrect");
            assertEquals(args.get(mockData.idInterro), mockData.sugoiResponseUserName, "Arguments passed to patchInterrogationsSetAccounts are incorrect");
        }
        assertEquals(mockDataList.size(), args.size(), "Arguments passed to patchInterrogationsSetAccounts are incorrect (wrong number)");
    }

    @Override
    public Map<String, Class> getVariablesAndTypes() {
        return Map.of(
                VARNAME_CURRENT_PARTITION_ID, String.class
        );
    }

    @Override
    protected String minimalValidCtxt() {
        return ctx_contexte_menage;
    }

    @Test
    void getPasswordSize_should_return8_for_menage() throws JsonProcessingException {
        //prepare
        ContexteProcessus schema = new ObjectMapper().readValue(ctx_contexte_menage, ContexteProcessus.class);
        //Call method under tests
        int pwdSize=task.getPasswordSize(schema);
        //Verify
        assertEquals(8,pwdSize,"Expected password size of 8 for menage");
    }

    @Test
    void getPasswordSize_should_return_12_for_not_menage() throws JsonProcessingException {
        //prepare
        ContexteProcessus schema = new ObjectMapper().readValue(ctx_contexte_entreprise, ContexteProcessus.class);
        //Call method under tests
        int pwdSize=task.getPasswordSize(schema);
        //Verify
        assertEquals(12,pwdSize,"Expected password size of 12 for entreprise");

        //prepare
        schema = new ObjectMapper().readValue(ctx_empty, ContexteProcessus.class);
        //Call method under tests
        pwdSize=task.getPasswordSize(schema);
        //Verify
        assertEquals(12,pwdSize,"Expected password size of 12 for undef");
    }


}