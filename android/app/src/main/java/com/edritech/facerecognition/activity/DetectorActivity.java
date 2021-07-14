package com.edritech.facerecognition.activity;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.util.Size;
import android.util.TypedValue;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.edritech.facerecognition.R;
import com.edritech.facerecognition.customview.OverlayView;
import com.edritech.facerecognition.env.BorderedText;
import com.edritech.facerecognition.env.ImageUtils;
import com.edritech.facerecognition.recognition.ModelFace;
import com.edritech.facerecognition.recognition.TrackerFace;
import com.edritech.facerecognition.recognition.InterfaceValidation;
import com.edritech.facerecognition.recognition.ValidationFace;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class DetectorActivity extends CameraActivity {
    private static final int TF_OD_API_INPUT_SIZE = 112;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final String TF_OD_API_MODEL_FILE = "mobile_face_net.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";

    private static final boolean MAINTAIN_ASPECT = false;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    private OverlayView trackingOverlay;
    private Integer sensorOrientation;

    public static InterfaceValidation detector;
    public static List<ModelFace> resultsUser;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;

    private boolean computingDetection = false;
    private boolean addPending = true;

    private long timestamp = 0;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private TrackerFace tracker;
    private FaceDetector faceDetector;
    private Bitmap portraitBmp = null;
    private Bitmap faceBmp = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .build();
        faceDetector = FaceDetection.getClient(options);
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        BorderedText borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new TrackerFace(this);

        try {
            detector =
                    ValidationFace.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
        } catch (final IOException e) {
            e.printStackTrace();
            finish();
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);

        int targetW, targetH;
        if (sensorOrientation == 90 || sensorOrientation == 270) {
            targetH = previewWidth;
            targetW = previewHeight;
        } else {
            targetW = previewWidth;
            targetH = previewHeight;
        }
        int cropW = (int) (targetW / 2.0);
        int cropH = (int) (targetH / 2.0);

        croppedBitmap = Bitmap.createBitmap(cropW, cropH, Config.ARGB_8888);
        portraitBmp = Bitmap.createBitmap(targetW, targetH, Config.ARGB_8888);
        faceBmp = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropW, cropH,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);
        trackingOverlay = findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(canvas -> {
            tracker.draw(canvas);
            if (isDebug()) {
                tracker.drawDebug(canvas);
            }
        });
        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        InputImage image = InputImage.fromBitmap(croppedBitmap, 0);
        faceDetector
                .process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.size() == 0) {
                        updateResults(currTimestamp, new LinkedList<>());
                        return;
                    }
                    runInBackground(() -> {
                            onFacesDetected(currTimestamp, faces, addPending);
                            addPending = false;
                    });
                });
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    private Matrix createTransform(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation) {

        Matrix matrix = new Matrix();
        if (applyRotation != 0) {
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);
            matrix.postRotate(applyRotation);
        }
        if (applyRotation != 0) {
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }
        return matrix;
    }

    private void updateResults(long currTimestamp, final List<ModelFace> mappedModelFaces) {
        tracker.trackResults(mappedModelFaces, currTimestamp);
        trackingOverlay.postInvalidate();
        computingDetection = false;

        if (mappedModelFaces.size() > 0) {
            ModelFace rec = mappedModelFaces.get(0);
            if (rec.getExtra() != null) {
                detector.register("User", rec);
            }
        }
    }

    private void onFacesDetected(long currTimestamp, List<Face> faces, boolean add) {
        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(2.0f);

        final List<ModelFace> mappedModelFaces = new LinkedList<ModelFace>();

        int sourceW = rgbFrameBitmap.getWidth();
        int sourceH = rgbFrameBitmap.getHeight();
        int targetW = portraitBmp.getWidth();
        int targetH = portraitBmp.getHeight();
        Matrix transform = createTransform(
                sourceW,
                sourceH,
                targetW,
                targetH,
                sensorOrientation);
        final Canvas cv = new Canvas(portraitBmp);
        cv.drawBitmap(rgbFrameBitmap, transform, null);
        final Canvas cvFace = new Canvas(faceBmp);

        for (Face face : faces) {
            final RectF boundingBox = new RectF(face.getBoundingBox());
            if (boundingBox != null) {
                cropToFrameTransform.mapRect(boundingBox);
                RectF faceBB = new RectF(boundingBox);
                transform.mapRect(faceBB);
                float sx = ((float) TF_OD_API_INPUT_SIZE) / faceBB.width();
                float sy = ((float) TF_OD_API_INPUT_SIZE) / faceBB.height();
                Matrix matrix = new Matrix();
                matrix.postTranslate(-faceBB.left, -faceBB.top);
                matrix.postScale(sx, sy);
                cvFace.drawBitmap(portraitBmp, matrix, null);

                String label = "";
                float confidence = -1f;
                int color = Color.RED;
                Object extra = null;
                Bitmap crop = null;
                if (add) {
                    crop = Bitmap.createBitmap(portraitBmp,
                            (int) faceBB.left,
                            (int) faceBB.top,
                            (int) faceBB.width(),
                            (int) faceBB.height());
                }
                resultsUser = detector.recognizeImage(faceBmp, add);
                if (resultsUser.size() > 0) {
                    ModelFace result = resultsUser.get(0);
                    extra = result.getExtra();
                    float conf = result.getDistance();
                    confidence = conf;
                    if (conf < 0.9f) {
                        label = result.getTitle();
                        if (result.getId().equals("0")) {
                            color = Color.GREEN;
                        } else {
                            color = Color.YELLOW;
                        }
                    }
                }
                if (getCameraFacing() == CameraCharacteristics.LENS_FACING_FRONT) {
                    Matrix flip = new Matrix();
                    if (sensorOrientation == 90 || sensorOrientation == 270) {
                        flip.postScale(1, -1, previewWidth / 2.0f, previewHeight / 2.0f);
                    } else {
                        flip.postScale(-1, 1, previewWidth / 2.0f, previewHeight / 2.0f);
                    }
                    flip.mapRect(boundingBox);
                }

                final ModelFace result = new ModelFace("0", label, confidence, boundingBox);
                result.setColor(color);
                result.setLocation(boundingBox);
                result.setExtra(extra);
                result.setCrop(crop);
                mappedModelFaces.add(result);
            }
        }
        updateResults(currTimestamp, mappedModelFaces);
    }
}
