package com.freshcheck.app.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.freshcheck.app.R;
import com.freshcheck.app.util.CameraImageHelper;
import com.freshcheck.app.util.FoodClassifierHelper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.Arrays;
import java.util.List;

public class FoodScannerActivity extends AppCompatActivity {
    private static final String TAG = "FoodScannerActivity";

    private static final String[] CATEGORIES = {
            "Vegetables", "Fruits", "Dairy", "Meat", "Fish", "Bakery", "Grains", "Beverages", "Snacks", "Others"
    };

    private ImageView ivPreview;
    private LinearLayout layoutPlaceholder;
    private ProgressBar pbScanning;
    private MaterialCardView cardResults;
    private TextView tvConfidence;
    private TextInputEditText etDetectedName;
    private TextView tvAlternativesLabel;
    private ChipGroup chipGroupCandidates;
    private ChipGroup chipGroupCategory;

    private String detectedName = "Fresh Food Item";
    private String detectedCategory = "Vegetables";
    private Uri selectedImageUri = null;
    private Uri tempCameraUri = null;

    // Camera Permission Launcher
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    if (Boolean.TRUE.equals(isGranted)) {
                        launchCamera();
                    } else {
                        Toast.makeText(FoodScannerActivity.this,
                                "Camera permission is required to capture photos. You can also pick from Gallery or use sample items.",
                                Toast.LENGTH_LONG).show();
                    }
                }
            });

    // High-Resolution Camera Capture Launcher via FileProvider
    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean success) {
                    if (Boolean.TRUE.equals(success) && tempCameraUri != null) {
                        selectedImageUri = tempCameraUri;
                        Bitmap bitmap = CameraImageHelper.loadOptimizedBitmap(FoodScannerActivity.this, tempCameraUri, 1280);
                        if (bitmap != null) {
                            ivPreview.setImageBitmap(bitmap);
                            layoutPlaceholder.setVisibility(View.GONE);
                            runDualMlKitRecognition(bitmap);
                        } else {
                            Toast.makeText(FoodScannerActivity.this,
                                    "Unable to load captured photo. Please try again.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(FoodScannerActivity.this,
                                "Photo capture cancelled or not saved.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });

    // Gallery Picker Launcher
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    if (uri != null) {
                        selectedImageUri = uri;
                        Bitmap bitmap = CameraImageHelper.loadOptimizedBitmap(FoodScannerActivity.this, uri, 1280);
                        if (bitmap != null) {
                            ivPreview.setImageBitmap(bitmap);
                            layoutPlaceholder.setVisibility(View.GONE);
                            runDualMlKitRecognition(bitmap);
                        } else {
                            Toast.makeText(FoodScannerActivity.this,
                                    "Failed to load image from gallery.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_scanner);

        Toolbar toolbar = findViewById(R.id.toolbar_scanner);
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

        ivPreview = findViewById(R.id.iv_food_preview);
        layoutPlaceholder = findViewById(R.id.layout_placeholder);
        pbScanning = findViewById(R.id.pb_scanning);
        cardResults = findViewById(R.id.card_detected_results);
        tvConfidence = findViewById(R.id.tv_detected_confidence);
        etDetectedName = findViewById(R.id.et_detected_name);
        tvAlternativesLabel = findViewById(R.id.tv_alternatives_label);
        chipGroupCandidates = findViewById(R.id.chip_group_candidates);
        chipGroupCategory = findViewById(R.id.chip_group_category);

        findViewById(R.id.btn_take_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndLaunchCamera();
            }
        });

        findViewById(R.id.btn_pick_gallery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                galleryLauncher.launch("image/*");
            }
        });

        // Sample food testing buttons
        setupSampleButton(R.id.btn_sample_apple, "Green Apple", "Fruits", "#4CAF50", "🍎");
        setupSampleButton(R.id.btn_sample_milk, "Organic Milk", "Dairy", "#2196F3", "🥛");
        setupSampleButton(R.id.btn_sample_bread, "Sourdough Bread", "Bakery", "#FF9800", "🍞");
        setupSampleButton(R.id.btn_sample_chicken, "Chicken Breast", "Meat", "#E91E63", "🍗");
        setupSampleButton(R.id.btn_sample_tomato, "Fresh Tomato", "Vegetables", "#F44336", "🍅");

        findViewById(R.id.btn_add_to_inventory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String finalName = etDetectedName.getText() != null ? etDetectedName.getText().toString().trim() : "";
                if (finalName.isEmpty()) {
                    finalName = detectedName;
                }

                Intent intent = new Intent(FoodScannerActivity.this, AddEditFoodActivity.class);
                intent.putExtra("prefill_name", finalName);
                intent.putExtra("prefill_category", detectedCategory);
                if (selectedImageUri != null) {
                    intent.putExtra("prefill_image_uri", selectedImageUri.toString());
                }
                startActivity(intent);
                finish();
            }
        });

        // Initialize category chips
        setupCategoryChips(detectedCategory);
    }

    private void checkAndLaunchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            tempCameraUri = CameraImageHelper.createTempImageUri(this);
            if (tempCameraUri != null) {
                cameraLauncher.launch(tempCameraUri);
            } else {
                Toast.makeText(this, "Could not initialize camera storage.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Camera launch error: " + e.getMessage());
            Toast.makeText(this, "Camera not available: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupSampleButton(int buttonId, final String foodName, final String category, final String colorHex, final String emoji) {
        View btn = findViewById(buttonId);
        if (btn == null) return;
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap sampleBitmap = createSampleBitmap(emoji, Color.parseColor(colorHex));
                ivPreview.setImageBitmap(sampleBitmap);
                layoutPlaceholder.setVisibility(View.GONE);

                FoodClassifierHelper.ClassificationResult sampleResult = new FoodClassifierHelper.ClassificationResult(
                        foodName,
                        category,
                        98,
                        Arrays.asList(foodName, category, "Organic Produce")
                );
                displayClassificationResult(sampleResult);
                Toast.makeText(FoodScannerActivity.this, "Recognized " + foodName + "!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Bitmap createSampleBitmap(String emoji, int bgColor) {
        int size = 400;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(bgColor);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(120f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);

        float yPos = (size / 2f) - ((paint.descent() + paint.ascent()) / 2f);
        canvas.drawText(emoji, size / 2f, yPos, paint);
        return bitmap;
    }

    /**
     * Executes both ML Kit Image Labeling and ML Kit Text Recognition in parallel
     * to accurately identify unpacked produce as well as packaged branded groceries.
     */
    private void runDualMlKitRecognition(final Bitmap bitmap) {
        if (bitmap == null) return;
        pbScanning.setVisibility(View.VISIBLE);
        cardResults.setVisibility(View.GONE);

        try {
            final InputImage image = InputImage.fromBitmap(bitmap, 0);
            final ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);
            final TextRecognizer textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            labeler.process(image)
                    .addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
                        @Override
                        public void onSuccess(final List<ImageLabel> labels) {
                            textRecognizer.process(image)
                                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                                        @Override
                                        public void onSuccess(Text ocrText) {
                                            pbScanning.setVisibility(View.GONE);
                                            FoodClassifierHelper.ClassificationResult result =
                                                    FoodClassifierHelper.classify(labels, ocrText);
                                            displayClassificationResult(result);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            pbScanning.setVisibility(View.GONE);
                                            FoodClassifierHelper.ClassificationResult result =
                                                    FoodClassifierHelper.classify(labels, null);
                                            displayClassificationResult(result);
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Try OCR even if labeling fails
                            textRecognizer.process(image)
                                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                                        @Override
                                        public void onSuccess(Text ocrText) {
                                            pbScanning.setVisibility(View.GONE);
                                            FoodClassifierHelper.ClassificationResult result =
                                                    FoodClassifierHelper.classify(null, ocrText);
                                            displayClassificationResult(result);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception ex) {
                                            pbScanning.setVisibility(View.GONE);
                                            FoodClassifierHelper.ClassificationResult fallback =
                                                    FoodClassifierHelper.classify(null, null);
                                            displayClassificationResult(fallback);
                                        }
                                    });
                        }
                    });
        } catch (Exception e) {
            pbScanning.setVisibility(View.GONE);
            Log.e(TAG, "Recognition initiation error: " + e.getMessage());
            FoodClassifierHelper.ClassificationResult fallback = FoodClassifierHelper.classify(null, null);
            displayClassificationResult(fallback);
        }
    }

    private void displayClassificationResult(FoodClassifierHelper.ClassificationResult result) {
        if (result == null) return;

        detectedName = result.foodName;
        detectedCategory = result.category;

        etDetectedName.setText(detectedName);
        tvConfidence.setText(result.confidence + "% Confidence");

        setupCategoryChips(detectedCategory);
        setupCandidateChips(result.candidateAlternatives);

        cardResults.setVisibility(View.VISIBLE);
    }

    private void setupCategoryChips(String currentCategory) {
        if (chipGroupCategory == null) return;
        chipGroupCategory.removeAllViews();

        for (final String cat : CATEGORIES) {
            Chip chip = new Chip(this);
            chip.setText(cat);
            chip.setCheckable(true);
            chip.setClickable(true);
            if (cat.equalsIgnoreCase(currentCategory)) {
                chip.setChecked(true);
            }

            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    detectedCategory = cat;
                }
            });

            chipGroupCategory.addView(chip);
        }
    }

    private void setupCandidateChips(List<String> candidates) {
        if (chipGroupCandidates == null) return;
        chipGroupCandidates.removeAllViews();

        if (candidates == null || candidates.isEmpty()) {
            if (tvAlternativesLabel != null) tvAlternativesLabel.setVisibility(View.GONE);
            chipGroupCandidates.setVisibility(View.GONE);
            return;
        }

        if (tvAlternativesLabel != null) tvAlternativesLabel.setVisibility(View.VISIBLE);
        chipGroupCandidates.setVisibility(View.VISIBLE);

        for (final String altName : candidates) {
            Chip chip = new Chip(this);
            chip.setText(altName);
            chip.setCheckable(false);
            chip.setClickable(true);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    detectedName = altName;
                    etDetectedName.setText(altName);
                    detectedCategory = FoodClassifierHelper.inferCategory(altName);
                    setupCategoryChips(detectedCategory);
                    Toast.makeText(FoodScannerActivity.this, "Selected " + altName, Toast.LENGTH_SHORT).show();
                }
            });

            chipGroupCandidates.addView(chip);
        }
    }
}
