package com.specknet.airrespeck.utils;


import android.content.Context;
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

import java.util.ArrayList;
import java.util.List;


public class ReadingView extends View {

    Context mContext;

    // Canvas
    private float mWidth, mHeight, mPadding;

    // Bar
    private float mBarLeft, mBarTop, mBarRight, mBarBottom;
    private boolean mIsGradientColour;
    private int mBarColour;
    static int[] mDefaultGradientColours =
            {Color.parseColor("#4DAF51"), Color.parseColor("#FFA500"), Color.parseColor("#FF0000")};

    // Custom scale
    private boolean mIsScale;
    private float mScaleMin, mScaleMax;
    private List<Float> mScaleValues;
    private List<Float> mScalePositions;

    // Custom gradient colours
    private boolean mIsCustomGradientColour;
    private List<Integer> mScaleColours;

    // Progress needle
    private float mNeedleX, mNeedleY, mNeedleWidth, mNeedleHeight;
    private int mNeedleColour;

    // Text
    private boolean mIsTitle, mIsValue, mIsUnits;
    private String mTitle, mValue, mValueUnits;
    private float mTitleX, mTitleY, mTitleFontSize, mValueX, mValueY, mValueFontSize;

    // Drawing objects
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint mBarPaint, mNeedlePaint, mTitleTextPaint, mValueTextPaint;
    private LinearGradient mLinearGradient;
    private Path mPath;
    private Rect mTextBounds;

    public ReadingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        init();
    }

    private void init() {
        mScalePositions = new ArrayList<>();

        mBarColour = Color.GRAY;
        mNeedleColour = Color.BLACK;

        mTitle = mValue = mValueUnits = "";
        mTitleX = mTitleY = mValueX = mValueY = 0;

        mTitleTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mValueTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mNeedlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mPath = new Path();
        mTextBounds = new Rect();

        mPath.setFillType(Path.FillType.EVEN_ODD);

        calculateObjectsDims();
    }

    private void calculateObjectsDims() {
        mPadding = 20;

        mBarLeft = mPadding;
        mBarTop = mPadding + mTitleY*3;
        mBarRight = mWidth - mPadding;
        mBarBottom = mBarTop + mHeight * 0.25f;

        mNeedleHeight = mHeight * 0.25f;
        mNeedleWidth = mNeedleHeight * 0.5f;
        mNeedleX = 0;
        mNeedleY = mBarTop * 0.5f + mBarBottom * 0.5f;

        mTitleFontSize = mHeight * 0.25f;
        mValueFontSize = mHeight * 0.2f;

        mTitleTextPaint.setTextSize(mTitleFontSize);
        mValueTextPaint.setTextSize(mValueFontSize);

        if (mIsScale) {
            mScaleMin = mScaleValues.get(0);
            mScaleMax = mScaleValues.get(mScaleValues.size() - 1);
        }
        else {
            mScaleMin = 0f;
            mScaleMax = mWidth - 2*mPadding;
        }

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

                        mLinearGradient = new LinearGradient(mBarLeft, mBarTop, mBarRight, mBarBottom,
                                colours, colourPos, Shader.TileMode.CLAMP);

                        mBarPaint.setShader(mLinearGradient);
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

            mLinearGradient = new LinearGradient(mBarLeft, mBarTop, mBarRight, mBarBottom,
                    colours, null, Shader.TileMode.CLAMP);

            mBarPaint.setShader(mLinearGradient);
        }
        else if (mIsGradientColour) {
            mLinearGradient = new LinearGradient(mBarLeft, mBarTop, mBarRight, mBarBottom,
                    mDefaultGradientColours, null, Shader.TileMode.CLAMP);

            mBarPaint.setShader(mLinearGradient);
        }
        else {
            mBarPaint.setColor(mBarColour);
        }

        mNeedlePaint.setColor(mNeedleColour);

        updateTitlePos();
    }

    private float calculateScalePosition(final float value) {
        return (value - mScaleMin) * (mWidth - 2*mPadding) / (mScaleMax - mScaleMin) + mPadding;
    }

    private void updateTitlePos() {
        if (!mIsTitle) {
            return;
        }

        mTitleTextPaint.getTextBounds(mTitle, 0, mTitle.length(), mTextBounds);

        mTitleX = (mWidth - mTextBounds.width()) * 0.5f;
        mTitleY = mPadding + mTextBounds.height();
    }

    private void updateValuePos(final int value) {
        mNeedleX = value * (mWidth - 2*mPadding) / mScaleMax + mPadding;

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

    public void setBarColour(final int colour) {
        mBarColour = colour;
    }

    public void setNeedleColour(final int colour) {
        mNeedleColour = colour;
    }

    public void setGradientColours(final boolean isGradientColour) {
        mIsGradientColour = isGradientColour;
    }

    public void setTitleFontSize( final float fontSize) {
        mTitleFontSize = fontSize;
        mTitleTextPaint.setTextSize(mTitleFontSize);
    }

    public void setValueFontSize( final float fontSize) {
        mValueFontSize = fontSize;
        mValueTextPaint.setTextSize(mValueFontSize);
    }

    public void setTitle(final String title) {
        mIsTitle = true;
        mTitle = title;
        updateTitlePos();
    }

    public void setValue(final int value) {
        mIsValue = true;

        if (mIsUnits) {
            mValue = String.valueOf(value) + " " + mValueUnits;
        }
        else {
            mValue = String.valueOf(value);
        }

        updateValuePos(value);

        invalidate();
    }

    public void setValueUnits(final String units) {
        mIsUnits = true;
        mValueUnits = units;
    }

    public void setScale(final List<Float> scale) {
        if (scale.size() < 2) {
            mIsScale = false;
            return;
        }

        mIsScale = true;
        mScaleValues = new ArrayList<>(scale);
    }

    public void setColours(final List<Integer> colours) {
        if (colours.size() < 2) {
            mIsCustomGradientColour = false;
            return;
        }

        mIsCustomGradientColour = true;
        mScaleColours = new ArrayList<>(colours);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mWidth = w;
        mHeight = w * 0.3f;     // Arbitrary height based on width
        calculateObjectsDims();

        mBitmap = Bitmap.createBitmap((int)mWidth, (int)mHeight, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
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
                        mBarPaint.setColor(mBarColour);     // TODO add random colour generation
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
        /*System.out.println("Canvas width: " + mCanvas.getWidth());
        System.out.println("Canvas height: " + mCanvas.getHeight());*/
    }
}