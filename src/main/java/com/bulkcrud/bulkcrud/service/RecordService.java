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

    public Map<String, Object> saveAll(List<Record> records) {
        return processBulk(records);
    }

    public Map<String, Object> saveExcelData(InputStream is) {
        List<Record> records = ExcelHelper.excelToRecords(is);
        return processBulk(records);
    }

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

        List<String> requestEmails = cleanedRecords.stream()
                .map(Record::getEmail)
                .distinct()
                .toList();

        List<Record> existingDB = recordRepository.findAllByEmailIn(requestEmails);
        Set<String> existingEmails = existingDB.stream()
                .map(Record::getEmail)
                .collect(Collectors.toSet());

        for (Record r : records) {

            Set<ConstraintViolation<Record>> violations = validator.validate(r);
            if (!violations.isEmpty()) {
                invalid.add(errorResponse(r, violations));
                continue; 
            }

            if (!seenInRequest.add(r.getEmail())) {
                invalid.add(errorSimple(r, "Duplicate email in request"));
                continue;
            }

            if (existingEmails.contains(r.getEmail())) {
                invalid.add(errorSimple(r, "Email already exists in database"));
                continue;
            }

            valid.add(r);
        }


        List<Record> saved = recordRepository.saveAll(valid);

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

        Record existingRecord = recordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Record with id " + id + " not found"));

        Set<ConstraintViolation<Record>> violations = validator.validate(updatedRecord);
        if (!violations.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    violations.iterator().next().getMessage()
            );
        }

        recordRepository.findByEmail(updatedRecord.getEmail())
            .ifPresent(existing -> {

            	if (existing.getId().equals(id) &&
            		    existing.getName().equals(updatedRecord.getName()) &&
            		    existing.getAge() == updatedRecord.getAge()) {  // <- use ==
            		    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            		            "Record already exists with same data");
            		}


                if (!existing.getId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Email already exists: " + updatedRecord.getEmail());
                }
            });

        existingRecord.setName(updatedRecord.getName());
        existingRecord.setEmail(updatedRecord.getEmail());
        existingRecord.setAge(updatedRecord.getAge());

        return recordRepository.save(existingRecord);
    }

}
