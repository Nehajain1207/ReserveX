package com.neha.ticketreservation.service;

import com.neha.ticketreservation.dto.AdminDashboardResponse;
import com.neha.ticketreservation.dto.BookingResponse;
import com.neha.ticketreservation.dto.RevenueResponse;

import java.util.List;

public interface AdminService {

    List<BookingResponse> getAllBookings();

    AdminDashboardResponse getDashboard();

    RevenueResponse getRevenue();

}