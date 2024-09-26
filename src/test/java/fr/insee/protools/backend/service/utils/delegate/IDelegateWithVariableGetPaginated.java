package fr.insee.protools.backend.service.utils.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.restclient.pagination.PageResponse;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public interface IDelegateWithVariableGetPaginated extends IDelegateWithVariables {

    ObjectMapper objectMapper = new ObjectMapper();

    void initReadValueMock(PageResponse pageResponse);
    String getOutListVariableName();


    @Override
    default void initExtraMocks(DelegateExecution execution) {
        IDelegateWithVariables.super.initExtraMocks(execution);
        PageResponse exepectedPageResponse = PageResponse.builder().currentPage(0).pageCount(1).content(List.of(objectMapper.createObjectNode().put("xx","yyy"))).build();
        initReadValueMock(exepectedPageResponse);
    }

    static Stream<Arguments> executeParamProvider() {
        return Stream.of(
                Arguments.of(null, true),
                Arguments.of(null, false),
                Arguments.of(0, true),
                Arguments.of(0, false),
                Arguments.of(1, true),
                Arguments.of(1, false),
                Arguments.of(99, true),
                Arguments.of(1011, false)
        );
    }

    @ParameterizedTest
    @MethodSource("executeParamProvider")
    default void execute_should_work_when_params_notNullWithPaginated(Integer currentPage,boolean isLastPage) {
        //Preconditions
        DelegateExecution execution = mock(DelegateExecution.class);
        lenient().when(execution.getProcessInstanceId()).thenReturn(dumyId);
        lenient().when(execution.getParent()).thenReturn(execution);

        initDefaultVariables(execution);

        //Pages data
        lenient().doReturn(currentPage).when(execution).getVariable(eq(VARNAME_INTERRO_LIST_PAGEABLE_CURRENT_PAGE), eq(Integer.class));
        lenient().doReturn(currentPage).when(execution).getVariableLocal(eq(VARNAME_INTERRO_LIST_PAGEABLE_CURRENT_PAGE), eq(Integer.class));

        lenient().doReturn(isLastPage).when(execution).getVariable(eq(VARNAME_INTERRO_LIST_PAGEABLE_IS_LAST_PAGE), eq(Boolean.class));
        lenient().doReturn(isLastPage).when(execution).getVariableLocal(eq(VARNAME_INTERRO_LIST_PAGEABLE_IS_LAST_PAGE), eq(Boolean.class));


        //PageResponse
        Integer expectedPageToRead=(currentPage==null)?0:currentPage+1;
        PageResponse exepectedPageResponse = PageResponse.builder().currentPage(expectedPageToRead).pageCount(1).content(List.of(objectMapper.createObjectNode().put("xx","yyy"))).build();
        initReadValueMock(exepectedPageResponse);
        //Call method under test
        getTaskUnderTest().execute(execution);


        if(!isLastPage) {
            // Verify the flowable utils are called to get and treat variables
            verify(execution, times(1)).getVariable(VARNAME_CURRENT_PARTITION_ID, String.class);

            ArgumentCaptor<Map<String, Object>> variablesMapCaptor = ArgumentCaptor.forClass(Map.class);


            verify(execution).setVariablesLocal(variablesMapCaptor.capture());
            Map<String, Object> capturedMap = variablesMapCaptor.getValue();
            assertEquals(exepectedPageResponse.getContent(), capturedMap.get(getOutListVariableName()));
        }
        else{
            //ArgumentCaptor<Map<String, Object>> variablesMapCaptor = ArgumentCaptor.forClass(Map.class);
            verify(execution,never()).setVariablesLocal(any());
        }
    }

}
