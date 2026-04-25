package com.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAnalyticsDTO {
    private Double totalRevenue;
    private Long activeSubscriptions;
    private Double mrr; // Monthly Recurring Revenue
    private Long totalOrders;
    private Long expiredSubscriptions;
    
    private Map<String, Double> revenueTrend;
    private List<RecentSaleDTO> recentSales;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentSaleDTO {
        private String studentName;
        private String courseTitle;
        private Double amount;
        private String date;
        private String status;
        private String type; // ONE_TIME or SUBSCRIPTION
    }
}
