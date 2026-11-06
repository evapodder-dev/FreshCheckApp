package com.freshcheck.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.freshcheck.app.model.FoodItem;
import com.freshcheck.app.model.MealPlan;
import com.freshcheck.app.model.Recipe;
import com.freshcheck.app.network.LocalRecipeRepository;

import java.util.ArrayList;
import java.util.List;

public class MealPlanRepository {
    private static final String TAG = "MealPlanRepository";
    private final DatabaseHelper dbHelper;

    public MealPlanRepository(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    public long insert(MealPlan plan) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = plan.toContentValues();
        long id = db.insert(DatabaseHelper.TABLE_MEAL_PLANS, null, cv);
        plan.setId(id);
        Log.d(TAG, "Inserted MealPlan id " + id + " for date " + plan.getPlanDate());
        return id;
    }

    public int update(MealPlan plan) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = plan.toContentValues();
        return db.update(DatabaseHelper.TABLE_MEAL_PLANS, cv,
                DatabaseHelper.COL_MEAL_ID + " = ?",
                new String[]{String.valueOf(plan.getId())});
    }

    public int delete(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLE_MEAL_PLANS,
                DatabaseHelper.COL_MEAL_ID + " = ?",
                new String[]{String.valueOf(id)});
    }

    public List<MealPlan> getPlansForDate(String date, String householdId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<MealPlan> list = new ArrayList<>();
        String selection = DatabaseHelper.COL_MEAL_DATE + " = ?";
        String[] args = new String[]{date};

        Cursor cursor = db.query(DatabaseHelper.TABLE_MEAL_PLANS, null,
                selection, args, null, null, DatabaseHelper.COL_MEAL_ID + " ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToMealPlan(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public List<MealPlan> getPlansForWeek(String startDate, String endDate, String householdId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<MealPlan> list = new ArrayList<>();
        String selection = DatabaseHelper.COL_MEAL_DATE + " BETWEEN ? AND ?";
        String[] args = new String[]{startDate, endDate};

        Cursor cursor = db.query(DatabaseHelper.TABLE_MEAL_PLANS, null,
                selection, args, null, null, DatabaseHelper.COL_MEAL_DATE + " ASC, " + DatabaseHelper.COL_MEAL_ID + " ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToMealPlan(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public List<String> getMissingIngredientsForWeek(String startDate, String endDate, FoodRepository foodRepo) {
        List<MealPlan> weekPlans = getPlansForWeek(startDate, endDate, null);
        List<FoodItem> activeFoods = foodRepo.getAllActive();
        List<String> missing = new ArrayList<>();

        for (MealPlan plan : weekPlans) {
            Recipe matchedRecipe = findRecipeByIdOrTitle(plan.getRecipeId(), plan.getRecipeTitle());
            if (matchedRecipe != null) {
                for (String ing : matchedRecipe.getIngredients()) {
                    boolean found = false;
                    String cleanIng = ing.replaceAll("\\(.*\\)", "").trim().toLowerCase();
                    for (FoodItem food : activeFoods) {
                        if (cleanIng.contains(food.getName().toLowerCase()) || food.getName().toLowerCase().contains(cleanIng)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found && !missing.contains(ing)) {
                        missing.add(ing);
                    }
                }
            }
        }
        return missing;
    }

    private Recipe findRecipeByIdOrTitle(String recipeId, String recipeTitle) {
        for (Recipe r : LocalRecipeRepository.getCuratedRecipes()) {
            if (r.getId().equals(recipeId) || r.getTitle().equalsIgnoreCase(recipeTitle)) {
                return r;
            }
        }
        return null;
    }

    private MealPlan cursorToMealPlan(Cursor c) {
        return new MealPlan(
                c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_MEAL_ID)),
                c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MEAL_DATE)),
                c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MEAL_TYPE)),
                c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MEAL_RECIPE_ID)),
                c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MEAL_RECIPE_TITLE)),
                c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MEAL_NOTES)),
                c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MEAL_HOUSEHOLD_ID)),
                c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_MEAL_IS_COMPLETED)) == 1
        );
    }
}