package fr.insee.protools.backend.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBObject;
import fr.insee.protools.backend.dto.platine_sabiane_questionnaire.surveyunit.SurveyUnitResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@Repository
@Slf4j
public class UniteEnqueteeImpl implements IUniteEnquetee {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Override
	public List<SurveyUnitResponseDto> getAllUniteEnquetee() {
		return mongoTemplate.findAll(SurveyUnitResponseDto.class);
	}
	private static final ObjectMapper objectMapper =
			new ObjectMapper()
					.configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
					.configure(FAIL_ON_MISSING_CREATOR_PROPERTIES, true);


	@Override
	public SurveyUnitResponseDto getUniteEnqueteeById(String UniteEnqueteeId) {
		Query query = new Query();
		query.addCriteria(where("userId").is(UniteEnqueteeId));
		return mongoTemplate.findOne(query, SurveyUnitResponseDto.class);
	}

	@Override
	public JsonNode addNewUniteEnquetee(JsonNode ue) {
		//JSonNode jackson
		//mongoTemplate.save(objectMapper.valueToTree(ue), "MaCollectionDeFarid");

		//JSonNode
//        mongoTemplate.save(ue, "SurveyUnitJSonNode");
		//String
		//            mongoTemplate.save(objectMapper.writeValueAsString(ue), "SurveyUnitString");
		// Now, user object will contain the ID as well

		String correlationID = UUID.randomUUID().toString();
//		((ObjectNode) ue).put("correlationID", correlationID);
//		((ObjectNode) ue).put("inProgress", false);
//		((ObjectNode) ue).put("done", false);

		BasicDBObject dbObject = new BasicDBObject();
		HashMap<String, Object> keyValuePairs = null;
		//            keyValuePairs = new ObjectMapper().readValue(ue.traverse(), HashMap.class);
		keyValuePairs = new HashMap<>();
		keyValuePairs.put("payload", ue);
		keyValuePairs.put("correlationID", correlationID);
		keyValuePairs.put("inProgress", false);
		keyValuePairs.put("done", false);
		dbObject.putAll(keyValuePairs);
		mongoTemplate.save(dbObject, "commandes");

		return ue;
	}

	@Override
	public void addManyUniteEnquetee(List<JsonNode> listeUe) {
		for(JsonNode ue : listeUe)
		{
			String correlationID = UUID.randomUUID().toString();

			BasicDBObject dbObject = new BasicDBObject();
			HashMap<String, Object> keyValuePairs = null;
			//            keyValuePairs = new ObjectMapper().readValue(ue.traverse(), HashMap.class);
			keyValuePairs = new HashMap<>();
			keyValuePairs.put("payload", ue);
			keyValuePairs.put("correlationID", correlationID);
			keyValuePairs.put("inProgress", false);
			keyValuePairs.put("done", false);
			dbObject.putAll(keyValuePairs);
			mongoTemplate.save(dbObject, "commandes");
		}
		log.info("===>La collection listeUe est vide<===");
	}

	@Override
	public  List<SurveyUnitResponseDto> getAllByInProgressIsFalse() {
		log.info("getAllByInProgressIsFalse.");
//		Query query = new Query(where("done").in(false, ""));
		Query query = new Query();
		query.addCriteria(
				Criteria.where("").andOperator(
						Criteria.where("inProgress").in(false, ""),
						Criteria.where("done").in(false, "")
				)
		);
		//return mongoTemplate.findById(query, UniteEnquetee.class);
		return mongoTemplate.find(query, SurveyUnitResponseDto.class);
	}

	@Override
	public  List<SurveyUnitResponseDto> getAllByDoneIsFalse() {
		log.info("getAllByDoneIsFalse.");
//		Query query = new Query(where("done").in(false, ""));
		Query query = new Query();
		query.addCriteria(
				Criteria.where("").andOperator(
						Criteria.where("inProgress").in(true, ""),
						Criteria.where("done").in(false, "")
				)
		);
		//return mongoTemplate.findById(query, UniteEnquetee.class);
		return mongoTemplate.find(query, SurveyUnitResponseDto.class);
	}

//	@Override
//	public SurveyUnitResponseDto updateUniteEnquetee(SurveyUnitResponseDto ue) {
////		findAndModify() apporte le meilleur des upsert() et save() . méthodes
////		Comme la méthode upsert() , la méthode findAndModify() accepte également des critères sur d'autres champs en dehors de '_id' pour trouver le document à mettre à jour.
////		Nous pouvons également effectuer une mise à jour delta ici, comme la méthode upsert() .
////		findAndModify() n'accepte pas l'intégralité de l'objet Document mais uniquement la UpdateDefinition contenant les détails dont tous les champs doivent être mis à jour
////		Comme la méthode save() , la méthode findAndModify() renvoie l’intégralité de l’objet document. De plus, nous avons la possibilité de mentionner si nous voulons l'ancien objet document avant la mise à jour ou le nouvel objet document après la mise à jour comme valeur de retour.
////		Nous pouvons choisir si nous voulons insérer le nouveau document ou non si aucun document ne correspond à la requête de recherche en fournissant l' option 'upsert(true)' à la méthode findAndModify() .
//		Query query = new Query();
//		query.addCriteria(where("correlationID").is(ue.getCorrelationID()));
////		KO - mongoTemplate.findAndReplace(query, ue);
//		Update updateDefinition = new Update().set("inProgress",ue.isInProgress()).set("done",ue.isDone()).set("replyTo",ue.getReplyTo());
//		FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true).upsert(false);
//		return mongoTemplate.findAndModify(query, updateDefinition, options, UniteEnquetee.class);
////		findAndReplace()
////		FindAndReplaceOptions options = new FindAndReplaceOptions().upsert().returnNew();
////		return mongoTemplate.findAndReplace(query, ue, options, UniteEnquetee.class, "uniteEnquetee", UniteEnquetee.class);
//	}


}