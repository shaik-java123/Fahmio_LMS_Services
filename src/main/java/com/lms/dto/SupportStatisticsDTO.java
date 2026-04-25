package com.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportStatisticsDTO {
    private long totalRequests;
    private long openRequests;
    private long inProgressRequests;
    private long resolvedRequests;
    private long closedRequests;
    private long urgentRequests;
    private long highPriorityRequests;

    public long getAverageResolutionTime() {
        // This would be calculated with actual data
        return 24; // hours
    }

    public double getResolutionRate() {
        if (totalRequests == 0) return 0;
        return ((double) (resolvedRequests + closedRequests) / totalRequests) * 100;
    }
}

