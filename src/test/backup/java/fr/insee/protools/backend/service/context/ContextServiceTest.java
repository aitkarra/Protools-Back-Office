package fr.insee.protools.backend.service.context;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.service.context.exception.BadContextIOException;
import fr.insee.protools.backend.service.context.exception.BadContextNotJSONBPMNError;
import fr.insee.protools.backend.service.exception.TaskNotFoundException;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.RepositoryServiceImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.engine.test.FlowableTest;
import org.flowable.task.service.impl.TaskQueryImpl;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@FlowableTest
class ContextServiceTest {
    final static String ressourceFolder = ClassUtils.convertClassNameToResourcePath(ContextServiceTest.class.getPackageName());

    @Spy
    private RuntimeService runtimeService;
    @Spy
    private RepositoryService repositoryService = new RepositoryServiceImpl();
    @Mock
    private TaskService taskService;

    @Spy
    @InjectMocks
    ContextServiceImpl contextService;

    private String dummyId="ID";

    private void initTaskServiceMock(){
        TaskEntityImpl task = new TaskEntityImpl();
        task.setProcessInstanceId(dummyId);
        TaskQueryImpl tq = mock(TaskQueryImpl.class);
        when(tq.taskId(any())).thenReturn(tq);
        when(tq.singleResult()).thenReturn(task);
        when(taskService.createTaskQuery()).thenReturn(tq);
    }


    private void initRuntimeSericeMock(){
        ProcessInstanceQuery piq = mock(ProcessInstanceQuery.class);
        ProcessInstance pi = new ExecutionEntityImpl();

        when(piq.processInstanceId(any())).thenReturn(piq);
        when(piq.singleResult()).thenReturn(pi);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(piq);
    }

    @Test
    void processContextFileAndCompleteTask_should_throw_when_fileNotJson() {
        //Preconditions
        MockMultipartFile multipartFile = new MockMultipartFile("file.xml", "file.xml", "text/xml", "some xml".getBytes());
        initTaskServiceMock();

        //Call method under test
        assertThrows(BadContextNotJSONBPMNError.class, () -> contextService.processContextFileAndCompleteTask(multipartFile,dummyId));
    }


    @Test
    void processContextFileAndCompleteTask_should_throw_when_incorectJson() {
        //Preconditions
        MockMultipartFile multipartFile = new MockMultipartFile("file.json", "file.json", "text/json", "{toto}".getBytes());
        initTaskServiceMock();

        //Call method under test
        assertThrows(BadContextIOException.class, () -> contextService.processContextFileAndCompleteTask(multipartFile,dummyId));
    }


    @Test
    void processContextFileAndCompleteTask_should_throw_when_taskNotExists() {
        //Preconditions
        MockMultipartFile multipartFile = new MockMultipartFile("file.json", "file.json", "text/json", "{}".getBytes());
        TaskQueryImpl tq = mock(TaskQueryImpl.class);
        when(tq.taskId(any())).thenReturn(tq);
        when(tq.singleResult()).thenReturn(null);
        when(taskService.createTaskQuery()).thenReturn(tq);


        //Call method under test
        assertThrows(TaskNotFoundException.class, () -> contextService.processContextFileAndCompleteTask(multipartFile,dummyId));
    }


    @Test
    void processContextFileAndCompleteTask_should_work_when_ContextOk() throws IOException {
        //Preconditions
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(ressourceFolder+"/protools-contexte-platine.json");
        MockMultipartFile multipartFile = new MockMultipartFile("file.json", "file.json", "text/json", resourceAsStream.readAllBytes());
        initTaskServiceMock();
        //we skip context verification by returning no error
        doReturn(Set.of()).when(contextService).isContextOKForBPMN(any(),any());
        //Do not try to continue in a BPMN
        doNothing().when(taskService).complete(any(),anyMap());
        //Call method under test
        assertThatCode(() -> contextService.processContextFileAndCompleteTask(multipartFile,dummyId)).doesNotThrowAnyException();
    }

    @Test
    void getContextByProcessInstance_should_work_when_exists_and_AlreadyLoaded() throws IOException {
        //Preconditions
        initRuntimeSericeMock();
        processContextFileAndCompleteTask_should_work_when_ContextOk();
        //Call method under test
        JsonNode contextRootNode = contextService.getContextByProcessInstance(dummyId);

        //Post conditions : We've got a valid context object
        assertNotNull(contextRootNode);
        assertNotNull(contextRootNode.get(ContextConstants.CTX_METADONNEES));
        assertNotNull(contextRootNode.get(ContextConstants.CTX_CAMPAGNE_ID));
        assertNotNull(contextRootNode.get(ContextConstants.CTX_CAMPAGNE_LABEL));
        assertEquals("DEM2022X00",contextRootNode.get(ContextConstants.CTX_CAMPAGNE_ID).asText());
    }

    @Test
    void processContextFileAndCreateProcessInstance_should_work_when_ContextOk() throws IOException {
        //Preconditions
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(ressourceFolder+"/protools-contexte-platine.json");
        MockMultipartFile multipartFile = new MockMultipartFile("file.json", "file.json", "text/json", resourceAsStream.readAllBytes());

        ProcessInstance pi = mock(ExecutionEntityImpl.class);
        when(pi.getProcessInstanceId()).thenReturn(dummyId);
        when(runtimeService.startProcessInstanceByKey(any(),any(),any())).thenReturn(pi);
        initRuntimeSericeMock();
        //we skip context verification by returning no error
        doReturn(Set.of()).when(contextService).isContextOKForBPMN(any(),any());

        //Call method under test
        String processId=contextService.processContextFileAndCreateProcessInstance(multipartFile,"simpleProcess",dummyId);

        //Post conditions : We've got a valid Process Instance
        assertEquals(dummyId,processId);
        //Process instance creation has been called once
        verify(runtimeService).startProcessInstanceByKey(any(),any(),any());
        //                  We've got a valid context object
        JsonNode contextRootNode = contextService.getContextByProcessInstance(dummyId);
        //Post conditions : We've got a valid context object
        assertNotNull(contextRootNode);
        assertNotNull(contextRootNode.get(ContextConstants.CTX_METADONNEES));
        assertNotNull(contextRootNode.get(ContextConstants.CTX_CAMPAGNE_ID));
        assertNotNull(contextRootNode.get(ContextConstants.CTX_CAMPAGNE_LABEL));
        assertEquals("DEM2022X00",contextRootNode.get(ContextConstants.CTX_CAMPAGNE_ID).asText());

    }

}
