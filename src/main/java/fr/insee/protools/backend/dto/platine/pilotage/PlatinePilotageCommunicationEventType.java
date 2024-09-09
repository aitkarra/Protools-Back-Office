package fr.insee.protools.backend.dto.platine.pilotage;

import lombok.Getter;

@Getter
public enum PlatinePilotageCommunicationEventType {
    COMMUNICATION_STATE_SENT("SENT");

    public final String label;

    private PlatinePilotageCommunicationEventType(String label) {
        this.label = label;
    }
}
