package com.campuscare.complaint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ComplaintRequest(
        @NotBlank(message = "Category is required")
        @Pattern(regexp = "(?i)College|Hostel", message = "Category must be College or Hostel")
        String category,

        @NotBlank(message = "Location is required")
        @Size(max = 160, message = "Location must be 160 characters or fewer")
        String location,

        @NotBlank(message = "Title is required")
        @Size(max = 160, message = "Title must be 160 characters or fewer")
        String title,

        @NotBlank(message = "Description is required")
        @Size(max = 2000, message = "Description must be 2000 characters or fewer")
        String description) {
}
