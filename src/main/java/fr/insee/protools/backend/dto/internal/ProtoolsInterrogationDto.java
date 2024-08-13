package fr.insee.protools.backend.dto.internal;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.dto.rem.REMSurveyUnitDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProtoolsInterrogationDto implements Serializable {
    String idInterrogation;
    JsonNode remInterrogation;
    String webPassword;
    //TODO: à supprimer
    String webConnectionId;
}
