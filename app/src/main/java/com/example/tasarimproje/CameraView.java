package com.example.tasarimproje;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.opencv.android.JavaCameraView;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

public class CameraView extends JavaCameraView implements PictureCallback {

    private static final Scalar[] renkler = {new Scalar(255,0,0,255),new Scalar(0,255,0,255),new Scalar(0,0,255,255),new Scalar(255,255,0,255),new Scalar(255,0,127,255),
            new Scalar(127,0,255,255),new Scalar(25,51,0,255),new Scalar(153,153,0,255)};
    private Random random = new Random();
    private static final String TAG = "Sample::Tutorial3View";
    private String mPictureFileName;
    private Context context;
    private Bitmap sonCikti;
    private MainActivity aktivite;
    private final int fontScale = 1;
    private Bitmap ciktiResim;
    private Classifier.TumSonuclar tumSonuclar;
    private int resimGenislik = 1280;
    private int resimUzunluk = 720;

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        aktivite = ((MainActivity)context);
        setResolution(1280,720);
    }


    public List<Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public List<Size> getPictureSizeList()
    {
        return mCamera.getParameters().getSupportedPictureSizes();
    }

    public void setResolution(int genislik, int uzunluk) {
        disconnectCamera();
        mMaxHeight = uzunluk;
        mMaxWidth = genislik;
        connectCamera(getWidth(), getHeight());
    }

    public Size getResimCozunurluk()
    {
        return mCamera.new Size(resimGenislik,resimUzunluk);
    }


    public void ResimBoyutuAta(int genislik, int uzunluk)
    {
        this.resimGenislik = genislik;
        this.resimUzunluk = uzunluk;
    }

    private void resimBoyutuAta(int genislik, int uzunluk)
    {
        Camera.Parameters params = mCamera.getParameters();

        params.setPictureSize(genislik,uzunluk);
        mCamera.setParameters(params);
    }

    public Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }

    public void takePicture(final String fileName) {
        Log.i(TAG, "Taking picture");
        resimBoyutuAta(resimGenislik,resimUzunluk);
        this.mPictureFileName = fileName;
        // Postview and jpeg are sent in the same buffers if the queue is not empty when performing a capture.
        // Clear up buffers to avoid mCamera.takePicture to be stuck because of a memory issue
        mCamera.setPreviewCallback(null);

        // PictureCallback is implemented by the current class
        mCamera.takePicture(null, null, this);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.i(TAG, "Saving a bitmap to file");
        // The camera preview was automatically stopped. Start it again.
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);

        Bitmap image = BitmapFactory.decodeByteArray(data,0,data.length);

        resimIslemleri(image);
    }

    public void resimIslemleri(Bitmap image)
    {
        Bitmap boyutlandirilmis = Bitmap.createScaledBitmap(image,
                aktivite.getResizeGenislik(),aktivite.getResizeUzunluk(),false);
        long start,end;
        start = System.currentTimeMillis();
        tumSonuclar = MainActivity.getClassifier().analizEt(boyutlandirilmis,
                aktivite.getSecilenSiniflar(),aktivite.getSeekBar().getProgress(),
                aktivite.getSeekBarEsik().getProgress()/100.0f);
        end = System.currentTimeMillis();
        Log.e("test zamani",""+(end-start));

        List<Classifier.TumSonuclar.TespitEdilenler> liste = tumSonuclar.getListe();

        Log.e("sonuclar", tumSonuclar.toString());

        int uzunluk = image.getHeight();
        int genislik = image.getWidth();
        Mat mat = new Mat(uzunluk,genislik, CvType.CV_8UC4); //4

        Utils.bitmapToMat(image,mat);//5

        Log.e("getSiniriGecenSayisi",String.valueOf(tumSonuclar.getSiniriGecenSayisi()));
        for(int i = 0; i< tumSonuclar.getSiniriGecenSayisi(); i++)
        {
            RectF kare = liste.get(i).getKutu();
            Scalar renk = renkler[random.nextInt(renkler.length)];
            String yazi = liste.get(i).getSinif();
            Imgproc.rectangle(mat,new Point(kare.left*genislik,kare.top*uzunluk),
                    new Point(kare.right*genislik,kare.bottom*uzunluk),renk,3);
            Imgproc.putText(mat,yazi,new Point(kare.left*genislik+fontScale,
                    kare.top*uzunluk-fontScale),Imgproc.FONT_HERSHEY_COMPLEX,fontScale,renk);
        }

        ciktiResim = Bitmap.createBitmap(genislik,uzunluk, Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(mat,ciktiResim);

        aktivite.getImageView().setImageBitmap(ciktiResim);

        this.setVisibility(GONE);
        this.setEnabled(false);

        aktivite.popupGecis(false,true);
        aktivite.resmiAktifEt();
    }

    public boolean resmiKaydet()
    {
        return savebitmap(ciktiResim,mPictureFileName,this);
    }

    private static boolean savebitmap(Bitmap bmp,String ad, CameraView cv){

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

        return cv.kaydet(bytes.toByteArray(),cv.essizAdOlustur("resim","jpg"));
    }


    public Bitmap getSonCikti() {
        return sonCikti;
    }

    public boolean sonuclariKaydet()
    {
        return kaydet(tumSonuclar.toString().getBytes(),essizAdOlustur("sonuc","txt"));
    }

    private String essizAdOlustur(String bas, String uzanti)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String currentDateandTime = sdf.format(new Date());
        String dosyAdi = "/" + bas + "_" + currentDateandTime + "." + uzanti;
        return dosyAdi;
    }

    private boolean kaydet(byte[] veri,String yol)
    {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        if(file.exists())
            file.mkdir();
        try {
            File kayitfile = new File(file,yol);
            FileOutputStream fos = new FileOutputStream(kayitfile);
            fos.write(veri);
            fos.flush();
            fos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("hata",e.getMessage());
            return false;
        }
    }
}
