package com.campuscare.complaint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record StatusRequest(
        @NotBlank(message = "Status is required")
        @Pattern(regexp = "Pending|In Progress|Resolved", message = "Status must be Pending, In Progress, or Resolved")
        String status) {
}
