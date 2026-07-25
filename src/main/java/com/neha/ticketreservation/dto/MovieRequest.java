package com.neha.ticketreservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovieRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Language is required")
    private String language;

    @NotNull(message = "Duration is required")
    private Integer duration;

    @NotBlank(message = "Genre is required")
    private String genre;
}