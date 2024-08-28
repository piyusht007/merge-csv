package com.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Hello world!
 *
 */
public class App {
    private static final String firstApproverName = "Bablu";
    private static final String firstApproverContact = "7987059744";
    private static final String secondApproverName = "Krishna Tiwari";
    private static final String secondApproverContact = "9993654016";
    private static final String thirdApproverName = "Santosh Tiwari";
    private static final String thirdApproverContact = "9302465234";

    public static void main(String[] args) {
        try {
            final String basePath = args.length == 0 ? "C:\\Users\\hp\\Desktop\\JUN\\" : args[0].trim();
            final String outputFilePath = args.length > 1 ? args[1].trim() : "C:\\Users\\hp\\Desktop\\master.csv";

            // final String basePath = "C:\\Users\\hp\\Desktop\\JUN\\";
            // final String outputFilePath = "C:\\Users\\hp\\Desktop\\master.csv";

            final Set<String> filesToMerge = Files.walk(Paths.get(basePath)).filter(Files::isRegularFile)
                    .map(file -> file.toString()).collect(Collectors.toSet());

            filesToMerge.stream().map(file -> {
                System.out.println("Reading file: " + file);
                Map<String, Integer> readHeaderColumnIndex = readHeaderColumnIndex(file.toString());
                return readFile(readHeaderColumnIndex, file.toString());
            }).forEach(records -> {
                try {
                    Files.write(Paths.get(outputFilePath), records, StandardOpenOption.CREATE,
                            StandardOpenOption.APPEND);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            // Files.write(Paths.get(outputFilePath), finalRecords);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Integer> readHeaderColumnIndex(String filePath) {
        final Map<String, Integer> nameToIndex = new HashMap<>();

        try (final InputStream inp = new FileInputStream(filePath); final XSSFWorkbook wb = new XSSFWorkbook(inp);) {
            final XSSFSheet sheet = wb.getSheetAt(0);
            final Iterator<Row> itr = sheet.iterator(); // iterating over excel file
            final List<String> headers = new ArrayList<>();

            while (itr.hasNext()) {
                final Row row = itr.next();
                final Iterator<Cell> cellIterator = row.cellIterator(); // iterating over each column

                while (cellIterator.hasNext()) {
                    final Cell cell = cellIterator.next();
                    final CellType cellType = cell.getCellType();

                    if (cellType.equals(CellType.STRING)) {
                        final String cellValue = cell.getStringCellValue();

                        if (isValidColumnName(cellValue)) {
                            nameToIndex.put(cellValue.toUpperCase(), cell.getColumnIndex());
                        }
                    } else if (cellType.equals(CellType.NUMERIC)) {
                        headers.add(String.valueOf(cell.getNumericCellValue()));
                    } else {
                        System.out.println("Skipping....");
                    }
                }

                break;
            }

            return nameToIndex;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception...");
        }

        return null;
    }

    private static boolean isValidColumnName(String cellValue) {
        return cellValue.toUpperCase().equalsIgnoreCase("CUSTOMER NAME")
                || cellValue.toUpperCase().equalsIgnoreCase("ENGINE NUMBER")
                || cellValue.toUpperCase().equalsIgnoreCase("REGISTRATION NUMBERS")
                || cellValue.toUpperCase().equalsIgnoreCase("CHASSIS NUMBER");
    }

    private static Set<String> readFile(Map<String, Integer> headerColIdx, String filePath) {
        final Set<String> lines = new HashSet<>();

        try (final InputStream inp = new FileInputStream(filePath); final XSSFWorkbook wb = new XSSFWorkbook(inp);) {
            final XSSFSheet sheet = wb.getSheetAt(0);
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
                    CellType cellType = cell.getCellType();

                    Integer idx = headerColIdx.get("REGISTRATION NUMBERS");

                    addToCell(line, cell, cellType, idx, 0);

                    idx = headerColIdx.get("CUSTOMER NAME");

                    addToCell(line, cell, cellType, idx, 1);

                    idx = headerColIdx.get("ENGINE NUMBER");

                    addToCell(line, cell, cellType, idx, 2);

                    idx = headerColIdx.get("CHASSIS NUMBER");

                    addToCell(line, cell, cellType, idx, 3);

                    if (idx == null) {
                        continue;
                    }
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

    private static void addToCell(List<String> line, Cell cell, CellType cellType, Integer sheetIdx, Integer writeIdx) {
        if (sheetIdx != null && cell.getColumnIndex() == sheetIdx) {
            if (cellType.equals(CellType.STRING)) {
                String cellValue = cell.getStringCellValue();
                line.set(writeIdx, cellValue);
            } else if (cellType.equals(CellType.BLANK) || cellType.equals(CellType._NONE)) {
                line.set(writeIdx, "NA");
            } else if (cellType.equals(CellType.NUMERIC)) {
                line.set(writeIdx, String.valueOf(cell.getNumericCellValue()));
            } else {
                System.out.println("Skipping....");
            }
        }
    }

    private static void addApprovers(List<String> line) {
        line.add(firstApproverName);
        line.add(firstApproverContact);
        line.add(secondApproverName);
        line.add(secondApproverContact);
        line.add(thirdApproverName);
        line.add(thirdApproverContact);
    }
}
