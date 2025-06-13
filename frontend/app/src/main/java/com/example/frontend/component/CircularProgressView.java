package com.example.frontend.component;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;

import com.example.frontend.R;

public class CircularProgressView extends View {

    private Paint backgroundPaint;
    private Paint progressPaint;
    private Paint textPaint;
    private RectF rectF;
    private float progress = 0; // en %

    public CircularProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.LTGRAY);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(20);
        backgroundPaint.setAntiAlias(true);

        progressPaint = new Paint();
        progressPaint.setColor(Color.GREEN);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(20);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(48); // Taille du texte
        textPaint.setAntiAlias(true);
        Typeface typeface = ResourcesCompat.getFont(getContext(), R.font.life_savers);
        textPaint.setTypeface(typeface);
        textPaint.setTextAlign(Paint.Align.CENTER);

        rectF = new RectF();
    }

    public void setProgress(float progress) {
        this.progress = progress;
        invalidate(); // Redessine la vue
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int padding = 20;

        rectF.set(padding, padding, width - padding, height - padding);

        // cercle de fond (360°)
        canvas.drawArc(rectF, 0, 360, false, backgroundPaint);

        // arc de progression (basé sur pourcentage)
        float angle = (progress / 100) * 360;
        canvas.drawArc(rectF, -90, angle, false, progressPaint);

        // Dessiner le texte au centre
        String percentageText = Math.round(progress) + "%";

        // Calculer la position verticale pour centrer le texte
        Rect textBounds = new Rect();
        textPaint.getTextBounds(percentageText, 0, percentageText.length(), textBounds);
        float x = width / 2f;
        float y = height / 2f - textBounds.exactCenterY();

        canvas.drawText(percentageText, x, y, textPaint);
    }
}
