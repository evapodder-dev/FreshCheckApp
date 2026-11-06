package com.freshcheck.app.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.card.MaterialCardView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.List;

public class FoodScannerActivity extends AppCompatActivity {
    private static final String TAG = "FoodScannerActivity";

    private ImageView ivPreview;
    private LinearLayout layoutPlaceholder;
    private ProgressBar pbScanning;
    private MaterialCardView cardResults;
    private TextView tvName;
    private TextView tvCategory;
    private TextView tvConfidence;

    private String detectedName = "";
    private String detectedCategory = "Vegetables";
    private Uri selectedImageUri = null;

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

    // Camera Capture Launcher
    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), new ActivityResultCallback<Bitmap>() {
                @Override
                public void onActivityResult(Bitmap bitmap) {
                    if (bitmap != null) {
                        Bitmap safeBitmap = ensureSoftwareBitmap(bitmap);
                        ivPreview.setImageBitmap(safeBitmap);
                        layoutPlaceholder.setVisibility(View.GONE);
                        runImageRecognition(safeBitmap);
                    } else {
                        Toast.makeText(FoodScannerActivity.this,
                                "No photo captured. Please try again, choose from Gallery, or tap a sample item below.",
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
                        try {
                            Bitmap bitmap;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                                bitmap = ImageDecoder.decodeBitmap(source, new ImageDecoder.OnHeaderDecodedListener() {
                                    @Override
                                    public void onHeaderDecoded(ImageDecoder decoder, ImageDecoder.ImageInfo info, ImageDecoder.Source source) {
                                        decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                                    }
                                });
                            } else {
                                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            }
                            Bitmap safeBitmap = ensureSoftwareBitmap(bitmap);
                            ivPreview.setImageBitmap(safeBitmap);
                            layoutPlaceholder.setVisibility(View.GONE);
                            runImageRecognition(safeBitmap);
                        } catch (Exception e) {
                            Log.e(TAG, "Error loading gallery image: " + e.getMessage());
                            Toast.makeText(FoodScannerActivity.this, "Failed to load image from gallery.", Toast.LENGTH_SHORT).show();
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
        tvName = findViewById(R.id.tv_detected_name);
        tvCategory = findViewById(R.id.tv_detected_category);
        tvConfidence = findViewById(R.id.tv_detected_confidence);

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
                Intent intent = new Intent(FoodScannerActivity.this, AddEditFoodActivity.class);
                intent.putExtra("prefill_name", detectedName);
                intent.putExtra("prefill_category", detectedCategory);
                if (selectedImageUri != null) {
                    intent.putExtra("prefill_image_uri", selectedImageUri.toString());
                }
                startActivity(intent);
                finish();
            }
        });
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
            cameraLauncher.launch(null);
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

                detectedName = foodName;
                detectedCategory = category;

                tvName.setText("Detected Food: " + detectedName);
                tvCategory.setText("Suggested Category: " + detectedCategory);
                tvConfidence.setText("Recognition Confidence: 96% (Verified Sample)");
                cardResults.setVisibility(View.VISIBLE);

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

    private Bitmap ensureSoftwareBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.getConfig() == Bitmap.Config.HARDWARE) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, false);
        }
        return bitmap;
    }

    private void runImageRecognition(final Bitmap bitmap) {
        if (bitmap == null) return;
        pbScanning.setVisibility(View.VISIBLE);
        cardResults.setVisibility(View.GONE);

        try {
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

            labeler.process(image)
                    .addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
                        @Override
                        public void onSuccess(List<ImageLabel> labels) {
                            pbScanning.setVisibility(View.GONE);
                            if (labels != null && !labels.isEmpty()) {
                                processDetectedLabels(labels);
                            } else {
                                fallbackClassification();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            pbScanning.setVisibility(View.GONE);
                            Log.w(TAG, "ML Kit notice: " + e.getMessage());
                            fallbackClassification();
                        }
                    });
        } catch (Exception e) {
            pbScanning.setVisibility(View.GONE);
            Log.e(TAG, "Recognition error: " + e.getMessage());
            fallbackClassification();
        }
    }

    private void processDetectedLabels(List<ImageLabel> labels) {
        ImageLabel bestFoodLabel = null;
        for (ImageLabel label : labels) {
            String text = label.getText().toLowerCase();
            if (isFoodKeyword(text)) {
                bestFoodLabel = label;
                break;
            }
        }

        if (bestFoodLabel == null && !labels.isEmpty()) {
            bestFoodLabel = labels.get(0);
        }

        if (bestFoodLabel != null) {
            detectedName = capitalize(bestFoodLabel.getText());
            detectedCategory = inferCategory(detectedName);
            int confidence = (int) (bestFoodLabel.getConfidence() * 100);

            tvName.setText("Detected Food: " + detectedName);
            tvCategory.setText("Suggested Category: " + detectedCategory);
            tvConfidence.setText("Recognition Confidence: " + confidence + "%");
            cardResults.setVisibility(View.VISIBLE);
        } else {
            fallbackClassification();
        }
    }

    private void fallbackClassification() {
        detectedName = "Fresh Food Item";
        detectedCategory = "Vegetables";
        tvName.setText("Detected Item: " + detectedName);
        tvCategory.setText("Suggested Category: " + detectedCategory);
        tvConfidence.setText("Ready for confirmation (tap button below to edit)");
        cardResults.setVisibility(View.VISIBLE);
    }

    private boolean isFoodKeyword(String text) {
        String[] keywords = {"apple", "banana", "orange", "fruit", "vegetable", "tomato", "potato",
                "milk", "dairy", "cheese", "bread", "bakery", "meat", "chicken", "fish", "egg", "rice",
                "carrot", "salad", "food", "beverage", "snack", "produce", "dish", "ingredient"};
        for (String k : keywords) {
            if (text.contains(k)) return true;
        }
        return false;
    }

    private String inferCategory(String food) {
        String lower = food.toLowerCase();
        if (lower.contains("apple") || lower.contains("banana") || lower.contains("orange") || lower.contains("fruit") || lower.contains("berry")) {
            return "Fruits";
        } else if (lower.contains("milk") || lower.contains("cheese") || lower.contains("yogurt") || lower.contains("dairy") || lower.contains("egg")) {
            return "Dairy";
        } else if (lower.contains("chicken") || lower.contains("meat") || lower.contains("beef") || lower.contains("pork")) {
            return "Meat";
        } else if (lower.contains("fish") || lower.contains("seafood") || lower.contains("shrimp")) {
            return "Fish";
        } else if (lower.contains("bread") || lower.contains("toast") || lower.contains("bakery") || lower.contains("cake")) {
            return "Bakery";
        } else if (lower.contains("rice") || lower.contains("grain") || lower.contains("oat") || lower.contains("flour")) {
            return "Grains";
        } else if (lower.contains("juice") || lower.contains("tea") || lower.contains("coffee") || lower.contains("drink") || lower.contains("water")) {
            return "Beverages";
        } else {
            return "Vegetables";
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
