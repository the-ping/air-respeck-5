package com.specknet.airrespeck.utils;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import com.specknet.airrespeck.R;

import java.util.ArrayList;
import java.util.List;


public class ReadingView extends View {

    // Constants. Relative to canvas height.
    private final float BAR_THICKNESS_PERCENT = 0.25f;
    private final float NEEDLE_HEIGHT_PERCENT = 0.20f;

    // Canvas
    private float mWidth, mHeight, mPadding = 10;

    // Bar
    private float mBarLeft, mBarTop, mBarRight, mBarBottom;
    private boolean mIsGradientColour;
    private int mBarColour = Color.GRAY;
    static int[] mDefaultGradientColours =
            {Color.parseColor("#4DAF51"), Color.parseColor("#FFA500"), Color.parseColor("#FF0000")};

    // Scale
    private boolean mIsScale;
    private float mScaleMin, mScaleMax;
    private List<Float> mScaleValues;
    private List<Float> mScalePositions;

    // Custom gradient colours
    private boolean mIsCustomGradientColour;
    private List<Integer> mScaleColours;

    // Progress needle
    private float mNeedleX, mNeedleY, mNeedleWidth, mNeedleHeight;
    private int mNeedleColour = Color.BLACK;

    private int mProgressValue = 0;

    // Text
    private boolean mIsTitle, mIsValue, mIsUnits;
    private String mTitle, mValue, mValueUnits;
    private float mTitleX, mTitleY, mTitleFontSize;
    private float mValueX, mValueY, mValueFontSize;
    private int mTitleColour = Color.BLACK, mValueColour = Color.BLACK;

    private Paint mBarPaint, mNeedlePaint, mTitleTextPaint, mValueTextPaint;
    private Path mPath;
    private Rect mTextBounds;

    public ReadingView(Context context) {
        super(context);
        init(null, 0);
    }

    public ReadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ReadingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().
                obtainStyledAttributes(attrs, R.styleable.ReadingView, defStyle, 0);
        // Nothing for now
        a.recycle();

        // Helper objects
        mScalePositions = new ArrayList<>();
        mTextBounds = new Rect();
        mPath = new Path();
        mPath.setFillType(Path.FillType.EVEN_ODD);

        // Default BarPaint
        mBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Update BarPaint and bar measurements
        invalidateBarPaintAndMeasurements();

        // Default NeedlePaint
        mNeedlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Update NeedlePaint and needle measurements
        invalidateNeedlePaintAndMeasurements();

        // Set up a default TextPaint object for the Title
        mTitleTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Update TextPaint and text measurements
        invalidateTitleTextPaintAndMeasurements();

        // Set up a default TextPaint object for the value
        mValueTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Update TextPaint and text measurements
        invalidateValueTextPaintAndMeasurements();
    }

    /**
     * Invalidates the previous dimensions and Paint for the bar object, and calculate
     * the new dimensions and set new Paint attributes.
     */
    private void invalidateBarPaintAndMeasurements() {
        float barThickness = mHeight * BAR_THICKNESS_PERCENT;

        mBarLeft = mPadding;
        mBarTop = (mHeight - barThickness) * 0.5f;
        mBarRight = mWidth - mPadding;
        mBarBottom = mBarTop + barThickness;

        if (mIsScale) {
            mScaleMin = mScaleValues.get(0);
            mScaleMax = mScaleValues.get(mScaleValues.size() - 1);
        }
        else {
            mScaleMin = 0f;
            mScaleMax = mWidth - 2*mPadding;
        }

        LinearGradient linearGradient;
        if (mIsScale) {
            mScalePositions.clear();
            for (int i = 0; i < mScaleValues.size(); ++i) {
                mScalePositions.add(calculateScalePosition(mScaleValues.get(i)));
            }

            if (mIsCustomGradientColour) {
                if (mIsGradientColour) {
                    if (mScaleValues.size() > 2) {
                        int arraySize = mScaleValues.size() - 1;

                        float lastVal, currVal;

                        float[] colourPos = new float[arraySize];
                        for (int i = 1; i < mScaleValues.size(); i++) {
                            lastVal = (mScaleValues.get(i - 1) - mScaleMin) / (mScaleMax - mScaleMin);
                            currVal = (mScaleValues.get(i) - mScaleMin) / (mScaleMax - mScaleMin);

                            colourPos[i - 1] = lastVal + ((currVal - lastVal) / 2);
                        }

                        int[] colours = new int[arraySize];
                        for (int i = 0; i < arraySize; i++) {
                            if (i < mScaleColours.size()) {
                                colours[i] = mScaleColours.get(i);
                            } else {
                                colours[i] = mBarColour;        // TODO add random colour generation
                            }
                        }

                        linearGradient = new LinearGradient(mBarLeft, mBarTop, mBarRight, mBarBottom,
                                colours, colourPos, Shader.TileMode.CLAMP);

                        mBarPaint.setShader(linearGradient);
                    } else {
                        mBarPaint.setColor(mScaleColours.get(0));
                    }
                }
            }
        }
        else if (mIsCustomGradientColour) {
            int[] colours = new int[mScaleColours.size()];

            for (int i = 0; i < mScaleColours.size(); i++) {
                colours[i] = mScaleColours.get(i);
            }

            linearGradient = new LinearGradient(mBarLeft, mBarTop, mBarRight, mBarBottom,
                    colours, null, Shader.TileMode.CLAMP);

            mBarPaint.setShader(linearGradient);
        }
        else if (mIsGradientColour) {
            linearGradient = new LinearGradient(mBarLeft, mBarTop, mBarRight, mBarBottom,
                    mDefaultGradientColours, null, Shader.TileMode.CLAMP);

            mBarPaint.setShader(linearGradient);
        }
        else {
            mBarPaint.setColor(mBarColour);
        }
    }

    /**
     * Invalidates the previous dimensions and Paint for the needle object, and calculate
     * the new dimensions and set new Paint attributes.
     */
    private void invalidateNeedlePaintAndMeasurements() {
        mNeedlePaint.setColor(mNeedleColour);

        mNeedleHeight = mHeight * NEEDLE_HEIGHT_PERCENT;
        mNeedleWidth = mNeedleHeight * 0.5f;
        mNeedleX = mProgressValue * (mWidth - 2*mPadding) / mScaleMax + mPadding;
        mNeedleY = (mBarTop + mBarBottom) * 0.5f;
    }

    /**
     * Invalidates the previous dimensions and Paint for the text title object, and calculate
     * the new dimensions and set new Paint attributes.
     */
    private void invalidateTitleTextPaintAndMeasurements() {
        if (!mIsTitle) {
            return;
        }

        mTitleTextPaint.setTextSize(mTitleFontSize);
        mTitleTextPaint.setColor(mTitleColour);

        mTitleTextPaint.getTextBounds(mTitle, 0, mTitle.length(), mTextBounds);
        mTitleX = (mWidth - mTextBounds.width()) * 0.5f;
        mTitleY = (mHeight * (1-BAR_THICKNESS_PERCENT) + 2 * mTextBounds.height()) * 0.25f;
    }

    /**
     * Invalidates the previous dimensions and Paint for the text value object, and calculate
     * the new dimensions and set new Paint attributes.
     */
    private void invalidateValueTextPaintAndMeasurements() {
        if (!mIsValue) {
            return;
        }

        mValueTextPaint.setTextSize(mValueFontSize);
        mValueTextPaint.setColor(mValueColour);

        if (mIsUnits) {
            mValue = String.valueOf(mProgressValue) + " " + mValueUnits;
        }
        else {
            mValue = String.valueOf(mProgressValue);
        }

        mValueTextPaint.getTextBounds(mValue, 0, mValue.length(), mTextBounds);

        mValueX = mNeedleX - mTextBounds.width() * 0.5f;
        mValueY = mNeedleY + mNeedleHeight + mTextBounds.height();

        if (mValueX < 0) {
            mValueX = 0;
        }
        else if (mValueX + mTextBounds.width() > mWidth) {
            mValueX = mWidth - mTextBounds.width();
        }
    }

    /**
     * Calculate the relative position of the given value in the bar.
     * @param value float Scale value.
     * @return float Relative position in the bar.
     */
    private float calculateScalePosition(final float value) {
        return (value - mScaleMin) * (mWidth - 2*mPadding) / (mScaleMax - mScaleMin) + mPadding;
    }

    /**
     * The bar colour.
     * @param colour int Colour integer value.
     */
    public void setBarColour(final int colour) {
        mBarColour = colour;
        invalidateBarPaintAndMeasurements();
    }

    /**
     * Set the needle colour.
     * @param colour int Colour integer value.
     */
    public void setNeedleColour(final int colour) {
        mNeedleColour = colour;
        invalidateNeedlePaintAndMeasurements();
    }

    /**
     * Set whether the bar should be drawn using a gradient of colours of with sharp edges.
     * If {@link #setColours(List)} is not called, a default scale of three colours is used.
     * @param isGradientColour
     */
    public void setGradientColours(final boolean isGradientColour) {
        mIsGradientColour = isGradientColour;
        invalidateBarPaintAndMeasurements();
    }

    /**
     * Set the title font size.
     * @param fontSize float Font size value.
     */
    public void setTitleFontSize(final float fontSize) {
        mTitleFontSize = fontSize;
        invalidateTitleTextPaintAndMeasurements();
    }

    /**
     * Set the title text colour.
     * @param colour int Colour integer value.
     */
    public void setTitleColour(final int colour) {
        mTitleColour = colour;
        invalidateTitleTextPaintAndMeasurements();
    }

    /**
     * Set the value font size.
     * @param fontSize float Font size value.
     */
    public void setValueFontSize(final float fontSize) {
        mValueFontSize = fontSize;
        invalidateValueTextPaintAndMeasurements();
    }

    /**
     * Set the value text colour.
     * @param colour int Colour integer value.
     */
    public void setValueColour(final int colour) {
        mValueColour = colour;
        invalidateValueTextPaintAndMeasurements();
    }

    /**
     * Set the title of the widget.
     * @param title String Title value.
     */
    public void setTitle(final String title) {
        if (title != null && !title.isEmpty()) {
            mIsTitle = true;
            mTitle = title;
            invalidateTitleTextPaintAndMeasurements();
        }
        else {
            mIsTitle = false;
        }
    }

    /**
     * Set the current value to be displayed along the bar.
     * @param value int Current value.
     */
    public void setValue(final int value) {
        mIsValue = true;
        mProgressValue = value;
        invalidateNeedlePaintAndMeasurements();
        invalidateValueTextPaintAndMeasurements();
        invalidate();
    }

    /**
     * Set the units for the value to be displayed.
     * @param units String Units value.
     */
    public void setValueUnits(final String units) {
        if (units != null && !units.isEmpty()) {
            mIsUnits = true;
            mValueUnits = units;
        }
        else {
            mIsUnits = false;
        }
    }

    /**
     * Set a custom scale for the bar containing ordered values from left to right.
     * It must contain at least 2 values: min and max.
     * @param scale List<Float> List containing the values of the custom scale.
     */
    public void setScale(final List<Float> scale) {
        if (scale.size() < 2) {
            mIsScale = false;
            return;
        }

        mIsScale = true;
        mScaleValues = new ArrayList<>(scale);
        invalidateBarPaintAndMeasurements();
    }

    /**
     * Set colours for the bar.
     * @param colours List<Integer> List containing the colours ordered from left to right.
     */
    public void setColours(final List<Integer> colours) {
        if (colours.size() < 2) {
            mIsCustomGradientColour = false;
            return;
        }

        mIsCustomGradientColour = true;
        mScaleColours = new ArrayList<>(colours);
        invalidateBarPaintAndMeasurements();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mWidth = w;
        mHeight = w * 0.3f;     // Arbitrary height based on width

        // Default values
        mPadding = mHeight * 0.01f;
        mTitleFontSize = mHeight * 0.25f;
        mValueFontSize = mHeight * 0.2f;

        invalidateBarPaintAndMeasurements();
        invalidateNeedlePaintAndMeasurements();
        invalidateTitleTextPaintAndMeasurements();
        invalidateValueTextPaintAndMeasurements();

        Bitmap mBitmap = Bitmap.createBitmap((int) mWidth, (int) mHeight, Bitmap.Config.ARGB_8888);
        Canvas mCanvas = new Canvas(mBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mWidth == 0) {
            return;
        }

        if (mIsTitle) {
            canvas.drawText(mTitle, mTitleX, mTitleY, mTitleTextPaint);
        }

        if (mIsScale) {
            if (mIsGradientColour) {
                canvas.drawRect(mBarLeft, mBarTop, mBarRight, mBarBottom, mBarPaint);
            }
            else {  // Hard-edges
                for (int i = 1; i < mScalePositions.size(); ++i) {
                    if (mIsCustomGradientColour && i - 1 < mScaleColours.size()) {
                        mBarPaint.setColor(mScaleColours.get(i - 1));
                    } else {
                        mBarPaint.setColor(mBarColour);
                    }

                    canvas.drawRect(mScalePositions.get(i - 1), mBarTop, mScalePositions.get(i), mBarBottom, mBarPaint);
                }
            }
        }
        else {
            canvas.drawRect(mBarLeft, mBarTop, mBarRight, mBarBottom, mBarPaint);
        }

        mPath.rewind();
        mPath.moveTo(mNeedleX, mNeedleY);
        mPath.lineTo(mNeedleX, mNeedleY);
        mPath.lineTo(mNeedleX - mNeedleWidth /2, mNeedleY + mNeedleHeight);
        mPath.lineTo(mNeedleX + mNeedleWidth /2, mNeedleY + mNeedleHeight);
        mPath.close();

        canvas.drawPath(mPath, mNeedlePaint);

        if (mIsValue) {
            canvas.drawText(mValue, mValueX, mValueY, mValueTextPaint);
        }
    }
}