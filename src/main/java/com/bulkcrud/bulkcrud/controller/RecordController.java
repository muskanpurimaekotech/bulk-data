package com.bulkcrud.bulkcrud.controller;

import com.bulkcrud.bulkcrud.entity.Record;
import com.bulkcrud.bulkcrud.service.RecordService;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/records")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @PostMapping("/bulk")
    public ResponseEntity<?> addRecords(@RequestBody  List<Record> records) {

        Map<String, Object> result = recordService.saveAll(records);

        return ResponseEntity.ok(Map.of(
                "message", "Bulk upload processed",
                "savedCount", result.get("savedCount"),
                "invalidCount", result.get("invalidCount"),
                "savedRecords", result.get("savedRecords"),
                "invalidRecords", result.get("invalidRecords")
        ));
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is missing"));
        }

        try {
            Map<String, Object> result = recordService.saveExcelData(file.getInputStream());

            return ResponseEntity.ok(Map.of(
                    "message", "Upload processed successfully",
                    "savedCount", result.get("savedCount"),
                    "invalidCount", result.get("invalidCount"),
                    "savedRecords", result.get("savedRecords"),
                    "invalidRecords", result.get("invalidRecords")
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping
    public ResponseEntity<List<Record>> getAllRecords() {
        return ResponseEntity.ok(recordService.getAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRecord(@PathVariable Long id) {
        recordService.deleteById(id);

        return ResponseEntity.ok(Map.of(
                "message", "Record with id " + id + " deleted successfully"
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRecord(
            @PathVariable Long id,
            @RequestBody @Valid Record record) {

        Record updated = recordService.update(id, record);

        return ResponseEntity.ok(Map.of(
                "message", "Record updated successfully",
                "updatedRecord", updated
        ));
    }

    
    @GetMapping("/paginated")
    public ResponseEntity<?> getRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {

        Page<Record> recordsPage = recordService.getRecords(page, size);

        return ResponseEntity.ok(Map.of(
                "records", recordsPage.getContent(),
                "totalPages", recordsPage.getTotalPages(),
                "totalRecords", recordsPage.getTotalElements()
        ));
    }

}
