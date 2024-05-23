package fr.insee.protools.backend.repository;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.dto.platine_sabiane_questionnaire.surveyunit.SurveyUnitResponseDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IUniteEnquetee {
	List<SurveyUnitResponseDto> getAllUniteEnquetee();
	SurveyUnitResponseDto getUniteEnqueteeById(String userId);

	@Transactional
	JsonNode addNewUniteEnquetee(JsonNode ue);

	@Transactional
	void addManyUniteEnquetee(List<JsonNode> listeUe);

//	@Query("{ 'replyTo': 0 }")
//	@Query(value = "{ 'replyTo' : 1 }", fields = "{ 'replyTo' : 1}")
	//@Query("{'replyTo' : null}")
//	List<UniteEnquetee> getAllByReplyToIsNull();
	List<SurveyUnitResponseDto> getAllByInProgressIsFalse();
	List<SurveyUnitResponseDto> getAllByDoneIsFalse();

//	@Transactional
//	SurveyUnitResponseDto updateUniteEnquetee(SurveyUnitResponseDto ue);
}