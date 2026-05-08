package com.va.v.v_app.social.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for updating user profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 200, message = "Display name cannot exceed 200 characters")
    private String displayName;

    @Size(max = 50, message = "Username cannot exceed 50 characters")
    private String username;

    @Size(max = 100, message = "Nickname cannot exceed 100 characters")
    private String nickname;

    @Size(max = 160, message = "Bio cannot exceed 160 characters")
    private String bio;

    private String about; // Detailed description

    @Size(max = 500, message = "Profile picture URL cannot exceed 500 characters")
    private String profilePictureUrl;

    @Size(max = 500, message = "Cover photo URL cannot exceed 500 characters")
    private String coverPhotoUrl;

    @Size(max = 200, message = "Location cannot exceed 200 characters")
    private String location;

    @Size(max = 500, message = "Website URL cannot exceed 500 characters")
    private String website;

    private LocalDateTime dateOfBirth;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phoneNumber;

    private Boolean isPrivate; // Privacy setting

    @Size(max = 20, message = "Verification tier cannot exceed 20 characters")
    private String verificationTier;
}
