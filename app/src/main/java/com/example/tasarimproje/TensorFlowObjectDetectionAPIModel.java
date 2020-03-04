/* Copyright 2016 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.example.tasarimproje;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Trace;
import android.util.Log;

import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Wrapper for frozen detection models trained using the Tensorflow Object Detection API:
 * github.com/tensorflow/models/tree/master/research/object_detection
 */
public class TensorFlowObjectDetectionAPIModel implements com.example.tasarimproje.Classifier {

  // O
  private static int MAX_RESULTS = 100;

  // Config values.
  private String inputName;
  private int inputSizeX;
  private int inputSizeY;

  // Pre-allocated buffers.
  private Vector<String> labels = new Vector<String>();
  private int[] intValues;
  private byte[] byteValues;
  private float[] outputLocations;
  private float[] outputScores;
  private float[] outputClasses;
  private float[] outputNumDetections;
  private String[] outputNames;

  private boolean logStats = false;

  private TensorFlowInferenceInterface inferenceInterface;


  public static Classifier create(final AssetManager assetManager,
                                  final String modelFilename, final String labelFilename,
                                  final int inputSizeX,final int inputSizeY) throws IOException
  {
      final TensorFlowObjectDetectionAPIModel d = new TensorFlowObjectDetectionAPIModel();

      InputStream labelsInput = null;
      labelsInput = assetManager.open(labelFilename);
      BufferedReader br = null;
      br = new BufferedReader(new InputStreamReader(labelsInput));
      String line;
      while ((line = br.readLine()) != null)
      {
        d.labels.add(line);
      }
      br.close();

      d.inferenceInterface = new TensorFlowInferenceInterface(assetManager, modelFilename);


      final Graph g = d.inferenceInterface.graph();

      d.inputName = "image_tensor";

      final Operation inputOp = g.operation(d.inputName);
      if (inputOp == null)
      {
        throw new RuntimeException("Failed to find input Node '" + d.inputName + "'");
      }
      d.inputSizeX = inputSizeX;
      d.inputSizeY = inputSizeY;

      final Operation outputOp1 = g.operation("detection_scores");
      if (outputOp1 == null)
      {
        throw new RuntimeException("Failed to find output Node 'detection_scores'");
      }
      final Operation outputOp2 = g.operation("detection_boxes");
      if (outputOp2 == null)
      {
        throw new RuntimeException("Failed to find output Node 'detection_boxes'");
      }
      final Operation outputOp3 = g.operation("detection_classes");
      if (outputOp3 == null)
      {
        throw new RuntimeException("Failed to find output Node 'detection_classes'");
      }

      d.outputNames = new String[] {"detection_boxes", "detection_scores",
                                    "detection_classes", "num_detections"};
      d.intValues = new int[d.inputSizeX * d.inputSizeY];
      d.byteValues = new byte[d.inputSizeX * d.inputSizeY * 3];
      d.outputScores = new float[MAX_RESULTS];
      d.outputLocations = new float[MAX_RESULTS * 4];
      d.outputClasses = new float[MAX_RESULTS];
      d.outputNumDetections = new float[1];
      return d;
  }

  private TensorFlowObjectDetectionAPIModel() {}

  public TumSonuclar analizEt(final Bitmap bitmap, ArrayList<String> secilenler,
                              int maxTespit, float esikDegeri)
  {

    Trace.beginSection("recognizeImage");
    Trace.beginSection("preprocessBitmap");

    bitmap.getPixels(intValues, 0, bitmap.getWidth(),
            0, 0, bitmap.getWidth(), bitmap.getHeight());

    for (int i = 0; i < intValues.length; ++i)
    {
      byteValues[i * 3 + 2] = (byte) (intValues[i] & 0xFF);
      byteValues[i * 3 + 1] = (byte) ((intValues[i] >> 8) & 0xFF);
      byteValues[i * 3 + 0] = (byte) ((intValues[i] >> 16) & 0xFF);
    }
    Trace.endSection();

    Trace.beginSection("feed");
    inferenceInterface.feed(inputName, byteValues, 1, inputSizeX, inputSizeY, 3);
    Trace.endSection();

    Trace.beginSection("run");
    inferenceInterface.run(outputNames, logStats);
    Trace.endSection();

    Trace.beginSection("fetch");
    outputLocations = new float[MAX_RESULTS * 4];
    outputScores = new float[MAX_RESULTS];
    outputClasses = new float[MAX_RESULTS];
    outputNumDetections = new float[1];
    inferenceInterface.fetch(outputNames[0], outputLocations);
    inferenceInterface.fetch(outputNames[1], outputScores);
    inferenceInterface.fetch(outputNames[2], outputClasses);
    inferenceInterface.fetch(outputNames[3], outputNumDetections);
    Trace.endSection();

    return new TumSonuclar(this,outputLocations,outputClasses,outputScores,
            outputNumDetections,secilenler,maxTespit,esikDegeri);
  }

  @Override
  public void enableStatLogging(final boolean logStats) {
    this.logStats = logStats;
  }

  @Override
  public String getStatString() {
    return inferenceInterface.getStatString();
  }

  @Override
  public void close() {
    inferenceInterface.close();
  }

  public String labelIndexAt(int indeks) {
    return labels.get(indeks);
  }


}
