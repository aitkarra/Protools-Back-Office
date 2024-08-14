package fr.insee.protools.backend.service;

/**
 * Class to store flowable variable identifiers
 */
public class FlowableVariableNameConstants {

    //TODO : renommer toutes ces variables pour remplacer les - par des _ (comme ca ca sera utilisable dans les resolvers)
    //Protools context
    public static final String VARNAME_CONTEXT="context";
    public static final String VARNAME_CONTEXT_PARTITION_ID_LIST="contexte_partition_id_list";
    public static final String VARNAME_CONTEXT_PARTITION_VARIABLES_BY_ID="contexte_partition_variables_by_partitionid";
    //To treat partitions one by one (Long)
    public static final String VARNAME_CURRENT_PARTITION_ID="current_partition_id";
    //to pass a list of REM interrogation ids (List<Long>)
    public static final String VARNAME_REM_INTERRO_ID_LIST ="rem_interrogation_id_list";
    //to pass a single REM survey unit's ID (Long)
    public static final String VARNAME_REM_SURVEY_UNIT_IDENTIFIER ="rem_survey_unit_id";
    //to pass a REM survey unit content (JsonNode)
    public static final String VARNAME_REM_INTERROGATION ="rem_interrogation";
    //To pass a list of REM interrogations content (JsonNode)
    public static final String VARNAME_REM_INTERRO_LIST ="rem_interrogation_list";
    //REM PAGEABLE
    public static final String VARNAME_REM_INTERRO_LIST_PAGEABLE_IS_LAST_PAGE ="rem_su_list_page_is_last";
    public static final String VARNAME_REM_INTERRO_LIST_PAGEABLE_CURRENT_PAGE ="rem_interro_list_page_current";

    public static final String VARNAME_REM_PROTOOLS_INTERRO_LIST = "rem_protools_interrogation_list";

    //Sugoi ID/PWD
    public static final String VARNAME_DIRECTORYACCESS_ID_CONTACT ="directory_access_id_contact";
    public static final String VARNAME_DIRECTORYACCESS_PWD_CONTACT ="directory_access_pwd_contact";

    public static final String VARNAME_DIRECTORYACCESS_PWD_FOR_INTERRO_ID_MAP ="directory_access_pwd_contact_by_interroration_id";

    //ERA response
    public static final String VARNAME_ERA_RESPONSE="era_response_list";
    public static final String VARNAME_ERA_QUERY_START_DATE="era_query_start_date";
    public static final String VARNAME_ERA_QUERY_END_DATE="era_query_end_date";
    //Platine Contact details
    public static final String VARNAME_PLATINE_CONTACT="platine_contact";
    //Indicates whether a questioning should be follow up or not
    public static final String VARNAME_SU_IS_TO_FOLLOWUP="survey_unit_is_to_follow_up";
    //List with for each SU the Instant when the opening communication has been sent
    //each row is Tuple<Instant: creation instant,String : partitionId , String : remSUId>
    public static final String VARNAME_SU_CREATION_ITEM="su_creation_date_item";

    private FlowableVariableNameConstants(){}
}
