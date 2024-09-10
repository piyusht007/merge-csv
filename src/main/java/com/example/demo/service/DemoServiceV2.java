package com.example.demo.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DemoServiceV2 {

    public File mergeFiles(String filesFolderPath,
                           String outputFilePath,
                           List<String> additionalHeaders) {
        final Instant start = Instant.now();
        Path uniqueOutputPath = getUniqueFilePath(outputFilePath);
        int fileCounter = 1;
        int totalRowsWritten = 0;
        BufferedWriter writer = null;

        try (final Stream<Path> paths = Files.walk(Paths.get(filesFolderPath))) {
            final Set<String> filesToMerge = paths
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(string -> string.endsWith(".xlsx"))
                    .collect(Collectors.toSet());

            writer = Files.newBufferedWriter(uniqueOutputPath, StandardOpenOption.CREATE);
            writeHeaders(writer, additionalHeaders);

            for (String file : filesToMerge) {
                System.out.println("Reading file: " + file);
                totalRowsWritten += processFile(file, writer, additionalHeaders);

                if (totalRowsWritten >= 1_000_000) {
                    System.out.println("Writing to new file as total rows written for the current file is: " + totalRowsWritten);
                    writer.close(); // Close current writer
                    String outputFilePath1 = outputFilePath.replaceFirst("(\\.[^.]*)?$", "_" + fileCounter + "$1");
                    uniqueOutputPath = getUniqueFilePath(outputFilePath1);

                    writer = Files.newBufferedWriter(uniqueOutputPath, StandardOpenOption.CREATE);
                    writeHeaders(writer, additionalHeaders);
                    fileCounter++;
                    totalRowsWritten = 0;
                }
            }

            System.out.println("Total rows written: " + totalRowsWritten);
            System.out.println("Merged file created at: " + uniqueOutputPath);
        } catch (IOException e) {
            System.out.println("Exception while merging files...");
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.out.println("Exception while closing the writer...");
                    e.printStackTrace();
                }
            }
        }

        final Instant end = Instant.now();
        System.out.println("Time taken: " + (end.toEpochMilli() - start.toEpochMilli()) + " ms");
        return new File(uniqueOutputPath.toString());
    }

    private static Path getUniqueFilePath(String outputFilePath) {
        Path path = Paths.get(outputFilePath);
        int counter = 1;

        while (Files.exists(path)) {
            final String newFileName = outputFilePath.replaceFirst("(\\.[^.]*)?$", "_" + counter + "$1");
            path = Paths.get(newFileName);
            counter++;
        }

        return path;
    }

    private void writeHeaders(BufferedWriter writer, List<String> additionalHeaders) throws IOException {
        String headers = Constants.MANDATORY_COLUMNS.stream().map(String::toUpperCase).collect(Collectors.joining(",")) + ",";
        String approverHeaders = "APPROVER 1 NAME,APPROVER 1 CONTACT,APPROVER 2 NAME,APPROVER 2 CONTACT,APPROVER 3 NAME,APPROVER 3 CONTACT, SOURCE FILE\n";
        String finalHeaders = additionalHeaders.isEmpty()
                ? headers + approverHeaders
                : headers + String.join(",", additionalHeaders.stream().map(String::toUpperCase).toList()) + "," + approverHeaders;
        writer.write(finalHeaders);
    }

    private int processFile(String filePath, BufferedWriter writer, List<String> additionalHeaders) {
        int rowsWritten = 0;

        try (final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filePath));
             final XSSFWorkbook wb = new XSSFWorkbook(bis)) {
            final XSSFSheet sheet = wb.getSheetAt(0);
            final int rowCount = sheet.getPhysicalNumberOfRows();

            System.out.println("Total number of physical rows: " + rowCount);

            final Map<String, Integer> columnMap = getColumnMapping(sheet.getRow(0), additionalHeaders);

            for (final Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header row

                int totalColumns = additionalHeaders.isEmpty()
                        ? Constants.NUM_FIXED_HEADERS
                        : Constants.NUM_FIXED_HEADERS + additionalHeaders.size();
                final List<String> line = new ArrayList<>(Collections.nCopies(totalColumns, "NA"));
                populateLine(row, columnMap, line, additionalHeaders);
                if (line.stream().allMatch(e -> e.equals("NA"))) continue; // Skip empty rows

                addApprovers(line);
                line.add(Paths.get(filePath).getFileName().getFileName().toString()); // Add source file name
                writer.write(String.join(",", line) + "\n");
                rowsWritten++;
            }
        } catch (IOException e) {
            System.out.println("Exception while processing file: " + filePath);
            e.printStackTrace();
        }

        return rowsWritten;
    }

    private void populateLine(Row row, Map<String, Integer> columnMap, List<String> line, List<String> additionalHeaders) {
        final List<String> columnNames = new ArrayList<>(Constants.MANDATORY_COLUMNS);

        if (!additionalHeaders.isEmpty()) {
            columnNames.addAll(additionalHeaders);
        }

        for (int i = 0; i < columnNames.size(); i++) {
            final Integer columnIdx = columnMap.get(columnNames.get(i));

            if (columnIdx != null && columnIdx != -1) {
                final Cell cell = row.getCell(columnIdx);
                addToCell(line, cell, columnIdx, i);
            }
        }

//        for (Cell cell : row) {
//            addToCell(line, cell, columnMap.get("registration number"), 0);
//            addToCell(line, cell, columnMap.get("customer name"), 1);
//            addToCell(line, cell, columnMap.get("engine number"), 2);
//            addToCell(line, cell, columnMap.get("chassis number"), 3);
//            addToCell(line, cell, columnMap.get("position"), 4);
//        }
    }

    private void addToCell(List<String> line, Cell cell, Integer sheetIdx, Integer writeIdx) {
        if (sheetIdx != null && cell != null && cell.getColumnIndex() == sheetIdx) {
            final CellType cellType = cell.getCellType();

            switch (cellType) {
                case STRING:
                    line.set(writeIdx, cell.getStringCellValue());
                    break;
                case NUMERIC:
                    line.set(writeIdx, String.valueOf(cell.getNumericCellValue()));
                    break;
                case BLANK:
                case _NONE:
                    line.set(writeIdx, "NA");
                    break;
                default:
                    System.out.println("Unsupported cell type, Skipping....");
            }
        }
    }

    private void addApprovers(List<String> line) {
        for (String[] approver : Constants.APPROVERS) {
            line.add(approver[0]); // approver name
            line.add(approver[1]); // approver contact
        }
    }

    private Map<String, Integer> getColumnMapping(Row headerRow, List<String> additionalHeaders) {
        if (headerRow == null) return Collections.emptyMap();

        Map<String, Integer> headerMap = new HashMap<>();
        for (Cell cell : headerRow) {
            headerMap.put(cell.getStringCellValue().trim().toLowerCase(), cell.getColumnIndex());
        }

        Map<String, Integer> columnMap = new HashMap<>();
        columnMap.put("customer name", findExactMatch(headerMap, Constants.CUSTOMER_NAME_VARIANTS));
        columnMap.put("engine number", findExactMatch(headerMap, Constants.ENGINE_NUMBER_VARIANTS));
        columnMap.put("registration number", findExactMatch(headerMap, Constants.REGISTRATION_NUMBER_VARIANTS));
        columnMap.put("chassis number", findExactMatch(headerMap, Constants.CHASSIS_NUMBER_VARIANTS));
        columnMap.put("position", findExactMatch(headerMap, Constants.POSITION_VARIANTS));
        columnMap.put("loan number", findExactMatch(headerMap, Constants.LOAN_VARIANTS));
        columnMap.put("model", findExactMatch(headerMap, Constants.MODEL_VARIANTS));
        columnMap.put("make", findExactMatch(headerMap, Constants.MAKE_VARIANTS));

        if (!additionalHeaders.isEmpty()) {
            for (String header : additionalHeaders) {
                columnMap.put(header, findExactMatch(headerMap, Collections.singletonList(header)));
            }
        } else {
            System.out.println("Additional headers not provided...");
        }

        return columnMap;
    }

    private String normalize(String input) {
        return input.trim().toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
    }

    private int findExactMatch(Map<String, Integer> headerMap, List<String> variants) {
        Map<String, Integer> normalizedHeaderMap = headerMap.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> normalize(entry.getKey()),
                        Map.Entry::getValue
                ));

        for (String variant : variants) {
            String normalizedVariant = normalize(variant);
            if (normalizedHeaderMap.containsKey(normalizedVariant)) {
                return normalizedHeaderMap.get(normalizedVariant);
            }
        }

        return -1;
    }
}
