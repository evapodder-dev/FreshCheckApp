package com.freshcheck.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.freshcheck.app.model.CategoryConstants;
import com.freshcheck.app.model.FoodItem;
import com.freshcheck.app.model.ShoppingItem;

import java.util.List;

public class BudgetRepository {
    private final DatabaseHelper dbHelper;

    public static class FinancialSummary {
        public String monthKey;
        public double monthlyBudget;
        public double totalGroceryExpenses;
        public double estimatedWasteCost;
        public double netSavings;
        public double budgetUsagePercentage;
        public String currencySymbol;

        public FinancialSummary() {
            this.currencySymbol = CategoryConstants.CURRENCY_SYMBOL;
        }
    }

    public BudgetRepository(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    public double getMonthlyBudget(String monthKey) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        double budget = 8000.0; // default 8000 BDT
        Cursor cursor = db.query(DatabaseHelper.TABLE_BUDGET_SETTINGS, null,
                DatabaseHelper.COL_BUDGET_MONTH + " = ?",
                new String[]{monthKey}, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                budget = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BUDGET_AMOUNT));
            }
            cursor.close();
        }
        return budget;
    }

    public void setMonthlyBudget(String monthKey, double amount) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_BUDGET_MONTH, monthKey);
        cv.put(DatabaseHelper.COL_BUDGET_AMOUNT, amount);
        cv.put(DatabaseHelper.COL_BUDGET_CURRENCY, CategoryConstants.CURRENCY_SYMBOL);

        int updated = db.update(DatabaseHelper.TABLE_BUDGET_SETTINGS, cv,
                DatabaseHelper.COL_BUDGET_MONTH + " = ?", new String[]{monthKey});
        if (updated == 0) {
            db.insert(DatabaseHelper.TABLE_BUDGET_SETTINGS, null, cv);
        }
    }

    public FinancialSummary calculateSummary(String monthKey, FoodRepository foodRepo,
                                             WasteRepository wasteRepo, ShoppingRepository shoppingRepo) {
        FinancialSummary summary = new FinancialSummary();
        summary.monthKey = monthKey;
        summary.monthlyBudget = getMonthlyBudget(monthKey);
        summary.currencySymbol = CategoryConstants.CURRENCY_SYMBOL;

        // Calculate grocery expense from active items + purchased shopping items
        double expense = 0.0;
        List<FoodItem> activeFoods = foodRepo.getAllActive();
        for (FoodItem f : activeFoods) {
            if (f.getPurchaseDate() != null && f.getPurchaseDate().startsWith(monthKey)) {
                expense += f.getEstimatedCost();
            } else if (f.getPurchaseDate() == null || f.getPurchaseDate().isEmpty()) {
                expense += f.getEstimatedCost();
            }
        }

        List<ShoppingItem> shoppingItems = shoppingRepo.getAll();
        for (ShoppingItem s : shoppingItems) {
            if (s.isPurchased() && s.getEstimatedCost() > 0) {
                expense += s.getEstimatedCost();
            }
        }
        summary.totalGroceryExpenses = expense;

        // Waste cost
        summary.estimatedWasteCost = wasteRepo.getTotalFinancialLoss();

        // Net Savings
        summary.netSavings = summary.monthlyBudget - summary.totalGroceryExpenses;
        if (summary.monthlyBudget > 0) {
            summary.budgetUsagePercentage = (summary.totalGroceryExpenses / summary.monthlyBudget) * 100.0;
        } else {
            summary.budgetUsagePercentage = 0.0;
        }

        return summary;
    }
}