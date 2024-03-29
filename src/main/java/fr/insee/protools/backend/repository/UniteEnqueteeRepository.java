package fr.insee.protools.backend.repository;

import fr.insee.protools.backend.dto.platine_sabiane_questionnaire.surveyunit.SurveyUnitResponseDto;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface UniteEnqueteeRepository extends MongoRepository<SurveyUnitResponseDto, String> {
}