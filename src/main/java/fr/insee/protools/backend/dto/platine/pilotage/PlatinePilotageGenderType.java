package fr.insee.protools.backend.dto.platine.pilotage;

import lombok.Getter;

@Getter
public enum PlatinePilotageGenderType {
    Female("1", "Female"), Male("2", "Male"),Undefined("3","Undefined")  ;

    private final String value;
    private final String label;

    PlatinePilotageGenderType(String value, String label) {
        this.value = value;
        this.label = label;
    }
}
