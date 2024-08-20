package fr.insee.protools.backend.service.context;

public final class ContextConstants {

    // Campaign context constants - Metadata
    public static final String CTX_CAMPAGNE_ID = "id";
    public static final String CTX_CAMPAGNE_LABEL = "label";
    public static final String CTX_CAMPAGNE_CONTEXTE = "contexte"; //Values: Enum CampaignContext...


    //Metadonnees Part
    public static final String CTX_METADONNEES = "metadonnees";
    public static final String CTX_META_OPERATION_ID = "operationId";//== platine : Survey
    public static final String CTX_META_SERIE_ID = "serieId"; //== platine : source
    public static final String CTX_META_ANNEE = "annee";
    public static final String CTX_META_PERIODE = "periode";
    public static final String CTX_META_PERIODICITE = "periodicite";

    public static final String CTX_META_LABEL_COURT_OPERATION = "operationLabelCourt";
    public static final String CTX_META_LABEL_LONG_OPERATION = "operationLabelLong";
    public static final String CTX_META_SERIE_LABEL_COURT = "serieLabelCourt";
    public static final String CTX_META_SERIE_LABEL_LONG = "serieLabelLong";
    public static final String CTX_META_PORTAIL_MES_ENQUETE_OPERATION = "portailMesEnquetesOperation";
    public static final String CTX_META_OBJECTIFS_COURTS = "objectifsCourts";
    public static final String CTX_META_OBJECTIFS_LONGS = "objectifsLongs";
    public static final String CTX_META_CARACTERE_OBLIGATOIRE = "caractereObligatoire";
    public static final String CTX_META_QUALITE_STATISTIQUE = "qualiteStatistique";
    public static final String CTX_META_TEST_NON_LABELLISE = "testNonLabellise";
    public static final String CTX_META_ANNEE_VISA = "anneeVisa";
    public static final String CTX_META_NUMERO_VISA = "numeroVisa";
    public static final String CTX_META_MINISTERE_TUTELLE = "ministereTutelle";
    public static final String CTX_META_PARUTION_JO = "parutionJo";
    public static final String CTX_META_DATE_PARUTION_JO = "dateParutionJo";
    public static final String CTX_META_RESPONSABLE_OPERATIONNEL =  "responsableOperationnel";
    public static final String CTX_META_RESPONSABLE_TRAITEMENT = "responsableTraitement";
    public static final String CTX_META_CNIS_URL = "cnisUrl";
    public static final String CTX_META_DIFFUSION_URL = "diffusionUrl";
    public static final String CTX_META_NOTICE_URL = "noticeUrl";
    public static final String CTX_META_SPECIMENT_URL = "specimenUrl";
    //Metadonnees proprietaire (platine)
    public static final String CTX_META_PROPRIETAIRE_ID = "proprietaireId";
    public static final String CTX_META_PROPRIETAIRE_LABEL = "proprietaireLabel";
    public static final String CTX_META_PROPRIETAIRE_LOGO = "proprietaireLogo";
    //Metadonnees assistance (platine)
    public static final String CTX_META_ASSISTANCE_NIVO2_ID = "assistanceNiveau2Id";
    public static final String CTX_META_ASSISTANCE_NIVO2_LABEL = "assistanceNiveau2Label";
    public static final String CTX_META_ASSISTANCE_NIVO2_TEL = "assistanceNiveau2Tel";
    public static final String CTX_META_ASSISTANCE_NIVO2_MAIL = "assitanceNiveau2Mail";
    public static final String CTX_META_ASSISTANCE_NIVO2_PAYS = "asssistanceNiveau2Pays";
    public static final String CTX_META_ASSISTANCE_NIVO2_NUMERO_VOIE = "assistanceNiveau2NumeroVoie";
    public static final String CTX_META_ASSISTANCE_NIVO2_NOM_VOIE = "assistanceNiveau2NomVoie";
    public static final String CTX_META_ASSISTANCE_NIVO2_COMMUNE = "assistanceNiveau2Commune";
    public static final String CTX_META_ASSISTANCE_NIVO2_CODE_POSTAL = "assistanceNiveau2CodePostal";
    public static final String CTX_META_MAIL_BOITE_RETOUR = "mailBoiteRetour";
    public static final String CTX_META_URL_ENQUETE = "urlEnquete";
    public static final String CTX_META_THEME_MIEUX_CONNAITRE_MAIL = "themeMieuxConnaitreMail";
    public static final String CTX_META_SRVC_COL_SIGN_FONCTION = "serviceCollecteurSignataireFonction";
    public static final String CTX_META_SRVC_COL_SIGN_NOM = "serviceCollecteurSignataireNom";
    public static final String CTX_META_PRESTATAIRE = "prestataire";
    public static final String CTX_META_MAIL_RESP_OPERATIONNEL = "mailResponsableOperationnel";
    public static final String CTX_META_LOGO_PRESTATAIRE = "logoPrestataire";
    //Metadonnes platine
    public static final String CTX_META_URL_LOI_RGPD = "urlLoiRGPD";
    public static final String CTX_META_URL_LOI_STATISTIQUE = "urlLoiStatistique";
    public static final String CTX_META_URL_LOI_INFORMATIQUE = "urlLoiInformatique";


    //Pour sabiane
    public static final String CTX_META_REPERAGE = "reperage";
    public static final String CTX_META_ESSAIS_CONTACT = "essaisContact";
    public static final String CTX_META_BILAN_CONTACT = "bilanContact";
    public static final String CTX_META_REFERENTS_PRINCIPAUX = "referentsPrincipaux";
    public static final String CTX_META_REFERENTS_SECONDAIRES = "referentsSecondaires";
    public static final String CTX_META_SITES_GESTION = "sitesGestion";
    //Referents principaux et secondaires (sabiane)
    public static final String CTX_META_REFERENT_NOM = "nom";
    public static final String CTX_META_REFERENT_PRENOM = "prenom";
    public static final String CTX_META_REFERENT_TELEPHONE = "telephone";

    // Partitions
    public static final String CTX_PARTITIONS = "partitions";
    public static final String CTX_PARTITION_ID = "id";
    public static final String CTX_PARTITION_LABEL = "label";
    public static final String CTX_PARTITION_TYPE_ECHANTILLON = "typeEchantillon";
    public static final String CTX_PARTITION_DATE_DEBUT_COLLECTE = "dateDebutCollecte";
    public static final String CTX_PARTITION_DATE_FIN_COLLECTE = "dateFinCollecte";
    public static final String CTX_PARTITION_DATE_RETOUR = "dateRetour";
    public static final String CTX_PARTITION_QUESTIONNAIRE_MODEL = "questionnaireModel";
    public static final String CTX_PARTITION_QUIREPOND1 = "quiRepond1";
    public static final String CTX_PARTITION_QUIREPOND2 = "quiRepond2";
    public static final String CTX_PARTITION_QUIREPOND3 = "quiRepond3";
    public static final String CTX_PARTITION_PRIORITAIRE = "prioritaire";

    // Partitions Communications
    public static final String CTX_PARTITION_COMMUNICATIONS = "communications";
    public static final String CTX_PARTITION_COMMUNICATION_ID = "id";
    public static final String CTX_PARTITION_COMMUNICATION_ECHEANCE = "echeance";

    public static final String CTX_PARTITION_COMMUNICATION_MOYEN = "moyenCommunication";
    public static final String CTX_PARTITION_COMMUNICATION_PHASE = "phaseCommunication";
    public static final String CTX_PARTITION_COMMUNICATION_AVEC_QUESTIONNAIRE_PAPIER = "avecQuestionnairePapier";
    public static final String CTX_PARTITION_COMMUNICATION_PROTOCOLE = "protocole";
    public static final String CTX_PARTITION_COMMUNICATION_MODE = "mode";
    public static final String CTX_PARTITION_COMMUNICATION_TYPE_MODELE = "typeModele";
    public static final String CTX_PARTITION_COMMUNICATION_OBJET_MAIL = "objetMail";
    public static final String CTX_PARTITION_COMMUNICATION_RELANCE_LIBRE_PARAGRAPHE1 = "relanceLibreMailParagraphe1";
    public static final String CTX_PARTITION_COMMUNICATION_RELANCE_LIBRE_PARAGRAPHE2 = "relanceLibreMailParagraphe2";
    public static final String CTX_PARTITION_COMMUNICATION_RELANCE_LIBRE_PARAGRAPHE3 = "relanceLibreMailParagraphe3";
    public static final String CTX_PARTITION_COMMUNICATION_RELANCE_LIBRE_PARAGRAPHE4 = "relanceLibreMailParagraphe4";
    public static final String CTX_PARTITION_COMMUNICATION_COMPLEMENT_CONNEXION = "complementConnexion";
    public static final String CTX_PARTITION_COMMUNICATION_TYPE = "typeCommunication";



    //Partitions, specific sabiane
    public static final String CTX_PARTITION_SABIANE_DATE_DEBUT_VISIBILITE_GESTIONNAIRE = "dateDebutVisibiliteGestionnaire";
    public static final String CTX_PARTITION_SABIANE_DATE_DEBUT_VISIBILITE_ENQUETEUR = "dateDebutVisibiliteEnqueteur";
    public static final String CTX_PARTITION_SABIANE_DATE_DEBUT_REPERAGE = "dateDebutReperage";
    public static final String CTX_PARTITION_SABIANE_DATE_FIN_TRAITEMENT = "dateFinTraitement";

    //Partitions, specific ERA
    public static final String CTX_PARTITION_ERA_SEXE = "sexe";

    // QuestionnaireModels
    public static final String CTX_QUESTIONNAIRE_MODELS = "questionnaireModels";
    public static final String CTX_QUESTIONNAIRE_MODEL_ID = "id";
    public static final String CTX_QUESTIONNAIRE_MODEL_CHEMIN_REPERTOIRE = "cheminRepertoire";
    public static final String CTX_QUESTIONNAIRE_MODEL_LABEL = "label";
    public static final String CTX_QUESTIONNAIRE_MODEL_REQUIRED_NOMENCLATURES = "requiredNomenclatureIds";

    // Nomenclatures
    public static final String CTX_NOMENCLATURES = "nomenclatures";
    public static final String CTX_NOMENCLATURE_ID = "id";
    public static final String CTX_NOMENCLATURE_CHEMIN_REPERTOIRE = "cheminRepertoire";
    public static final String CTX_NOMENCLATURE_LABEL = "label";
    //Constants class should not be initialized
    private ContextConstants() {
    }
 }