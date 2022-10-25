package com.protools.flowableDemo.controllers;

import com.protools.flowableDemo.services.EngineService.HistoryInfoService;
import com.protools.flowableDemo.services.Utils.RessourceUtils;
import io.swagger.v3.oas.annotations.Operation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flowable.bpmn.model.FlowElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
public class BpmnExportController {
    private Logger logger = LogManager.getLogger(BpmnExportController.class);

    @Autowired
    private RessourceUtils ressourceUtils;

    @CrossOrigin
    @Operation(summary = "Return BPMN file (String format) by processKey")
    @GetMapping(value = "/getBPMNFile/{processKey}", produces = MediaType.APPLICATION_XML_VALUE)
    public String getBPMNInfo(@PathVariable String processKey) throws IOException {
        logger.info("\t >> Getting BPMN file (String format) with processKey : "+processKey);
        return(ressourceUtils.getResourceFileAsString(processKey));
    }
}
