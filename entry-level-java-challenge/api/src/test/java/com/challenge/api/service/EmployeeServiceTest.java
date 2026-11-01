package com.challenge.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.challenge.api.model.Employee;
import com.challenge.api.web.CreateEmployeeRequest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class EmployeeServiceTest {

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeService();
    }

    @Test
    void createEmployeeTrimsFieldsAndGeneratesIdentity() {
        Employee employee = employeeService.createEmployee(validRequest("  Ada  ", "  Lovelace  "));

        assertNotNull(employee.getUuid());
        assertEquals("Ada", employee.getFirstName());
        assertEquals("Lovelace", employee.getLastName());
        assertEquals("Ada Lovelace", employee.getFullName());
        assertEquals("Engineer", employee.getJobTitle());
        assertEquals("ada@example.com", employee.getEmail());
        assertTrue(employeeService.getAllEmployees().stream()
                .anyMatch(stored -> stored.getUuid().equals(employee.getUuid())));
    }

    @Test
    void missingEmployeeThrowsNotFound() {
        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> employeeService.getEmployeeByUuid(UUID.randomUUID()));
        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void blankNameIsRejected() {
        CreateEmployeeRequest request = validRequest(" ", "Ng");
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> employeeService.createEmployee(request));
        assertEquals("Required employee attributes must be provided", exception.getMessage());
    }

    private CreateEmployeeRequest validRequest(String firstName, String lastName) {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setSalary(72000);
        request.setAge(31);
        request.setJobTitle(" Engineer ");
        request.setEmail(" ada@example.com ");
        request.setContractHireDate(Instant.parse("2021-06-01T00:00:00Z"));
        return request;
    }
}
