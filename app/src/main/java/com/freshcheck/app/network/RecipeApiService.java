package com.freshcheck.app.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.freshcheck.app.model.Recipe;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles HTTP requests and JSON parsing for recipe discovery.
 * Connects to TheMealDB public REST API and falls back to
 * local curated recipes when offline or if query fails.
 */
public class RecipeApiService {
    private static final String TAG = "RecipeApiService";
    private static final String SEARCH_URL = "https://www.themealdb.com/api/json/v1/1/search.php?s=";
    private static final String FILTER_URL = "https://www.themealdb.com/api/json/v1/1/filter.php?i=";

    public interface RecipeCallback {
        void onSuccess(List<Recipe> recipes, boolean isFromNetwork);
        void onError(String errorMessage, List<Recipe> fallbackRecipes);
    }

    private final ExecutorService executor;
    private final Handler mainHandler;

    public RecipeApiService() {
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void searchRecipes(final String query, final RecipeCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String cleanQuery = (query != null) ? query.trim() : "";
                    String urlString = SEARCH_URL + java.net.URLEncoder.encode(cleanQuery, "UTF-8");
                    Log.d(TAG, "Requesting recipes from HTTP: " + urlString);

                    String jsonResponse = executeHttpGet(urlString);
                    List<Recipe> recipes = parseMealsJson(jsonResponse);

                    if (recipes != null && !recipes.isEmpty()) {
                        postSuccess(callback, recipes, true);
                    } else {
                        // If no live results found, return matching local recipes
                        Log.d(TAG, "No remote meals found for '" + query + "', using local repository");
                        List<Recipe> localFallback = LocalRecipeRepository.suggestForIngredients(
                                cleanQuery.isEmpty() ? null : Arrays.asList(cleanQuery)
                        );
                        postSuccess(callback, localFallback, false);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Network request failed: " + e.getMessage() + ", falling back to local recipes");
                    final List<Recipe> fallback = LocalRecipeRepository.suggestForIngredients(
                            query != null && !query.isEmpty() ? Arrays.asList(query) : null
                    );
                    postError(callback, "Network request unavailable. Loaded offline recipes.", fallback);
                }
            }
        });
    }

    private String executeHttpGet(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(6000);
        conn.setReadTimeout(6000);
        conn.setRequestProperty("Accept", "application/json");

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP error code: " + responseCode);
        }

        InputStream is = conn.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        conn.disconnect();
        return sb.toString();
    }

    private List<Recipe> parseMealsJson(String jsonStr) {
        List<Recipe> list = new ArrayList<>();
        if (jsonStr == null || jsonStr.trim().isEmpty()) return list;

        try {
            JSONObject root = new JSONObject(jsonStr);
            if (!root.has("meals") || root.isNull("meals")) {
                return list;
            }

            JSONArray meals = root.getJSONArray("meals");
            for (int i = 0; i < meals.length(); i++) {
                JSONObject m = meals.getJSONObject(i);

                String id = m.optString("idMeal", "meal_" + i);
                String title = m.optString("strMeal", "Untitled Recipe");
                String category = m.optString("strCategory", "Main Course");
                String instructionsText = m.optString("strInstructions", "");
                String thumb = m.optString("strMealThumb", "");
                String youtube = m.optString("strYoutube", "");

                // Parse ingredients and measures
                List<String> ingredients = new ArrayList<>();
                for (int k = 1; k <= 20; k++) {
                    String ing = m.optString("strIngredient" + k, "").trim();
                    String measure = m.optString("strMeasure" + k, "").trim();
                    if (!ing.isEmpty() && !ing.equalsIgnoreCase("null")) {
                        if (!measure.isEmpty() && !measure.equalsIgnoreCase("null")) {
                            ingredients.add(measure + " " + ing);
                        } else {
                            ingredients.add(ing);
                        }
                    }
                }

                // Split instructions into individual steps
                List<String> steps = new ArrayList<>();
                if (!instructionsText.isEmpty()) {
                    String[] rawSteps = instructionsText.split("\r\n|\n|\r");
                    for (String s : rawSteps) {
                        String trimmed = s.trim();
                        if (!trimmed.isEmpty()) {
                            steps.add(trimmed);
                        }
                    }
                }

                Recipe recipe = new Recipe(
                        id,
                        title,
                        category,
                        25,
                        "Medium",
                        ingredients,
                        steps,
                        thumb,
                        youtube,
                        false
                );
                list.add(recipe);
            }
        } catch (Exception e) {
            Log.e(TAG, "JSON parsing error: " + e.getMessage(), e);
        }
        return list;
    }

    private void postSuccess(final RecipeCallback callback, final List<Recipe> recipes, final boolean isNetwork) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onSuccess(recipes, isNetwork);
                }
            }
        });
    }

    private void postError(final RecipeCallback callback, final String message, final List<Recipe> fallback) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onError(message, fallback);
                }
            }
        });
    }
}
