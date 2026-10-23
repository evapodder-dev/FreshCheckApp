package com.freshcheck.app.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.freshcheck.app.R;
import com.freshcheck.app.model.Recipe;

public class RecipeDetailActivity extends AppCompatActivity {
    private static final String TAG = "RecipeDetail_Lifecycle";
    public static final String EXTRA_RECIPE = "extra_recipe";

    private Recipe recipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() called");
        setContentView(R.layout.activity_recipe_detail);

        Toolbar toolbar = findViewById(R.id.toolbar_recipe_detail);
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

        if (getIntent().hasExtra(EXTRA_RECIPE)) {
            recipe = (Recipe) getIntent().getSerializableExtra(EXTRA_RECIPE);
        }

        if (recipe == null) {
            Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindRecipeDetails();
    }

    private void bindRecipeDetails() {
        TextView tvTitle = findViewById(R.id.tv_detail_title);
        TextView tvCategory = findViewById(R.id.tv_detail_category);
        TextView tvTime = findViewById(R.id.tv_detail_time);
        TextView tvDifficulty = findViewById(R.id.tv_detail_difficulty);
        TextView tvIngredients = findViewById(R.id.tv_detail_ingredients);
        TextView tvInstructions = findViewById(R.id.tv_detail_instructions);

        tvTitle.setText(recipe.getTitle());
        tvCategory.setText(recipe.getCategory());
        tvTime.setText("⏱️ " + recipe.getPrepTimeMinutes() + " mins");
        tvDifficulty.setText("• " + recipe.getDifficulty());

        tvIngredients.setText(recipe.getIngredientsBulletList());
        tvInstructions.setText(recipe.getInstructionsFormatted());

        // Implicit Intent: Share Recipe Text
        findViewById(R.id.btn_share_recipe).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareRecipeText();
            }
        });

        // Implicit Intent: Search Video / View Online
        findViewById(R.id.btn_watch_video).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openVideoTutorial();
            }
        });
    }

    private void shareRecipeText() {
        Log.d(TAG, "Launching implicit Intent to share recipe");
        StringBuilder sb = new StringBuilder();
        sb.append("🍽️ Recipe: ").append(recipe.getTitle()).append("\n\n");
        sb.append("Ingredients:\n").append(recipe.getIngredientsBulletList()).append("\n\n");
        sb.append("Instructions:\n").append(recipe.getInstructionsFormatted()).append("\n");
        sb.append("Shared via FreshCheck App");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, recipe.getTitle() + " Recipe");
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(shareIntent, "Share Recipe via"));
    }

    private void openVideoTutorial() {
        Log.d(TAG, "Launching implicit Intent ACTION_VIEW to search video tutorial");
        String url;
        if (recipe.getSourceUrl() != null && !recipe.getSourceUrl().isEmpty()) {
            url = recipe.getSourceUrl();
        } else {
            url = "https://www.youtube.com/results?search_query=" + Uri.encode(recipe.getTitle() + " recipe tutorial");
        }

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(this, "No browser or video application found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() called");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() called");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
    }
}
