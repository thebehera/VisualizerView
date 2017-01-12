package com.github.thebehera.visualizerviewlib;

import android.content.Context;
import android.content.res.TypedArray;
import android.databinding.BindingAdapter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import java.util.ArrayList;
import java.util.List;


public class VisualizerView extends View {

    /**
     * The frequency of the sinus wave. The higher the value, the more sinus wave peaks you will have.
     * Default: 1.5
     */
    private float frequency;
    /**
     * The current amplitude
     */
    private float amplitude;
    /**
     * The amplitude that is used when the incoming amplitude is near zero.
     * Setting a value greater 0 provides a more vivid visualization.
     * Default: 0.01
     */
    private float idleAmplitude;
    private int numberOfWaves;
    private float phaseShift;
    private float density;
    private float level;
    private float phase;

    private Paint primaryLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint secondaryLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<Path> pathsToDraw = new ArrayList<>();
    private List<Paint> paintsToDraw = new ArrayList<>();
    private Picture cachedPicture = new Picture();



    public VisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public VisualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public VisualizerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray constantsDefined = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.VisualizerView, 0, 0);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        primaryLinePaint.setStrokeJoin(Paint.Join.ROUND);
        primaryLinePaint.setStrokeCap(Paint.Cap.ROUND);
        primaryLinePaint.setStrokeMiter(10);
        primaryLinePaint.setStyle(Paint.Style.STROKE);
        secondaryLinePaint.setStrokeJoin(Paint.Join.ROUND);
        secondaryLinePaint.setStrokeCap(Paint.Cap.ROUND);
        secondaryLinePaint.setStrokeMiter(10);
        secondaryLinePaint.setStyle(Paint.Style.STROKE);


        try {
            frequency = constantsDefined.getFloat(R.styleable.VisualizerView_frequency, 1.5f);
            amplitude = constantsDefined.getFloat(R.styleable.VisualizerView_amplitude, 1.0f);
            idleAmplitude = constantsDefined.getFloat(R.styleable.VisualizerView_idleAmplitude, 0.01f);
            numberOfWaves = constantsDefined.getInt(R.styleable.VisualizerView_numberOfWaves, 5);
            phaseShift = constantsDefined.getFloat(R.styleable.VisualizerView_phaseShift, -0.15f);
            int densityPXDefault = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, displayMetrics));
            density = constantsDefined.getDimension(R.styleable.VisualizerView_density, densityPXDefault);
            float primaryLineWidth = constantsDefined.getDimension(R.styleable.VisualizerView_primaryWaveWidth,
                    Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3.0f, displayMetrics)));
            primaryLinePaint.setStrokeWidth(primaryLineWidth);

            float secondaryLineWidth = constantsDefined.getDimension(R.styleable.VisualizerView_secondaryWaveWidth,
                    Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.0f, displayMetrics)));
            secondaryLinePaint.setStrokeWidth(secondaryLineWidth);

            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
            primaryLinePaint.setColor(constantsDefined.getColor(R.styleable.VisualizerView_primaryWaveColor, typedValue.data));
            context.getTheme().resolveAttribute(R.attr.colorAccent, typedValue, true);
            int secondaryWaveColor = typedValue.data;
            secondaryLinePaint.setColor(secondaryWaveColor);

            level = constantsDefined.getFloat(R.styleable.VisualizerView_level, 0.5f);
        } finally {
            constantsDefined.recycle();
        }

        for (int i = 0; i < numberOfWaves; i++) {
            Paint p = new Paint(i==0 ? primaryLinePaint: secondaryLinePaint);
            paintsToDraw.add(p);
        }

        // Not using any layers to animate ... so software should be faster than hardware
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }
    }


    public void setLevel(float level) {
        this.level = level;
        this.phase += phaseShift;
        this.amplitude = Math.max(level, idleAmplitude);
        List<Path> paths = new ArrayList<>(numberOfWaves);
        RectF boundingRect = new RectF(0, 0, 0, 0);
        Picture tempPicture = new Picture();
        Canvas recordingCanvas = tempPicture.beginRecording(getWidth(), getHeight());
        for (int lineNumber = numberOfWaves - 1; lineNumber >= 0; lineNumber--) {
            Path path = new Path();
            Paint paintForLine = lineNumber == 0 ? new Paint(primaryLinePaint) : new Paint(secondaryLinePaint);

            float halfHeight = getHeight() /2f;
            float mid = getWidth()/2f;

            float maxAmplitude = halfHeight - (paintForLine.getStrokeWidth() / 2f);
            float progress = 1f - (((float)lineNumber) / ((float)numberOfWaves));
            float normedAmplitude = (1.5f * progress - 0.5f) * amplitude;
            float multiplier = Math.min(1.0f, (progress / 3f * 2f) + (1.0f / 3.0f));
            int paintColor = paintForLine.getColor();
            float multipliedValueOnZeroToOneScale = zeroTo255TozeroToOne(Color.alpha(paintColor)) * multiplier;
            int newAlpha = zeroToOneScaleToZero255(multipliedValueOnZeroToOneScale);
            int color = Color.argb(newAlpha,Color.red(paintColor), Color.green(paintColor), Color.blue(paintColor));
            paintForLine.setColor(color);
            for (float x = 0; x < getWidth() + density; x+=density) {
                double scaling = -Math.pow(1f/mid * (x - mid), 2f)+ 1f;
                float y = (float)(scaling * maxAmplitude * normedAmplitude * Math.sin(2f * Math.PI * (x / getWidth()) * frequency + phase) + halfHeight);
                if (x == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
                boundingRect.union(x, y);
            }
            recordingCanvas.drawPath(path, paintForLine);
//            paths.add(path);
        }
        tempPicture.endRecording();

        cachedPicture = tempPicture;
//        pathsToDraw.clear();
//        pathsToDraw.addAll(paths);
        Rect roundedOutBounds = new Rect();
        boundingRect.roundOut(roundedOutBounds);
        if (Looper.myLooper() == Looper.getMainLooper()) {
            invalidate(roundedOutBounds);
        } else  {
            postInvalidate(roundedOutBounds.left, roundedOutBounds.top, roundedOutBounds.right, roundedOutBounds.bottom);
        }
    }

    @BindingAdapter("app:level")
    public static void setAudioLevel(View view, float level) {
        if (view instanceof VisualizerView) {
            VisualizerView self =(VisualizerView) view;
            self.setLevel(level);
        }
    }

    private float zeroTo255TozeroToOne(int zeroTo255) {
        return (float)zeroTo255 / 255f;
    }

    private int zeroToOneScaleToZero255(float zeroToOne) {
        return  Math.max(Math.min(255, ((int)Math.round(Math.floor(zeroToOne == 1.0 ? 255 : zeroToOne * 256.0)))), 0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
//        for (int i = 0; i < pathsToDraw.size() && i < paintsToDraw.size(); i++) {
//            canvas.drawPath(pathsToDraw.get(i), paintsToDraw.get(i));
//        }

        canvas.drawPicture(cachedPicture);

    }

    public float getLevel() {
        return level;
    }
}
