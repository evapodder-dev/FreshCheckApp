package com.freshcheck.app.ui.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.freshcheck.app.R;

/**
 * Premium Custom View rendering a donut chart illustrating
 * food inventory freshness (Fresh vs Expiring Soon vs Expired).
 * Fully dynamic and theme-aware (Light & Dark themes).
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
        float strokeWidth = dpToPx(18);

        freshPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        freshPaint.setStyle(Paint.Style.STROKE);
        freshPaint.setStrokeWidth(strokeWidth);
        freshPaint.setColor(ContextCompat.getColor(getContext(), R.color.chart_fresh));
        freshPaint.setStrokeCap(Paint.Cap.ROUND);

        expiringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        expiringPaint.setStyle(Paint.Style.STROKE);
        expiringPaint.setStrokeWidth(strokeWidth);
        expiringPaint.setColor(ContextCompat.getColor(getContext(), R.color.chart_expiring));
        expiringPaint.setStrokeCap(Paint.Cap.ROUND);

        expiredPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        expiredPaint.setStyle(Paint.Style.STROKE);
        expiredPaint.setStrokeWidth(strokeWidth);
        expiredPaint.setColor(ContextCompat.getColor(getContext(), R.color.chart_expired));
        expiredPaint.setStrokeCap(Paint.Cap.ROUND);

        backgroundRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundRingPaint.setStyle(Paint.Style.STROKE);
        backgroundRingPaint.setStrokeWidth(strokeWidth);
        backgroundRingPaint.setColor(ContextCompat.getColor(getContext(), R.color.chart_track));
        backgroundRingPaint.setStrokeCap(Paint.Cap.ROUND);

        centerTextLargePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerTextLargePaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        centerTextLargePaint.setTextSize(spToPx(28));
        centerTextLargePaint.setTextAlign(Paint.Align.CENTER);
        centerTextLargePaint.setFakeBoldText(true);

        centerTextSmallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerTextSmallPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        centerTextSmallPaint.setTextSize(spToPx(13));
        centerTextSmallPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(int fresh, int expiring, int expired) {
        this.freshCount = Math.max(0, fresh);
        this.expiringCount = Math.max(0, expiring);
        this.expiredCount = Math.max(0, expired);
        updateColors();
        invalidate();
    }

    private void updateColors() {
        if (freshPaint != null) {
            freshPaint.setColor(ContextCompat.getColor(getContext(), R.color.chart_fresh));
            expiringPaint.setColor(ContextCompat.getColor(getContext(), R.color.chart_expiring));
            expiredPaint.setColor(ContextCompat.getColor(getContext(), R.color.chart_expired));
            backgroundRingPaint.setColor(ContextCompat.getColor(getContext(), R.color.chart_track));
            centerTextLargePaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
            centerTextSmallPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredSize = (int) dpToPx(160);

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

        float strokeWidth = dpToPx(18);
        freshPaint.setStrokeWidth(strokeWidth);
        expiringPaint.setStrokeWidth(strokeWidth);
        expiredPaint.setStrokeWidth(strokeWidth);
        backgroundRingPaint.setStrokeWidth(strokeWidth);

        float padding = strokeWidth / 2f + dpToPx(4);
        ovalRect.set(padding, padding, width - padding, height - padding);

        int total = freshCount + expiringCount + expiredCount;

        // Draw track ring
        canvas.drawArc(ovalRect, 0, 360, false, backgroundRingPaint);

        if (total == 0) {
            canvas.drawText("0%", width / 2f, height / 2f - dpToPx(1), centerTextLargePaint);
            canvas.drawText("Freshness", width / 2f, height / 2f + dpToPx(18), centerTextSmallPaint);
            return;
        }

        float startAngle = -90f; // 12 o'clock

        // Expired segment
        if (expiredCount > 0) {
            float sweep = (expiredCount / (float) total) * 360f;
            canvas.drawArc(ovalRect, startAngle, sweep, false, expiredPaint);
            startAngle += sweep;
        }

        // Expiring Soon segment
        if (expiringCount > 0) {
            float sweep = (expiringCount / (float) total) * 360f;
            canvas.drawArc(ovalRect, startAngle, sweep, false, expiringPaint);
            startAngle += sweep;
        }

        // Fresh segment
        if (freshCount > 0) {
            float sweep = (freshCount / (float) total) * 360f;
            canvas.drawArc(ovalRect, startAngle, sweep, false, freshPaint);
        }

        // Center typography: Freshness %
        int freshPercentage = (int) Math.round((freshCount / (float) total) * 100);
        canvas.drawText(freshPercentage + "%", width / 2f, height / 2f - dpToPx(1), centerTextLargePaint);
        canvas.drawText("Freshness", width / 2f, height / 2f + dpToPx(18), centerTextSmallPaint);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}
