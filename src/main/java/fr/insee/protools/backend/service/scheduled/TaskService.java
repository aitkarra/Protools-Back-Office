package fr.insee.protools.backend.service.scheduled;

import fr.insee.protools.backend.repository.IUniteEnquetee;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TaskService {

  private final IUniteEnquetee iUniteEnquetee;

  public TaskService(IUniteEnquetee iUniteEnquetee) {
    this.iUniteEnquetee = iUniteEnquetee;
  }

  public void isTerminated() throws InterruptedException {
    log.info("IUniteEnquetee.isTerminated");
    iUniteEnquetee.isTerminated();
  }


}