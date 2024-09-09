package fr.insee.protools.backend.service.sugoi.delegate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.history.HistoricVariableInstanceQuery;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_DIRECTORYACCESS_PWD_CONTACT;

/**
 * Supprime la variable mot de passe et supprime aussi toutes les occurences de l'historique
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogCredentialsTask implements JavaDelegate {

    private final ProcessEngineConfigurationImpl processEngineConfiguration;

    @Override
    public void execute(DelegateExecution execution) {
        HistoryService historyService = processEngineConfiguration.getHistoryService();
        HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(execution.getProcessInstanceId())
                .variableName(VARNAME_DIRECTORYACCESS_PWD_CONTACT);
        List<HistoricVariableInstance> variableInstances = query.list();

        Set<String> excludeKeys = new HashSet<>();
        excludeKeys.add("context");

        execution.getVariables().entrySet().stream()
                .filter(entry -> !excludeKeys.contains(entry.getKey()))
                .forEach(entry -> log.info(entry.getKey() + ": " + entry.getValue()));
    }
}
