package fr.insee.protools.backend.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.collect;

@Repository
@Slf4j
public class UniteEnqueteeImpl implements IUniteEnquetee {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Setter
    @Getter
    private String processInstanceId;
	@Setter
    @Getter
    private String idCampaign;
	@Setter
    @Getter
    private String questionnaireId;

	private static final ObjectMapper objectMapper =
			new ObjectMapper()
					.configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
					.configure(FAIL_ON_MISSING_CREATOR_PROPERTIES, true);


	private BasicDBObject getUEUpdate(JsonNode test){
		String correlationID = UUID.randomUUID().toString();
		BasicDBObject dbObject = new BasicDBObject();
		HashMap<String, Object> keyValuePairs = null;
		keyValuePairs = new HashMap<>();
        try {
            keyValuePairs.put("payload",  new ObjectMapper().writeValueAsString(test));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        keyValuePairs.put("correlationID", correlationID);
		keyValuePairs.put("inProgress", false);
		keyValuePairs.put("done", false);
		keyValuePairs.put("processInstanceID", getProcessInstanceId());
		keyValuePairs.put("CampaignID", getIdCampaign());
		keyValuePairs.put("questionnaireID", getQuestionnaireId());
		dbObject.putAll(keyValuePairs);
		return dbObject;
	}


	@Override
	public void addManyUniteEnquetee(List<JsonNode> listeUe) {
		List<BasicDBObject> bo = listeUe.parallelStream()
				.map(this::getUEUpdate)
				.toList();
		mongoTemplate.insert(bo, "commandes");
	}

	@Override
	public void addManyUniteEnqueteeDeleteColonneClass(List<JsonNode> listeUe) {
		List<BasicDBObject> bo = listeUe.parallelStream()
				.map(this::getUEUpdate)
				.toList();
		//remove _class
		MappingMongoConverter converter =
				new MappingMongoConverter(mongoTemplate.getMongoDatabaseFactory(), new MongoMappingContext());
		converter.setTypeMapper(new DefaultMongoTypeMapper(null));
		MongoTemplate mongoTemplate2 = new MongoTemplate(mongoTemplate.getMongoDatabaseFactory(), converter);
		mongoTemplate2.insert(bo, "commandes");
	}

	@Override
	public void addManyUniteEnquetee(List<JsonNode> listeUe, String processInstanceId, String campaignId, String questionnaireId) {
		this.setProcessInstanceId(processInstanceId);
		this.setIdCampaign(campaignId);
		this.setQuestionnaireId(questionnaireId);
		List<BasicDBObject> bo = listeUe.parallelStream()
				.map(this::getUEUpdate)
				.toList();
		//remove _class
		MappingMongoConverter converter =
				new MappingMongoConverter(mongoTemplate.getMongoDatabaseFactory(), new MongoMappingContext());
		converter.setTypeMapper(new DefaultMongoTypeMapper(null));
		MongoTemplate mongoTemplate2 = new MongoTemplate(mongoTemplate.getMongoDatabaseFactory(), converter);
		mongoTemplate2.insert(bo, "commandes");
	}

}