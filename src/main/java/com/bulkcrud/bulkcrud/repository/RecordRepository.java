package com.bulkcrud.bulkcrud.repository;


import com.bulkcrud.bulkcrud.entity.Record;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordRepository extends JpaRepository<Record, Long> {
	Optional<Record> findByEmail(String email);
	List<Record> findAllByEmailIn(List<String> emails);
}

