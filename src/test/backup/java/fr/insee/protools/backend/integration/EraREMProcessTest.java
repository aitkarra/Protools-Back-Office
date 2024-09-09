package fr.insee.protools.backend.integration;


import fr.insee.protools.backend.service.context.ContextServiceImpl;
import fr.insee.protools.backend.service.rem.RemService;
import fr.insee.protools.backend.dto.rem_tmp.REMSurveyUnitDto;
import fr.insee.protools.backend.dto.rem_tmp.SuIdMappingJson;
import fr.insee.protools.backend.dto.rem_tmp.SuIdMappingRecord;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.FlowableTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@FlowableTest
@SpringBootTest
@TestPropertySource(locations = "/application-test.properties")
@ExtendWith(MockitoExtension.class)
class EraREMProcessTest {

  @Autowired ContextServiceImpl contextService;
  @MockBean EraService eraService;
  @MockBean RemService remService;

  final static String ressourceFolder = ClassUtils.convertClassNameToResourcePath(EraREMProcessTest.class.getPackageName());
  final static String ctx_json = ressourceFolder + "/collecte_web_no_com.json";

  static AtomicLong i=new AtomicLong(0l);

  @Test
  @Deployment(resources = { "fr/insee/protools/backend/integration/noCTX_eraREM.bpmn20.xml" })
  void testBPMN_ERA_REM() {

    //ERA SU for 2 Partitions (2 SU for each partition)
    //42 : Men
    //43 : Females
    String partition1="42",partition2="41";
    CensusJsonDto eraP1SU1=createCensusJsonDto("1",partition1);
    CensusJsonDto eraP1SU2=createCensusJsonDto("2",partition1);
    CensusJsonDto eraP2SU1=createCensusJsonDto("3",partition2);
    CensusJsonDto eraP2SU2=createCensusJsonDto("4",partition2);

    List<CensusJsonDto> listOfEraSU_P1 = List.of(eraP1SU1,eraP1SU2);
    List<CensusJsonDto> listOfEraSU_P2 = List.of(eraP2SU1,eraP2SU2);
    SuIdMappingJson mapping_P1 = createSuIdMappingJson(partition1,listOfEraSU_P1);
    SuIdMappingJson mapping_P2 = createSuIdMappingJson(partition2,listOfEraSU_P2);
    LocalDate startDate = LocalDate.now().minusDays(1);
    LocalDate endDate = LocalDate.now();

    Mockito.when(eraService.getSUForPeriodAndSex(startDate,endDate, GenderType.MALE)).thenReturn(listOfEraSU_P1);
    Mockito.when(eraService.getSUForPeriodAndSex(startDate,endDate,GenderType.FEMALE)).thenReturn(listOfEraSU_P2);

    Mockito.when(remService.writeERASUList(partition1,listOfEraSU_P1))
            .thenReturn(mapping_P1);
    Mockito.when(remService.writeERASUList(partition2,listOfEraSU_P2))
            .thenReturn(mapping_P2);

    REMSurveyUnitDto rem_P1SU1 = createRemSUFromMapping(mapping_P1.getData().get(0));
    REMSurveyUnitDto rem_P1SU2 = createRemSUFromMapping(mapping_P1.getData().get(1));
    REMSurveyUnitDto rem_P2SU1 = createRemSUFromMapping(mapping_P2.getData().get(0));
    REMSurveyUnitDto rem_P2SU2 = createRemSUFromMapping(mapping_P2.getData().get(1));


    Mockito.when(remService.getSurveyUnit(mapping_P1.getData().get(0).repositoryId()))
            .thenReturn(rem_P1SU1);
    Mockito.when(remService.getSurveyUnit(mapping_P1.getData().get(1).repositoryId()))
            .thenReturn(rem_P1SU2);
    Mockito.when(remService.getSurveyUnit(mapping_P2.getData().get(0).repositoryId()))
            .thenReturn(rem_P2SU1);
    Mockito.when(remService.getSurveyUnit(mapping_P2.getData().get(1).repositoryId()))
            .thenReturn(rem_P2SU2);

    try (InputStream is = EraREMProcessTest.class.getClassLoader().getResourceAsStream(ctx_json)){
      MockMultipartFile multipartFile = new MockMultipartFile("file.json", "file.json", "text/json", is.readAllBytes());
      String processID = contextService.processContextFileAndCreateProcessInstance(multipartFile,"eraREM","toto");
      assertThat(processID).isNotBlank();

    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static CensusJsonDto createCensusJsonDto(String eraId, String partitionID){
    return CensusJsonDto.builder()
            .id(eraId)
            .identifiantCompte("SU1_PART_"+partitionID)
            .build();
  }
  private static SuIdMappingJson createSuIdMappingJson(String partitionId,List<CensusJsonDto> eraSUList){
    List<SuIdMappingRecord> mappingEraREM = eraSUList.stream().map(censusJsonDto -> new SuIdMappingRecord(i.incrementAndGet(),String.valueOf(censusJsonDto.getId()))).toList();
    return SuIdMappingJson.builder()
            .count(mappingEraREM.size())
            .partitionId(partitionId)
            .data(mappingEraREM)
            .build();
  }

  private static REMSurveyUnitDto createRemSUFromMapping(SuIdMappingRecord mapping){
    return REMSurveyUnitDto.builder()
            .repositoryId(mapping.repositoryId())
            .externalId(mapping.externalId())
            .build();

  }
}