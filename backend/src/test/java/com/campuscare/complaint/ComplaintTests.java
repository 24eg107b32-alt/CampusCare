package com.campuscare.complaint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class ComplaintTests {

    @Test
    void newComplaintDefaultsToPendingBeforePersistence() {
        Complaint complaint = new Complaint("College", "Library", "Broken light", "The light is out");

        complaint.assignDefaultStatus();

        assertEquals("Pending", complaint.getStatus());
    }

    @Test
    void assigningDefaultStatusDoesNotReplaceExistingStatus() {
        Complaint complaint = new Complaint("Hostel", "Room 4", "Leaking tap", "Tap is leaking");
        complaint.setStatus("Resolved");

        complaint.assignDefaultStatus();

        assertEquals("Resolved", complaint.getStatus());
    }
}
