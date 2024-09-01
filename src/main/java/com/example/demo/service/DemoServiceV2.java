package com.example.demo.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DemoServiceV2 {

    public File mergeFiles(String filesFolderPath, String outputFilePath) {
        final Instant start = Instant.now();
        final Path uniqueOutputPath = getUniqueFilePath(outputFilePath);

        try (final Stream<Path> paths = Files.walk(Paths.get(filesFolderPath))) {
            final Set<String> filesToMerge = paths
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(string -> string.endsWith(".xlsx"))
                    .collect(Collectors.toSet());

            try (BufferedWriter writer = Files.newBufferedWriter(uniqueOutputPath, StandardOpenOption.CREATE)) {
                writeHeaders(writer);

                for (String file : filesToMerge) {
                    System.out.println("Reading file: " + file);
                    processFile(file, writer);
                }
            }
            System.out.println("Merged file created at: " + uniqueOutputPath);
        } catch (IOException e) {
            System.out.println("Exception while merging files...");
            e.printStackTrace();
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

    private void writeHeaders(BufferedWriter writer) throws IOException {
        writer.write("REGISTRATION NUMBER,CUSTOMER NAME,ENGINE NUMBER,CHASSIS NUMBER,APPROVER 1 NAME,APPROVER 1 CONTACT,APPROVER 2 NAME,APPROVER 2 CONTACT,APPROVER 3 NAME,APPROVER 3 CONTACT\n");
    }

    private void processFile(String filePath, BufferedWriter writer) {
        try (final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filePath));
             final XSSFWorkbook wb = new XSSFWorkbook(bis)) {
            final XSSFSheet sheet = wb.getSheetAt(0);
            final int rowCount = sheet.getPhysicalNumberOfRows();

            System.out.println("Total number of physical rows: " + rowCount);

            final Map<String, Integer> columnMap = getColumnMapping(sheet.getRow(0));

            for (final Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header row

                final List<String> line = new ArrayList<>(Collections.nCopies(4, "NA"));
                populateLine(row, columnMap, line);
                if (line.stream().allMatch(e -> e.equals("NA"))) continue; // Skip empty rows

                addApprovers(line);
                writer.write(String.join(",", line) + "\n");
            }
        } catch (IOException e) {
            System.out.println("Exception while processing file: " + filePath);
            e.printStackTrace();
        }
    }

    private void populateLine(Row row, Map<String, Integer> columnMap, List<String> line) {
        for (Cell cell : row) {
            addToCell(line, cell, columnMap.get("registration number"), 0);
            addToCell(line, cell, columnMap.get("customer name"), 1);
            addToCell(line, cell, columnMap.get("engine number"), 2);
            addToCell(line, cell, columnMap.get("chassis number"), 3);
        }
    }

    private void addToCell(List<String> line, Cell cell, Integer sheetIdx, Integer writeIdx) {
        if (sheetIdx != null && cell.getColumnIndex() == sheetIdx) {
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

    private Map<String, Integer> getColumnMapping(Row headerRow) {
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
