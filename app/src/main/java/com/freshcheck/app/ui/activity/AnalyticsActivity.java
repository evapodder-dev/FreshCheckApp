package com.freshcheck.app.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.freshcheck.app.R;
import com.freshcheck.app.database.ConsumptionRepository;
import com.freshcheck.app.database.WasteRepository;
import com.freshcheck.app.model.WasteRecord;
import com.freshcheck.app.ui.custom.WasteBreakdownChartView;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsActivity extends AppCompatActivity {
    private static final String TAG = "AnalyticsActivity";

    private WasteRepository wasteRepo;
    private ConsumptionRepository consumptionRepo;

    private TextView tvTotalLoss;
    private WasteBreakdownChartView chartView;
    private TextView tvFrequentConsumed;
    private TextView tvFrequentWasted;

    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        wasteRepo = new WasteRepository(this);
        consumptionRepo = new ConsumptionRepository(this);

        Toolbar toolbar = findViewById(R.id.toolbar_analytics);
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

        tvTotalLoss = findViewById(R.id.tv_analytics_total_loss);
        chartView = findViewById(R.id.chart_waste_analytics);
        tvFrequentConsumed = findViewById(R.id.tv_frequent_consumed);
        tvFrequentWasted = findViewById(R.id.tv_frequent_wasted);

        loadAnalytics();
    }

    private void loadAnalytics() {
        double totalLoss = wasteRepo.getTotalFinancialLoss();
        tvTotalLoss.setText("Total Financial Loss: ৳" + df.format(totalLoss));

        // Chart data
        Map<String, Float> costByReason = wasteRepo.getWasteCostByReason();
        chartView.setData(costByReason);

        // Frequent consumed
        List<Map.Entry<String, Integer>> consumed = consumptionRepo.getFrequentlyConsumed(5);
        if (consumed.isEmpty()) {
            tvFrequentConsumed.setText("• No consumption history recorded yet.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> e : consumed) {
                sb.append("• ").append(e.getKey()).append(" (").append(e.getValue()).append(" times)\n");
            }
            tvFrequentConsumed.setText(sb.toString().trim());
        }

        // Frequent wasted
        List<WasteRecord> wasteRecords = wasteRepo.getAll();
        if (wasteRecords.isEmpty()) {
            tvFrequentWasted.setText("• Great job! Zero food items recorded as wasted.");
        } else {
            Map<String, Double> wastedMap = new HashMap<>();
            for (WasteRecord r : wasteRecords) {
                Double prev = wastedMap.get(r.getFoodName());
                wastedMap.put(r.getFoodName(), (prev != null ? prev : 0.0) + r.getEstimatedCost());
            }
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Double> e : wastedMap.entrySet()) {
                sb.append("• ").append(e.getKey()).append(" — ৳").append(df.format(e.getValue())).append("\n");
            }
            tvFrequentWasted.setText(sb.toString().trim());
        }
    }
}