package fr.insee.protools.backend.integration;


import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.test.Deployment;
import org.flowable.spring.impl.test.FlowableSpringExtension;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;


@ContextConfiguration(value = {
        "classpath:fr/insee/protools/backend/integration/springTypicalUsageTest-context.xml",
        "classpath:fr/insee/protools/backend/integration/springTestDelegate-context.xml"
})
@TestPropertySource(locations = "/application-test.properties")
@ExtendWith(FlowableSpringExtension.class)
class SimpleProcessTest {

    //private ProcessEngine processEngine;
    private RuntimeService runtimeService;
    private TaskService taskService;

    @BeforeEach
    void setUp(ProcessEngine processEngine) {
        //this.processEngine = processEngine;
        this.runtimeService = processEngine.getRuntimeService();
        this.taskService = processEngine.getTaskService();
    }

    @Test
    @Deployment
    void testSimpleProcess() {
        runtimeService.startProcessInstanceByKey("simpleProcess");

        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("My User Task");

        taskService.complete(task.getId());
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
    }

    @Test
    @Deployment
    void testDelegateProcess() {
        runtimeService.startProcessInstanceByKey("delegateProcess");

        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("My User Task");

        taskService.complete(task.getId());
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

    }
}
