package com.freshcheck.app.network;

import com.freshcheck.app.model.Recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class LocalRecipeRepository {
    private LocalRecipeRepository() {}

    public static List<Recipe> getCuratedRecipes() {
        List<Recipe> list = new ArrayList<>();

        // 1. Classic French Toast (Great for using leftover / stale bread and milk)
        list.add(new Recipe(
                "local_1",
                "Golden Sourdough French Toast",
                "Breakfast",
                15,
                "Easy",
                Arrays.asList("Sourdough Bread (4 slices)", "Organic Eggs (2)", "Fresh Milk (1/2 cup)", "Cinnamon (1/2 tsp)", "Butter (1 tbsp)", "Maple syrup"),
                Arrays.asList(
                        "Whisk eggs, milk, and cinnamon in a shallow wide bowl.",
                        "Melt butter in a non-stick skillet over medium heat.",
                        "Dip each bread slice into the egg mixture for 20 seconds per side until soaked.",
                        "Cook in skillet for 2-3 minutes per side until golden brown and crisp.",
                        "Serve hot drizzled with maple syrup."
                ),
                "",
                "https://www.allrecipes.com/recipe/7016/french-toast-i/",
                true
        ));

        // 2. Spinach and Feta Scramble
        list.add(new Recipe(
                "local_2",
                "Quick Spinach & Egg Scramble",
                "Breakfast",
                10,
                "Easy",
                Arrays.asList("Organic Eggs (3)", "Baby Spinach (1 cup)", "Olive Oil (1 tsp)", "Salt and black pepper", "Cheddar Cheese or Feta (optional)"),
                Arrays.asList(
                        "Heat olive oil in a skillet over medium heat.",
                        "Add baby spinach and sauté for 1-2 minutes until wilted.",
                        "Crack eggs into the pan and gently scramble with a spatula.",
                        "Season with salt and freshly cracked black pepper.",
                        "Remove from heat immediately when eggs are softly set. Top with cheese."
                ),
                "",
                "https://www.allrecipes.com/recipe/241857/spinach-scrambled-eggs/",
                true
        ));

        // 3. Greek Yogurt Berry Parfait
        list.add(new Recipe(
                "local_3",
                "Honey Greek Yogurt Parfait",
                "Breakfast / Snack",
                5,
                "Easy",
                Arrays.asList("Greek Yogurt (1 cup)", "Rolled Oats or Granola (1/3 cup)", "Honey (1 tbsp)", "Fresh Berries or Apples (sliced)"),
                Arrays.asList(
                        "Spoon half of the Greek yogurt into a glass or small bowl.",
                        "Layer with half of the rolled oats and fruit slices.",
                        "Add the remaining yogurt, top with remaining oats and fruit.",
                        "Drizzle with raw honey and enjoy cold."
                ),
                "",
                "https://www.allrecipes.com/recipe/228828/fruit-and-yogurt-parfait/",
                true
        ));

        // 4. One-Pan Garlic Herb Chicken Breast
        list.add(new Recipe(
                "local_4",
                "Skillet Garlic Herb Chicken",
                "Dinner",
                25,
                "Medium",
                Arrays.asList("Chicken Breast (500g)", "Olive Oil (2 tbsp)", "Garlic (2 cloves, minced)", "Salt, pepper, dried oregano", "Lemon juice (1 tbsp)"),
                Arrays.asList(
                        "Pat chicken breasts dry with paper towel. Season generously with salt, pepper, and oregano.",
                        "Heat olive oil in a heavy skillet over medium-high heat.",
                        "Sear chicken breasts for 6-7 minutes on first side without moving.",
                        "Flip chicken, add minced garlic and butter to the skillet, and cook 5-6 more minutes until internal temperature reaches 165°F (74°C).",
                        "Rest 5 minutes, drizzle lemon juice, and slice."
                ),
                "",
                "https://www.allrecipes.com/recipe/223042/chicken-with-garlic-and-lemon/",
                true
        ));

        // 5. Hearty Fried Brown Rice
        list.add(new Recipe(
                "local_5",
                "Leftover Brown Rice Stir-Fry",
                "Lunch / Dinner",
                20,
                "Easy",
                Arrays.asList("Cooked Brown Rice (2 cups)", "Organic Eggs (2)", "Baby Spinach or mixed veggies (1 cup)", "Soy Sauce (2 tbsp)", "Sesame Oil (1 tbsp)", "Green onions"),
                Arrays.asList(
                        "Heat 1 tablespoon sesame oil in a wok or large pan over high heat.",
                        "Push rice to one side and scramble the eggs on the other side.",
                        "Combine eggs and cold cooked brown rice, breaking up clumps.",
                        "Toss in spinach and vegetables. Stir-fry for 3-4 minutes.",
                        "Pour soy sauce around the rim of the pan and toss evenly. Serve piping hot."
                ),
                "",
                "https://www.allrecipes.com/recipe/79543/fried-rice-restaurant-style/",
                true
        ));

        // 6. Rustic Tomato & Bread Salad (Panzanella)
        list.add(new Recipe(
                "local_6",
                "Tuscan Tomato Panzanella",
                "Salad",
                15,
                "Easy",
                Arrays.asList("Sourdough Bread (cubed, 2 cups)", "Ripe Tomatoes (3, chopped)", "Olive Oil (3 tbsp)", "Red wine vinegar (1 tbsp)", "Fresh basil or greens", "Salt"),
                Arrays.asList(
                        "Toast bread cubes lightly in an oven or skillet until crunchy on the outside.",
                        "Chop ripe tomatoes into bite-sized chunks and toss in a large bowl with salt to release juices.",
                        "Add toasted bread cubes, allowing them to soak up tomato juices.",
                        "Whisk olive oil and vinegar; pour over the salad and toss with fresh herbs.",
                        "Let stand 10 minutes before serving so flavors meld."
                ),
                "",
                "https://www.allrecipes.com/recipe/14352/panzanella/",
                true
        ));

        // 7. Creamy Spinach Omelette
        list.add(new Recipe(
                "local_7",
                "Folded Cheese & Spinach Omelette",
                "Breakfast",
                12,
                "Easy",
                Arrays.asList("Organic Eggs (3)", "Baby Spinach (1/2 cup)", "Cheddar Cheese (1/4 cup shredded)", "Butter (1 tbsp)", "Pinch of salt and black pepper"),
                Arrays.asList(
                        "Beat eggs with salt and pepper until thoroughly blended.",
                        "Melt butter in a 8-inch non-stick skillet over medium-low heat.",
                        "Pour in eggs; gently lift edges to let raw egg flow underneath.",
                        "When almost set, scatter spinach and cheese over half the omelette.",
                        "Fold over in half, slide onto plate, and serve warm."
                ),
                "",
                "https://www.allrecipes.com/recipe/13950/simple-spinach-omelet/",
                true
        ));

        // 8. Savory Tomato Egg Drop Soup
        list.add(new Recipe(
                "local_8",
                "Comforting Tomato Egg Soup",
                "Soup",
                15,
                "Easy",
                Arrays.asList("Ripe Tomatoes (2, diced)", "Organic Eggs (2, beaten)", "Water or broth (3 cups)", "Sesame oil (1 tsp)", "Cornstarch (1 tsp with water)", "Salt and pepper"),
                Arrays.asList(
                        "Sauté chopped tomatoes in a pot with a touch of oil until soft and pulpy.",
                        "Pour in 3 cups of broth or water and bring to a rolling boil.",
                        "Stir in cornstarch slurry to lightly thicken soup.",
                        "Slowly pour beaten eggs in a thin stream while gently stirring in one direction to create silky egg ribbons.",
                        "Drizzle sesame oil, season with salt, and serve."
                ),
                "",
                "https://www.allrecipes.com/recipe/42964/chinese-tomato-egg-soup/",
                true
        ));

        // 9. Crispy Chicken Fried Rice Bowl
        list.add(new Recipe(
                "local_9",
                "Chicken & Brown Rice Protein Bowl",
                "Dinner",
                30,
                "Medium",
                Arrays.asList("Chicken Breast (300g, cubed)", "Cooked Brown Rice (1.5 cups)", "Baby Spinach (1 cup)", "Olive Oil (2 tbsp)", "Garlic powder", "Soy sauce"),
                Arrays.asList(
                        "Season chicken cubes with garlic powder, salt, and black pepper.",
                        "Sauté chicken in olive oil over medium-high heat until golden and cooked through (6-8 minutes).",
                        "Add spinach to pan until wilted, then toss with cooked brown rice.",
                        "Drizzle 1 tablespoon soy sauce and stir everything together.",
                        "Portion into bowls and serve."
                ),
                "",
                "https://www.allrecipes.com/recipe/215447/chicken-rice-bowl/",
                true
        ));

        // 10. Overnight Oatmeal Jar
        list.add(new Recipe(
                "local_10",
                "No-Cook Overnight Oats",
                "Breakfast",
                5,
                "Easy",
                Arrays.asList("Rolled Oats (1/2 cup)", "Fresh Milk (1/2 cup)", "Greek Yogurt (1/4 cup)", "Honey or Maple Syrup (1 tbsp)", "Cinnamon (pinch)"),
                Arrays.asList(
                        "Combine rolled oats, milk, yogurt, and sweetener in a jar or airtight container.",
                        "Stir thoroughly until well combined.",
                        "Seal and refrigerate overnight (at least 6 hours).",
                        "In the morning, stir and enjoy cold or warmed up in the microwave."
                ),
                "",
                "https://www.allrecipes.com/recipe/244251/no-cook-overnight-oatmeal/",
                true
        ));

        // 11. Creamy Tomato Pasta
        list.add(new Recipe(
                "local_11",
                "Garlic Tomato Pan Pasta",
                "Dinner",
                20,
                "Easy",
                Arrays.asList("Pasta or Noodles (200g)", "Tomatoes (3, crushed)", "Garlic (2 cloves)", "Olive Oil (2 tbsp)", "Fresh Milk or Cream (1/4 cup)", "Cheese"),
                Arrays.asList(
                        "Boil pasta in salted water until al dente; reserve 1/2 cup pasta water.",
                        "In a skillet, sauté minced garlic in olive oil, then add crushed tomatoes and simmer 8 minutes.",
                        "Stir in milk or cream and a splash of pasta water to create a silky sauce.",
                        "Toss in cooked pasta and coat thoroughly.",
                        "Top with grated cheese and serve immediately."
                ),
                "",
                "https://www.allrecipes.com/recipe/269500/creamy-tomato-pasta/",
                true
        ));

        // 12. Green Power Smoothie
        list.add(new Recipe(
                "local_12",
                "Antioxidant Spinach Smoothie",
                "Beverage",
                5,
                "Easy",
                Arrays.asList("Baby Spinach (2 generous cups)", "Greek Yogurt (1/2 cup)", "Fresh Milk (1 cup)", "Honey (1 tbsp)", "Apple or banana slices"),
                Arrays.asList(
                        "Place milk and Greek yogurt at the bottom of the blender.",
                        "Add fresh baby spinach and fruit pieces.",
                        "Blend on high speed for 60-90 seconds until completely smooth.",
                        "Pour into a chilled glass and drink fresh."
                ),
                "",
                "https://www.allrecipes.com/recipe/241601/green-spinach-smoothie/",
                true
        ));

        return list;
    }

    /**
     * Finds recipes from the local repository that match any of the provided food names.
     */
    public static List<Recipe> suggestForIngredients(List<String> foodNames) {
        List<Recipe> all = getCuratedRecipes();
        if (foodNames == null || foodNames.isEmpty()) {
            return all;
        }

        List<Recipe> matched = new ArrayList<>();
        List<Recipe> others = new ArrayList<>();

        for (Recipe r : all) {
            boolean hasMatch = false;
            for (String food : foodNames) {
                if (r.matchesIngredient(food)) {
                    hasMatch = true;
                    break;
                }
            }
            if (hasMatch) {
                matched.add(r);
            } else {
                others.add(r);
            }
        }

        // Return matched recipes first, followed by others
        matched.addAll(others);
        return matched;
    }
}
