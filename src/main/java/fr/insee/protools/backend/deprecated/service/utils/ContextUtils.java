package fr.insee.protools.backend.deprecated.service.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;

import static fr.insee.protools.backend.service.context.ContextConstants.CTX_PARTITIONS;
import static fr.insee.protools.backend.service.context.ContextConstants.CTX_PARTITION_ID;

//TODO : à déplacer dans ContextService?? A supprimer intégralement
public class ContextUtils {

    //TODO: supprimer??
    //Search for the current partition in the contexte.
    public static JsonNode getCurrentPartitionNode(JsonNode contextRootNode, String currentPartitionId) {
        JsonNode currentPartitionNode=null;
        // Search for the correct partition based on it's ID
        for (JsonNode subNode : contextRootNode.path(CTX_PARTITIONS)) {
            if (subNode.has(CTX_PARTITION_ID) && subNode.get(CTX_PARTITION_ID).asText().equals(currentPartitionId)) {
                currentPartitionNode = subNode;
                break;
            }
        }
        if (currentPartitionNode == null) {
            throw new FlowableIllegalArgumentException(String.format("Partition id=[%s] not found in contexte", currentPartitionId));
        }
        return currentPartitionNode;
    }

    private ContextUtils(){}
}
