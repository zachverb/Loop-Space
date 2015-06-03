package com.capstone.zacharyverbeck.loopspace.Java;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ImageButton;

import java.util.Arrays;

import static com.capstone.zacharyverbeck.loopspace.R.attr;

/**
 * Created by zacharyverbeck on 4/24/15.
 */

public class LoopButton extends ImageButton {

    private static final int PRESSED_COLOR_LIGHTUP = 255 / 25;
    private static final int PRESSED_RING_ALPHA = 75;
    private static final int DEFAULT_PRESSED_RING_WIDTH_DIP = 4;
    private static final int ANIMATION_TIME_ID = android.R.integer.config_shortAnimTime;

    private int centerY;
    private int centerX;
    private int outerRadius;
    private int pressedRingRadius;

    private Paint circlePaint;
    private Paint focusPaint;

    private float animationProgress;

    private int pressedRingWidth;
    private int defaultColor = Color.BLACK;
    private int pressedColor;
    private ObjectAnimator pressedAnimator;

    private String text;
    private Paint textPaint;

    public LoopButton(Context context) {
        super(context);
        init(context, null);
    }

    public LoopButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public LoopButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public void setSelected(boolean pressed) {

        if (circlePaint != null) {
            circlePaint.setColor(pressed ? pressedColor : defaultColor);
        }

        if (pressed) {
            showPressedRing();
        } else {
            hidePressedRing();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(centerX, centerY, pressedRingRadius + animationProgress, focusPaint);
        canvas.drawCircle(centerX, centerY, outerRadius - pressedRingWidth, circlePaint);
        if (textPaint != null) {
            final float baselineOffset = (textPaint.descent() + textPaint.ascent()) / 2;
            canvas.drawText(text, centerX, centerY - baselineOffset, textPaint);
        }
        super.onDraw(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2;
        centerY = h / 2;
        outerRadius = Math.min(w, h) / 2;
        pressedRingRadius = outerRadius - pressedRingWidth - pressedRingWidth / 2;
    }

    public float getAnimationProgress() {
        return animationProgress;
    }

    public void setAnimationProgress(float animationProgress) {
        this.animationProgress = animationProgress;
        this.invalidate();
    }

    public void setColor(int color) {
        this.defaultColor = color;
        this.pressedColor = getHighlightColor(color, PRESSED_COLOR_LIGHTUP);

        circlePaint.setColor(defaultColor);
        focusPaint.setColor(defaultColor);
        focusPaint.setAlpha(PRESSED_RING_ALPHA);

        this.invalidate();
    }

    private void hidePressedRing() {
        pressedAnimator.setFloatValues(pressedRingWidth, 0f);
        pressedAnimator.start();
    }

    private void showPressedRing() {
        pressedAnimator.setFloatValues(animationProgress, pressedRingWidth);
        pressedAnimator.start();
    }

    private void init(Context context, AttributeSet attrs) {
        this.setFocusable(true);
        this.setScaleType(ScaleType.CENTER_INSIDE);
        setClickable(true);

        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setStyle(Paint.Style.FILL);

        focusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        focusPaint.setStyle(Paint.Style.STROKE);

        pressedRingWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_PRESSED_RING_WIDTH_DIP, getResources()
                .getDisplayMetrics());

        int color = Color.BLACK;
        float textSize = 22.0f;
        int textColor = Color.WHITE;
        if (attrs != null) {
            final TypedArray a = context.obtainStyledAttributes(attrs, ATTRS);
            color = a.getColor(attrValueIndex(attr.cb_color_loop), color);
            pressedRingWidth = (int) a.getDimension(attrValueIndex(attr.cb_pressedRingWidth_loop), pressedRingWidth);
            this.text = a.getString(attrValueIndex(android.R.attr.text));
            textSize = a.getDimension(attrValueIndex(android.R.attr.textSize), textSize);
            textColor = a.getColor(attrValueIndex(android.R.attr.textColor), textColor);
            a.recycle();
        }

        setColor(color);

        if (this.text != null) {
            setText(text, textSize, textColor);
        }

        focusPaint.setStrokeWidth(pressedRingWidth);
        final int pressedAnimationTime = getResources().getInteger(ANIMATION_TIME_ID);
        pressedAnimator = ObjectAnimator.ofFloat(this, "animationProgress", 0f, 0f);
        pressedAnimator.setDuration(pressedAnimationTime);
    }

    private int getHighlightColor(int color, int amount) {
        return Color.argb(Math.min(255, Color.alpha(color)), Math.min(255, Color.red(color) + amount),
                Math.min(255, Color.green(color) + amount), Math.min(255, Color.blue(color) + amount));
    }

    private static final int[] ATTRS = new int[]{
            android.R.attr.text,
            android.R.attr.textColor,
            android.R.attr.textSize,
            attr.cb_color_loop,
            attr.cb_pressedRingWidth_loop
    };

    static {
        Arrays.sort(ATTRS);
    }

    private static int attrValueIndex(int attrIndex) {
        for (int i = 0; i < ATTRS.length; i++) {
            if (ATTRS[i] == attrIndex) {
                return i;
            }
        }
        return -1;
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text, float textSize, int textColor) {
        this.text = text;
        this.textPaint = new Paint();
        textPaint.setColor(textColor);
        textPaint.setTextSize(textSize);
        textPaint.setAntiAlias(true);
        textPaint.setFakeBoldText(true);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        Typeface tf = Typeface.create("sans-serif-thin",Typeface.NORMAL);
        textPaint.setTypeface(tf);

        this.invalidate();
    }

}

