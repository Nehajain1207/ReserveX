package com.neha.ticketreservation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MovieResponse {

    private Long id;

    private String title;

    private String language;

    private Integer duration;

    private String genre;
}