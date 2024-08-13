package fr.insee.protools.backend.httpclients.webclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.insee.protools.backend.httpclients.keycloak.KeycloakService;
import fr.insee.protools.backend.httpclients.configuration.APIProperties;
import fr.insee.protools.backend.httpclients.configuration.ApiConfigProperties;
import fr.insee.protools.backend.httpclients.exception.ApiNotConfiguredBPMNError;
import fr.insee.protools.backend.httpclients.exception.KeycloakTokenConfigBPMNError;
import fr.insee.protools.backend.httpclients.exception.runtime.HttpClient4xxBPMNError;
import fr.insee.protools.backend.httpclients.exception.runtime.HttpClient5xxBPMNError;
import fr.insee.protools.backend.httpclients.exception.runtime.HttpClientNetworkExceptionBPMNError;
import fr.insee.protools.backend.httpclients.exception.runtime.HttpClientRequestExceptionBPMNError;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.logging.LogLevel;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.PrematureCloseException;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import java.lang.reflect.Field;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.UnresolvedAddressException;
import java.util.*;

/**
 * Helper class for WebClient
 */
@Component
@Slf4j
public class WebClientHelper {

        private static final int DEFAULT_FILE_BUFFER_SIZE = 100 * 1024*1024;
        private static final int DEFAULT_API_BUFFER_SIZE =  100 * 1024*1024;
        private final KeycloakService keycloakService;
        private final ApiConfigProperties apiConfigProperties;

        //TODO : a mutualiser entre les deux clients
        private final EnumMap<ApiConfigProperties.KNOWN_API, WebClient> initializedClients = new EnumMap<>(ApiConfigProperties.KNOWN_API.class);

        public WebClientHelper(KeycloakService keycloakService, ApiConfigProperties apiConfigProperties) {
                this.keycloakService = keycloakService;
                this.apiConfigProperties = apiConfigProperties;
        }

        //I cannot have a single builder and store it in a private variable because every call to .filter(...) append a new filter to the builder
        private WebClient.Builder getBuilder() {
                return WebClient.builder()
                        .defaultStatusHandler(HttpStatusCode::isError, clientResponse ->
                                {
                                        String initialRequestDescription = extractClientResponseRequestDescriptionPrivateFiledUsingReflexion(clientResponse);
                                        Mono<RuntimeException> result;
                                        String errorMsg = String.format("request=[%s] - statusCode=[%s]",
                                                initialRequestDescription,clientResponse.statusCode());
                                        if (clientResponse.statusCode() == HttpStatus.UNAUTHORIZED) {
                                                //Keycloak error?
                                                errorMsg = "HttpStatus.UNAUTHORIZED. WWW-Authenticate=[" + String.join("", clientResponse.headers().header("WWW-Authenticate") + "]");
                                        }

                                        String finalErrorMsg = errorMsg;
                                        result = clientResponse.bodyToMono(String.class).defaultIfEmpty("No error message provided by API")
                                                .flatMap(error -> {
                                                        if (clientResponse.statusCode().is4xxClientError()) {
                                                                return Mono.error(new HttpClient4xxBPMNError(finalErrorMsg + " - " + error, clientResponse.statusCode()));
                                                        } else {
                                                                return Mono.error(new HttpClient5xxBPMNError(finalErrorMsg + " - " + error));
                                                        }
                                                });
                                        return result;
                                }
                        )
                        .filter((request, next) ->
                                next.exchange(request)
                                        .onErrorResume(WebClientRequestException.class, ex -> {
                                                //TODO : here i can access the method and URI ; could be usefull to log ??
                                                if(containsCauseOfType(ex,
                                                        List.of(SocketException.class,UnresolvedAddressException.class,PrematureCloseException.class,
                                                                ConnectTimeoutException.class, UnknownHostException.class))){
                                                        return Mono.error(new HttpClientNetworkExceptionBPMNError(ex));
                                                }
                                                return Mono.error(new HttpClientRequestExceptionBPMNError(ex));
                                        })
                        )
                        //To allow up to 20Mb
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(DEFAULT_API_BUFFER_SIZE))
                        .clientConnector(
                                new ReactorClientHttpConnector(
                                        HttpClient.create()
                                                //Handles a proxy conf passed on system properties
                                                .proxyWithSystemProperties()
                                                //enable logging of request/responses
                                                //configurable in properties as if it was this class logers
                                                .wiretap(this.getClass().getCanonicalName(), LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL)));
        }

        public static void logJson(String msg, Object dto, Logger logger, Level level) {
                if (logger.isEnabledForLevel(level)) {
                        try {
                                String json = new ObjectMapper().writeValueAsString(dto);
                                String logLine = msg +" - " + json;
                                switch (level) {
                                        case TRACE -> logger.trace(logLine);
                                        case DEBUG -> logger.debug(logLine);
                                        case INFO -> logger.info(logLine);
                                        case WARN -> logger.warn(logLine);
                                        case ERROR -> logger.error(logLine);
                                        default -> logger.trace(logLine);
                                }
                        } catch (JsonProcessingException e) {
                                log.error("Could not parse json");
                        }
                }
        }

        /**
         * init a new WebClient proxy aware (default one ignore system proxy)
         */
        public WebClient getWebClient() {
                return getBuilder()
                        .build();
        }

        /**
         * init a new WebClient proxy aware (default one ignore system proxy)
         * with increased buffer size to 20Mo
         *
         * @return a Webclient
         */
        public WebClient getWebClientForFile() {
                return getWebClientForFile(DEFAULT_FILE_BUFFER_SIZE);
        }

        public static int getDefaultFileBufferSize()  { return DEFAULT_FILE_BUFFER_SIZE; }

        /**
         * init a new WebClient proxy aware (default one ignore system proxy)
         * with increased buffer size to <byteCount>
         *
         * @return a Webclient
         */
        public WebClient getWebClientForFile(int byteCount) {
                return getBuilder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(byteCount))
                        .build();
        }

        /**
         * Get a webclient preconfigured for proxy and able to get the JWT token required for authentification
         *
         * @param api the client will connect to this api
         * @return preconfigured WebClient for the api
         */
        public WebClient getWebClient(ApiConfigProperties.KNOWN_API api) {
                APIProperties apiProperties = apiConfigProperties.getAPIProperties(api);
                if (apiProperties == null) {
                        throw new ApiNotConfiguredBPMNError(String.format("API %s is not configured in properties", api));
                } else if (Boolean.FALSE.equals(apiProperties.getEnabled())) {
                        throw new ApiNotConfiguredBPMNError(String.format("API %s is disabled in properties", api));
                }
                return initializedClients.computeIfAbsent(api,
                        knownApi ->
                                getBuilder()
                                        .filter(new KeycloakHeadersFilter(keycloakService, apiProperties.getAuth()))
                                        .baseUrl(apiProperties.getUrl())
                                        .build());
        }


        public Map<String, String> getTokenDetailsByAPI(){
                Map<String, String> result = new HashMap<>();
                for (var api :ApiConfigProperties.KNOWN_API.values() ) {
                        try {
                                APIProperties apiProperties = apiConfigProperties.getAPIProperties(api);
                                if (apiProperties == null) {
                                        throw new ApiNotConfiguredBPMNError(String.format("API %s is not configured in properties", api));
                                } else if (Boolean.FALSE.equals(apiProperties.getEnabled())) {
                                        throw new ApiNotConfiguredBPMNError(String.format("API %s is disabled in properties", api));
                                }
                                var token = keycloakService.getToken(apiProperties.getAuth());
                                if(token !=null && !token.isBlank()) {
                                        String details = analyseToken(token);
                                        result.put(api.name(),details);

                                }
                        } catch (KeycloakTokenConfigBPMNError | ApiNotConfiguredBPMNError e) {
                                result.put(api.name(),e.getMessage());
                        }
                        catch (Exception e){
                                result.put(api.name(),"Internal error with token");
                        }
                }
                return result;
        }

        /**
         * @return A json with the configuration of the APIs handled by protools
         */
        public JsonNode getAPIConfigDetails(){
                ObjectMapper objectMapper = new ObjectMapper();
                ArrayNode rootNode = objectMapper.createArrayNode();
                for (var api :ApiConfigProperties.KNOWN_API.values() ) {
                        APIProperties apiProperties = apiConfigProperties.getAPIProperties(api);
                        ObjectNode apiNode = objectMapper.valueToTree(apiProperties);
                        apiNode.put("name",api.name());
                        rootNode.add(apiNode);
                }
                return rootNode;
        }

        //analyse a single token to retrieve roles
        private static String analyseToken(String token) {
                String result;
                String[] chunks = token.split("\\.");
                if(chunks.length<2){
                        return "Token size is incorrect. It should contain at least one dot";
                }
                Base64.Decoder decoder = Base64.getUrlDecoder();
                String payload = new String(decoder.decode(chunks[1]));
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                        JsonNode tokenPayloadNode = objectMapper.readTree(payload);
                        String roles = tokenPayloadNode.path("realm_access").path("roles").toString();
                        if(roles==null || roles.isBlank()){
                                result="No Role found in token";
                        }
                        else{
                                result= roles;
                        }
                } catch (JsonProcessingException e) {
                        result=payload;
                }
                return result;
        }

        /**
         * Hack function to access the requestDescription private filed
         * will only work if clientResponse is an instance od DefaultClientResponse which has this field
         * this hack is mandatory to know the original call while handling an error
         * @param clientResponse the object to treat.
         * @return the original call made or an error message if it is not possible
         */
        private static String extractClientResponseRequestDescriptionPrivateFiledUsingReflexion(ClientResponse clientResponse){
                try {
                        Field privateField = clientResponse.getClass().getDeclaredField("requestDescription");
                        // Set the accessibility as true
                        privateField.setAccessible(true);
                        // Store the value of private field in variable
                        return (String)privateField.get(clientResponse);
                } catch (Exception e) {
                        log.error("Internal error while trying to extract the requestDescription from ClientResponse");
                        return "Initial request cannot be retrieved";
                }
        }

        /**
         * special version of NestedRuntimeException.contains accepting a list of types :
         * Check whether the Ex exception contains an exception of the given list of types:
         * either it is of the given class itself or it contains a nested cause of one of these types.
         * @param ex : The exception
         * @param exTypes : The searched types
         * @return true if a matching exception type has been found
         */
        @SuppressWarnings("java:S3776")
        protected static boolean containsCauseOfType(Exception ex, @Nullable List<Class<?>> exTypes) {
                if (exTypes == null || exTypes.isEmpty()) {
                        return false;
                } else {
                        if (exTypes.stream().anyMatch(exType -> exType.isInstance(ex))) {
                                return true;
                        }

                        //Current exception is not of the configured types ; check parents
                        Throwable cause = ex.getCause();
                        if (cause == ex) {
                                return false;
                        }  else {
                                while (cause != null) {
                                        Throwable finalCause = cause; //to be used in lambda
                                        if (exTypes.stream().anyMatch(exType -> exType.isInstance(finalCause))) {
                                                return true;
                                        }

                                        if (cause.getCause() == cause) {
                                                break;
                                        }

                                        cause = cause.getCause();
                                }

                                return false;
                        }
                }
        }
}
