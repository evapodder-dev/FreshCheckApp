package com.freshcheck.app.ui.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.freshcheck.app.R;
import com.freshcheck.app.database.ConsumptionRepository;
import com.freshcheck.app.database.FoodRepository;
import com.freshcheck.app.database.WasteRepository;
import com.freshcheck.app.util.WasteChallengeManager;

public class WasteChallengeActivity extends AppCompatActivity {
    private static final String TAG = "WasteChallengeActivity";

    private FoodRepository foodRepo;
    private WasteRepository wasteRepo;
    private ConsumptionRepository consumptionRepo;

    private TextView tvScoreValue;
    private TextView tvScoreTier;
    private TextView tvTasksSummary;
    private LinearLayout layoutBadgesContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waste_challenge);

        foodRepo = new FoodRepository(this);
        wasteRepo = new WasteRepository(this);
        consumptionRepo = new ConsumptionRepository(this);

        Toolbar toolbar = findViewById(R.id.toolbar_challenge);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        tvScoreValue = findViewById(R.id.tv_score_value);
        tvScoreTier = findViewById(R.id.tv_score_tier);
        tvTasksSummary = findViewById(R.id.tv_tasks_summary);
        layoutBadgesContainer = findViewById(R.id.layout_badges_container);

        loadChallengeData();
    }

    private void loadChallengeData() {
        WasteChallengeManager.ChallengeStatus status =
                WasteChallengeManager.evaluateStatus(foodRepo, wasteRepo, consumptionRepo);

        tvScoreValue.setText(String.valueOf(status.foodSaverScore));
        tvScoreTier.setText(status.scoreTier);

        // Build weekly tasks summary
        StringBuilder taskSb = new StringBuilder();
        for (int i = 0; i < status.weeklyTasks.size(); i++) {
            if (i < status.completedTasksCount) {
                taskSb.append("✓ ");
            } else {
                taskSb.append("○ ");
            }
            taskSb.append(status.weeklyTasks.get(i)).append("\n");
        }
        tvTasksSummary.setText(taskSb.toString().trim());

        // Build badges
        layoutBadgesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (WasteChallengeManager.Badge badge : status.badges) {
            View badgeView = inflater.inflate(R.layout.item_storage_guide, layoutBadgesContainer, false);
            TextView tvName = badgeView.findViewById(R.id.tv_storage_food_name);
            TextView tvTag = badgeView.findViewById(R.id.tv_storage_category_tag);
            TextView tvLoc = badgeView.findViewById(R.id.tv_storage_location);
            TextView tvTips = badgeView.findViewById(R.id.tv_storage_tips);
            TextView tvFreezing = badgeView.findViewById(R.id.tv_freezing_tips);

            tvName.setText(badge.iconEmoji + " " + badge.title);
            tvTag.setText(badge.isUnlocked ? "Unlocked ✓" : "In Progress");
            if (!badge.isUnlocked) {
                tvTag.setBackgroundResource(R.drawable.bg_badge_expiring);
                tvTag.setTextColor(getResources().getColor(R.color.status_expiring));
            }
            tvLoc.setVisibility(View.GONE);
            tvTips.setText(badge.description);
            tvFreezing.setVisibility(View.GONE);

            layoutBadgesContainer.addView(badgeView);
        }
    }
}