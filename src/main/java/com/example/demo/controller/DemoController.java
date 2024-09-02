package com.example.demo.controller;

import com.example.demo.service.DemoService;
import com.example.demo.service.DemoServiceV2;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;

@RestController
public class DemoController {

    private final DemoServiceV2 demoServiceV2;

    public DemoController(DemoServiceV2 demoServiceV2) {
        this.demoServiceV2 = demoServiceV2;
    }

    @GetMapping("/merge")
    public ResponseEntity<InputStreamResource> mergeFiles(
            @Parameter(name = "filesFolderPath",
                    description = "The folder path containing the files to merge",
                    schema = @Schema(example = "C:\\Users\\hp\\Desktop\\JUN\\"))
            @RequestParam final String filesFolderPath,
            @Parameter(name = "outputFilePath",
                    description = "The path of the merged output file",
                    schema = @Schema(example = "C:\\Users\\hp\\Desktop\\master.csv"))
            @RequestParam final String outputFilePath,
            @Parameter(name = "additionalHeaders",
                    description = "List of additional headers to be written in the output file",
                    schema = @Schema(example = "loan number, insurer, policy number"))
            @RequestParam(required = false) final String additionalHeaders) throws Exception {
        System.out.println("filesFolderPath: " + filesFolderPath);
        System.out.println("outputFilePath: " + outputFilePath);
        System.out.println("additionalHeaders: " + additionalHeaders);

        final File file = demoServiceV2.mergeFiles(filesFolderPath,
                outputFilePath,
                additionalHeaders == null ? Collections.emptyList() : List.of(additionalHeaders.split(",")));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=" + file.getName())
                .body(new InputStreamResource(new FileInputStream(file)));
    }
}
