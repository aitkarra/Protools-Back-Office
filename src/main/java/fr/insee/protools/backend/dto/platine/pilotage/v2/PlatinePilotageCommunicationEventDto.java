package fr.insee.protools.backend.dto.platine.pilotage.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class PlatinePilotageCommunicationEventDto {
    String interrogationId;
    String communicationRequestId;
    String communcationId;
    String state;
}
