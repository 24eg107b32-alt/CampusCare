package com.campuscare.complaint;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String category;

    @Column(nullable = false, length = 160)
    private String location;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, length = 32)
    private String status;

    protected Complaint() {
    }

    public Complaint(String category, String location, String title, String description) {
        this.category = category;
        this.location = location;
        this.title = title;
        this.description = description;
    }

    @PrePersist
    void assignDefaultStatus() {
        if (status == null || status.isBlank()) {
            status = "Pending";
        }
    }

    public Long getId() { return id; }
    public String getCategory() { return category; }
    public String getLocation() { return location; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }
}
