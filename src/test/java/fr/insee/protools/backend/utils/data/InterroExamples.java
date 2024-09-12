package fr.insee.protools.backend.utils.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.ClassUtils;
import java.util.UUID;

public class InterroExamples {


    public record InterroWithId(String id, JsonNode interro){}

    public static InterroWithId generateEmptyInterro(){
        String uuid = UUID.randomUUID().toString();
        ObjectNode interro = JsonNodeFactory.instance.objectNode();
        interro.put("id", uuid);
        return new InterroWithId(uuid, interro);
    }
    public static final String interro1_empty =
            """
                {
                  "id": "b958cfac-2bf3-478d-a97a-dda5e751898c"
                }
            """;
    public static final String interro1_empty_id="b958cfac-2bf3-478d-a97a-dda5e751898c";

    private InterroExamples(){}
}
