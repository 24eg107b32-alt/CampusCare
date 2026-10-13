package com.campuscare.complaint;

public class ComplaintNotFoundException extends RuntimeException {

    public ComplaintNotFoundException(Long id) {
        super("Complaint " + id + " was not found");
    }
}
