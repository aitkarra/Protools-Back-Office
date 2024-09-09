package fr.insee.protools.backend.utils.data;

import org.springframework.util.ClassUtils;

public class CtxExamples {


    public static final String ctx_empty =
            """
                {
                  "id": "b958cfac-2bf3-478d-a97a-dda5e751898c"
                }
            """;
    public static final String ctx_empty_id="b958cfac-2bf3-478d-a97a-dda5e751898c";

    final static String ressourceFolder = ClassUtils.convertClassNameToResourcePath(CtxExamples.class.getPackageName());
    public final static String context_minimal = ressourceFolder+"/ctx_minimal.json";


    private CtxExamples(){}
}
