package com.hufsphere.linkboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamDashboardResponse {

    private int totalWorkItems;
    private Map<String, Integer> statusCounts;
    private List<MemberStatus> members;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberStatus {
        private String authorLogin;
        private int totalAssigned;
        private Map<String, Integer> statusCounts;
    }
}