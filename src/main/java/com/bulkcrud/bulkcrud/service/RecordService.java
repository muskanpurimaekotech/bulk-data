package com.bulkcrud.bulkcrud.service;

import com.bulkcrud.bulkcrud.entity.Record;
import com.bulkcrud.bulkcrud.helper.ExcelHelper;
import com.bulkcrud.bulkcrud.repository.RecordRepository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecordService {

    private final RecordRepository recordRepository;
    private final Validator validator;

    public RecordService(RecordRepository recordRepository, Validator validator) {
        this.recordRepository = recordRepository;
        this.validator = validator;
    }

    // JSON Bulk
    public Map<String, Object> saveAll(List<Record> records) {
        return processBulk(records);
    }

    // Excel Bulk
    public Map<String, Object> saveExcelData(InputStream is) {
        List<Record> records = ExcelHelper.excelToRecords(is);
        return processBulk(records);
    }

    // ------------------ BULK PROCESSING (Reusable) ------------------
    private Map<String, Object> processBulk(List<Record> records) {

        if (records == null || records.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Record list cannot be null or empty");
        }

        List<Map<String, Object>> invalid = new ArrayList<>();
        List<Record> valid = new ArrayList<>();
        Set<String> seenInRequest = new HashSet<>();

        // Remove records with null/blank email first
        List<Record> cleanedRecords = records.stream()
                .filter(r -> r.getEmail() != null && !r.getEmail().isBlank())
                .toList();

        if (cleanedRecords.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All emails are null or blank");
        }

        // Extract distinct emails for DB check
        List<String> requestEmails = cleanedRecords.stream()
                .map(Record::getEmail)
                .distinct()
                .toList();

        // Fetch existing records from DB in one query
        List<Record> existingDB = recordRepository.findAllByEmailIn(requestEmails);
        Set<String> existingEmails = existingDB.stream()
                .map(Record::getEmail)
                .collect(Collectors.toSet());

        for (Record r : records) {

            // 1) Validate entity fields (including required email)
            Set<ConstraintViolation<Record>> violations = validator.validate(r);
            if (!violations.isEmpty()) {
                invalid.add(errorResponse(r, violations));
                continue; // move to next record
            }

            // 2) Check duplicate in request
            if (!seenInRequest.add(r.getEmail())) {
                invalid.add(errorSimple(r, "Duplicate email in request"));
                continue;
            }

            // 3) Check duplicate in DB
            if (existingEmails.contains(r.getEmail())) {
                invalid.add(errorSimple(r, "Email already exists in database"));
                continue;
            }

            valid.add(r);
        }


        // Save valid records
        List<Record> saved = recordRepository.saveAll(valid);

        // Prepare response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("savedCount", saved.size());
        response.put("invalidCount", invalid.size());
        response.put("savedRecords", saved);
        response.put("invalidRecords", invalid);

        return response;
    }


    private Map<String, Object> errorResponse(Record r, Set<ConstraintViolation<Record>> violations) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("record", r);

        error.put("errors", violations.stream()
                .map(ConstraintViolation::getMessage)
                .toList());

        return error;
    }

    private Map<String, Object> errorSimple(Record r, String msg) {
        return Map.of(
                "record", r,
                "errors", List.of(msg)
        );
    }

    // ------------------ NORMAL CRUD ------------------

    public List<Record> getAll() {
        return recordRepository.findAll();
    }

    public Page<Record> getRecords(int page, int size) {
        return recordRepository.findAll(PageRequest.of(page, size));
    }

    public void deleteById(Long id) {
        if (!recordRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record with id " + id + " not found");
        }
        recordRepository.deleteById(id);
    }

    public Record update(Long id, Record updatedRecord) {

        // 1️⃣ Fetch existing record by ID
        Record existingRecord = recordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Record with id " + id + " not found"));

        // 2️⃣ Validate updated record fields
        Set<ConstraintViolation<Record>> violations = validator.validate(updatedRecord);
        if (!violations.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    violations.iterator().next().getMessage()
            );
        }

        // 3️⃣ Check email existence in DB
        recordRepository.findByEmail(updatedRecord.getEmail())
            .ifPresent(existing -> {

                // ✅ Case: Same ID and same data
            	if (existing.getId().equals(id) &&
            		    existing.getName().equals(updatedRecord.getName()) &&
            		    existing.getAge() == updatedRecord.getAge()) {  // <- use ==
            		    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            		            "Record already exists with same data");
            		}


                // ✅ Case: Same email in different ID
                if (!existing.getId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Email already exists: " + updatedRecord.getEmail());
                }
            });

        // 4️⃣ Update record fields
        existingRecord.setName(updatedRecord.getName());
        existingRecord.setEmail(updatedRecord.getEmail());
        existingRecord.setAge(updatedRecord.getAge());

        // 5️⃣ Save updated record
        return recordRepository.save(existingRecord);
    }

}
