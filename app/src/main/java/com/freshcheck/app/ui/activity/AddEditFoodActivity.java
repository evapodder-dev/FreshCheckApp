package com.freshcheck.app.ui.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.freshcheck.app.R;
import com.freshcheck.app.database.FoodRepository;
import com.freshcheck.app.database.HouseholdRepository;
import com.freshcheck.app.model.CategoryConstants;
import com.freshcheck.app.model.FoodItem;
import com.freshcheck.app.util.DateUtils;
import com.google.android.material.button.MaterialButton;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddEditFoodActivity extends AppCompatActivity {
    private static final String TAG = "AddEditFood_Lifecycle";
    public static final String EXTRA_FOOD_ID = "extra_food_id";

    private EditText etName, etQuantity, etCost, etNotes;
    private Spinner spUnit, spCategory, spLocation;
    private TextView tvPurchaseDate, tvExpiryDate;
    private MaterialButton btnSave;

    private FoodRepository repository;
    private HouseholdRepository householdRepository;
    private long foodId = -1;
    private FoodItem editingItem;

    private String selectedPurchaseDate;
    private String selectedExpiryDate;
    private String prefillImageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() called");
        setContentView(R.layout.activity_add_edit_food);

        repository = new FoodRepository(this);
        householdRepository = new HouseholdRepository(this);

        Toolbar toolbar = findViewById(R.id.toolbar_add_edit);
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

        initViews();
        setupSpinners();
        setupDatePickers();

        if (getIntent().hasExtra(EXTRA_FOOD_ID)) {
            foodId = getIntent().getLongExtra(EXTRA_FOOD_ID, -1);
            if (foodId != -1) {
                editingItem = repository.getById(foodId);
                if (editingItem != null) {
                    populateFields(editingItem);
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle(R.string.title_edit_food);
                    }
                    btnSave.setText(R.string.btn_update);
                }
            }
        } else {
            // Check for prefill data from Food Scanner
            if (getIntent().hasExtra("prefill_name")) {
                String name = getIntent().getStringExtra("prefill_name");
                etName.setText(name);
            }
            if (getIntent().hasExtra("prefill_category")) {
                String cat = getIntent().getStringExtra("prefill_category");
                setSpinnerSelection(spCategory, CategoryConstants.CATEGORIES, cat);
            }
            if (getIntent().hasExtra("prefill_image_uri")) {
                prefillImageUri = getIntent().getStringExtra("prefill_image_uri");
            }
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFoodItem();
            }
        });
    }

    private void initViews() {
        etName = findViewById(R.id.et_input_food_name);
        etQuantity = findViewById(R.id.et_input_quantity);
        etCost = findViewById(R.id.et_input_cost);
        etNotes = findViewById(R.id.et_input_notes);
        spUnit = findViewById(R.id.spinner_input_unit);
        spCategory = findViewById(R.id.spinner_input_category);
        spLocation = findViewById(R.id.spinner_input_location);
        tvPurchaseDate = findViewById(R.id.tv_btn_purchase_date);
        tvExpiryDate = findViewById(R.id.tv_btn_expiry_date);
        btnSave = findViewById(R.id.btn_save_food_item);

        selectedPurchaseDate = DateUtils.getTodayString();
        selectedExpiryDate = DateUtils.getFutureDateString(4);

        tvPurchaseDate.setText(selectedPurchaseDate);
        tvExpiryDate.setText(selectedExpiryDate);
    }

    private void setupSpinners() {
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, CategoryConstants.UNITS);
        spUnit.setAdapter(unitAdapter);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, CategoryConstants.CATEGORIES);
        spCategory.setAdapter(catAdapter);

        ArrayAdapter<String> locAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, CategoryConstants.STORAGE_LOCATIONS);
        spLocation.setAdapter(locAdapter);
    }

    private void setupDatePickers() {
        tvPurchaseDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(selectedPurchaseDate, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        Calendar cal = Calendar.getInstance();
                        cal.set(year, month, dayOfMonth);
                        selectedPurchaseDate = DateUtils.getDateFormat().format(cal.getTime());
                        tvPurchaseDate.setText(selectedPurchaseDate);
                    }
                });
            }
        });

        tvExpiryDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(selectedExpiryDate, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        Calendar cal = Calendar.getInstance();
                        cal.set(year, month, dayOfMonth);
                        selectedExpiryDate = DateUtils.getDateFormat().format(cal.getTime());
                        tvExpiryDate.setText(selectedExpiryDate);
                    }
                });
            }
        });
    }

    private void showDatePicker(String currentDateStr, DatePickerDialog.OnDateSetListener listener) {
        Date date = DateUtils.parseDate(currentDateStr);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        new DatePickerDialog(this, listener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void populateFields(FoodItem item) {
        etName.setText(item.getName());
        etQuantity.setText(String.valueOf(item.getQuantity()));
        etCost.setText(String.format(Locale.US, "%.2f", item.getEstimatedCost()));
        etNotes.setText(item.getNotes());

        selectedPurchaseDate = item.getPurchaseDate();
        selectedExpiryDate = item.getExpiryDate();
        tvPurchaseDate.setText(selectedPurchaseDate);
        tvExpiryDate.setText(selectedExpiryDate);

        setSpinnerSelection(spUnit, CategoryConstants.UNITS, item.getUnit());
        setSpinnerSelection(spCategory, CategoryConstants.CATEGORIES, item.getCategory());
        setSpinnerSelection(spLocation, CategoryConstants.STORAGE_LOCATIONS, item.getLocation());
    }

    private void setSpinnerSelection(Spinner spinner, String[] array, String value) {
        if (value == null) return;
        int index = Arrays.asList(array).indexOf(value);
        if (index >= 0) {
            spinner.setSelection(index);
        }
    }

    private void saveFoodItem() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            etName.setError("Food name is required");
            return;
        }

        double qty = 1.0;
        try {
            qty = Double.parseDouble(etQuantity.getText().toString().trim());
        } catch (Exception ignored) {}

        double cost = 0.0;
        try {
            cost = Double.parseDouble(etCost.getText().toString().trim());
        } catch (Exception ignored) {}

        String unit = spUnit.getSelectedItem().toString();
        String category = spCategory.getSelectedItem().toString();
        String location = spLocation.getSelectedItem().toString();
        String notes = etNotes.getText().toString().trim();

        if (editingItem == null) {
            FoodItem newItem = new FoodItem(0, name, qty, unit, category,
                    selectedPurchaseDate, selectedExpiryDate, location, cost,
                    false, false, notes, null);
            if (prefillImageUri != null) {
                newItem.setImageUri(prefillImageUri);
            }
            String activeHouseholdId = householdRepository.getActiveHouseholdId();
            if (activeHouseholdId != null && !activeHouseholdId.isEmpty()) {
                newItem.setHouseholdId(activeHouseholdId);
            }
            repository.insert(newItem);
            Toast.makeText(this, "Added " + name + " to inventory (৳" + String.format(Locale.US, "%.1f", cost) + ")", Toast.LENGTH_SHORT).show();
        } else {
            editingItem.setName(name);
            editingItem.setQuantity(qty);
            editingItem.setUnit(unit);
            editingItem.setCategory(category);
            editingItem.setPurchaseDate(selectedPurchaseDate);
            editingItem.setExpiryDate(selectedExpiryDate);
            editingItem.setLocation(location);
            editingItem.setEstimatedCost(cost);
            editingItem.setNotes(notes);
            repository.update(editingItem);
            Toast.makeText(this, "Updated " + name, Toast.LENGTH_SHORT).show();
        }

        finish();
    }
}
