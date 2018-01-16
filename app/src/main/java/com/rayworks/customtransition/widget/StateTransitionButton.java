package com.rayworks.customtransition.widget;

import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.rayworks.customtransition.R;

import java.util.Locale;


/**
 * Created by Sean on 1/13/18.
 * <p>
 * A plain view used to display normal loading case and animated successful state.
 *
 * @see State
 */

public class StateTransitionButton extends View {

    public static final String PROPERTY_ROTATE = "rotate";
    public static final String PROPERTY_ALPHA = "alpha";
    private static final long animSpeedInMs = 25;
    private static final long animMsBetweenStrokes = 200;

    private Paint paint;
    private int rotate;
    private RectF oval;
    private ValueAnimator rotateAnimator;
    private Path path;
    private int strokeWidth;
    private boolean pathInitialized;

    private int lineColor = Color.BLACK;

    private State state = State.STATE_LOADING;

    private long animLastUpdate;
    private boolean animRunning;
    private int animCurrentCountour;
    private float animCurrentPos;
    private Path animPath;
    private PathMeasure animPathMeasure;

    public StateTransitionButton(Context context) {
        this(context, null);
    }

    public StateTransitionButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StateTransitionButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public StateTransitionButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
                                 int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StateTransitionButton);
            lineColor = a.getColor(R.styleable.StateTransitionButton_line_color, Color.BLACK);

            strokeWidth = a.getDimensionPixelSize(R.styleable.StateTransitionButton_stroke_line_width,
                    getResources().getDimensionPixelSize(R.dimen.paint_width));
            a.recycle();
        }

        if (strokeWidth == 0) {
            strokeWidth = getResources().getDimensionPixelSize(R.dimen.paint_width);
        }

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setColor(lineColor);

        path = new Path();
    }

    public void showLoading() {
        state = State.STATE_LOADING;

        drawRotateCircle();
        invalidate();
    }

    public void showSuccessState() {
        state = State.STATE_SUCCESS;
        if (rotateAnimator != null) {
            rotateAnimator.cancel();
            rotateAnimator.removeAllUpdateListeners();
        }

        startAnimation();
    }

    private void drawRotateCircle() {
        if (rotateAnimator == null) {
            rotateAnimator = new ValueAnimator();

            // take the consideration of ProgressBar
            PropertyValuesHolder propertyRotate = PropertyValuesHolder.ofInt(PROPERTY_ROTATE, 0, 360);
            PropertyValuesHolder propertyAlpha = PropertyValuesHolder.ofFloat(PROPERTY_ALPHA, 0f, 1.f);

            rotateAnimator.setValues(propertyRotate, propertyAlpha);
            rotateAnimator.setDuration(500);
            rotateAnimator.setInterpolator(new LinearInterpolator());
            rotateAnimator.setRepeatCount(ValueAnimator.INFINITE);

        } else {
            rotateAnimator.cancel();
            rotateAnimator.removeAllUpdateListeners();
        }

        rotateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                rotate = (int) animation.getAnimatedValue(PROPERTY_ROTATE);
                invalidate();
            }
        });

        rotateAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // draw the board
        //canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

        int width = getWidth();
        int height = getHeight();

        if (oval == null) {
            oval = new RectF(strokeWidth, strokeWidth, width - strokeWidth, height - strokeWidth);
        }

        formPath(height);

        switch (state) {
            case STATE_LOADING:
                canvas.drawArc(oval, rotate, 270.f, false, paint);
                break;
            case STATE_SUCCESS:
                if (animRunning) {
                    drawAnimation(canvas);
                } else {
                    canvas.drawPath(path, paint);
                }
                break;
        }
    }

    /***
     * Generates the path for the triangle drawn as shape '√'
     *
     * @param size boundray length
     */
    private void formPath(int size) {
        if (!pathInitialized) {
            pathInitialized = true;

            path.reset();

            float x0, y0;
            float x1, y1;
            float x2, y2;

            double l = size * 5.0 / 12 / Math.sin(Math.PI / 3);
            x0 = (float) (l * 0.5);
            y0 = size / 2;

            double sin60 = Math.sin(Math.PI / 3);
            x1 = (float) (x0 + l / sin60 * 0.5 * sin60);
            y1 = (float) (y0 + l / sin60 * 0.5 * 0.5);

            x2 = (float) (x1 + l / sin60 * 0.5);
            y2 = (float) (y1 - l);

            path.moveTo(x0, y0);
            path.lineTo(x1, y1);
            path.lineTo(x2, y2);

            System.out.println(String.format(Locale.ENGLISH,
                    "(%.2f : %.2f) - (%.2f:%.2f) - (%.2f : %.2f)", x0, y0, x1, y1, x2, y2));
        }
    }

    /***
     * NB: http://www.tokotoko.se/blog/2011/animated-paths-android/
     * Draws path animation.
     *
     * @param canvas {@link Canvas}
     */
    private void drawAnimation(Canvas canvas) {
        if (animPathMeasure == null) {
            // Start of animation. Set it up.
            animPathMeasure = new PathMeasure(path, false);
            animPathMeasure.nextContour();
            animPath = new Path();
            animLastUpdate = System.currentTimeMillis();
            animCurrentCountour = 0;
            animCurrentPos = 0.0f;
        } else {
            // Get time since last frame
            long now = System.currentTimeMillis();
            long timeSinceLast = now - animLastUpdate;

            if (animCurrentPos == 0.0f) {
                timeSinceLast -= animMsBetweenStrokes;
            }

            if (timeSinceLast > 0) {
                // Get next segment of path
                float newPos = (float) (timeSinceLast) / animSpeedInMs + animCurrentPos;
                boolean moveTo = (animCurrentPos == 0.0f);
                animPathMeasure.getSegment(animCurrentPos, newPos, animPath, moveTo);
                animCurrentPos = newPos;
                animLastUpdate = now;

                // If this stroke is done, move on to next
                if (newPos > animPathMeasure.getLength()) {
                    animCurrentPos = 0.0f;
                    animCurrentCountour++;
                    boolean more = animPathMeasure.nextContour();
                    // Check if finished
                    if (!more) {
                        animRunning = false;
                    }
                }
            }

            // Draw path
            canvas.drawPath(animPath, paint);
        }

        invalidate();
    }

    private void startAnimation() {
        animRunning = true;
        animPathMeasure = null;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // simply take the default value into consideration
        int desiredSize = getResources().getDimensionPixelSize(R.dimen.default_size);

        setMeasuredDimension(resolveSize(desiredSize, widthMeasureSpec),
                resolveSize(desiredSize, heightMeasureSpec));
    }

    public enum State {
        STATE_LOADING,
        STATE_SUCCESS
    }
}
