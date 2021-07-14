package com.edritech.facerecognition.recognition;

import android.graphics.Bitmap;

import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;

import java.nio.ByteBuffer;

public class HelperFace {
    private final int IMAGE_SIZE = 112;

    private final ImageProcessor imageTensorProcessor = new ImageProcessor.Builder()
            .add(new ResizeOp(IMAGE_SIZE, IMAGE_SIZE, ResizeOp.ResizeMethod.BILINEAR))
            .add(new NormalizeOp(127.5f, 127.5f))
            .build();

    public ByteBuffer convertBitmapToBuffer(Bitmap bitmap) {
        TensorImage imageTensor = imageTensorProcessor.process(TensorImage.fromBitmap(bitmap));
        return imageTensor.getBuffer();
    }
}
