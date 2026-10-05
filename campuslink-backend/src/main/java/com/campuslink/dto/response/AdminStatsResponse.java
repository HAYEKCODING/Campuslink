package com.campuslink.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Statistiques globales sur les utilisateurs — {@code GET /admin/stats}.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {

    private long totalUsers;
    private Map<String, Long> usersByStatus;
    private Map<String, Long> usersByRole;
    private long emailVerifiedCount;
    private long emailUnverifiedCount;
    private long newUsersLast7Days;
    private long newUsersLast30Days;

}
