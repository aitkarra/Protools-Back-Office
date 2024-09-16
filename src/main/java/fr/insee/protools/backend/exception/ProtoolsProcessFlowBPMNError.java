package fr.insee.protools.backend.exception;

import static fr.insee.protools.backend.service.BPMNErrorCode.BPMNERROR_CODE_DEFAULT;

public class ProtoolsProcessFlowBPMNError extends ProtoolsBpmnError {
    public ProtoolsProcessFlowBPMNError(String message) {
        super(BPMNERROR_CODE_DEFAULT, message);
    }
}
