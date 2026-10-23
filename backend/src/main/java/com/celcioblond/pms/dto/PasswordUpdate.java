package com.celcioblond.pms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param currentPassword required when users change their own password; ignored for admin resets
 */
public record PasswordUpdate(
        String currentPassword,
        @NotBlank @Size(min = 8, max = 72) String newPassword
) {
}
