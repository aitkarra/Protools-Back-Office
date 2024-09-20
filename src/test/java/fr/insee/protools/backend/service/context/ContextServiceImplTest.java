package fr.insee.protools.backend.service.context;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.insee.protools.backend.dto.ContexteProcessus;
import fr.insee.protools.backend.service.context.exception.BadContextIOException;
import fr.insee.protools.backend.service.context.exception.BadContextNotJSONBPMNError;
import fr.insee.protools.backend.service.exception.ProcessDefinitionNotFoundException;
import fr.insee.protools.backend.service.exception.TaskNotFoundException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_CONTEXT;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FlowableTest
class ContextServiceImplTest {

    static final String ressourceFolder = ClassUtils.convertClassNameToResourcePath(ContextServiceImplTest.class.getPackageName());
    static final ObjectReader objectReader = new ObjectMapper().registerModule(new JavaTimeModule()).reader(); // maybe with configs
    
    @Spy
    @InjectMocks
    ContextServiceImpl contextService;
    //ne marche pas avec FlowbaleTest ; il faut @ExtendWith(SpringExtension.class)
    //    @Value("classpath:fr/insee/protools/backend/service/context/minimal_valid_context.json")
    //    private Resource minimalValidCtx;
    @Spy
    private RuntimeService runtimeService;
    @Spy
    private RepositoryService repositoryService = new RepositoryServiceImpl();
    @Mock
    private TaskService taskService;
    private String dummyId = "ID";

    static String[] businessKeyProvided() {
        return new String[]{"myBusinessKeyForTest", null, " ", ""};
    }

    private void initTaskServiceMock() {
        TaskEntityImpl task = new TaskEntityImpl();
        task.setProcessInstanceId(dummyId);
        TaskQueryImpl tq = mock(TaskQueryImpl.class);
        lenient().when(tq.taskId(any())).thenReturn(tq);
        lenient().when(tq.singleResult()).thenReturn(task);
        lenient().when(taskService.createTaskQuery()).thenReturn(tq);
    }

    private void initRuntimeServiceMock() {
        ProcessInstanceQuery piq = mock(ProcessInstanceQuery.class);
        ProcessInstance pi = new ExecutionEntityImpl();

        lenient().when(piq.processInstanceId(any())).thenReturn(piq);
        lenient().when(piq.singleResult()).thenReturn(pi);
        lenient().when(runtimeService.createProcessInstanceQuery()).thenReturn(piq);
    }

    @Test
    void processContextFileAndCompleteTask_should_throw_when_fileNotJson() {
        //Preconditions
        MockMultipartFile multipartFile = new MockMultipartFile("file.xlk", "file.xlk", "text/xml", "some json".getBytes());
        initTaskServiceMock();

        //Call method under test
        assertThrows(BadContextNotJSONBPMNError.class, () -> contextService.processContextFileAndCompleteTask(multipartFile, dummyId));
    }

    @Test
    void processContextFileAndCompleteTask_should_throw_when_taskBlank() {
        //Preconditions
        MockMultipartFile multipartFile = new MockMultipartFile("file.json", "file.json", "text/json", "some json".getBytes());
        initTaskServiceMock();

        //Call method under test
        assertThrows(TaskNotFoundException.class, () -> contextService.processContextFileAndCompleteTask(multipartFile, null));
        assertThrows(TaskNotFoundException.class, () -> contextService.processContextFileAndCompleteTask(multipartFile, ""));
        assertThrows(TaskNotFoundException.class, () -> contextService.processContextFileAndCompleteTask(multipartFile, " "));
        assertThrows(TaskNotFoundException.class, () -> contextService.processContextFileAndCompleteTask(multipartFile, "   "));
    }

    @Test
    void processContextFileAndCompleteTask_should_throw_when_taskNotFound() {
        //Preconditions
        MockMultipartFile multipartFile = new MockMultipartFile("file.json", "file.json", "text/json", "some json".getBytes());

        TaskQueryImpl tq = mock(TaskQueryImpl.class);
        when(tq.taskId(any())).thenReturn(tq);
        when(tq.singleResult()).thenReturn(null); //Return null task
        when(taskService.createTaskQuery()).thenReturn(tq);
        //Call method under test
        assertThrows(TaskNotFoundException.class, () -> contextService.processContextFileAndCompleteTask(multipartFile, dummyId));
    }

    @Test
    void processContextFileAndCompleteTask_should_throw_when_taskNoProcessInstanceId() {
        //Preconditions
        MockMultipartFile multipartFile = new MockMultipartFile("file.json", "file.json", "text/json", "some json".getBytes());

        TaskEntityImpl task = new TaskEntityImpl();
        task.setProcessInstanceId(null); //null process Instance ID
        TaskQueryImpl tq = mock(TaskQueryImpl.class);
        when(tq.taskId(any())).thenReturn(tq);
        when(tq.singleResult()).thenReturn(task);
        when(taskService.createTaskQuery()).thenReturn(tq);
        //Call method under test
        assertThrows(TaskNotFoundException.class, () -> contextService.processContextFileAndCompleteTask(multipartFile, dummyId));
    }

    @Test
    void processContextFileAndCompleteTask_should_throw_when_incorectJson() {
        //Preconditions
        MockMultipartFile multipartFile = new MockMultipartFile("file.json", "file.json", "text/json", "{toto}".getBytes());
        initTaskServiceMock();

        //Call method under test
        assertThrows(BadContextIOException.class, () -> contextService.processContextFileAndCompleteTask(multipartFile, dummyId));
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
        assertThrows(TaskNotFoundException.class, () -> contextService.processContextFileAndCompleteTask(multipartFile, dummyId));
    }

    @Test
    void processContextFileAndCompleteTask_should_work_when_ContextOk() throws IOException {
        //Preconditions
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(ressourceFolder + "/minimal_valid_context.json");
        MockMultipartFile multipartFile = new MockMultipartFile("file.json", "file.json", "text/json", resourceAsStream);
        initTaskServiceMock();
        //we skip context verification by returning no error
        doReturn(Set.of()).when(contextService).isContextOKForBPMN(any(), any());
        //Do not try to continue in a BPMN
        doNothing().when(taskService).complete(any(), anyMap());
        //Call method under test
        assertThatCode(() -> contextService.processContextFileAndCompleteTask(multipartFile, dummyId)).doesNotThrowAnyException();
    }

    @Test
    void getContextByProcessInstance_should_work_when_exists_and_AlreadyLoaded() throws IOException {
        //Preconditions
        initRuntimeServiceMock();
        processContextFileAndCompleteTask_should_work_when_ContextOk();
        //Call method under test
        JsonNode contextRootNode = contextService.getContextJsonNodeByProcessInstance(dummyId);

        //Post conditions : We've got a valid context object
        assertNotNull(contextRootNode);
        assertNotNull(contextRootNode.get("metadonnees"));
        assertNotNull(contextRootNode.get("labelCourt"));
        assertEquals("006aa47d-1574-424b-a67f-40705bec81f6", contextRootNode.get("id").asText());
    }

    @ParameterizedTest
    @MethodSource("businessKeyProvided")
    void processContextFileAndCreateProcessInstance_should_work_when_ContextOk(String businessKey) throws IOException {
        //Preconditions
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(ressourceFolder + "/minimal_valid_context.json");
        byte[] inputCtxAsByteArray = resourceAsStream.readAllBytes();
        MockMultipartFile multipartFile = new MockMultipartFile("file.json", "file.json", "text/json", inputCtxAsByteArray);

        String mockedPid = "CUSTOM_PID";
        ProcessInstance pi = mock(ExecutionEntityImpl.class);
        when(pi.getProcessInstanceId()).thenReturn(mockedPid);
        if (businessKey == null || businessKey.isBlank()) {
            when(runtimeService.startProcessInstanceByKey(any(), anyMap())).thenReturn(pi);
        } else {
            when(runtimeService.startProcessInstanceByKey(any(), any(), any())).thenReturn(pi);
        }
        initRuntimeServiceMock();
        //we skip context verification by returning no error
        doReturn(Set.of()).when(contextService).isContextOKForBPMN(any(), any());

        //Call method under test
        String processId = contextService.processContextFileAndCreateProcessInstance(multipartFile, "simpleProcess", businessKey);

        //Post conditions : We've got a valid Process Instance
        assertEquals(mockedPid, processId);
        //                  We've got a valid context object
        JsonNode contextRootNode = contextService.getContextJsonNodeByProcessInstance(mockedPid);
        //Post conditions : We've got a valid context object
        assertNotNull(contextRootNode);
        assertNotNull(contextRootNode.get("metadonnees"));
        assertNotNull(contextRootNode.get("labelCourt"));
        assertEquals("006aa47d-1574-424b-a67f-40705bec81f6", contextRootNode.get("id").asText());

        //We should have called the correct startProcessInstanceByKey
        ArgumentCaptor<Map<String, Object>> variablesMapCaptor = ArgumentCaptor.forClass(Map.class);
        if (businessKey == null || businessKey.isBlank()) {
            verify(runtimeService, times(1)).startProcessInstanceByKey(eq("simpleProcess"), variablesMapCaptor.capture());
        } else {
            verify(runtimeService, times(1)).startProcessInstanceByKey(eq("simpleProcess"), eq(businessKey), variablesMapCaptor.capture());
        }
        //and stored the correct context in process variables
        Map<String, Object> variableMap = variablesMapCaptor.getValue();
        String ctxAsString = (String) variableMap.get(VARNAME_CONTEXT);
        String inputAsString = new String(inputCtxAsByteArray, StandardCharsets.UTF_8);
        assertEquals(inputAsString, ctxAsString, "The stored contexte is incorrect");
    }


    @Test
    void processContextFileAndCreateProcessInstance_should_throw_when_processDefinitionId_is_null_or_blank() throws IOException {
        //Preconditions
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(ressourceFolder + "/minimal_valid_context.json");
        MockMultipartFile multipartFile = new MockMultipartFile("file.json", "file.json", "text/json", resourceAsStream.readAllBytes());

        //Call method under test
        assertThrows(ProcessDefinitionNotFoundException.class, () -> contextService.processContextFileAndCreateProcessInstance(multipartFile, "  ", dummyId));
        assertThrows(ProcessDefinitionNotFoundException.class, () -> contextService.processContextFileAndCreateProcessInstance(multipartFile, "", dummyId));
        assertThrows(ProcessDefinitionNotFoundException.class, () -> contextService.processContextFileAndCreateProcessInstance(multipartFile, null, dummyId));
    }

    @Test
    void processContextFileAndCreateProcessInstance_should_throw_when_processDefinitionId_is_not_Found() throws IOException {
        //Preconditions
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(ressourceFolder + "/minimal_valid_context.json");
        MockMultipartFile multipartFile = new MockMultipartFile("file.json", "file.json", "text/json", resourceAsStream.readAllBytes());
        initRuntimeServiceMock();
        doThrow(new FlowableObjectNotFoundException("msg")).when(runtimeService).startProcessInstanceByKey(any(),any(),anyMap());
        doReturn(new ContextServiceImpl.ContextPair("{}",null)).when(contextService).processContextFile(any(),any());
        //Call method under test
        assertThrows(ProcessDefinitionNotFoundException.class, () -> contextService.processContextFileAndCreateProcessInstance(multipartFile, "throwingId", dummyId));
    }

    @Test
    void processContextFile_should_throw_when_notJsonExtension(){
        //Prepare
        MockMultipartFile multipartFile1 = new MockMultipartFile("file.xlk", "file.xlk", "text/xml", "some content".getBytes());
        MockMultipartFile multipartFile2 = new MockMultipartFile("file.", "file.", "text/xml", "some content".getBytes());
        MockMultipartFile multipartFile3 = new MockMultipartFile("file", "file", "text/xml", "some content".getBytes());

        //Call method under test
        assertThrows(BadContextNotJSONBPMNError.class, () -> contextService.processContextFile(multipartFile1, dummyId));
        assertThrows(BadContextNotJSONBPMNError.class, () -> contextService.processContextFile(multipartFile2, dummyId));
        assertThrows(BadContextNotJSONBPMNError.class, () -> contextService.processContextFile(multipartFile3, dummyId));
    }

    @Test
    void jsonReadAndSchemaValidation_should_work_when_validJson() throws IOException {
        // Prepare
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(ressourceFolder + "/minimal_valid_context.json");
        JsonNode rootContext = objectReader.readTree(resourceAsStream);

        // Call method under test
        ContexteProcessus result = contextService.jsonReadAndSchemaValidation(rootContext);

        // Verify
        assertNotNull(result);
    }

    @Test
    void jsonReadAndSchemaValidation_should_throw_when_invalidJson() throws JsonProcessingException {
        // Prepare
        String jsonString = "{\"key\": \"value\"}"; // Replace with valid JSON
        JsonNode rootContext = objectReader.readTree(jsonString);

        // Act & Assert
        BadContextNotJSONBPMNError exception = assertThrows(
                BadContextNotJSONBPMNError.class,
                () -> contextService.jsonReadAndSchemaValidation(rootContext)
        );
        assertTrue(exception.getMessage().contains("Uploaded context is not correct according to the json-schema"));
    }
}
