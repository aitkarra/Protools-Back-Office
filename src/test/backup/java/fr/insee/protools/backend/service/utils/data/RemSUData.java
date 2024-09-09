package fr.insee.protools.backend.service.utils.data;

import org.springframework.util.ClassUtils;

public class RemSUData {
    private RemSUData() {}

    final static String ressourceFolder = ClassUtils.convertClassNameToResourcePath(RemSUData.class.getPackageName());


    public final static String rem_su_1personne = ressourceFolder+"/rem-su_1personne.json";
    public final static String rem_su_3personnes = ressourceFolder+"/rem-su_3personnes.json";
    public final static String getRem_su_1personne_noContact = ressourceFolder+"/rem-su-noMainOrSurveyed.json";
    public final static String rem_su_test_selection_3personnes = ressourceFolder+"/rem-su-test-selection.json";

}
