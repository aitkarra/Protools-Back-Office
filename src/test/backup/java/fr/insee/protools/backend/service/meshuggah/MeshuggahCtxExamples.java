package fr.insee.protools.backend.service.meshuggah;

import fr.insee.protools.backend.service.utils.data.CtxExamples;

public class MeshuggahCtxExamples {

    static final Long ctx_partition1 = 99L;
    static final Long ctx_partition2 = 100L;
    //Values
    //static final String ctx_minTutelle = "MIN_TUT";
    static final String ctx_complementConnexion = "COMP_CONNEXION";
    static final String ctx_logoPrestataire = "LOGO_PRESTA";
    static final String ctx_mailResponsableOperationnel = "contact@insee.fr";
    static final Boolean ctx_prestataire = Boolean.FALSE;

    static final String ctx_relanceLibreMailParagraphe1 ="ligne1";
    static final String ctx_relanceLibreMailParagraphe1_partition2_com1 ="ligne1_partition2_com1";
    static final String ctx_relanceLibreMailParagraphe1_partition2_com2 ="ligne1_partition2_com2";

    static final String ctx_relanceLibreMailParagraphe2 ="ligne2";
    static final String ctx_relanceLibreMailParagraphe3 ="ligne3";
    static final String ctx_relanceLibreMailParagraphe4 ="ligne4";
    static final String ctx_responsableOperationnel="Mr X";
    static final String ctx_responsableTraitement="l'Insee";
    static final String ctx_themeMieuxConnaitreMail="les familles";
    static final String ctx_serviceCollecteurSignataireFonction="La directrice des statistiques démographiques et sociales de l’Insee";
    static final String ctx_serviceCollecteurSignataireNom="Mme XXX";
    static final String ctx_urlEnquete="https://toto.insee.fr/";
    static final String ctx_boiteRetour="toto.toto@insee.fr";
    static final String ctx_objetMail="Insee - enquêtes XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";

    static final String ctx_OK_1part_ouverture_courrier=
        """
            {
              "id": "AAC2023A00",
              "metadonnees": {
                "ministereTutelle": "MIN_TUT",
                "responsableOperationnel": "Mr X",
                "responsableTraitement": "l'Insee",
                "mailBoiteRetour": "toto.toto@insee.fr",
                "urlEnquete": "https://toto.insee.fr/",
                "themeMieuxConnaitreMail": "les familles",
                "serviceCollecteurSignataireFonction": "La directrice des statistiques démographiques et sociales de l’Insee",
                "serviceCollecteurSignataireNom": "Mme XXX",
                "prestataire": false,
                "mailResponsableOperationnel": "contact@insee.fr",
                "logoPrestataire": "LOGO_PRESTA"
              },
              "partitions": [{
                "id": 99,
                "communications" :[
                  {
                    "moyenCommunication" : "courrier",
                    "phaseCommunication" : "ouverture",
                    "avecQuestionnairePapier" : false,
                    "protocole" : null,
                    "mode" : "web",
                    "typeModele" : "relance_libre",
                    "objetMail" : "Insee - enquêtes XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
                    "relanceLibreMailParagraphe1" : "ligne1",
                    "relanceLibreMailParagraphe2" : "ligne2",
                    "relanceLibreMailParagraphe3" : "ligne3",
                    "relanceLibreMailParagraphe4" : "ligne4",
                    "complementConnexion" : "COMP_CONNEXION",
                     "echeances" : ["J+0"]
                  }
                ]
              }]
            }
        """;


    static final String ctx_OK_2partitions_2com_ouverture_relance_ok=
            """
                {
                  "id": "AAC2023A00",
                  "metadonnees": {
                    "ministereTutelle": "MIN_TUT",
                    "responsableOperationnel": "Mr X",
                    "responsableTraitement": "l'Insee",
                    "mailBoiteRetour": "toto.toto@insee.fr",
                    "urlEnquete": "https://toto.insee.fr/",
                    "themeMieuxConnaitreMail": "les familles",
                    "serviceCollecteurSignataireFonction": "La directrice des statistiques démographiques et sociales de l’Insee",
                    "serviceCollecteurSignataireNom": "Mme XXX",
                    "prestataire": false,
                    "mailResponsableOperationnel": "contact@insee.fr",
                    "logoPrestataire": "LOGO_PRESTA"
                  },
                  "partitions": [{
                    "id": 99,
                    "communications" :[
                      {
                        "moyenCommunication" : "courrier",
                        "phaseCommunication" : "ouverture",
                        "avecQuestionnairePapier" : false,
                        "protocole" : null,
                        "mode" : "web",
                        "typeModele" : "relance_libre",
                        "objetMail" : "Insee - enquêtes XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
                        "relanceLibreMailParagraphe1" : "ligne1",
                        "relanceLibreMailParagraphe2" : "ligne2",
                        "relanceLibreMailParagraphe3" : "ligne3",
                        "relanceLibreMailParagraphe4" : "ligne4",
                        "complementConnexion" : "COMP_CONNEXION",
                         "echeances" : ["J+0"]
                      },
                      {
                        "moyenCommunication" : "mail",
                        "phaseCommunication" : "relance",
                        "avecQuestionnairePapier" : false,
                        "protocole" : null,
                        "mode" : "web",
                        "typeModele" : "relance_libre",
                        "objetMail" : "Insee - enquêtes XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
                        "relanceLibreMailParagraphe1" : "ligne1",
                        "relanceLibreMailParagraphe2" : "ligne2",
                        "relanceLibreMailParagraphe3" : "ligne3",
                        "relanceLibreMailParagraphe4" : "ligne4",
                        "complementConnexion" : "COMP_CONNEXION",
                         "echeances" : ["J+15"]
                      }
                    ]
                  },
                  {
                    "id": 100,
                    "communications" :[
                      {
                        "moyenCommunication" : "courrier",
                        "phaseCommunication" : "ouverture",
                        "avecQuestionnairePapier" : false,
                        "protocole" : null,
                        "mode" : "web",
                        "typeModele" : "relance_libre",
                        "objetMail" : "Insee - enquêtes XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
                        "relanceLibreMailParagraphe1" : "ligne1_partition2_com1",
                        "relanceLibreMailParagraphe2" : "ligne2",
                        "relanceLibreMailParagraphe3" : "ligne3",
                        "relanceLibreMailParagraphe4" : "ligne4",
                        "complementConnexion" : "COMP_CONNEXION",
                         "echeances" : ["J+0"]
                      },
                      {
                        "moyenCommunication" : "mail",
                        "phaseCommunication" : "relance",
                        "avecQuestionnairePapier" : false,
                        "protocole" : null,
                        "mode" : "web",
                        "typeModele" : "relance_libre",
                        "objetMail" : "Insee - enquêtes XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
                        "relanceLibreMailParagraphe1" : "ligne1_partition2_com2",
                        "relanceLibreMailParagraphe2" : "ligne2",
                        "relanceLibreMailParagraphe3" : "ligne3",
                        "relanceLibreMailParagraphe4" : "ligne4",
                        "complementConnexion" : "COMP_CONNEXION",
                         "echeances" : ["J+15"]
                      }
                    ]
                  }
                  ]
                }
            """;

    static final String ctx_OK_envoi_mail_1part_ouverture_mail=
        """
            {
              "id": "AAC2023A00",
              "partitions": [{
                "id": 99,
                "communications" :[
                  {
                    "moyenCommunication" : "mail",
                    "phaseCommunication" : "ouverture",
                    "avecQuestionnairePapier" : false,
                    "protocole" : null,
                    "mode" : "web",
                    "typeModele" : "relance_libre"
                  }
                ]
              }]
            }
        """;


    //CTX ERROR
    static final String ctx_ERROR_no_part = CtxExamples.ctx_no_part;

    static final String ctx_ERROR_no_com = CtxExamples.ctx_idCampagne_idPartition;

    static final String ctx_ERROR_no_moyen =
        """
            {
              "id": "AAC2023A00",
              "partitions": [{
                "id": 99,
                "communications" :[
                  {
                    "phaseCommunication" : "ouverture",
                    "avecQuestionnairePapier" : false,
                    "protocole" : null,
                    "mode" : "web",
                    "typeModele" : "relance_libre"
                  }
                ]
              }]
            }
        """;

    static final String ctx_ERROR_typo_phase=
        """
            {
              "id": "AAC2023A00",
              "partitions": [{
                "id": 99,
                "communications" :[
                  {
                    "moyenCommunication" : "mail",
                    "phaseCommunication" : "ouvertureeee",
                    "avecQuestionnairePapier" : false,
                    "protocole" : null,
                    "mode" : "web",
                    "typeModele" : "relance_libre"
                  }
                ]
              }]
            }
        """;

    static final String ctx_ERROR_typo_moyen=
        """
            {
              "id": "AAC2023A00",
              "partitions": [{
                "id": 99,
                "communications" :[
                  {
                    "moyenCommunication" : "email",
                    "phaseCommunication" : "ouverture",
                    "avecQuestionnairePapier" : false,
                    "protocole" : null,
                    "mode" : "web",
                    "typeModele" : "relance_libre"
                  }
                ]
              }]
            }
        """;

    static final String ctx_ERROR_1partitions_2com_1_typo_moyen=
        """
            {
              "id": "AAC2023A00",
              "partitions": [{
                "id": 99,
                "communications" :[
                  {
                    "moyenCommunication" : "mail",
                    "phaseCommunication" : "relance",
                    "avecQuestionnairePapier" : false,
                    "protocole" : null,
                    "mode" : "web",
                    "typeModele" : "relance_libre"
                  },
                  {
                    "moyenCommunication" : "xxxxxxxx",
                    "phaseCommunication" : "ouverture",
                    "avecQuestionnairePapier" : false,
                    "protocole" : null,
                    "mode" : "web",
                    "typeModele" : "relance_libre"
                  }
                ]
              }]
            }
        """;


    static final String ctx_ERROR_2partitions_2com_1_typo_phase=
            """
                {
                  "id": "AAC2023A00",
                  "partitions": [{
                    "id": 99,
                    "communications" :[
                      {
                        "moyenCommunication" : "mail",
                        "phaseCommunication" : "relance",
                        "avecQuestionnairePapier" : false,
                        "protocole" : null,
                        "mode" : "web",
                        "typeModele" : "relance_libre"
                      },
                      {
                        "moyenCommunication" : "courrier",
                        "phaseCommunication" : "relance",
                        "avecQuestionnairePapier" : false,
                        "protocole" : null,
                        "mode" : "web",
                        "typeModele" : "relance_libre"
                      }
                    ]
                  },
                  {
                    "id": 100,
                    "communications" :[
                      {
                        "moyenCommunication" : "mail",
                        "phaseCommunication" : "relance",
                        "avecQuestionnairePapier" : false,
                        "protocole" : null,
                        "mode" : "web",
                        "typeModele" : "relance_libre"
                      },
                      {
                        "moyenCommunication" : "courrier",
                        "phaseCommunication" : "xxxxxxxxxx",
                        "avecQuestionnairePapier" : false,
                        "protocole" : null,
                        "mode" : "web",
                        "typeModele" : "relance_libre"
                      }
                    ]
                  }
                  ]
                }
            """;

    private MeshuggahCtxExamples(){}

}
