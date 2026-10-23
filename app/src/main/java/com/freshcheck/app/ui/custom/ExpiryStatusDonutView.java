package com.freshcheck.app.ui.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Custom View that renders a donut chart illustrating
 * food inventory health (Fresh vs Expiring Soon vs Expired).
 */
public class ExpiryStatusDonutView extends View {
    private Paint freshPaint;
    private Paint expiringPaint;
    private Paint expiredPaint;
    private Paint backgroundRingPaint;
    private Paint centerTextLargePaint;
    private Paint centerTextSmallPaint;

    private final RectF ovalRect = new RectF();

    private int freshCount = 0;
    private int expiringCount = 0;
    private int expiredCount = 0;

    public ExpiryStatusDonutView(Context context) {
        super(context);
        init();
    }

    public ExpiryStatusDonutView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ExpiryStatusDonutView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float strokeWidth = dpToPx(24);

        freshPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        freshPaint.setStyle(Paint.Style.STROKE);
        freshPaint.setStrokeWidth(strokeWidth);
        freshPaint.setColor(Color.parseColor("#43A047")); // Green
        freshPaint.setStrokeCap(Paint.Cap.BUTT);

        expiringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        expiringPaint.setStyle(Paint.Style.STROKE);
        expiringPaint.setStrokeWidth(strokeWidth);
        expiringPaint.setColor(Color.parseColor("#FB8C00")); // Orange
        expiringPaint.setStrokeCap(Paint.Cap.BUTT);

        expiredPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        expiredPaint.setStyle(Paint.Style.STROKE);
        expiredPaint.setStrokeWidth(strokeWidth);
        expiredPaint.setColor(Color.parseColor("#E53935")); // Red
        expiredPaint.setStrokeCap(Paint.Cap.BUTT);

        backgroundRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundRingPaint.setStyle(Paint.Style.STROKE);
        backgroundRingPaint.setStrokeWidth(strokeWidth);
        backgroundRingPaint.setColor(Color.parseColor("#E8EFE8"));

        centerTextLargePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerTextLargePaint.setColor(Color.parseColor("#1C261C"));
        centerTextLargePaint.setTextSize(spToPx(26));
        centerTextLargePaint.setTextAlign(Paint.Align.CENTER);
        centerTextLargePaint.setFakeBoldText(true);

        centerTextSmallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerTextSmallPaint.setColor(Color.parseColor("#596659"));
        centerTextSmallPaint.setTextSize(spToPx(13));
        centerTextSmallPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(int fresh, int expiring, int expired) {
        this.freshCount = Math.max(0, fresh);
        this.expiringCount = Math.max(0, expiring);
        this.expiredCount = Math.max(0, expired);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredSize = (int) dpToPx(170);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width = (widthMode == MeasureSpec.EXACTLY) ? widthSize : desiredSize;
        int height = (heightMode == MeasureSpec.EXACTLY) ? heightSize : desiredSize;

        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        float strokeWidth = dpToPx(20);
        freshPaint.setStrokeWidth(strokeWidth);
        expiringPaint.setStrokeWidth(strokeWidth);
        expiredPaint.setStrokeWidth(strokeWidth);
        backgroundRingPaint.setStrokeWidth(strokeWidth);

        float padding = strokeWidth / 2f + dpToPx(6);
        ovalRect.set(padding, padding, width - padding, height - padding);

        int total = freshCount + expiringCount + expiredCount;

        if (total == 0) {
            // Empty state ring
            canvas.drawArc(ovalRect, 0, 360, false, backgroundRingPaint);
            canvas.drawText("0", width / 2f, height / 2f - dpToPx(2), centerTextLargePaint);
            canvas.drawText("Items", width / 2f, height / 2f + dpToPx(18), centerTextSmallPaint);
            return;
        }

        // Base ring
        canvas.drawArc(ovalRect, 0, 360, false, backgroundRingPaint);

        float startAngle = -90f; // Start at top 12 o'clock

        // Fresh segment
        if (freshCount > 0) {
            float sweep = (freshCount / (float) total) * 360f;
            canvas.drawArc(ovalRect, startAngle, sweep, false, freshPaint);
            startAngle += sweep;
        }

        // Expiring Soon segment
        if (expiringCount > 0) {
            float sweep = (expiringCount / (float) total) * 360f;
            canvas.drawArc(ovalRect, startAngle, sweep, false, expiringPaint);
            startAngle += sweep;
        }

        // Expired segment
        if (expiredCount > 0) {
            float sweep = (expiredCount / (float) total) * 360f;
            canvas.drawArc(ovalRect, startAngle, sweep, false, expiredPaint);
        }

        // Center labels: Fresh % or Total Count
        int freshPercentage = (int) Math.round((freshCount / (float) total) * 100);
        canvas.drawText(freshPercentage + "%", width / 2f, height / 2f - dpToPx(2), centerTextLargePaint);
        canvas.drawText("Fresh", width / 2f, height / 2f + dpToPx(18), centerTextSmallPaint);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}
