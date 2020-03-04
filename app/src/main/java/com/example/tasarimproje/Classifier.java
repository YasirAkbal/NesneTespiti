/* Copyright 2015 The TensorFlow Authors. All Rights Reserved.

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

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic interface for interacting with different recognition engines.
 */
public interface Classifier {
  class TumSonuclar {
    private int tespitSayisi;
    private int siniriGecenSayisi;
    private List<TespitEdilenler> liste;
    private float esikDegeri;
    private int id;
    private int sayac;
    private TensorFlowObjectDetectionAPIModel tfapi;
    private int maksTespit;

    public TumSonuclar(TensorFlowObjectDetectionAPIModel tfapi, float[] _konum,
                       float[] _sinif, float[] _skor,
                       float[] _tespitSayisi,ArrayList<String> secilenler, int maksTespit, float esikDegeri) {

      this.id = sayac++;
      this.tfapi = tfapi;
      this.maksTespit = maksTespit;
      this.esikDegeri = esikDegeri;

      liste = new ArrayList<TespitEdilenler>();
      tespitSayisi = (int)_tespitSayisi[0];
      for(int i=0;i<tespitSayisi && siniriGecenSayisi < maksTespit ;i++)
      {
        if(_skor[i] < esikDegeri || (secilenler.size() != 0
                && !secilenler.contains(tfapi.labelIndexAt((int)_sinif[i]-1))))
          continue;
        liste.add(new TespitEdilenler(new RectF(_konum[4*i+1],_konum[4*i],
                _konum[4*i+3],_konum[4*i+2]),_skor[i],tfapi.labelIndexAt((int)_sinif[i]-1)));
        siniriGecenSayisi++;
      }
    }

    class TespitEdilenler
    {
      private RectF kutu;
      private float skor;
      private String sinif;

      public TespitEdilenler(RectF kutu, float skor, String sinif) {
        this.kutu = kutu;
        this.skor = skor;
        this.sinif = sinif;
      }

      public RectF getKutu() {
        return kutu;
      }

      public float getSkor() {
        return skor;
      }

      public String getSinif() {
        return sinif;
      }

      @Override
      public String toString() {
        return "Tespitler{" +
                "kutu=" + kutu +
                ", skor=" + skor +
                ", sinif='" + sinif + '\'' +
                '}';
      }
    }

    public int getTespitSayisi() {
      return tespitSayisi;
    }

    public int getSiniriGecenSayisi() {
      return siniriGecenSayisi;
    }

    public List<TespitEdilenler> getListe() {
      return liste;
    }

    @Override
    public String toString() {

      String cikti = "Tespit sayısı = " + tespitSayisi + "\nSınırı geçenlerin sayısı = " + siniriGecenSayisi +
              "\nEşik değeri = " + esikDegeri + "\n<----------------------------->";
      for(int i=0;i<siniriGecenSayisi;i++)
      {
        TespitEdilenler temp = liste.get(i);
        cikti += "\nNesne adı = " + temp.getSinif() + "\nSkor = " +
                temp.getSkor() + "\n<----------------------------->";
      }
      return cikti;
    }
  }

  TumSonuclar analizEt(Bitmap bitmap,ArrayList<String> secilenler,int maxTespit,float esik);

  void enableStatLogging(final boolean debug);

  String getStatString();

  void close();

}
