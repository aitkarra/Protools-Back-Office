package fr.insee.protools.backend;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okio.Buffer;
import okio.Okio;
import org.apache.commons.io.IOUtils;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utilitary class to load ressource files in String, POJO (using jackson) or JsonNode
 */
public class ProtoolsTestUtils {

    /**
     * Load the file in <filePath> in a String
     */
    public static String asString(String filePath) {
        try {
           return IOUtils.toString(ProtoolsTestUtils.class.getClassLoader().getResource(filePath), UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Load the file in <filePath> in a POJO using jackson
     */
    public static <T> T asObject(String filePath, Class<T> targetClass) {
        try {
            String s = asString(filePath);
            return  new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(s,targetClass);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Load the file in <filePath> in a JsonNode using jackson
     */
    public static JsonNode asJsonNode(String jsonRessourcePath) {
        try (InputStream is = ProtoolsTestUtils.class.getClassLoader().getResourceAsStream(jsonRessourcePath)){

            return  new ObjectMapper().readTree(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String contentType(String path) {
        if (path.endsWith(".png")) return MediaType.IMAGE_PNG_VALUE;
        if (path.endsWith(".jpg")) return  MediaType.IMAGE_JPEG_VALUE;
        if (path.endsWith(".jpeg")) return MediaType.IMAGE_JPEG_VALUE;
        if (path.endsWith(".gif")) return MediaType.IMAGE_GIF_VALUE;
        if (path.endsWith(".html")) return MediaType.TEXT_HTML_VALUE;
        if (path.endsWith(".txt")) return MediaType.TEXT_PLAIN_VALUE;
        if (path.endsWith(".json")) return MediaType.APPLICATION_JSON_VALUE;
        if (path.endsWith(".xml")) return MediaType.APPLICATION_XML_VALUE;
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
    public static  Buffer fileToBytes(File file) throws IOException {
        Buffer result = new Buffer();
        result.writeAll(Okio.source(file));
        return result;
    }

}
