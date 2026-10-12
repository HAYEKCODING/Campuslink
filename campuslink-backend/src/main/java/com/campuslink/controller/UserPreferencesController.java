package com.campuslink.controller;

import com.campuslink.dto.request.UserPreferencesRequest;
import com.campuslink.dto.response.ApiResponse;
import com.campuslink.dto.response.UserPreferencesResponse;
import com.campuslink.security.UserPrincipal;
import com.campuslink.service.UserPreferencesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/settings/me")
@RequiredArgsConstructor
public class UserPreferencesController {

    private final UserPreferencesService userPreferencesService;

    @GetMapping
    public ApiResponse<UserPreferencesResponse> get(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(userPreferencesService.get(principal.getId()));
    }

    @PatchMapping
    public ApiResponse<UserPreferencesResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserPreferencesRequest request) {
        return ApiResponse.success(userPreferencesService.update(principal.getId(), request));
    }
}
