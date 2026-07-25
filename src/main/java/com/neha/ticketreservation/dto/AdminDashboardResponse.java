package com.neha.ticketreservation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AdminDashboardResponse {

    private long totalUsers;

    private long totalMovies;

    private long totalShows;

    private long totalBookings;

    private long confirmedBookings;

    private long pendingBookings;

    private long cancelledBookings;

}