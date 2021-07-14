package com.edritech.facerecognition.recognition;

import android.graphics.Bitmap;

import java.util.List;

public interface InterfaceValidation {

    void register(String name, ModelFace modelFace);

    List<ModelFace> recognizeImage(Bitmap bitmap, boolean getExtra);

    void enableStatLogging(final boolean debug);

    String getStatString();

    void close();
}
