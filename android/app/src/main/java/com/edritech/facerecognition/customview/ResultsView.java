package com.edritech.facerecognition.customview;

import com.edritech.facerecognition.recognition.ModelFace;

import java.util.List;

public interface ResultsView {
    public void setResults(final List<ModelFace> results);
}
