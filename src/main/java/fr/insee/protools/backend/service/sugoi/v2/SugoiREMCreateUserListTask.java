package fr.insee.protools.backend.service.sugoi.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.dto.internal.ProtoolsInterrogationDto;
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
import org.apache.commons.lang3.tuple.Pair;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static fr.insee.protools.backend.service.FlowableVariableNameConstants.*;
import static fr.insee.protools.backend.service.context.ContextConstants.CTX_CAMPAGNE_CONTEXTE;

@Slf4j
@Component
@RequiredArgsConstructor
public class SugoiREMCreateUserListTask implements JavaDelegate, DelegateContextVerifier {

    protected static final Habilitation PLATINE_HABILITATION = new Habilitation("platine", "repondant", null);
    protected static final User createSugoiUserBody = User.builder().habilitations(List.of(PLATINE_HABILITATION)).build();

    private final SugoiService sugoiService;
    private final RemService remService;

    private final PasswordService passwordService;
    private final ContextService protoolsContext;

    public static final int householdPasswordSize=8;
    public static final int defaultPasswordSize=12;

    @Override
    public void execute(DelegateExecution execution) {
        log.debug("ProcessInstanceId={} begin", execution.getProcessInstanceId());
        JsonNode contextRootNode = protoolsContext.getContextByProcessInstance(execution.getProcessInstanceId());
        Long currentLotId = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_CURRENT_PARTITION_ID, Long.class);
        checkContextOrThrow(log,execution.getProcessInstanceId(), contextRootNode);
        Long[] interrogationIdsWithoutAccount = remService.getInterrogationIdsWithoutAccountForLot(currentLotId);

        Map<Long, String> userByInterroId = new LinkedHashMap<>(interrogationIdsWithoutAccount.length);
        Map<Long, String> pwdByInterroId = new LinkedHashMap<>(interrogationIdsWithoutAccount.length);

        int passwordSize = getPasswordSize(contextRootNode);
        for (Long interrogationId : interrogationIdsWithoutAccount){
            //Create User
            User createdUser = sugoiService.postCreateUsers(createSugoiUserBody);
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

    public static int getPasswordSize(JsonNode contextRootNode){
        String contexte=contextRootNode.path(CTX_CAMPAGNE_CONTEXTE).asText();
        CampaignContextEnum contextEnum = CampaignContextEnum.fromLabel(contexte);
        if(CampaignContextEnum.HOUSEHOLD.equals(contextEnum)){
            return householdPasswordSize;
        }
        return defaultPasswordSize;
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
                        CTX_CAMPAGNE_CONTEXTE
                );

        results.addAll(DelegateContextVerifier.computeMissingChildrenMessages(requiredNodes,contextRootNode,getClass()));

        String contexte = contextRootNode.path(CTX_CAMPAGNE_CONTEXTE).asText();
        if(! EnumUtils.isValidEnumIgnoreCase(CampaignContextEnum.class, contexte)){
            results.add(DelegateContextVerifier.computeIncorrectEnumMessage(CTX_CAMPAGNE_CONTEXTE,contexte, Arrays.toString(CampaignContextEnum.values()),getClass()));
        }

        return results;
    }

}
