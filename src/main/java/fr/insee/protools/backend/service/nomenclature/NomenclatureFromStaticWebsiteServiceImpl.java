package fr.insee.protools.backend.service.nomenclature;

import fr.insee.protools.backend.service.context.exception.BadContextIncorrectBPMNError;
import fr.insee.protools.backend.httpclients.webclient.WebClientHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * This service retrieves the nomenclatures from a static website based on a property uri
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NomenclatureFromStaticWebsiteServiceImpl implements NomenclatureService{

    //TODO : expose mandatory configuration?
    @Value("${fr.insee.protools.nomenclature.uri}")
    private String nomenclatureUri;

    private final WebClientHelper webClientHelper;

    @Override
    public String getNomenclatureContent(String nomenclatureId, String folderPath) {
        log.debug("Get Naming Model Value for nomenclatureId={}", nomenclatureId);
        String uri;
        String fullPath=nomenclatureUri+ "/" +folderPath+ "/"+nomenclatureId + ".json";
        try {
           uri = new URI(fullPath).normalize().toString();
        } catch (URISyntaxException e) {
            throw new BadContextIncorrectBPMNError(String.format("nomenclatureId=[%s] - folderPath=[%s] : fullPath=[%s] cannot be parsed: Error=[%s]"
            ,nomenclatureId,folderPath,fullPath, e.getMessage()));
        }
        return
                webClientHelper.getWebClientForFile()
                        .get()
                        .uri(uri)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
    }
}
