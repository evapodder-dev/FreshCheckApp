package com.freshcheck.app.ui.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.freshcheck.app.R;
import com.freshcheck.app.database.FoodRepository;
import com.freshcheck.app.database.MealPlanRepository;
import com.freshcheck.app.database.ShoppingRepository;
import com.freshcheck.app.model.FoodItem;
import com.freshcheck.app.model.IngredientMatchResult;
import com.freshcheck.app.model.MealPlan;
import com.freshcheck.app.model.Recipe;
import com.freshcheck.app.model.ShoppingItem;
import com.freshcheck.app.network.LocalRecipeRepository;
import com.freshcheck.app.util.DateUtils;
import com.freshcheck.app.util.RecipeMatcher;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MealPlannerActivity extends AppCompatActivity {
    private static final String TAG = "MealPlannerActivity";

    private MealPlanRepository mealPlanRepo;
    private FoodRepository foodRepo;
    private ShoppingRepository shoppingRepo;

    private Calendar currentCalendar;
    private TextView tvDateTitle;
    private TextView tvBreakfast;
    private TextView tvLunch;
    private TextView tvDinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_planner);

        mealPlanRepo = new MealPlanRepository(this);
        foodRepo = new FoodRepository(this);
        shoppingRepo = new ShoppingRepository(this);

        currentCalendar = Calendar.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar_meal_planner);
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

        tvDateTitle = findViewById(R.id.tv_selected_date_title);
        tvBreakfast = findViewById(R.id.tv_breakfast_item);
        tvLunch = findViewById(R.id.tv_lunch_item);
        tvDinner = findViewById(R.id.tv_dinner_item);

        findViewById(R.id.btn_prev_day).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCalendar.add(Calendar.DAY_OF_YEAR, -1);
                loadDayPlans();
            }
        });

        findViewById(R.id.btn_next_day).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCalendar.add(Calendar.DAY_OF_YEAR, 1);
                loadDayPlans();
            }
        });

        findViewById(R.id.btn_add_breakfast).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddMealDialog("Breakfast");
            }
        });

        findViewById(R.id.btn_add_lunch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddMealDialog("Lunch");
            }
        });

        findViewById(R.id.btn_add_dinner).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddMealDialog("Dinner");
            }
        });

        findViewById(R.id.btn_generate_grocery_list).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateGroceryListFromMissing();
            }
        });

        loadDayPlans();
    }

    private String getSelectedDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(currentCalendar.getTime());
    }

    private void loadDayPlans() {
        String dateStr = getSelectedDateString();
        SimpleDateFormat displayFormat = new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
        tvDateTitle.setText(displayFormat.format(currentCalendar.getTime()));

        tvBreakfast.setText("No meal planned yet");
        tvLunch.setText("No meal planned yet");
        tvDinner.setText("No meal planned yet");

        List<MealPlan> plans = mealPlanRepo.getPlansForDate(dateStr, "");
        for (MealPlan plan : plans) {
            String label = plan.getRecipeTitle();
            if (plan.getNotes() != null && !plan.getNotes().isEmpty()) {
                label += " (" + plan.getNotes() + ")";
            }
            if ("Breakfast".equalsIgnoreCase(plan.getMealType())) {
                tvBreakfast.setText(label);
            } else if ("Lunch".equalsIgnoreCase(plan.getMealType())) {
                tvLunch.setText(label);
            } else if ("Dinner".equalsIgnoreCase(plan.getMealType())) {
                tvDinner.setText(label);
            }
        }
    }

    private void showAddMealDialog(final String mealType) {
        List<FoodItem> activeFoods = foodRepo.getAllActive();
        List<Recipe> allRecipes = LocalRecipeRepository.getCuratedRecipes();
        final List<IngredientMatchResult> matched = RecipeMatcher.matchAndRank(allRecipes, activeFoods);

        final String[] options = new String[matched.size()];
        for (int i = 0; i < matched.size(); i++) {
            IngredientMatchResult r = matched.get(i);
            String title = r.getRecipe().getTitle() + " [" + r.getMatchPercentage() + "% Ready]";
            if (r.hasExpiringIngredients()) {
                title += " 🔥 Uses expiring items";
            }
            options[i] = title;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Plan " + mealType);
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                IngredientMatchResult selected = matched.get(which);
                MealPlan plan = new MealPlan(
                        0,
                        getSelectedDateString(),
                        mealType,
                        selected.getRecipe().getId(),
                        selected.getRecipe().getTitle(),
                        selected.hasExpiringIngredients() ? "Uses expiring food" : "Ready: " + selected.getMatchPercentage() + "%",
                        "",
                        false
                );
                mealPlanRepo.insert(plan);
                Toast.makeText(MealPlannerActivity.this, "Added to " + mealType, Toast.LENGTH_SHORT).show();
                loadDayPlans();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void generateGroceryListFromMissing() {
        Calendar cal = (Calendar) currentCalendar.clone();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String startDate = sdf.format(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, 6);
        String endDate = sdf.format(cal.getTime());

        List<String> missing = mealPlanRepo.getMissingIngredientsForWeek(startDate, endDate, foodRepo);
        if (missing.isEmpty()) {
            Toast.makeText(this, "All planned meals have ingredients ready in your pantry!", Toast.LENGTH_LONG).show();
            return;
        }

        for (String ing : missing) {
            ShoppingItem item = new ShoppingItem(0, ing, 1.0, "items", "Others", false, 0.0, "From weekly meal plan", "");
            shoppingRepo.insert(item);
        }

        Toast.makeText(this, "Added " + missing.size() + " missing items to your Grocery List!", Toast.LENGTH_LONG).show();
    }
}