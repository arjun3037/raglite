package com.raglite.ask.controller;

import jakarta.validation.constraints.NotBlank;

public record AskRequest(
        @NotBlank String question
) {
}