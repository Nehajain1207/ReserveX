package com.neha.ticketreservation.controller;

import com.neha.ticketreservation.dto.AdminDashboardResponse;
import com.neha.ticketreservation.dto.BookingResponse;
import com.neha.ticketreservation.service.AdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.neha.ticketreservation.dto.RevenueResponse;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ==========================================
    // Dashboard
    // ==========================================
    @GetMapping("/dashboard")
    public AdminDashboardResponse getDashboard() {

        return adminService.getDashboard();
    }

    @GetMapping("/revenue")
    public RevenueResponse getRevenue() {

        return adminService.getRevenue();
    }



    // ==========================================
    // View All Bookings
    // ==========================================
    @GetMapping("/bookings")
    public List<BookingResponse> getAllBookings() {

        return adminService.getAllBookings();
    }

}