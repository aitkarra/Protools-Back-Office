package fr.insee.protools.backend.httpclients.exception.runtime;

import fr.insee.protools.backend.exception.ProtoolsBpmnError;
import org.springframework.http.HttpStatusCode;

import static fr.insee.protools.backend.service.BPMNErrorCode.BPMNERROR_CODE_DEFAULT;

public class HttpClient4xxBPMNError extends ProtoolsBpmnError {

    private final HttpStatusCode httpStatusCodeError;

    public HttpClient4xxBPMNError(String message, HttpStatusCode httpStatusCodeError) {
        super(BPMNERROR_CODE_DEFAULT, message);
        this.httpStatusCodeError = httpStatusCodeError;
    }

    public HttpStatusCode getHttpStatusCodeError() {
        return httpStatusCodeError;
    }


}
