package fr.insee.protools.backend.service.sabiane.delegate;

public class SabianeCtxExamples {


    public static final String ctx_ok_idCampagne_idPartition_typeIndividu_prioritaire =
            """
                {
                  "id": "AAC2023A00",
                  "partitions": [{
                    "id": 99,
                    "typeEchantillon" : "individu",
                    "prioritaire": "true"
                  }]
                }
            """;

    public static final String ctx_ok_idCampagne_idPartition_typeIndividu_nonPrioritaire =
            """
                {
                  "id": "AAC2023A00",
                  "partitions": [{
                    "id": 99,
                    "typeEchantillon" : "individu",
                    "prioritaire": "false"
                  }]
                }
            """;

    public static final String ctx_ok_idCampagne_idPartition_typeLogement_prioritaire =
            """
                {
                  "id": "AAC2023A00",
                  "partitions": [{
                    "id": 99,
                    "typeEchantillon" : "individu",
                    "prioritaire": "true"
                  }]
                }
            """;

    public static final String ctx_ok_idCampagne_idPartition_typeLogement_nonPrioritaire =
            """
                {
                  "id": "AAC2023A00",
                  "partitions": [{
                    "id": 99,
                    "typeEchantillon" : "individu",
                    "prioritaire": "false"
                  }]
                }
            """;

    public static final String ctx_ok_idCampagne_idQuestionnaireModel_idPartition_questionnaireModelPartition =
            """
                {
                    "id": "AAC2023A00",
                    "partitions": [{
                        "id": 99,
                        "questionnaireModel": "ID_1"
                    }],
                    "questionnaireModels": [
                        {
                          "id": "ID_1"
                        }]
                }
            """;

    private SabianeCtxExamples(){}
}
