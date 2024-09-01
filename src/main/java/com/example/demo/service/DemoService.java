package com.example.demo.service;

import org.apache.commons.text.similarity.FuzzyScore;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
public class DemoService {
    private static final List<String> CUSTOMER_NAME_VARIANTS = Arrays.asList("customer name", "cust name", "client name", "name");
    private static final List<String> ENGINE_NUMBER_VARIANTS = Arrays.asList("engine number", "eng no", "engine no", "engine", "eng");
    private static final List<String> REGISTRATION_NUMBER_VARIANTS = Arrays.asList("registration number", "reg no", "reg num", "reg", "registration");
    private static final List<String> CHASSIS_NUMBER_VARIANTS = Arrays.asList("chassis number", "chassis", "chass no", "chass", "chassis no");
    private static final Set<String[]> APPROVERS = new HashSet<>(Arrays.asList(
            new String[]{"Bablu", "7987059744"},
            new String[]{"Krishna Tiwari", "9993654016"},
            new String[]{"Santosh Tiwari", "9302465234"}
    ));

    public File mergeFiles(String filesFolderPath, String outputFilePath) {
        final Path uniqueOutputPath = getUniqueFilePath(outputFilePath);

        try (final Stream<Path> paths = Files.walk(Paths.get(filesFolderPath));) {
            final Set<String> filesToMerge = paths
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(string -> string.endsWith(".xlsx"))
                    .collect(Collectors.toSet());

            final boolean[] headersWritten = {false};

            filesToMerge.parallelStream().map(file -> {
                System.out.println("Reading file: " + file);
                return readFile(file);
            }).filter(Objects::nonNull).forEach(records -> {
                try {
                    writeHeadersIfApplicable(headersWritten, uniqueOutputPath);
                    Files.write(uniqueOutputPath, records, StandardOpenOption.CREATE,
                            StandardOpenOption.APPEND);
                } catch (IOException e) {
                    System.out.println("Exception while writing to file: " + uniqueOutputPath);
                    e.printStackTrace();
                }
            });
            System.out.println("Merged file created at: " + uniqueOutputPath);
        } catch (Exception e) {
            System.out.println("Exception while merging files...");
            e.printStackTrace();
        }

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

    private void writeHeadersIfApplicable(boolean[] headersWritten, Path path) throws IOException {
        if (!headersWritten[0]) {
            Files.write(path, ("REGISTRATION NUMBER," +
                            "CUSTOMER NAME," +
                            "ENGINE NUMBER," +
                            "CHASSIS NUMBER," +
                            "APPROVER 1 NAME," +
                            "APPROVER 1 CONTACT," +
                            "APPROVER 2 NAME," +
                            "APPROVER 2 CONTACT," +
                            "APPROVER 3 NAME," +
                            "APPROVER 3 CONTACT\n").getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
            headersWritten[0] = true;
        }
    }

    private Map<String, Integer> getColumnMapping(Row headerRow) {
        if (headerRow == null) return Collections.emptyMap();

        Map<String, Integer> headerMap = new HashMap<>();
        for (Cell cell : headerRow) {
            headerMap.put(cell.getStringCellValue().trim().toLowerCase(), cell.getColumnIndex());
        }

        Map<String, Integer> columnMap = new HashMap<>();
        columnMap.put("customer name", findBestMatch(headerMap, CUSTOMER_NAME_VARIANTS));
        columnMap.put("engine number", findBestMatch(headerMap, ENGINE_NUMBER_VARIANTS));
        columnMap.put("registration number", findBestMatch(headerMap, REGISTRATION_NUMBER_VARIANTS));
        columnMap.put("chassis number", findBestMatch(headerMap, CHASSIS_NUMBER_VARIANTS));

        return columnMap;
    }

    private int findBestMatch(Map<String, Integer> headerMap, List<String> variants) {
        FuzzyScore fuzzyScore = new FuzzyScore(Locale.ENGLISH);
        Set<String> headers = headerMap.keySet();
        return variants.stream()
                .map(variant -> headers.stream()
                        .map(header -> new AbstractMap.SimpleEntry<>(header, fuzzyScore.fuzzyScore(variant, header)))
                        .max(Map.Entry.comparingByValue())
                        .orElse(new AbstractMap.SimpleEntry<>("", 0)))
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .map(headerMap::get)
                .orElse(-1);
    }

    private Set<String> readFile(String filePath) {
        final Set<String> lines = new HashSet<>();

        try (final InputStream inp = new FileInputStream(filePath);
             final XSSFWorkbook wb = new XSSFWorkbook(inp)) {
            final XSSFSheet sheet = wb.getSheetAt(0);
            final Map<String, Integer> columnMap = getColumnMapping(sheet.getRow(0));
            final Iterator<Row> itr = sheet.iterator(); // iterating over excel file

            while (itr.hasNext()) {
                final List<String> line = new ArrayList<>(15);
                line.add("NA");
                line.add("NA");
                line.add("NA");
                line.add("NA");

                final Row row = itr.next();

                if (row.getRowNum() == 0) {
                    continue;
                }

                final Iterator<Cell> cellIterator = row.cellIterator(); // iterating over each column

                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();

                    addToCell(line, cell, columnMap.get("registration number"), 0);
                    addToCell(line, cell, columnMap.get("customer name"), 1);
                    addToCell(line, cell, columnMap.get("engine number"), 2);
                    addToCell(line, cell, columnMap.get("chassis number"), 3);
                }

                if (line.stream().allMatch(e -> e.equals("NA"))) {
                    continue;
                }

                // Fill columns that are not available using "NA"
                final int naToBeFilled = 4 - line.size();
                IntStream.range(0, naToBeFilled).forEach(i -> line.add("NA"));
                addApprovers(line);
                lines.add(String.join(",", line));
            }

            return lines;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception...");
        }

        return null;
    }

    private void addToCell(List<String> line, Cell cell, Integer sheetIdx, Integer writeIdx) {
        if (sheetIdx != null && cell.getColumnIndex() == sheetIdx) {
            final CellType cellType = cell.getCellType();

            switch (cellType) {
                case STRING:
                    String cellValue = cell.getStringCellValue();
                    line.set(writeIdx, cellValue);
                    break;
                case BLANK:
                case _NONE:
                    line.set(writeIdx, "NA");
                    break;
                case NUMERIC:
                    line.set(writeIdx, String.valueOf(cell.getNumericCellValue()));
                    break;
                default:
                    System.out.println("Unsupported cell type, Skipping....");
            }
        }
    }

    private void addApprovers(List<String> line) {
        for (String[] approver : APPROVERS) {
            line.add(approver[0]); // approver name
            line.add(approver[1]); // approver contact
        }
    }
}
