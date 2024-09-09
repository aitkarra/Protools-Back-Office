package fr.insee.protools.backend.flowable.engine;


import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.FlowableTest;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@FlowableTest
class SimpleProcessTest {

  private ProcessEngine processEngine;
  private RuntimeService runtimeService;
  private TaskService taskService;

  @BeforeEach
  void setUp(ProcessEngine processEngine) {
    this.processEngine = processEngine;
    this.runtimeService = processEngine.getRuntimeService();
    this.taskService = processEngine.getTaskService();
  }

  @Test
  @Deployment
  void testSimpleProcess() {
    runtimeService.startProcessInstanceByKey("simpleProcess");

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("My Task");

    taskService.complete(task.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

  }
}