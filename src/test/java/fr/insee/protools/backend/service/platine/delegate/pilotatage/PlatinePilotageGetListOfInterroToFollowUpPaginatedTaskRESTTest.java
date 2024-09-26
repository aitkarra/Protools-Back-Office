package fr.insee.protools.backend.service.platine.delegate.pilotatage;

import fr.insee.protools.backend.restclient.pagination.PageResponse;
import fr.insee.protools.backend.service.platine.service.PlatinePilotageServiceImpl;
import fr.insee.protools.backend.service.utils.delegate.IDelegateWithVariableGetPaginated;
import org.flowable.engine.delegate.JavaDelegate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_CURRENT_PARTITION_ID;
import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_REM_INTERRO_LIST;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlatinePilotageGetListOfInterroToFollowUpPaginatedTaskRESTTest
        implements IDelegateWithVariableGetPaginated {


    @Mock
    PlatinePilotageServiceImpl platinePilotageService;
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
    public Map<String, Class> getVariablesAndTypes() {
        return Map.of(
                VARNAME_CURRENT_PARTITION_ID, String.class
        );
    }

    @Override
    public String getOutListVariableName() {
        return VARNAME_REM_INTERRO_LIST;
    }

}