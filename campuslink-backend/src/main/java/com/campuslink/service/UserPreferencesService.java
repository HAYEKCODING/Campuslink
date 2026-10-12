package com.campuslink.service;

import com.campuslink.dto.request.UserPreferencesRequest;
import com.campuslink.dto.response.UserPreferencesResponse;

import java.util.UUID;

public interface UserPreferencesService {
    UserPreferencesResponse get(UUID userId);

    UserPreferencesResponse update(UUID userId, UserPreferencesRequest request);
}
