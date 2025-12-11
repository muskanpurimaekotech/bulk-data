package com.bulkcrud.bulkcrud.helper;

import com.bulkcrud.bulkcrud.entity.Record;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExcelHelper {

    public static boolean isExcelFile(String type) {
        return type.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    public static List<Record> excelToRecords(InputStream is) {
        try {
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);

            Iterator<Row> rows = sheet.iterator();
            List<Record> records = new ArrayList<>();

            int rowNumber = 0;

            while (rows.hasNext()) {
                Row currentRow = rows.next();

                if (rowNumber == 0) {
                    rowNumber++;
                    continue; // Skip header
                }

                Record record = new Record();

                // Name
                Cell nameCell = currentRow.getCell(0);
                record.setName(nameCell != null ? nameCell.getStringCellValue().trim() : null);

                // Email (null-safe)
                Cell emailCell = currentRow.getCell(1);
                record.setEmail(emailCell != null ? emailCell.getStringCellValue().trim() : null);

                // Age
                Cell ageCell = currentRow.getCell(2);
                record.setAge(ageCell != null ? (int) ageCell.getNumericCellValue() : null);

                records.add(record);
            }

            workbook.close();
            return records;

        } catch (Exception e) {
            throw new RuntimeException("Excel पढ़ने में समस्या: " + e.getMessage());
        }
    }

}
