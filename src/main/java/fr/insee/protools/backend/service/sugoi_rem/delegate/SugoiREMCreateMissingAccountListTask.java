package fr.insee.protools.backend.service.sugoi_rem.delegate;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.dto.ContexteProcessus;
import fr.insee.protools.backend.dto.sugoi.Habilitation;
import fr.insee.protools.backend.dto.sugoi.User;
import fr.insee.protools.backend.service.DelegateContextVerifier;
import fr.insee.protools.backend.service.context.ContextService;
import fr.insee.protools.backend.service.context.enums.CampaignContextEnum;
import fr.insee.protools.backend.service.rem.RemService;
import fr.insee.protools.backend.service.sugoi.SugoiService;
import fr.insee.protools.backend.service.utils.FlowableVariableUtils;
import fr.insee.protools.backend.service.utils.password.PasswordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.*;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_CURRENT_PARTITION_ID;
import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_DIRECTORYACCESS_PWD_FOR_INTERRO_ID_MAP;
import static fr.insee.protools.backend.service.context.ContextConstants.CTX_CAMPAGNE_CONTEXTE;

@Slf4j
@Component
@RequiredArgsConstructor
public class SugoiREMCreateMissingAccountListTask implements JavaDelegate, DelegateContextVerifier {

    protected static final Habilitation PLATINE_HABILITATION = new Habilitation("platine", "repondant", null);
    protected static final User createSugoiUserBody = User.builder().habilitations(List.of(PLATINE_HABILITATION)).build();

    private final SugoiService sugoiService;
    private final RemService remService;

    private final PasswordService passwordService;
    private final ContextService protoolsContext;

    public static final int HOUSEHOLD_PASSWORD_SIZE=8;
    public static final int DEFAULT_PASSWORD_SIZE=12;

    @Override
    public void execute(DelegateExecution execution) {
        log.debug("ProcessInstanceId={} begin", execution.getProcessInstanceId());
        ContexteProcessus context = protoolsContext.getContextDtoByProcessInstance(execution.getProcessInstanceId());
        checkContextOrThrow(log,execution.getProcessInstanceId(), context);

        String currentPartitionId = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_CURRENT_PARTITION_ID, String.class);
        List<String> interrogationIdsWithoutAccount = remService.getInterrogationIdsWithoutAccountForPartition(currentPartitionId);

        Map<String, String> userByInterroId = new LinkedHashMap<>(interrogationIdsWithoutAccount.size());
        Map<String, String> pwdByInterroId = new LinkedHashMap<>(interrogationIdsWithoutAccount.size());

        int passwordSize = getPasswordSize(context);
        for (String interrogationId : interrogationIdsWithoutAccount){
            //Create User
            User createdUser = sugoiService.postCreateUser(createSugoiUserBody);
            //init password
            String userPassword = passwordService.generatePassword(passwordSize);
            sugoiService.postInitPassword(createdUser.getUsername(), userPassword);
            userByInterroId.put(interrogationId,createdUser.getUsername());
            pwdByInterroId.put(interrogationId,userPassword);
            log.debug("ProcessInstanceId={} - username={} ", createdUser.getUsername());
        }

        remService.patchInterrogationsSetAccounts(userByInterroId);

        execution.setVariableLocal(VARNAME_DIRECTORYACCESS_PWD_FOR_INTERRO_ID_MAP, pwdByInterroId);
        log.info("ProcessInstanceId={} - size={} end", execution.getProcessInstanceId(), userByInterroId.size());
    }

    public static int getPasswordSize(ContexteProcessus context){
        if(context!=null && context.getContexte()!=null && context.getContexte().equals(ContexteProcessus.Contexte.MENAGE)){
            return HOUSEHOLD_PASSWORD_SIZE;
        }
        return DEFAULT_PASSWORD_SIZE;
    }

    @Override
    public Set<String> getContextErrors(ContexteProcessus contexteProcessus) {
        if(contexteProcessus==null){
            return Set.of("Context is null");
        } else if (contexteProcessus.getContexte()==null) {
            return Set.of("Contexte is missing");
        }
        else if (contexteProcessus.getId()==null) {
            return Set.of("Context Id");
        }
        return Set.of();
    }
}
