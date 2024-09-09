package fr.insee.protools.backend.service.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.protools.backend.deprecated.service.utils.ContextUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContextUtilsTest {

    final String json0Partition =
            "{ \"partitions\": [ ]   }";
    final String json1Partition =
            "{ \"partitions\": [{ \"id\": 1  ,  \"toto\":  \"val1\"  }]   }";
    final String json2Partition =
            "{ \"partitions\": [{ \"id\": 1  ,  \"toto\": \"val1\" }, { \"id\": 2  ,  \"toto\": \"val2\" }]   }";

    @Test
    void getCurrentPartitionNode_ShouldReturnCorrectPartitionNode_WhenFound() throws JsonProcessingException {
        // Arrange
        JsonNode contextRootNode = new ObjectMapper().readTree(json1Partition);
        // Act
        JsonNode result = ContextUtils.getCurrentPartitionNode(contextRootNode, "1");
        // Assert
        assertEquals("val1",result.path("toto").asText());
    }

    @Test
    void getCurrentPartitionNode_ShouldReturnCorrectPartitionNode_WhenFound2() throws JsonProcessingException {
        // Arrange
        JsonNode contextRootNode = new ObjectMapper().readTree(json2Partition);
        // Act
        JsonNode result = ContextUtils.getCurrentPartitionNode(contextRootNode, "2");
        // Assert
        assertEquals("val2",result.path("toto").asText());
    }


    @Test
    void getCurrentPartitionNode_ShouldThrowFlowableIllegalArgumentException_WhenPartitionNotFound() throws JsonProcessingException {
        // Arrange
        JsonNode contextRootNode = new ObjectMapper().readTree(json0Partition);

        // Act
        assertThrows(FlowableIllegalArgumentException.class,
                () -> ContextUtils.getCurrentPartitionNode(contextRootNode, "55"));
    }

    @Test
    void getCurrentPartitionNode_ShouldThrowFlowableIllegalArgumentException_WhenPartitionNoPartition() throws JsonProcessingException {
        // Arrange
        JsonNode contextRootNode = new ObjectMapper().readTree("");

        // Act
        assertThrows(FlowableIllegalArgumentException.class,
                () -> ContextUtils.getCurrentPartitionNode(contextRootNode, "55"));
    }
}