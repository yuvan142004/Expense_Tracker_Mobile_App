package com.example.expensetracker.utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Local rule-based AI for analyzing spending patterns and generating insights
 */
public class SpendingInsightsAnalyzer {

    /**
     * Simple data class for category spending
     */
    public static class CategorySpending {
        public String categoryName;
        public double amount;
        
        public CategorySpending(String name, double amt) {
            this.categoryName = name;
            this.amount = amt;
        }
    }

    /**
     * Analyze spending data and generate insights
     * @param categoryData Map of category name to spending amount
     * @param totalSpent Total amount spent in current period
     * @param previousTotal Total amount spent in previous period
     * @param periodName Name of the period (e.g., "this week", "this month")
     * @return Formatted insights text
     */
    public static String generateInsights(
            Map<String, Double> categoryData,
            double totalSpent,
            double previousTotal,
            String periodName) {
        
        StringBuilder insights = new StringBuilder();
        
        if (categoryData.isEmpty() || totalSpent <= 0) {
            return "No spending data available for " + periodName + ".";
        }
        
        // 1. Top spending category
        String topCategory = "";
        double topAmount = 0;
        for (Map.Entry<String, Double> entry : categoryData.entrySet()) {
            if (entry.getValue() > topAmount) {
                topAmount = entry.getValue();
                topCategory = entry.getKey();
            }
        }
        
        double topPercentage = (topAmount / totalSpent) * 100;
        insights.append("💰 Top Spending: ")
                .append(topCategory)
                .append(" (").append(String.format(Locale.getDefault(), "%.0f%%", topPercentage))
                .append(" of total)\n\n");
        
        // 2. Compare with previous period
        if (previousTotal > 0) {
            double change = ((totalSpent - previousTotal) / previousTotal) * 100;
            
            if (change > 20) {
                insights.append("⚠️ Alert: Spending increased by ")
                        .append(String.format(Locale.getDefault(), "%.0f%%", change))
                        .append(" compared to previous ").append(periodName).append("\n\n");
            } else if (change < -20) {
                insights.append("✅ Great! Spending decreased by ")
                        .append(String.format(Locale.getDefault(), "%.0f%%", Math.abs(change)))
                        .append(" compared to previous ").append(periodName).append("\n\n");
            } else if (Math.abs(change) <= 10) {
                insights.append("📊 Spending is stable compared to previous ")
                        .append(periodName).append("\n\n");
            }
        }
        
        // 3. Spending concentration warning
        if (topPercentage > 50) {
            insights.append("⚡ Tip: Over 50% of spending is in one category. ")
                    .append("Consider diversifying your budget.\n\n");
        }
        
        // 4. Number of categories insight
        int activeCategories = categoryData.size();
        if (activeCategories == 1) {
            insights.append("📊 All expenses in one category. ")
                    .append("Consider tracking other spending areas.\n\n");
        } else if (activeCategories >= 5) {
            insights.append("📈 Spending across ")
                    .append(activeCategories)
                    .append(" categories - good budget diversity!\n\n");
        }
        
        // 5. Average spending insight
        double avgPerCategory = totalSpent / activeCategories;
        insights.append("💳 Average per category: ₹")
                .append(String.format(Locale.getDefault(), "%.0f", avgPerCategory))
                .append("\n\n");
        
        // 6. General tips
        if (totalSpent > 10000) {
            insights.append("💡 Tip: Set category-wise budgets to track spending better.");
        } else if (totalSpent < 1000) {
            insights.append("✨ Light spending period! Good job controlling expenses.");
        } else {
            insights.append("👍 Keep tracking your expenses to stay in control!");
        }
        
        return insights.toString();
    }
}
