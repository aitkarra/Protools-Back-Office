package fr.insee.protools.backend.service.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.security.oauth2.jwt.Jwt;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public class CustomJWTHelper {

    private static final String SECRET_KEY = "my-secret-key";
    private static final String ALGO = "HmacSHA256";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private CustomJWTHelper() {
    }

    public static String getEncodedToken(List<String> roles) throws Exception {

        ObjectNode realmAccessNode = new ObjectMapper().createObjectNode();
        ArrayNode rolesNode = new ObjectMapper().createArrayNode();
        for (String role:roles){
            rolesNode.add(role);
        }
        realmAccessNode.put("roles", rolesNode);

        Jwt kcToken = Jwt.withTokenValue("token").header("alg", ALGO).
                claim("realm_access", realmAccessNode)
                .build();
        return getEncodedToken(kcToken);
    }

    public static String getEncodedToken(Jwt token) throws Exception {

        byte[] a = objectMapper.writeValueAsString(token.getHeaders()).getBytes(StandardCharsets.UTF_8);
        byte[] b = objectMapper.writeValueAsString(token.getClaims()).getBytes(StandardCharsets.UTF_8);

        String encodedHeader = Base64.getEncoder().encodeToString(a);
        String encodedPayload = Base64.getEncoder().encodeToString(b);

        // Create the signature
        String signature = createSignature(encodedHeader, encodedPayload);

        // Concatenate header, payload, and signature to form the JWT
        String signedToken = encodedHeader + "." + encodedPayload + "." + signature;
        return signedToken;

    }

    private static String createSignature(String header, String payload) throws Exception {
        String data = header + "." + payload;
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGO);
        sha256Hmac.init(secretKeySpec);

        byte[] signatureBytes = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().encodeToString(signatureBytes);
    }
}
