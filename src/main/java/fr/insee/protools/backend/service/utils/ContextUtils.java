package fr.insee.protools.backend.service.utils;

import com.fasterxml.jackson.databind.JsonNode;
import fr.insee.protools.backend.service.exception.IncoherentBPMNContextError;
import org.apache.commons.collections4.IteratorUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static fr.insee.protools.backend.service.context.ContextConstants.*;

//TODO : à déplacer dans ContextService??
public class ContextUtils {

    //TODO: supprimer??
    //Search for the current partition in the contexte.
    public static JsonNode getCurrentPartitionNode(JsonNode contextRootNode, Long currentPartitionId) {
        JsonNode currentPartitionNode=null;
        // Search for the correct partition based on it's ID
        for (JsonNode subNode : contextRootNode.path(CTX_PARTITIONS)) {
            if (subNode.has(CTX_PARTITION_ID) && subNode.get(CTX_PARTITION_ID).asLong()==currentPartitionId) {
                currentPartitionNode = subNode;
                break;
            }
        }
        if (currentPartitionNode == null) {
            throw new FlowableIllegalArgumentException(String.format("Partition id=[%d] not found in contexte", currentPartitionId));
        }
        return currentPartitionNode;
    }


    public static Optional<JsonNode> getPartitionNodeIfExists(JsonNode contextRootNode, String partitionId) {
        JsonNode currentPartitionNode=null;
        // Search for the correct partition based on it's ID
        for (JsonNode subNode : contextRootNode.path(CTX_PARTITIONS)) {
            if (subNode.has(CTX_PARTITION_ID) && subNode.get(CTX_PARTITION_ID).asText().equals(partitionId)) {
                currentPartitionNode = subNode;
                break;
            }
        }
        return Optional.ofNullable(currentPartitionNode);
    }

    public static Optional<JsonNode> getCommunicationFromPartition(JsonNode contextRootNode,String partitionId, String communicationId){
        return getPartitionNodeIfExists(contextRootNode,partitionId)
                .map(partitionNode -> getCommunicationFromPartition(partitionNode,communicationId))
                .orElse(Optional.empty());
    }

    public static Optional<JsonNode> getCommunicationFromPartition(JsonNode partitionNode, String communicationId){
        var communicationsIterator = partitionNode.path(CTX_PARTITION_COMMUNICATIONS).elements();
        return StreamSupport.stream(((Iterable<JsonNode>) () -> communicationsIterator).spliterator(), false)
                .filter(node -> node.path(CTX_PARTITION_COMMUNICATION_ID).asText().equals(communicationId))
                .findFirst();
    }


    public static List<JsonNode> getCommunicationsFromPartition(JsonNode contextRootNode, String partitionId) {
        Optional<JsonNode> partitionNode = getPartitionNodeIfExists(contextRootNode, partitionId);
        return partitionNode.map(
                        partNode -> {
                            var communicationIterator = partNode.path(CTX_PARTITION_COMMUNICATIONS).elements();
                            return IteratorUtils.toList(communicationIterator);
                        })
                .orElse(new ArrayList<>());
    }

    public static List<JsonNode> getCommunicationsFromPartition(JsonNode partitionNode) {
        return IteratorUtils.toList(partitionNode.path(CTX_PARTITION_COMMUNICATIONS).elements());
    }

    private ContextUtils(){}
}
