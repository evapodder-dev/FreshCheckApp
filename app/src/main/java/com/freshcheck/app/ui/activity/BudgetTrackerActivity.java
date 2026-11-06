package com.freshcheck.app.ui.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.freshcheck.app.R;
import com.freshcheck.app.database.BudgetRepository;
import com.freshcheck.app.database.FoodRepository;
import com.freshcheck.app.database.ShoppingRepository;
import com.freshcheck.app.database.WasteRepository;
import com.freshcheck.app.util.DateUtils;

import java.text.DecimalFormat;

public class BudgetTrackerActivity extends AppCompatActivity {
    private static final String TAG = "BudgetTrackerActivity";

    private BudgetRepository budgetRepo;
    private FoodRepository foodRepo;
    private WasteRepository wasteRepo;
    private ShoppingRepository shoppingRepo;

    private TextView tvMonthTitle;
    private TextView tvBudgetAmount;
    private ProgressBar pbUsage;
    private TextView tvUsageLabel;
    private TextView tvTotalSpent;
    private TextView tvWasteLoss;
    private TextView tvNetSavings;

    private String currentMonthKey;
    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_tracker);

        budgetRepo = new BudgetRepository(this);
        foodRepo = new FoodRepository(this);
        wasteRepo = new WasteRepository(this);
        shoppingRepo = new ShoppingRepository(this);

        currentMonthKey = DateUtils.getTodayString().substring(0, 7); // e.g. "2026-11"

        Toolbar toolbar = findViewById(R.id.toolbar_budget);
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

        tvMonthTitle = findViewById(R.id.tv_month_title);
        tvBudgetAmount = findViewById(R.id.tv_budget_amount);
        pbUsage = findViewById(R.id.pb_budget_usage);
        tvUsageLabel = findViewById(R.id.tv_budget_usage_label);
        tvTotalSpent = findViewById(R.id.tv_total_spent);
        tvWasteLoss = findViewById(R.id.tv_waste_loss);
        tvNetSavings = findViewById(R.id.tv_net_savings);

        findViewById(R.id.btn_edit_budget).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditBudgetDialog();
            }
        });

        loadBudgetMetrics();
    }

    private void loadBudgetMetrics() {
        BudgetRepository.FinancialSummary summary = budgetRepo.calculateSummary(
                currentMonthKey, foodRepo, wasteRepo, shoppingRepo
        );

        tvMonthTitle.setText("Budget Overview (" + currentMonthKey + ")");
        tvBudgetAmount.setText(summary.currencySymbol + df.format(summary.monthlyBudget));
        tvTotalSpent.setText(summary.currencySymbol + df.format(summary.totalGroceryExpenses));
        tvWasteLoss.setText(summary.currencySymbol + df.format(summary.estimatedWasteCost));

        tvNetSavings.setText(summary.currencySymbol + df.format(summary.netSavings));
        if (summary.netSavings < 0) {
            tvNetSavings.setTextColor(ContextCompat.getColor(this, R.color.status_expired));
        } else {
            tvNetSavings.setTextColor(ContextCompat.getColor(this, R.color.primary));
        }

        int progress = (int) Math.min(100, summary.budgetUsagePercentage);
        pbUsage.setProgress(progress);
        if (progress > 90) {
            pbUsage.setProgressTintList(ContextCompat.getColorStateList(this, R.color.status_expired));
        } else {
            pbUsage.setProgressTintList(ContextCompat.getColorStateList(this, R.color.primary));
        }

        tvUsageLabel.setText((int) summary.budgetUsagePercentage + "% spent ("
                + summary.currencySymbol + df.format(summary.totalGroceryExpenses) + " of "
                + summary.currencySymbol + df.format(summary.monthlyBudget) + ")");
    }

    private void showEditBudgetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Monthly Grocery Budget (৳)");

        final EditText etBudget = new EditText(this);
        etBudget.setHint("e.g. 8000");
        etBudget.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etBudget.setText(String.valueOf((int) budgetRepo.getMonthlyBudget(currentMonthKey)));
        etBudget.setPadding(40, 30, 40, 30);
        builder.setView(etBudget);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String input = etBudget.getText().toString().trim();
                try {
                    double val = Double.parseDouble(input);
                    if (val > 0) {
                        budgetRepo.setMonthlyBudget(currentMonthKey, val);
                        Toast.makeText(BudgetTrackerActivity.this, "Budget updated to ৳" + val, Toast.LENGTH_SHORT).show();
                        loadBudgetMetrics();
                    }
                } catch (Exception e) {
                    Toast.makeText(BudgetTrackerActivity.this, "Invalid amount", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}