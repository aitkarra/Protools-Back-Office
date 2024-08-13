package fr.insee.protools.backend.service.sugoi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.dto.internal.ProtoolsInterrogationDto;
import fr.insee.protools.backend.dto.sugoi.Habilitation;
import fr.insee.protools.backend.dto.sugoi.User;
import fr.insee.protools.backend.service.DelegateContextVerifier;
import fr.insee.protools.backend.service.context.ContextService;
import fr.insee.protools.backend.service.context.enums.CampaignContextEnum;
import fr.insee.protools.backend.service.utils.FlowableVariableUtils;
import fr.insee.protools.backend.service.utils.password.PasswordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
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
public class SugoiCreateUserListTask implements JavaDelegate, DelegateContextVerifier {

    protected static final Habilitation PLATINE_HABILITATION = new Habilitation("platine", "repondant", null);
    protected static final User createSugoiUserBody = User.builder().habilitations(List.of(PLATINE_HABILITATION)).build();

    private final SugoiService sugoiService;
    private final PasswordService passwordService;
    private final ContextService protoolsContext;

    public static final int householdPasswordSize=8;
    public static final int defaultPasswordSize=12;

    private static final ObjectMapper objectMapper = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES,false);


    @Override
    public void execute(DelegateExecution execution) {
        log.debug("ProcessInstanceId={} begin", execution.getProcessInstanceId());
        JsonNode contextRootNode = protoolsContext.getContextByProcessInstance(execution.getProcessInstanceId());
        List<ProtoolsInterrogationDto> interrogationList = FlowableVariableUtils.getVariableOrThrow(execution, VARNAME_REM_INTERRO_LIST, List.class);

        checkContextOrThrow(log,execution.getProcessInstanceId(), contextRootNode);
        int passwordSize = getPasswordSize(contextRootNode);

        for (ProtoolsInterrogationDto interrogation : interrogationList){
            //Create User
            User createdUser = sugoiService.postCreateUsers(createSugoiUserBody);
            //init password
            String userPassword = passwordService.generatePassword(passwordSize);
            sugoiService.postInitPassword(createdUser.getUsername(), userPassword);

            //TODO : bidouille à supprimer (à la cible on met à jour REM)
            interrogation.setWebConnectionId(createdUser.getUsername());

            interrogation.setWebPassword(userPassword);
            log.debug("ProcessInstanceId={} - username={} ", createdUser.getUsername());
        }

        execution.setVariableLocal(VARNAME_REM_INTERRO_LIST, interrogationList);
        log.info("ProcessInstanceId={} - size={} end", execution.getProcessInstanceId(), interrogationList.size());
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
