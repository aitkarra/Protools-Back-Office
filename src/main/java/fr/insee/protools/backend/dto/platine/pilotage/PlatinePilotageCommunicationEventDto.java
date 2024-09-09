package fr.insee.protools.backend.dto.platine.pilotage;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class PlatinePilotageCommunicationEventDto {
    String interrogationId;
    String communicationRequestId;
    String communcationId;
    PlatinePilotageCommunicationEventType state;
}
