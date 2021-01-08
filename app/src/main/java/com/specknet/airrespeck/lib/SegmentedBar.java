package com.specknet.airrespeck.lib;

/**
 * Source: https://github.com/gspd-mobi/SegmentedBarView-Android
 */

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import androidx.core.content.ContextCompat;
import android.text.Html;
import android.text.Layout;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.specknet.airrespeck.R;

import java.text.DecimalFormat;
import java.util.List;

@SuppressWarnings("unused")
public class SegmentedBar extends View {

    private List<Segment> segments;
    private String unit;

    private Float value;
    private Integer valueSegment;
    private String valueSegmentText;

    private Rect rectBounds;
    private Rect valueSignBounds;
    private RectF roundRectangleBounds;
    private Paint fillPaint;
    private Paint segmentTextPaint;
    private Paint descriptionTextPaint;

    private DecimalFormat formatter;

    private int valueSignHeight;
    private int valueSignColor;
    private int emptySegmentColor;
    private int valueSignWidth;
    private int arrowHeight;
    private int arrowWidth;
    private int gapWidth;
    private int barHeight;
    private int descriptionBoxHeight;
    private int valueSignRound;
    private String emptySegmentText;

    private int barRoundingRadius = 0;

    private int valueSignCenter = -1;

    private boolean showDescriptionText;
    private boolean showSegmentText;

    private int sideStyle = SegmentedBarSideStyle.ROUNDED;
    private int sideTextStyle = SegmentedBarSideTextStyle.ONE_SIDED;

    private int valueTextSize;
    private int descriptionTextSize;

    private int segmentTextSize;
    private int valueTextColor = Color.WHITE;
    private int descriptionTextColor = Color.DKGRAY;

    private int segmentTextColor = Color.WHITE;
    private TextPaint valueTextPaint;
    private Path trianglePath;
    private StaticLayout valueTextLayout;
    private Point point1;
    private Point point2;
    private Point point3;
    private Rect segmentRect;

    public SegmentedBar(Context context) {
        super(context);
        init(context, null);
    }

    public SegmentedBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);

    }

    private void init(Context context, AttributeSet attrs) {

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.SegmentedBar,
                0, 0);

        try {
            Resources resources = getResources();
            segmentTextSize = a.getDimensionPixelSize(R.styleable.SegmentedBar_sb_segment_text_size,
                    resources.getDimensionPixelSize(R.dimen.sb_segment_text_size));
            valueTextSize = a.getDimensionPixelSize(R.styleable.SegmentedBar_sb_value_text_size,
                    resources.getDimensionPixelSize(R.dimen.sb_value_text_size));
            descriptionTextSize = a.getDimensionPixelSize(R.styleable.SegmentedBar_sb_description_text_size,
                    resources.getDimensionPixelSize(R.dimen.sb_description_text_size));
            barHeight = a.getDimensionPixelSize(R.styleable.SegmentedBar_sb_bar_height,
                    resources.getDimensionPixelSize(R.dimen.sb_bar_height));
            valueSignHeight = a.getDimensionPixelSize(R.styleable.SegmentedBar_sb_value_sign_height,
                    resources.getDimensionPixelSize(R.dimen.sb_value_sign_height));
            valueSignWidth = a.getDimensionPixelSize(R.styleable.SegmentedBar_sb_value_sign_width,
                    resources.getDimensionPixelSize(R.dimen.sb_value_sign_width));
            arrowHeight = a.getDimensionPixelSize(R.styleable.SegmentedBar_sb_arrow_height,
                    resources.getDimensionPixelSize(R.dimen.sb_arrow_height));
            arrowWidth = a.getDimensionPixelSize(R.styleable.SegmentedBar_sb_arrow_width,
                    resources.getDimensionPixelSize(R.dimen.sb_arrow_width));
            gapWidth = a.getDimensionPixelSize(R.styleable.SegmentedBar_sb_segment_gap_width,
                    resources.getDimensionPixelSize(R.dimen.sb_segment_gap_width));
            valueSignRound = a.getDimensionPixelSize(R.styleable.SegmentedBar_sb_value_sign_round,
                    resources.getDimensionPixelSize(R.dimen.sb_value_sign_round));
            descriptionBoxHeight = a.getDimensionPixelSize(R.styleable.SegmentedBar_sb_description_box_height,
                    resources.getDimensionPixelSize(R.dimen.sb_description_box_height));

            showSegmentText = a.getBoolean(R.styleable.SegmentedBar_sb_show_segment_text, true);
            showDescriptionText = a.getBoolean(R.styleable.SegmentedBar_sb_show_description_text, false);

            valueSegmentText = a.getString(R.styleable.SegmentedBar_sb_value_segment_text);
            if (valueSegmentText == null) {
                valueSegmentText = resources.getString(R.string.sb_value_segment);
            }
            emptySegmentText = a.getString(R.styleable.SegmentedBar_sb_empty_segment_text);
            if (emptySegmentText == null) {
                emptySegmentText = resources.getString(R.string.sb_empty);
            }

            valueSignColor = a.getColor(R.styleable.SegmentedBar_sb_value_sign_background,
                    ContextCompat.getColor(context, R.color.sb_value_sign_background));
            emptySegmentColor = a.getColor(R.styleable.SegmentedBar_sb_empty_segment_background,
                    ContextCompat.getColor(context, R.color.sb_empty_segment_background));

            sideStyle = a.getInt(R.styleable.SegmentedBar_sb_side_style,
                    SegmentedBarSideStyle.ROUNDED);
            sideTextStyle = a.getInt(R.styleable.SegmentedBar_sb_side_text_style,
                    SegmentedBarSideTextStyle.ONE_SIDED);


        } finally {
            a.recycle();
        }

        formatter = new DecimalFormat("##.#");

        segmentTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        segmentTextPaint.setColor(Color.WHITE);
        segmentTextPaint.setStyle(Paint.Style.FILL);

        valueTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        valueTextPaint.setColor(Color.WHITE);
        valueTextPaint.setStyle(Paint.Style.FILL);
        valueTextPaint.setTextSize(valueTextSize);
        valueTextPaint.setColor(valueTextColor);

        descriptionTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        descriptionTextPaint.setColor(Color.DKGRAY);
        descriptionTextPaint.setStyle(Paint.Style.FILL);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        rectBounds = new Rect();
        roundRectangleBounds = new RectF();
        valueSignBounds = new Rect();
        segmentRect = new Rect();

        trianglePath = new Path();
        trianglePath.setFillType(Path.FillType.EVEN_ODD);
        point1 = new Point();
        point2 = new Point();
        point3 = new Point();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        valueSignCenter = -1;
        int segmentsSize = segments == null ? 0 : segments.size();
        if (segmentsSize > 0) {
            for (int segmentIndex = 0; segmentIndex < segmentsSize; segmentIndex++) {
                Segment segment = segments.get(segmentIndex);

                drawSegment(canvas, segment, segmentIndex, segmentsSize);
            }
        } else {
            drawEmptySegment(canvas);
        }

        if (!valueIsEmpty()) {
            drawValueSign(canvas, valueSignSpaceHeight(), valueSignCenter);
        }
    }

    private void drawEmptySegment(Canvas canvas) {
        int segmentsSize = 1;

        int singleSegmentWidth = getContentWidth() / segmentsSize;
        rectBounds.set(getPaddingLeft(), valueSignSpaceHeight() + getPaddingTop(), singleSegmentWidth + getPaddingLeft(), barHeight + valueSignSpaceHeight() + getPaddingTop());

        fillPaint.setColor(emptySegmentColor);

        barRoundingRadius = rectBounds.height() / 2;
        if (barRoundingRadius > singleSegmentWidth / 2) {
            sideStyle = SegmentedBarSideStyle.NORMAL;
        }

        segmentRect.set(rectBounds);

        switch (sideStyle) {
            case SegmentedBarSideStyle.ROUNDED:
                roundRectangleBounds.set(rectBounds.left, rectBounds.top, rectBounds.right, rectBounds.bottom);
                canvas.drawRoundRect(roundRectangleBounds, barRoundingRadius, barRoundingRadius, fillPaint);
                break;
            case SegmentedBarSideStyle.ANGLE:
                rectBounds.set(barRoundingRadius + getPaddingLeft(),
                        valueSignSpaceHeight() + getPaddingTop(),
                        getWidth() - getPaddingRight() - barRoundingRadius,
                        barHeight + valueSignSpaceHeight() + getPaddingTop());
                canvas.drawRect(
                        rectBounds,
                        fillPaint
                );
                //Draw left triangle
                point1.set(rectBounds.left - barRoundingRadius, rectBounds.top + barRoundingRadius);
                point2.set(rectBounds.left, rectBounds.top);
                point3.set(rectBounds.left, rectBounds.bottom);

                drawTriangle(canvas, point1, point2, point3, fillPaint);

                //Draw right triangle
                point1.set(rectBounds.right + barRoundingRadius, rectBounds.top + barRoundingRadius);
                point2.set(rectBounds.right, rectBounds.top);
                point3.set(rectBounds.right, rectBounds.bottom);

                drawTriangle(canvas, point1, point2, point3, fillPaint);
                break;
            case SegmentedBarSideStyle.NORMAL:
                canvas.drawRect(
                        rectBounds,
                        fillPaint
                );
            default:
                break;
        }


        if (showSegmentText) {
            String textToShow;
            textToShow = emptySegmentText;
            segmentTextPaint.setTextSize(segmentTextSize);
            drawTextCentredInRectWithSides(canvas, segmentTextPaint, textToShow, segmentRect.left, segmentRect.top, segmentRect.right, segmentRect.bottom);
        }
    }

    private int getContentWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private int getContentHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    private void drawSegment(Canvas canvas, Segment segment, int segmentIndex, int segmentsSize) {
        boolean isLeftSegment = segmentIndex == 0;
        boolean isRightSegment = segmentIndex == segmentsSize - 1;
        boolean isLeftAndRight = isLeftSegment && isRightSegment;

        int singleSegmentWidth = (getContentWidth() + gapWidth) / segmentsSize - gapWidth;
        int segmentLeft = (singleSegmentWidth + gapWidth) * segmentIndex;
        int segmentRight = segmentLeft + singleSegmentWidth;

        // Segment bounds
        rectBounds.set(segmentLeft + getPaddingLeft(), valueSignSpaceHeight() + getPaddingTop(), segmentRight + getPaddingLeft(), barHeight + valueSignSpaceHeight() + getPaddingTop());

        // Calculating value sign position
        if (valueSegment != null && valueSegment == segmentIndex) {
            valueSignCenter = segmentLeft + getPaddingLeft() + (singleSegmentWidth / 2);
        } else if (value != null && (value >= segment.getMinValue() && value < segment.getMaxValue() || (isRightSegment && segment.getMaxValue() == value))) {
            float valueSignCenterPercent = (value - segment.getMinValue()) / (segment.getMaxValue() - segment.getMinValue());
            valueSignCenter = (int) (segmentLeft + getPaddingLeft() + valueSignCenterPercent * singleSegmentWidth);
        }

        fillPaint.setColor(segment.getColor());

        segmentRect.set(rectBounds);

        // Drawing segment (with specific bounds if left or right)
        if (isLeftSegment || isRightSegment) {
            barRoundingRadius = rectBounds.height() / 2;
            if (barRoundingRadius > singleSegmentWidth / 2) {
                sideStyle = SegmentedBarSideStyle.NORMAL;
            }

            switch (sideStyle) {
                case SegmentedBarSideStyle.ROUNDED:
                    roundRectangleBounds.set(rectBounds.left, rectBounds.top, rectBounds.right, rectBounds.bottom);
                    canvas.drawRoundRect(roundRectangleBounds, barRoundingRadius, barRoundingRadius, fillPaint);
                    if (!isLeftAndRight) {
                        if (isLeftSegment) {
                            rectBounds.set(segmentLeft + barRoundingRadius + getPaddingLeft(), valueSignSpaceHeight() + getPaddingTop(), segmentRight + getPaddingLeft(), barHeight + valueSignSpaceHeight() + getPaddingTop());
                            canvas.drawRect(
                                    rectBounds,
                                    fillPaint
                            );
                        } else {
                            rectBounds.set(segmentLeft + getPaddingLeft(), valueSignSpaceHeight() + getPaddingTop(), segmentRight - barRoundingRadius + getPaddingLeft(), barHeight + valueSignSpaceHeight() + getPaddingTop());
                            canvas.drawRect(
                                    rectBounds,
                                    fillPaint
                            );
                        }
                    }
                    break;
                case SegmentedBarSideStyle.ANGLE:
                    if (isLeftAndRight) {
                        rectBounds.set(segmentLeft + barRoundingRadius + getPaddingLeft(), valueSignSpaceHeight() + getPaddingTop(), segmentRight - barRoundingRadius + getPaddingLeft(), barHeight + valueSignSpaceHeight() + getPaddingTop());
                        canvas.drawRect(
                                rectBounds,
                                fillPaint
                        );
                        //Draw left triangle
                        point1.set(rectBounds.left - barRoundingRadius, rectBounds.top + barRoundingRadius);
                        point2.set(rectBounds.left, rectBounds.top);
                        point3.set(rectBounds.left, rectBounds.bottom);

                        drawTriangle(canvas, point1, point2, point3, fillPaint);

                        //Draw right triangle
                        point1.set(rectBounds.right + barRoundingRadius, rectBounds.top + barRoundingRadius);
                        point2.set(rectBounds.right, rectBounds.top);
                        point3.set(rectBounds.right, rectBounds.bottom);

                        drawTriangle(canvas, point1, point2, point3, fillPaint);
                    } else {
                        if (isLeftSegment) {
                            rectBounds.set(segmentLeft + barRoundingRadius + getPaddingLeft(), valueSignSpaceHeight() + getPaddingTop(), segmentRight + getPaddingLeft(), barHeight + valueSignSpaceHeight() + getPaddingTop());
                            canvas.drawRect(
                                    rectBounds,
                                    fillPaint
                            );
                            //Draw left triangle
                            point1.set(rectBounds.left - barRoundingRadius, rectBounds.top + barRoundingRadius);
                            point2.set(rectBounds.left, rectBounds.top);
                            point3.set(rectBounds.left, rectBounds.bottom);

                            drawTriangle(canvas, point1, point2, point3, fillPaint);
                        } else {
                            rectBounds.set(segmentLeft + getPaddingLeft(), valueSignSpaceHeight() + getPaddingTop(), segmentRight - barRoundingRadius + getPaddingLeft(), barHeight + valueSignSpaceHeight() + getPaddingTop());
                            canvas.drawRect(
                                    rectBounds,
                                    fillPaint
                            );
                            //Draw right triangle
                            point1.set(rectBounds.right + barRoundingRadius, rectBounds.top + barRoundingRadius);
                            point2.set(rectBounds.right, rectBounds.top);
                            point3.set(rectBounds.right, rectBounds.bottom);

                            drawTriangle(canvas, point1, point2, point3, fillPaint);
                        }
                    }
                    break;
                case SegmentedBarSideStyle.NORMAL:
                    canvas.drawRect(
                            rectBounds,
                            fillPaint
                    );
                default:
                    break;
            }
        } else {
            canvas.drawRect(
                    rectBounds,
                    fillPaint
            );
        }

        // Drawing segment text
        if (showSegmentText) {
            String textToShow;
            if (segment.getCustomText() != null) {
                textToShow = segment.getCustomText();
            } else {
                if (isLeftSegment || isRightSegment) {
                    if (isLeftAndRight || sideTextStyle == SegmentedBarSideTextStyle.TWO_SIDED) {
                        textToShow = String.format("%s - %s", formatter.format(segment.getMinValue()), formatter.format(segment.getMaxValue()));
                    } else if (isLeftSegment) {
                        textToShow = String.format("<%s", formatter.format(segment.getMaxValue()));
                    } else {
                        textToShow = String.format(">%s", formatter.format(segment.getMinValue()));
                    }
                } else {
                    textToShow = String.format("%s - %s", formatter.format(segment.getMinValue()), formatter.format(segment.getMaxValue()));
                }
            }

            segmentTextPaint.setTextSize(segmentTextSize);
            segmentTextPaint.setColor(segmentTextColor);
            drawTextCentredInRect(canvas, segmentTextPaint, textToShow, segmentRect);
        }

        //Drawing segment description text
        if (showDescriptionText) {
            descriptionTextPaint.setTextSize(descriptionTextSize);
            descriptionTextPaint.setColor(descriptionTextColor);
            drawTextCentredInRectWithSides(canvas, descriptionTextPaint, segment.getDescriptionText(), segmentRect.left, segmentRect.bottom, segmentRect.right, segmentRect.bottom + descriptionBoxHeight);
        }
    }

    private void drawValueSign(Canvas canvas, int valueSignSpaceHeight, int valueSignCenter) {
        boolean valueNotInSegments = valueSignCenter == -1;
        if (valueNotInSegments) {
            valueSignCenter = getContentWidth() / 2 + getPaddingLeft();
        }
        valueSignBounds.set(valueSignCenter - valueSignWidth / 2,
                getPaddingTop(),
                valueSignCenter + valueSignWidth / 2,
                valueSignHeight - arrowHeight + getPaddingTop());
        fillPaint.setColor(valueSignColor);

        // Move if not fit horizontal
        if (valueSignBounds.left < getPaddingLeft()) {
            int difference = -valueSignBounds.left + getPaddingLeft();
            roundRectangleBounds.set(valueSignBounds.left + difference, valueSignBounds.top, valueSignBounds.right + difference, valueSignBounds.bottom);
        } else if (valueSignBounds.right > getMeasuredWidth() - getPaddingRight()) {
            int difference = valueSignBounds.right - getMeasuredWidth() + getPaddingRight();
            roundRectangleBounds.set(valueSignBounds.left - difference, valueSignBounds.top, valueSignBounds.right - difference, valueSignBounds.bottom);
        } else {
            roundRectangleBounds.set(valueSignBounds.left, valueSignBounds.top, valueSignBounds.right, valueSignBounds.bottom);
        }
        canvas.drawRoundRect(
                roundRectangleBounds,
                valueSignRound,
                valueSignRound,
                fillPaint
        );

        // Draw arrow
        if (!valueNotInSegments) {
            int difference = 0;
            if (valueSignCenter - arrowWidth / 2 < barRoundingRadius + getPaddingLeft()) {
                difference = barRoundingRadius - valueSignCenter + getPaddingLeft();
            } else if (valueSignCenter + arrowWidth / 2 > getMeasuredWidth() - barRoundingRadius - getPaddingRight()) {
                difference = (getMeasuredWidth() - barRoundingRadius) - valueSignCenter - getPaddingRight();
            }

            point1.set(valueSignCenter - arrowWidth / 2 + difference, valueSignSpaceHeight - arrowHeight + getPaddingTop());
            point2.set(valueSignCenter + arrowWidth / 2 + difference, valueSignSpaceHeight - arrowHeight + getPaddingTop());
            point3.set(valueSignCenter + difference, valueSignSpaceHeight + getPaddingTop());

            drawTriangle(canvas, point1, point2, point3, fillPaint);
        }

        // Draw value text
        if (valueTextLayout != null) {
            canvas.translate(roundRectangleBounds.left, roundRectangleBounds.top + roundRectangleBounds.height() / 2 - valueTextLayout.getHeight() / 2);
            valueTextLayout.draw(canvas);
        }
    }

    private void drawTriangle(Canvas canvas, Point point1, Point point2, Point point3, Paint paint) {
        trianglePath.reset();
        trianglePath.moveTo(point1.x, point1.y);
        trianglePath.lineTo(point2.x, point2.y);
        trianglePath.lineTo(point3.x, point3.y);
        trianglePath.lineTo(point1.x, point1.y);
        trianglePath.close();

        canvas.drawPath(trianglePath, paint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int minWidth = getPaddingLeft() + getPaddingRight();
        int minHeight = barHeight + getPaddingBottom() + getPaddingTop();
        if (!valueIsEmpty()) {
            minHeight += valueSignHeight + arrowHeight;
        }
        if (showDescriptionText) {
            minHeight += descriptionBoxHeight;
        }
        int w = resolveSizeAndState(minWidth, widthMeasureSpec, 0);
        int h = resolveSizeAndState(minHeight, heightMeasureSpec, 0);

        setMeasuredDimension(w, h);
    }

    private int valueSignSpaceHeight() {
        if (valueIsEmpty()) return 0;
        return valueSignHeight;
    }

    private boolean valueIsEmpty() {
        return value == null && valueSegment == null;
    }

    public void drawTextCentredInRect(Canvas canvas, Paint paint, String text, Rect outsideRect) {
        drawTextCentredInRectWithSides(canvas, paint, text, outsideRect.left, outsideRect.top, outsideRect.right, outsideRect.bottom);
    }

    public void drawTextCentredInRectWithSides(Canvas canvas, Paint paint, String text, float left, float top, float right, float bottom) {
        paint.setTextAlign(Paint.Align.CENTER);

        float textHeight = paint.descent() - paint.ascent();
        float textOffset = (textHeight / 2) - paint.descent();

        canvas.drawText(text, (left + right) / 2, (top + bottom) / 2 + textOffset, paint);
    }

    private void createValueTextLayout() {
        if (valueIsEmpty()) {
            valueTextLayout = null;
            return;
        }
        String text = value != null ? formatter.format(value) : valueSegmentText;
        if (value != null && unit != null && !unit.isEmpty())
            text += String.format(" <small>%s</small>", unit);
        Spanned spanned = Html.fromHtml(text);

        valueTextLayout = new StaticLayout(spanned, valueTextPaint, valueSignWidth, Layout.Alignment.ALIGN_CENTER, 1, 0, false);
    }

    public String getValueSegmentText() {
        return valueSegmentText;
    }

    public void setValueSegmentText(String valueSegmentText) {
        this.valueSegmentText = valueSegmentText;
        createValueTextLayout();
        invalidate();
        requestLayout();
    }


    public void setSegments(List<Segment> segments) {
        this.segments = segments;
        invalidate();
        requestLayout();
    }

    public void setUnit(String unit) {
        this.unit = unit;
        createValueTextLayout();
        invalidate();
        requestLayout();
    }

    public void setValue(Float value) {
        this.value = value;
        createValueTextLayout();
        invalidate();
        requestLayout();
    }

    public void setValueWithUnit(Float value, String unitHtml) {
        this.value = value;
        this.unit = unitHtml;
        if (!valueIsEmpty()) createValueTextLayout();
        invalidate();
        requestLayout();
    }

    public void setGapWidth(int gapWidth) {
        this.gapWidth = gapWidth;
        invalidate();
        requestLayout();
    }

    public void setBarHeight(int barHeight) {
        this.barHeight = barHeight;
        invalidate();
        requestLayout();
    }

    public void setShowDescriptionText(boolean showDescriptionText) {
        this.showDescriptionText = showDescriptionText;
        invalidate();
        requestLayout();
    }

    public void setValueSignSize(int width, int height) {
        this.valueSignWidth = width;
        this.valueSignHeight = height;
        if (!valueIsEmpty()) createValueTextLayout();
        invalidate();
        requestLayout();
    }

    public void setValueSignColor(int valueSignColor) {
        this.valueSignColor = valueSignColor;
        invalidate();
        requestLayout();
    }

    public void setShowSegmentText(boolean showSegmentText) {
        this.showSegmentText = showSegmentText;
        invalidate();
        requestLayout();
    }

    public void setSideStyle(int sideStyle) {
        this.sideStyle = sideStyle;
        invalidate();
        requestLayout();
    }

    public void setEmptySegmentColor(int emptySegmentColor) {
        this.emptySegmentColor = emptySegmentColor;
        invalidate();
        requestLayout();
    }

    public void setSideTextStyle(int sideTextStyle) {
        this.sideTextStyle = sideTextStyle;
        invalidate();
        requestLayout();
    }

    public void setDescriptionTextSize(int descriptionTextSize) {
        this.descriptionTextSize = descriptionTextSize;
        invalidate();
        requestLayout();
    }

    public void setSegmentTextSize(int segmentTextSize) {
        this.segmentTextSize = segmentTextSize;
        invalidate();
        requestLayout();
    }

    public void setValueTextSize(int valueTextSize) {
        this.valueTextSize = valueTextSize;
        valueTextPaint.setTextSize(valueTextSize);
        invalidate();
        requestLayout();
    }

    public void setDescriptionTextColor(int descriptionTextColor) {
        this.descriptionTextColor = descriptionTextColor;
        invalidate();
        requestLayout();
    }

    public void setSegmentTextColor(int segmentTextColor) {
        this.segmentTextColor = segmentTextColor;
        invalidate();
        requestLayout();
    }

    public void setValueTextColor(int valueTextColor) {
        this.valueTextColor = valueTextColor;
        valueTextPaint.setColor(valueTextColor);
        invalidate();
        requestLayout();
    }

    public void setDescriptionBoxHeight(int descriptionBoxHeight) {
        this.descriptionBoxHeight = descriptionBoxHeight;
        invalidate();
        requestLayout();
    }


    public Integer getValueSegment() {
        return valueSegment;
    }

    public void setValueSegment(Integer valueSegment) {
        this.valueSegment = valueSegment;
    }

    public static Builder builder(Context context) {
        return new SegmentedBar(context).new Builder();
    }

    public class Builder {

        private Builder() {
        }

        public Builder segments(List<Segment> segments) {
            SegmentedBar.this.segments = segments;
            return this;
        }

        public Builder unit(String unit) {
            SegmentedBar.this.unit = unit;
            SegmentedBar.this.createValueTextLayout();
            return this;
        }

        public Builder value(Float value) {
            SegmentedBar.this.value = value;
            SegmentedBar.this.createValueTextLayout();
            return this;
        }

        public Builder valueSegment(Integer valueSegment) {
            SegmentedBar.this.valueSegment = valueSegment;
            SegmentedBar.this.createValueTextLayout();
            return this;
        }

        public Builder valueSegmentText(String valueSegmentText) {
            SegmentedBar.this.valueSegmentText = valueSegmentText;
            SegmentedBar.this.createValueTextLayout();
            return this;
        }

        public Builder gapWidth(int gapWidth) {
            SegmentedBar.this.gapWidth = gapWidth;
            return this;
        }

        public Builder barHeight(int barHeight) {
            SegmentedBar.this.barHeight = barHeight;
            return this;
        }

        public Builder showDescriptionText(boolean showDescriptionText) {
            SegmentedBar.this.showDescriptionText = showDescriptionText;
            return this;
        }

        public Builder valueSignSize(int width, int height) {
            SegmentedBar.this.valueSignWidth = width;
            SegmentedBar.this.valueSignHeight = height;
            return this;
        }

        public Builder valueSignColor(int valueSignColor) {
            SegmentedBar.this.valueSignColor = valueSignColor;
            return this;
        }

        public Builder showSegmentText(boolean showText) {
            SegmentedBar.this.showSegmentText = showText;
            return this;
        }

        public Builder sideStyle(int sideStyle) {
            SegmentedBar.this.sideStyle = sideStyle;
            return this;
        }

        public Builder emptySegmentColor(int emptySegmentColor) {
            SegmentedBar.this.emptySegmentColor = emptySegmentColor;
            return this;
        }

        public Builder sideTextStyle(int sideTextStyle) {
            SegmentedBar.this.sideTextStyle = sideTextStyle;
            return this;
        }

        public Builder descriptionTextSize(int descriptionTextSize) {
            SegmentedBar.this.descriptionTextSize = descriptionTextSize;
            return this;
        }

        public Builder segmentTextSize(int segmentTextSize) {
            SegmentedBar.this.segmentTextSize = segmentTextSize;
            return this;
        }

        public Builder valueTextSize(int valueTextSize) {
            SegmentedBar.this.valueTextSize = valueTextSize;
            return this;
        }

        public Builder descriptionTextColor(int descriptionTextColor) {
            SegmentedBar.this.descriptionTextColor = descriptionTextColor;
            return this;
        }

        public Builder segmentTextColor(int segmentTextColor) {
            SegmentedBar.this.segmentTextColor = segmentTextColor;
            return this;
        }

        public Builder valueTextColor(int valueTextColor) {
            SegmentedBar.this.valueTextColor = valueTextColor;
            return this;
        }

        public Builder descriptionBoxHeight(int descriptionBoxHeight) {
            SegmentedBar.this.descriptionBoxHeight = descriptionBoxHeight;
            return this;
        }

        public SegmentedBar build() {
            return SegmentedBar.this;
        }

    }
}
