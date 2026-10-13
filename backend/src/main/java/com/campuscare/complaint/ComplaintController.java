package com.campuscare.complaint;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintRepository complaintRepository;

    public ComplaintController(ComplaintRepository complaintRepository) {
        this.complaintRepository = complaintRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createComplaint(@Valid @RequestBody ComplaintRequest request) {
        Complaint complaint = new Complaint(
                request.category().trim(),
                request.location().trim(),
                request.title().trim(),
                request.description().trim());
        complaintRepository.save(complaint);
        return ResponseEntity.status(201).body(Map.of("message", "Complaint submitted successfully"));
    }

    @GetMapping
    public List<Complaint> getComplaints() {
        return complaintRepository.findAll();
    }

    @GetMapping("/{id}")
    public Complaint getComplaint(@PathVariable Long id) {
        return findComplaint(id);
    }

    @PutMapping("/{id}/status")
    public Complaint updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        Complaint complaint = findComplaint(id);
        complaint.setStatus(request.status());
        return complaintRepository.save(complaint);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComplaint(@PathVariable Long id) {
        Complaint complaint = findComplaint(id);
        complaintRepository.delete(complaint);
        return ResponseEntity.noContent().build();
    }

    private Complaint findComplaint(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ComplaintNotFoundException(id));
    }
}
