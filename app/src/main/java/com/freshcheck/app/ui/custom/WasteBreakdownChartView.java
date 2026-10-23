package com.freshcheck.app.ui.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Custom View that visualizes financial food waste breakdown
 * across categories (Spoiled, Past Expiry, Overcooked, Leftovers, Other).
 */
public class WasteBreakdownChartView extends View {
    private Paint barBackgroundPaint;
    private Paint barFillPaint;
    private Paint labelPaint;
    private Paint valuePaint;
    private Paint emptyTextPaint;

    private final RectF barBgRect = new RectF();
    private final RectF barFillRect = new RectF();

    private final Map<String, Float> costByReason = new HashMap<>();
    private final Map<String, Integer> colorMap = new HashMap<>();

    public WasteBreakdownChartView(Context context) {
        super(context);
        init();
    }

    public WasteBreakdownChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WasteBreakdownChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        barBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barBackgroundPaint.setColor(Color.parseColor("#EAEFEA"));

        barFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.parseColor("#1C261C"));
        labelPaint.setTextSize(spToPx(13));
        labelPaint.setFakeBoldText(true);

        valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(Color.parseColor("#596659"));
        valuePaint.setTextSize(spToPx(12));

        emptyTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        emptyTextPaint.setColor(Color.parseColor("#757575"));
        emptyTextPaint.setTextSize(spToPx(14));
        emptyTextPaint.setTextAlign(Paint.Align.CENTER);

        // Assign recognizable colors per waste reason
        colorMap.put("Spoiled / Moldy", Color.parseColor("#E53935"));      // Red
        colorMap.put("Past Expiry Date", Color.parseColor("#FB8C00"));     // Orange
        colorMap.put("Overcooked / Burned", Color.parseColor("#8E24AA"));  // Purple
        colorMap.put("Uneaten Leftover", Color.parseColor("#039BE5"));     // Blue
        colorMap.put("Other / Accidental", Color.parseColor("#78909C"));   // Slate Gray
    }

    public void setData(Map<String, Float> data) {
        costByReason.clear();
        if (data != null) {
            costByReason.putAll(data);
        }
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int rows = Math.max(1, costByReason.size());
        int rowHeight = (int) dpToPx(48);
        int desiredHeight = (costByReason.isEmpty()) ? (int) dpToPx(110) : (rows * rowHeight + (int) dpToPx(16));

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int height = (heightMode == MeasureSpec.EXACTLY) ? heightSize : desiredHeight;
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        if (costByReason.isEmpty()) {
            canvas.drawText("No food waste recorded. Great job!", width / 2f, height / 2f, emptyTextPaint);
            return;
        }

        // Calculate total cost
        float totalCost = 0f;
        for (Float val : costByReason.values()) {
            if (val != null) totalCost += val;
        }
        if (totalCost <= 0.001f) totalCost = 1f;

        float paddingLeft = dpToPx(12);
        float paddingRight = dpToPx(12);
        float usableWidth = width - paddingLeft - paddingRight;

        float currentY = dpToPx(18);
        float barHeight = dpToPx(10);
        float cornerRadius = dpToPx(5);

        for (Map.Entry<String, Float> entry : costByReason.entrySet()) {
            String reason = entry.getKey();
            float cost = entry.getValue() != null ? entry.getValue() : 0f;
            float ratio = Math.min(1.0f, cost / totalCost);
            int percentage = Math.round(ratio * 100);

            // Draw reason label on left
            canvas.drawText(reason, paddingLeft, currentY, labelPaint);

            // Draw cost & percentage text on right
            String valueText = String.format(Locale.US, "$%.2f (%d%%)", cost, percentage);
            float valueWidth = valuePaint.measureText(valueText);
            canvas.drawText(valueText, width - paddingRight - valueWidth, currentY, valuePaint);

            // Draw bar
            float barY = currentY + dpToPx(6);
            barBgRect.set(paddingLeft, barY, paddingLeft + usableWidth, barY + barHeight);
            canvas.drawRoundRect(barBgRect, cornerRadius, cornerRadius, barBackgroundPaint);

            // Draw filled bar
            float fillWidth = Math.max(dpToPx(6), usableWidth * ratio);
            barFillRect.set(paddingLeft, barY, paddingLeft + fillWidth, barY + barHeight);

            Integer barColor = colorMap.get(reason);
            if (barColor == null) barColor = Color.parseColor("#4CAF50");
            barFillPaint.setColor(barColor);
            canvas.drawRoundRect(barFillRect, cornerRadius, cornerRadius, barFillPaint);

            currentY += dpToPx(48);
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}
