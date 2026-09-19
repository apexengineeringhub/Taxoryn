package com.taxoryn.module.marketing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEarlyAccessRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Firm / Practice name is required")
    @Size(max = 200, message = "Firm name must not exceed 200 characters")
    private String practiceName;

    @Size(max = 30, message = "Phone number must not exceed 30 characters")
    private String phone;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "Practice profile must not exceed 100 characters")
    private String practiceProfile;

    @Size(max = 100, message = "Primary area of interest must not exceed 100 characters")
    private String primaryArea;

    @Size(max = 100, message = "Source must not exceed 100 characters")
    private String source;

    /**
     * Invisible anti-spam honeypot field. Bots fill it; legitimate human forms leave it empty.
     */
    private String honeypot;
}
