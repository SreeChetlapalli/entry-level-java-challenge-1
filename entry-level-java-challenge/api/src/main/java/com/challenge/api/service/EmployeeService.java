package com.challenge.api.service;

import com.challenge.api.model.Employee;
import com.challenge.api.model.EmployeeRecord;
import com.challenge.api.web.CreateEmployeeRequest;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmployeeService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);
    private final Map<UUID, Employee> employees = new ConcurrentHashMap<>();

    public EmployeeService() {
        addSampleEmployee(
                "Avery",
                "Jordan",
                92000,
                34,
                "Product Manager",
                "avery.jordan@example.com",
                Instant.parse("2020-03-16T00:00:00Z"),
                null);
        addSampleEmployee(
                "Morgan",
                "Lee",
                68000,
                29,
                "Software Engineer",
                "morgan.lee@example.com",
                Instant.parse("2022-08-01T00:00:00Z"),
                null);
    }

    public List<Employee> getAllEmployees() {
        return employees.values().stream()
                .sorted(Comparator.comparing(Employee::getUuid))
                .toList();
    }

    public Employee getEmployeeByUuid(UUID uuid) {
        Employee employee = employees.get(uuid);
        if (employee == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
        }
        return employee;
    }

    public List<Employee> getActiveEmployees() {
        return getAllEmployees().stream()
                .filter(employee -> employee.getContractTerminationDate() == null)
                .toList();
    }

    public Employee createEmployee(CreateEmployeeRequest request) {
        validate(request);

        EmployeeRecord employee = new EmployeeRecord();
        employee.setUuid(UUID.randomUUID());
        employee.setFirstName(request.getFirstName().trim());
        employee.setLastName(request.getLastName().trim());
        employee.setFullName(employee.getFirstName() + " " + employee.getLastName());
        employee.setSalary(request.getSalary());
        employee.setAge(request.getAge());
        employee.setJobTitle(request.getJobTitle().trim());
        employee.setEmail(request.getEmail().trim());
        employee.setContractHireDate(request.getContractHireDate());
        employee.setContractTerminationDate(request.getContractTerminationDate());
        employees.put(employee.getUuid(), employee);
        logger.info("Created employee with id {}", employee.getUuid());
        return employee;
    }

    private void addSampleEmployee(
            String firstName,
            String lastName,
            int salary,
            int age,
            String jobTitle,
            String email,
            Instant hireDate,
            Instant terminationDate) {
        EmployeeRecord employee = new EmployeeRecord();
        employee.setUuid(UUID.randomUUID());
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setFullName(firstName + " " + lastName);
        employee.setSalary(salary);
        employee.setAge(age);
        employee.setJobTitle(jobTitle);
        employee.setEmail(email);
        employee.setContractHireDate(hireDate);
        employee.setContractTerminationDate(terminationDate);
        employees.put(employee.getUuid(), employee);
    }

    private void validate(CreateEmployeeRequest request) {
        if (request == null
                || isBlank(request.getFirstName())
                || isBlank(request.getLastName())
                || request.getSalary() == null
                || request.getAge() == null
                || isBlank(request.getJobTitle())
                || isBlank(request.getEmail())
                || request.getContractHireDate() == null) {
            throw new IllegalArgumentException("Required employee attributes must be provided");
        }
        if (request.getSalary() < 0 || request.getAge() <= 0) {
            throw new IllegalArgumentException("Salary must be non-negative and age must be positive");
        }
        if (!request.getEmail().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Email address is invalid");
        }
        if (request.getContractTerminationDate() != null
                && request.getContractTerminationDate().isBefore(request.getContractHireDate())) {
            throw new IllegalArgumentException("Contract termination date cannot precede hire date");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
