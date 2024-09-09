package fr.insee.protools.backend.service.utils.data;

public class CtxExamples {

    public static final String ctx_partition1 = "99";

    //CTX
    public static final String ctx_no_part =
            """
                {
                  "id": "AAC2023A00"
                }
            """;

    public static final String ctx_idCampagne_idPartition =
            """
                {
                  "id": "AAC2023A00",
                  "partitions": [{
                    "id": 99
                  }]
                }
            """;

    public static final String ctx_idCampagne_1emptyPartition =
            """
                {
                  "id": "AAC2023A00",
                  "partitions": [{
                  }]
                }
            """;

    public static final String ctx_idCampagne_idPartition_typeLogement =
            """
                {
                  "id": "AAC2023A00",
                  "partitions": [{
                    "id": 99,
                    "typeEchantillon" : "LOGEMENT"
                  }]
                }
            """;

    public static final String ctx_idCampagne_idPartition_typeIndividu =
            """
                {
                  "id": "AAC2023A00",
                  "partitions": [{
                    "id": 99,
                    "typeEchantillon" : "individu"
                  }]
                }
            """;

    public static final String ctx_questionnaireModels_no_part =
            """
                {
                  "id": "AAC2023A00",
                    "questionnaireModels": [{
                              "id": "ID_1",
                              "cheminRepertoire": "path",
                              "label": "ID_1_LABEL",
                              "requiredNomenclatureIds": [
                                "NOMA-1-1-0",
                                "NOMB-1-1-0"
                              ]
                            }]
                }
            """;

    public static final String ctx_idCampagne_questionnaireModels_idPartition =
            """
                {
                "id": "AAC2023A00",
                "partitions": [{
                "id": 99
                }],
                "questionnaireModels": [{
                      "id": "ID_1",
                      "cheminRepertoire": "path",
                      "label": "ID_1_LABEL",
                      "requiredNomenclatureIds": [
                        "NOMA-1-1-0",
                        "NOMB-1-1-0"
                      ]
                    }]
                }
            """;

    public static final String ctx_idCampagne_questionnaireModels_1emptyPartition =
            """
                {
                  "id": "AAC2023A00",
                  "partitions": [{
                  }],
                "questionnaireModels": [{
                      "id": "ID_1",
                      "cheminRepertoire": "path",
                      "label": "ID_1_LABEL",
                      "requiredNomenclatureIds": [
                        "NOMA-1-1-0",
                        "NOMB-1-1-0"
                      ]
                    }]
                }
            """;

    public static final String ctx_idCampagne_emptyQuestionnaireModels_idPartition =
            """
                {
                "id": "AAC2023A00",
                "partitions": [{
                "id": 99
                }],
                "questionnaireModels": []
                }
            """;



    private CtxExamples(){}
}
