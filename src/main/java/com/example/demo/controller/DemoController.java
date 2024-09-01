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

@RestController
public class DemoController {

    private final DemoService demoService;
    private final DemoServiceV2 demoServiceV2;

    public DemoController(DemoService demoService,
                          DemoServiceV2 demoServiceV2) {
        this.demoService = demoService;
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
            @RequestParam final String outputFilePath) throws Exception {
        System.out.println("filesFolderPath: " + filesFolderPath);
        System.out.println("outputFilePath: " + outputFilePath);
        final File file = demoServiceV2.mergeFiles(filesFolderPath, outputFilePath);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=" + file.getName())
                .body(new InputStreamResource(new FileInputStream(file)));
    }
}
