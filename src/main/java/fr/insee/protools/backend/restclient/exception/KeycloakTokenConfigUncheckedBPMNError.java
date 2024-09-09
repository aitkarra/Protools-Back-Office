package fr.insee.protools.backend.restclient.exception;

import fr.insee.protools.backend.exception.ProtoolsBpmnError;

import static fr.insee.protools.backend.service.BPMNErrorCode.BPMNERROR_CODE_DEFAULT;

public class KeycloakTokenConfigUncheckedBPMNError extends ProtoolsBpmnError {

    public KeycloakTokenConfigUncheckedBPMNError(Exception e) {
        super(BPMNERROR_CODE_DEFAULT, e.getMessage());
    }
}
