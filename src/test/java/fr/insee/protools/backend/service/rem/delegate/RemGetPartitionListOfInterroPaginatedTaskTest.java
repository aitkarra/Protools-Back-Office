package fr.insee.protools.backend.service.rem.delegate;

import fr.insee.protools.backend.restclient.pagination.PageResponse;
import fr.insee.protools.backend.service.platine.delegate.pilotatage.PlatinePilotageGetListOfInterroToFollowUpPaginatedTaskREST;
import fr.insee.protools.backend.service.platine.service.PlatinePilotageService;
import fr.insee.protools.backend.service.rem.RemService;
import fr.insee.protools.backend.service.utils.delegate.IDelegateWithVariableGetPaginated;
import fr.insee.protools.backend.service.utils.delegate.PaginationHelper;
import org.flowable.engine.delegate.JavaDelegate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_CURRENT_PARTITION_ID;
import static fr.insee.protools.backend.service.FlowableVariableNameConstants.VARNAME_REM_INTERRO_LIST;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class RemGetPartitionListOfInterroPaginatedTaskTest         implements IDelegateWithVariableGetPaginated {

    @Mock
    RemService remService;
    @InjectMocks
    RemGetPartitionListOfInterroPaginatedTask task;

    @Override
    public JavaDelegate getTaskUnderTest() {
        return task;
    }

    @Override
    public Map<String, Class> getVariablesAndTypes() {
        return Map.of(
                VARNAME_CURRENT_PARTITION_ID, String.class
        );
    }

    @Override
    public void initReadValueMock(PageResponse expectedPageResponse) {
        lenient().doReturn(expectedPageResponse).when(remService).getPartitionAllInterroPaginated(anyString(), anyLong());
    }

    @Override
    public String getOutListVariableName() {
        return VARNAME_REM_INTERRO_LIST;
    }
}