package com.societegenerale.requestmanagement.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRequestCommand(
        @NotBlank(message = "Name is mandatory")
        String name,

        @NotBlank(message = "Body is mandatory")
        String body
) {
}
