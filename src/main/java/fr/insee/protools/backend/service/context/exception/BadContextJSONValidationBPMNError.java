package fr.insee.protools.backend.service.context.exception;

import fr.insee.protools.backend.exception.ProtoolsBpmnError;

import static fr.insee.protools.backend.service.BPMNErrorCode.BPMNERROR_CODE_DEFAULT;

/**
 * Runtime exception indicating the context json is not correct according to the json-schema
 */
public class BadContextJSONValidationBPMNError extends ProtoolsBpmnError {

    public BadContextJSONValidationBPMNError(String message) {
        super(BPMNERROR_CODE_DEFAULT, message);
    }
}
