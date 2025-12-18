package com.example.college.controller;

import com.example.college.model.EmployeeModel;
import com.example.college.repository.EmployeeRepo;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class EmployeeController {

    private final EmployeeRepo repo;

    @Autowired
    public EmployeeController(EmployeeRepo repo) {
        this.repo = repo;
    }

    // ----------------------------
    // GET /api/employe  -> list all employees
    // ----------------------------
    @GetMapping("/employe")
    public ResponseEntity<List<EmployeeModel>> getEmployees() {
        List<EmployeeModel> list = repo.findAll();
        return ResponseEntity.ok(list);
    }

    // ----------------------------
    // GET /api/employe/{id}  -> get single employee by id
    // ----------------------------
    @GetMapping("/employe/{id}")
    public ResponseEntity<?> getEmployee(@PathVariable Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "id is required"));
        }

        Optional<EmployeeModel> opt = repo.findById(id);
        if (opt.isPresent()) {
            return ResponseEntity.ok(opt.get());
        } else {
            return ResponseEntity.status(404).body(Map.of("message", "employee not found in db"));
        }
    }

    // ----------------------------
    // POST /api/employee  -> create employee
    // ----------------------------
    @PostMapping("/employee")
    public ResponseEntity<?> createEmployee(@RequestBody EmployeeModel employee) {
        // Basic validation: require name and role (adjust according to your model)
        if (employee == null
                || employee.getName() == null || employee.getName().strip().isEmpty()
                || employee.getRole() == null || employee.getRole().strip().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "name and role are required to enter"));
        }

        EmployeeModel saved = repo.save(employee);

        // Build Location URI for created resource: /api/employe/{id}
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/employe/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(saved);
    }

    // ----------------------------
    // PUT /api/employee/{id}  -> update employee
    // ----------------------------
    @PutMapping("/employee/{id}")
public ResponseEntity<?> updateEmployee(
        @PathVariable Long id,
        @RequestBody EmployeeModel updatedEmployee) {

    if (id == null) {
        return ResponseEntity.badRequest().body(Map.of("message", "id is required"));
    }

    if (updatedEmployee == null) {
        return ResponseEntity.badRequest().body(Map.of("message", "request body is required"));
    }

    Optional<EmployeeModel> opt = repo.findById(id);
    if (opt.isEmpty()) {
        return ResponseEntity.status(404).body(Map.of("message", "employee not found"));
    }

    EmployeeModel existing = opt.get();
    if (updatedEmployee.getName() != null) existing.setName(updatedEmployee.getName());
    if (updatedEmployee.getRole() != null) existing.setRole(updatedEmployee.getRole());
    if (updatedEmployee.getAbsent() != null) existing.setAbsent(updatedEmployee.getAbsent());
    // add more fields if needed

    EmployeeModel saved = repo.save(existing);
    return ResponseEntity.ok(saved);
}


    // ----------------------------
    // DELETE /api/employee/{id}  -> delete employee
    // ----------------------------
    @DeleteMapping("/employee/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "id is required"));
        }

        boolean exists = repo.existsById(id);
        if (!exists) {
            return ResponseEntity.status(404).body(Map.of("message", "employee not found"));
        }

        repo.deleteById(id);
        return ResponseEntity.noContent().build(); // 204
    }
}
