package com.freshcheck.app.ui.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
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
    private String detectedCategory = "Others";
    private Uri selectedImageUri = null;

    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), new ActivityResultCallback<Bitmap>() {
                @Override
                public void onActivityResult(Bitmap bitmap) {
                    if (bitmap != null) {
                        ivPreview.setImageBitmap(bitmap);
                        layoutPlaceholder.setVisibility(View.GONE);
                        runImageRecognition(bitmap);
                    }
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    if (uri != null) {
                        selectedImageUri = uri;
                        try {
                            Bitmap bitmap;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri));
                            } else {
                                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            }
                            ivPreview.setImageBitmap(bitmap);
                            layoutPlaceholder.setVisibility(View.GONE);
                            runImageRecognition(bitmap);
                        } catch (Exception e) {
                            Log.e(TAG, "Error loading gallery image: " + e.getMessage());
                            Toast.makeText(FoodScannerActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
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
                try {
                    cameraLauncher.launch(null);
                } catch (Exception e) {
                    Toast.makeText(FoodScannerActivity.this, "Camera not available: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.btn_pick_gallery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                galleryLauncher.launch("image/*");
            }
        });

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

    private void runImageRecognition(final Bitmap bitmap) {
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
                            Log.w(TAG, "ML Kit labeler notice: " + e.getMessage());
                            fallbackClassification();
                        }
                    });
        } catch (Exception e) {
            pbScanning.setVisibility(View.GONE);
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
        detectedName = "Fresh Produce / Food Item";
        detectedCategory = "Vegetables";
        tvName.setText("Item: " + detectedName);
        tvCategory.setText("Category: " + detectedCategory);
        tvConfidence.setText("Classification: Ready for manual confirmation");
        cardResults.setVisibility(View.VISIBLE);
    }

    private boolean isFoodKeyword(String text) {
        String[] keywords = {"apple", "banana", "orange", "fruit", "vegetable", "tomato", "potato",
                "milk", "dairy", "cheese", "bread", "bakery", "meat", "chicken", "fish", "egg", "rice",
                "carrot", "salad", "food", "beverage", "snack"};
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