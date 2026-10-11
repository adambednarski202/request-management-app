package com.societegenerale.requestmanagement.dto;

import jakarta.validation.constraints.NotBlank;

public record ModifyRequestCommand(
        @NotBlank(message = "Body is mandatory")
        String body
) {
}
