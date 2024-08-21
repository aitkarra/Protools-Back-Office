package fr.insee.protools.backend.repository;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IUniteEnquetee {

	@Transactional
	void addManyUniteEnquetee(List<JsonNode> listeUe);

	void addManyUniteEnquetee(List<JsonNode> listeUe, String processInstanceId, String campaignId, String questionnaireId);

	void addManyUniteEnqueteeDeleteColonneClass(List<JsonNode> listeUe);

	boolean isTerminated();
}