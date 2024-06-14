# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased 1.1.1-SNAPSHOT] - xx-xx-xxxx
### Changed
#### BPMN TASKS
- [sugoiCreateUserTask](TODO)
  La longueur du mot de passe crée dépend du contexte (8 pour household ; 12 sinon)
- [cleanUserPasswordTask](TODO)
  Suppression de toutes les variables de mots de passe (sur le contexte et dans l'historique)

## [1.1.0] - 14-05-2024
### Added
#### BPMN TASKS
- [SabianePilotageCreateSUTask](http://preparation_collecte.gitlab-pages.insee.fr/prepadoc/Protools/taches/#cr%c3%a9er-une-ue-dans-la-plateforme-de-collecte-enqu%c3%aateur-partie-pilotage)
  Tâche de création d'une UE dans sabiane pilotage
- [SabianeQuestionnaireCreateSUTask](http://preparation_collecte.gitlab-pages.insee.fr/prepadoc/Protools/taches/#cr%c3%a9er-une-ue-dans-la-plateforme-de-collecte-enqu%c3%aateur-partie-questionnaire)
  Tâche de création d'une UE dans sabiane questionnaire
- [sugoiCreateUserTask](http://preparation_collecte.gitlab-pages.insee.fr/prepadoc/Protools/taches/#cr%c3%a9ation-de-comptes-dans-lannuaire-sugoi)
  Tâche de création d'un utilisateur dans l'annuaire Sugoi et initialization d'un mot de passe par défaut
- [platinePilotageGetSUIsToFollowUpTask]( TODO)
  Tâche de lecture dans Platine Pilotage de l'état a-relancer/eligible/isToFollowUp d'une UE (d'une partition).
- [platinePilotageAddSUFollowUpTask]( TODO)
  Tâche de d'ajout d'un évènement FOLLOW à une UE dans Platine Pilotage.
- [flowcontrolIsSUToFollowUp]( TODO)
  Prépare aux relances


## [1.0.0] - 01-01-2024

Montée de version du moteur flowable de 7.0.0.M1 à 7.1.0
Travail sur les tâches BPMN pour ERA et REM.

### Added
#### BPMN TASKS
- [platinePilotageCreateSurveyUnitTask](http://preparation_collecte.gitlab-pages.insee.fr/prepadoc/Protools/taches/#cr%c3%a9er-une-ue-dans-la-plateforme-de-collecte-web-partie-pilotage)
  Tâche de création d'une UE dans Platine pilotage
- [platineQuestionnaireCreateSurveyUnitTask](http://preparation_collecte.gitlab-pages.insee.fr/prepadoc/Protools/taches/#cr%c3%a9er-une-ue-dans-la-plateforme-de-collecte-web-partie-questionnaire)
  Tâche de création d'une UE dans Platine questionnaire
- [remGetPartitionListOfSuIdTask](http://preparation_collecte.gitlab-pages.insee.fr/prepadoc/Protools/taches/#r%c3%a9cup%c3%a9rer-dans-rem-des-identifiants-des-ue-dune-partition)
  Tâche de récupération de tous les ID d'UE d'une partition REM.
- [remGetSUTask](http://preparation_collecte.gitlab-pages.insee.fr/prepadoc/Protools/taches/#r%c3%a9cup%c3%a9ration-dune-ue-dans-rem)
  Tâche de récupération d'une UE dans REM à partir de son ID.
- [extractContactIdentifierFromREMSUTask](http://preparation_collecte.gitlab-pages.insee.fr/prepadoc/Protools/taches/#extraire-lidentifiant-de-compte-des-additionals-info-dans-une-ue-rem)
  Tâche d'extraction de l'identifiant internet depuis les additionalInformations d'un json d'UE REM
- [remWriteEraSUListTask](http://preparation_collecte.gitlab-pages.insee.fr/prepadoc/Protools/taches/#ecriture-dune-liste-due-dans-une-partition-rem)
  Tâche d'écriture dans REM d'une liste d'UE récupérée dans ERA
- [eraGetSUForPeriodAndGenderTask](http://preparation_collecte.gitlab-pages.insee.fr/prepadoc/Protools/taches/#extractions-de-donn%c3%a9ees-du-rp-avec-era)
  Tâche de lecture d'UE dans ERA pour un interval et un sexe donnée.
- [platinePilotageGetSUContactTask]( http://preparation_collecte.gitlab-pages.insee.fr/prepadoc/Protools/taches/#suivi-dans-les-plateformes-de-collecte)
  Tâche de lecture dans Platine Pilotage des informations de contact d'une UE (d'une partition).
- [MeshuggahCreateCommunicationsContextTask](http://preparation_collecte.gitlab-pages.insee.fr/prepadoc/Protools/taches/#envoi-de-communication-avec-meshuggah)
  Tâche de création d'une communication dans Meshuggah
- [meshuggahSendOpeningMailCommunicationForSUTask](http://preparation_collecte.gitlab-pages.insee.fr/prepadoc/Protools/taches/#envoi-du-mail-douverture-avec-meshuggah)
  Tâche d'envoi du mail d'ouverture avec Meshuggah



#### Endpoints
-  /api_configuration qui renverra la configuration de chaque API orchestrée par protools. Cela permet donc de savoir
   quelle plateforme est appelée.


#### Autres
- Vérification que le fichier de contexte satisfait bien toutes les tâches du BPMN associé au processus.
- Possibilité d'utiliser la date de début et de fin de collecte d'une partition dans les expressions BPMN via PartitionCtxResolver
- Ajout des périodes X01 à X99 à l'énumération pour pilotage

-
### Changed
#### Variables du processus
- "sugoi-id-contact" devient "directory_access-id-contact" (peut casser les BPMN la référençant en dur).

### Fixed

## [0.0.3] - 2023-05-16

### Added
Création de contexte de campagne dans sabiane et platine
### Changed
### Fixed
