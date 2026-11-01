package com.challenge.api.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createEmployeeReturnsCreatedRecordAndItCanBeRetrieved() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validEmployeeRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.firstName").value("Taylor"))
                .andExpect(jsonPath("$.lastName").value("Ng"))
                .andExpect(jsonPath("$.fullName").value("Taylor Ng"))
                .andExpect(jsonPath("$.contractTerminationDate").value(nullValue()))
                .andReturn();

        JsonNode employee = objectMapper.readTree(result.getResponse().getContentAsString());
        String uuid = employee.get("uuid").asText();

        mockMvc.perform(get("/api/v1/employee/{uuid}", uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(uuid));
        mockMvc.perform(get("/api/v1/employee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].uuid", hasItem(uuid)));
    }

    @Test
    void getActiveEmployeesExcludesTerminatedEmployees() throws Exception {
        String activeUuid = createEmployee("active@example.com", null);
        String terminatedUuid = createEmployee("former@example.com", "2024-01-01T00:00:00Z");

        mockMvc.perform(get("/api/v1/employee/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].uuid", hasItem(activeUuid)))
                .andExpect(jsonPath("$[*].uuid", not(hasItem(terminatedUuid))));
    }

    @Test
    void missingEmployeeReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/employee/{uuid}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void malformedUuidReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/employee/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void missingRequiredCreateAttributesReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Taylor\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required employee attributes must be provided"));
    }

    @Test
    void invalidDatesAndMalformedJsonReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validEmployeeRequest()
                                .replace(
                                        "\"contractHireDate\": \"2021-06-01T00:00:00Z\"",
                                        "\"contractHireDate\": \"not-a-date\"")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidEmployeeValuesReturnBadRequest() throws Exception {
        String request = validEmployeeRequest().replace("\"salary\": 72000", "\"salary\": -1");

        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Salary must be non-negative and age must be positive"));
    }

    @Test
    void invalidEmailReturnsBadRequest() throws Exception {
        String request = validEmployeeRequest().replace("taylor.ng@example.com", "not-an-email");

        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email address is invalid"));
    }

    @Test
    void terminationBeforeHireReturnsBadRequest() throws Exception {
        String request = validEmployeeRequest()
                .replace(
                        "\"contractHireDate\": \"2021-06-01T00:00:00Z\"",
                        "\"contractHireDate\": \"2021-06-01T00:00:00Z\","
                                + "\n  \"contractTerminationDate\": \"2020-01-01T00:00:00Z\"");

        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Contract termination date cannot precede hire date"));
    }

    @Test
    void getAllEmployeesReturnsOkArray() throws Exception {
        mockMvc.perform(get("/api/v1/employee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].uuid").exists())
                .andExpect(jsonPath("$[0].fullName").exists());
    }

    private String createEmployee(String email, String terminationDate) throws Exception {
        String request = validEmployeeRequest().replace("taylor.ng@example.com", email);
        if (terminationDate != null) {
            request = request.replace(
                    "\"contractHireDate\": \"2021-06-01T00:00:00Z\"",
                    "\"contractHireDate\": \"2021-06-01T00:00:00Z\"," + "\n  \"contractTerminationDate\": \""
                            + terminationDate + "\"");
        }
        MvcResult result = mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode employee = objectMapper.readTree(result.getResponse().getContentAsString());
        return employee.get("uuid").asText();
    }

    private String validEmployeeRequest() {
        return """
                {
                  "firstName": "Taylor",
                  "lastName": "Ng",
                  "salary": 72000,
                  "age": 31,
                  "jobTitle": "Engineer",
                  "email": "taylor.ng@example.com",
                  "contractHireDate": "2021-06-01T00:00:00Z"
                }
                """;
    }
}
