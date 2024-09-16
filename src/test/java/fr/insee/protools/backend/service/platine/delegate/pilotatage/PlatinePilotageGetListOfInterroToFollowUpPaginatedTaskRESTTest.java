package fr.insee.protools.backend.service.platine.delegate.pilotatage;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.restclient.pagination.PageResponse;
import fr.insee.protools.backend.service.platine.service.PlatinePilotageService;
import fr.insee.protools.backend.service.utils.delegate.IDelegateWithVariableGetPaginated;
import fr.insee.protools.backend.service.utils.delegate.PaginationHelper;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlatinePilotageGetListOfInterroToFollowUpPaginatedTaskRESTTest
        implements IDelegateWithVariableGetPaginated {


    @Mock
    PlatinePilotageService platinePilotageService;
    @InjectMocks
    PlatinePilotageGetListOfInterroToFollowUpPaginatedTaskREST task;

    @Override
    public JavaDelegate getTaskUnderTest() {
        return task;
    }

    @Override
    public void initReadValueMock(PageResponse expectedPageResponse) {
        lenient().doReturn(expectedPageResponse).when(platinePilotageService).getInterrogationToFollowUpPaginated(anyString(), anyLong(),any());
    }

    @Override
    public void initExtraMocks(DelegateExecution execution){
        lenient().doReturn(PageResponse.builder().currentPage(0).content(List.of()).pageSize(10).build()).when(platinePilotageService).getInterrogationToFollowUpPaginated(anyString(), anyLong(),any());
    }

    @Override
    public Map<String, Class> getVariablesAndTypes() {
        return Map.of(
                VARNAME_CURRENT_PARTITION_ID, String.class
        );
    }

    @Override
    public String getOutListVariableName() {
        return VARNAME_REM_INTERRO_LIST;
    }

    @Override
    public PaginationHelper.IGetFromService getServiceMock() {
        return spy(PaginationHelper.IGetFromService.class);
    }

    @Override
    public Object[] getServiceParams() {
        Object[] ret = new Object[1];
        ret[0] = "Default_partition_id";
        return ret;
    }
}