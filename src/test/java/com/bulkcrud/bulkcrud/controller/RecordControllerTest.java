package com.bulkcrud.bulkcrud.controller;

import com.bulkcrud.bulkcrud.entity.Record;
import com.bulkcrud.bulkcrud.service.RecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecordController.class)
class RecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecordService recordService;

    @Autowired
    private ObjectMapper objectMapper;

    private Record record1;
    private Record record2;

    @BeforeEach
    void setUp() {
        record1 = new Record("Muskan Goswami", "muskan2@gmail.com", 25);
        record1.setId(1L);

        record2 = new Record("Goswami Muskan", "goswami1@gmail.com", 30);
        record2.setId(2L);
    }

    @Test
    void givenRecords_whenGetAllRecords_thenReturnsOkAndRecords() throws Exception {
        when(recordService.getAll()).thenReturn(Arrays.asList(record1, record2));

        mockMvc.perform(get("/records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2));
    }

    @Test
    void givenRecords_whenAddRecordsBulk_thenReturnsSavedRecords() throws Exception {
        Map<String, Object> mockResponse = new LinkedHashMap<>();
        mockResponse.put("savedCount", 2);
        mockResponse.put("invalidCount", 0);
        mockResponse.put("savedRecords", Arrays.asList(record1, record2));
        mockResponse.put("invalidRecords", List.of());

        when(recordService.saveAll(anyList())).thenReturn(mockResponse);

        mockMvc.perform(post("/records/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Arrays.asList(record1, record2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedRecords.length()").value(2))
                .andExpect(jsonPath("$.invalidRecords.length()").value(0))
                .andExpect(jsonPath("$.savedCount").value(2))
                .andExpect(jsonPath("$.invalidCount").value(0));
    }

    @Test
    void givenEmptyRecords_whenAddRecordsBulk_thenReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/records/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Record list cannot be null or empty"));
    }
}
