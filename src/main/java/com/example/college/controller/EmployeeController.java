package com.example.college.controller;

import org.springframework.web.bind.annotation.RestController;

import com.example.college.model.EmployeeModel;
import com.example.college.repository.EmployeeRepo;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;


@RestController
@RequestMapping("/api")
public class EmployeeController {

    @Autowired
    private EmployeeRepo repo;
    
    @GetMapping("/employe")
    public List<EmployeeModel> getEmployees() {
        System.out.println("EP Triggred");
        return repo.findAll();
    }

    @GetMapping("/employe/{id}")
    public Optional<EmployeeModel> getEmployee(@PathVariable Long id) {
        return Optional.of(repo.findById(id).orElse(new EmployeeModel()));
    }
    
    @PostMapping("/employee")
    public void postMethodName(@RequestBody EmployeeModel employee) {
        repo.save(employee);
    }

    @PutMapping("/employee/{id}")
    public ResponseEntity<EmployeeModel> updateEmployee(
        @PathVariable Long id,
        @RequestBody EmployeeModel updatedEmployee) {

        return repo.findById(id)
                .map(existing -> {
                    existing.setName(updatedEmployee.getName());
                    existing.setRole(updatedEmployee.getRole());
                    existing.setAbsent(updatedEmployee.getAbsent());
                    // add more fields if needed

                    EmployeeModel saved = repo.save(existing);
                    return ResponseEntity.ok(saved);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
        
    @DeleteMapping("/employee/{id}")
    public void delete(@PathVariable Long id){
        repo.deleteById(id);
    }
}
