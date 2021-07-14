package com.edritech.facerecognition.customview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.edritech.facerecognition.recognition.ModelFace;

import java.util.List;

public class RecognitionScoreView extends View implements ResultsView {
    private static final float TEXT_SIZE_DIP = 14;
    private final float textSizePx;
    private final Paint fgPaint;
    private final Paint bgPaint;
    private List<ModelFace> results;

    public RecognitionScoreView(final Context context, final AttributeSet set) {
        super(context, set);
        textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        fgPaint = new Paint();
        fgPaint.setTextSize(textSizePx);

        bgPaint = new Paint();
        bgPaint.setColor(0xcc4285f4);
    }

    @Override
    public void setResults(final List<ModelFace> results) {
        this.results = results;
        postInvalidate();
    }

    @Override
    public void onDraw(final Canvas canvas) {
        final int x = 10;
        int y = (int) (fgPaint.getTextSize() * 1.5f);

        canvas.drawPaint(bgPaint);

        if (results != null) {
            for (final ModelFace recog : results) {
                canvas.drawText(recog.getTitle() + ": " + recog.getDistance(), x, y, fgPaint);
                y += (int) (fgPaint.getTextSize() * 1.5f);
            }
        }
    }
}
