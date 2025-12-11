package com.bulkcrud.bulkcrud.service;

import com.bulkcrud.bulkcrud.entity.Record;
import com.bulkcrud.bulkcrud.repository.RecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecordServiceTest {

    @InjectMocks
    private RecordService recordService;

    @Mock
    private RecordRepository recordRepository;

    private Record record1;
    private Record record2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        record1 = new Record("Muskan Goswami", "muskan2@gmail.com", 25);
        record1.setId(1L);

        record2 = new Record("Goswami Muskan", "goswami1@gmail.com", 30);
        record2.setId(2L);
    }

    @Test
    void givenValidRecords_whenSaveAll_thenRecordsSavedSuccessfully() {
        // Arrange
        when(recordRepository.findAllByEmailIn(anyList())).thenReturn(Collections.emptyList());
        when(recordRepository.saveAll(anyList())).thenReturn(Arrays.asList(record1, record2));

        // Act
        Map<String, Object> result = recordService.saveAll(Arrays.asList(record1, record2));

        // Assert
        assertEquals(2, result.get("savedCount"));
        assertEquals(0, result.get("invalidCount"));
        assertEquals(Arrays.asList(record1, record2), result.get("savedRecords"));
        assertEquals(Collections.emptyList(), result.get("invalidRecords"));

        verify(recordRepository, times(1)).saveAll(anyList());
    }

    @Test
    void givenDuplicateRecord_whenSaveAll_thenInvalidRecordsReturned() {
        // Arrange
        when(recordRepository.findAllByEmailIn(anyList())).thenReturn(Arrays.asList(record1));

        // Act
        Map<String, Object> result = recordService.saveAll(Arrays.asList(record1));

        // Assert
        assertEquals(0, result.get("savedCount"));
        assertEquals(1, result.get("invalidCount"));
        List<?> invalidRecords = (List<?>) result.get("invalidRecords");
        assertTrue(invalidRecords.size() == 1);
    }

    @Test
    void givenUpdatedRecord_whenUpdate_thenRecordUpdatedSuccessfully() {
        // Arrange
        Record updatedRecord = new Record("Muskan Updated", "muskan2@gmail.com", 26);

        when(recordRepository.findById(record1.getId())).thenReturn(Optional.of(record1));
        when(recordRepository.findByEmail(updatedRecord.getEmail())).thenReturn(Optional.of(record1));
        when(recordRepository.save(any(Record.class))).thenReturn(updatedRecord);

        // Act
        Record result = recordService.update(record1.getId(), updatedRecord);

        // Assert
        assertEquals("Muskan Updated", result.getName());
        assertEquals(26, result.getAge());
        assertEquals("muskan2@gmail.com", result.getEmail());
    }

    @Test
    void givenDuplicateEmail_whenUpdate_thenThrowsException() {
        // Arrange
        Record anotherRecord = new Record("Someone Else", "someone@example.com", 35);
        anotherRecord.setId(3L);

        Record updatedRecord = new Record("Muskan Updated", "someone@example.com", 26);

        when(recordRepository.findById(record1.getId())).thenReturn(Optional.of(record1));
        when(recordRepository.findByEmail(updatedRecord.getEmail())).thenReturn(Optional.of(anotherRecord));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> recordService.update(record1.getId(), updatedRecord));

        assertEquals("Email already exists: someone@example.com", exception.getReason());
    }

    @Test
    void givenValidId_whenDeleteById_thenRecordDeletedSuccessfully() {
        doNothing().when(recordRepository).deleteById(record1.getId());
        recordService.deleteById(record1.getId());
        verify(recordRepository, times(1)).deleteById(record1.getId());
    }
}
