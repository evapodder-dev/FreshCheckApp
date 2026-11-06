package com.freshcheck.app.util;

import com.freshcheck.app.database.ConsumptionRepository;
import com.freshcheck.app.database.FoodRepository;
import com.freshcheck.app.database.WasteRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class WasteChallengeManager {

    public static class Badge implements Serializable {
        public String title;
        public String description;
        public String iconEmoji;
        public boolean isUnlocked;

        public Badge(String title, String description, String iconEmoji, boolean isUnlocked) {
            this.title = title;
            this.description = description;
            this.iconEmoji = iconEmoji;
            this.isUnlocked = isUnlocked;
        }
    }

    public static class ChallengeStatus implements Serializable {
        public int foodSaverScore; // 0 to 100
        public String scoreTier; // "Novice Saver", "Smart Planner", "Pantry Champion", "Zero-Waste Master"
        public int itemsSavedCount;
        public int itemsWastedCount;
        public List<Badge> badges;
        public List<String> weeklyTasks;
        public int completedTasksCount;

        public ChallengeStatus() {
            this.badges = new ArrayList<>();
            this.weeklyTasks = new ArrayList<>();
        }
    }

    public static ChallengeStatus evaluateStatus(FoodRepository foodRepo, WasteRepository wasteRepo,
                                                 ConsumptionRepository consumptionRepo) {
        ChallengeStatus status = new ChallengeStatus();

        int consumed = consumptionRepo.getTotalConsumedCount();
        int wasted = wasteRepo.getTotalItemsWasted();
        int active = foodRepo.getTotalActiveCount();

        status.itemsSavedCount = consumed;
        status.itemsWastedCount = wasted;

        // Calculate Food Saver Score (0-100)
        // High consumption ratio, proactive inventory audits, and minimal waste increase score
        int baseScore = 65;
        if (consumed + wasted > 0) {
            double saveRatio = (double) consumed / (consumed + wasted);
            baseScore = (int) (saveRatio * 75.0);
        }
        if (active > 0) {
            baseScore += Math.min(25, active * 2);
        }
        baseScore = Math.max(10, Math.min(100, baseScore));
        status.foodSaverScore = baseScore;

        if (baseScore >= 90) status.scoreTier = "Zero-Waste Master 🌟";
        else if (baseScore >= 75) status.scoreTier = "Pantry Champion 🏆";
        else if (baseScore >= 60) status.scoreTier = "Smart Planner 🥦";
        else status.scoreTier = "Novice Saver 🌱";

        // Badges
        status.badges.add(new Badge("Pantry Starter", "Added your first food items to FreshCheck", "🌱", active > 0));
        status.badges.add(new Badge("Food Rescuer", "Consumed food items before expiration", "🍽️", consumed >= 3));
        status.badges.add(new Badge("Waste Cutter", "Recorded and analyzed food waste entries", "📉", wasted > 0));
        status.badges.add(new Badge("Zero Waste Hero", "Maintained 85%+ Food Saver Score", "🛡️", baseScore >= 85));
        status.badges.add(new Badge("Master Chef", "Cooked meals utilizing expiring pantry items", "🍳", consumed >= 5));

        // Weekly Tasks
        status.weeklyTasks.add("Audit your pantry and refrigerator items");
        status.weeklyTasks.add("Cook 2 recipes using ingredients expiring soon");
        status.weeklyTasks.add("Transfer all grocery purchases to inventory");
        status.weeklyTasks.add("Keep total weekly waste cost under ৳200");

        status.completedTasksCount = (active > 0 ? 1 : 0) + (consumed >= 2 ? 1 : 0) + (wasteRepo.getTotalFinancialLoss() < 500 ? 1 : 0);

        return status;
    }
}