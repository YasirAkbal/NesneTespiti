package com.example.tasarimproje;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.widget.AdapterView.OnItemClickListener;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends CameraActivity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";

    private CameraView mOpenCvCameraView;
    private boolean mCameraStarted = false;
    private int resizeGenislik;
    private int resizeUzunluk;
    private static Classifier classifier;
    private final String fileName = "resim.jpg";
    private ArrayList<String> siniflar = new ArrayList<>();
    private ArrayAdapter<String> siniflarAdaptor;
    private ArrayList<String> secilenSiniflar = new ArrayList<>();
    private FloatingActionButton fabKamera;

    private ImageView imageView;
    private Toolbar toolbar;
    private ListView listeNesne;

    private AlertDialog.Builder builderTespit;
    private AlertDialog alertTespit;
    private SeekBar seekBar;

    private AlertDialog.Builder builderEsik;
    private AlertDialog alertEsik;
    private SeekBar seekBarEsik;

    private AlertDialog.Builder builderNesne;
    private AlertDialog alertNesne;

    private AlertDialog.Builder builderEkranCoz;
    private AlertDialog alertEkranCoz;
    private ListView listeEkranCoz;

    private AlertDialog.Builder builderResimCoz;
    private AlertDialog alertResimCoz;
    private ListView listeResimCoz;

    private AlertDialog.Builder builderModelSecici;
    private AlertDialog alertModelSecici;
    private ListView listeModelSecici;


    private List<Size> ekranCozSizeList;
    private List<Size> resimCozSizeList;
    private ArrayAdapter<String> ekranCozAdapter;
    private ArrayAdapter<String> resimCozAdapter;
    private ArrayAdapter<String> modelSeciciAdapter;
    private int secilenModelId;
    private int oncekiModelId = -1;

    private final String benimModel = "model_benim_ssd_mobilenet_v2.pb";
    private final String benimLabels = "labelmap_benim_ssd_mobilenet_v2.txt";
    private final String ssdMnetv2oidv4Model = "model_ssd_mnet_v2_oid_v4.pb";
    private final String ssdMnetv2oidv4Labels = "labelmap_ssd_mnet_v2_oid_v4.txt";
    private final String ssdMnetv2cocoModel = "model_ssd_mnet_v2_coco.pb";
    private final String ssdMnetv2cocoLabels = "labelmap_ssd_mnet_v2_coco.txt";
    private final String ssdMnetv1fpncocoModel = "model_ssd_mnet_v1_fpn640_coco.pb";
    private final String ssdMnetv1fpncocoLabels = "labelmap_ssd_mnet_v1_fpn640_coco.txt";

    private String secilenModel;
    private String secilenModelLabels;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }


    private void siniflariOku() {

        BufferedReader br = null;
        String okunan;
        if(!siniflar.isEmpty())
            siniflar.clear();
        try {
            br = new BufferedReader(new InputStreamReader(getAssets().open(secilenModelLabels)));
            while((okunan = br.readLine()) != null)
            {
                siniflar.add(okunan);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(br != null) {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private ArrayList<String> sizeToStringList(List<Size> liste)
    {
        ArrayList<String> temp = new ArrayList<>();
        for(Size s : liste)
        {
            temp.add(s.width + "x" + s.height);
        }
        return temp;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        fabKamera = findViewById(R.id.fabKamera);

        imageView = findViewById(R.id.imageView);
        imageView.setVisibility(View.GONE);
        imageView.setClickable(false);
        imageView.setEnabled(false);

        mOpenCvCameraView = (CameraView) findViewById(R.id.tutorial3_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.options_menu);
        toolbar.setOnMenuItemClickListener(ilkToolbarListener);

        ekranCozSizeList = mOpenCvCameraView.getResolutionList();
        resimCozSizeList = mOpenCvCameraView.getPictureSizeList();

        ekranCozAdapter = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_list_item_single_choice,android.R.id.text1,
                sizeToStringList(mOpenCvCameraView.getResolutionList()));
        listeEkranCoz = new ListView(MainActivity.this);
        listeEkranCoz.setAdapter(ekranCozAdapter);
        listeEkranCoz.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        ekranCozIcinOlustur();

        resimCozAdapter = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_list_item_single_choice,android.R.id.text1,
                sizeToStringList(mOpenCvCameraView.getPictureSizeList()));
        listeResimCoz = new ListView(MainActivity.this);
        listeResimCoz.setAdapter(resimCozAdapter);
        listeResimCoz.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        resimCozIcinOlustur();

        maksTespitSeciciOlustur();
        esikGirisiIcinOlustur();

        fabKamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setClickable(false);
                mOpenCvCameraView.takePicture(fileName);
            }
        });
        modelSeciciIcinOlustur();
        alertModelSecici.show();
    }

    Toolbar.OnMenuItemClickListener ilkToolbarListener = new Toolbar.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId())
            {
                case R.id.action_esik:
                    alertEsik.show();
                    return true;
                case R.id.action_maksTespit:
                    alertTespit.show();
                    return true;
                case R.id.action_nesneSecimi:
                    alertNesne.show();
                    return true;
                case R.id.action_iptal:
                    popupGecis(true,false);
                    kamerayaDon();
                    fabKamera.setClickable(true);
                    return true;
                case R.id.action_resmiKaydet:
                    if(mOpenCvCameraView.resmiKaydet())
                        Toast.makeText(MainActivity.this,"Resim kaydedildi",Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(MainActivity.this,"Resim kaydedilemedi",Toast.LENGTH_SHORT).show();
                    return true;
                case R.id.action_sonuclariKaydet:
                    if(mOpenCvCameraView.sonuclariKaydet())
                        Toast.makeText(MainActivity.this,"Sonuçlar kaydedildi",Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(MainActivity.this,"Sonuçlar kaydedilemedi",Toast.LENGTH_SHORT).show();
                    return true;
                case R.id.action_ekranCozunurluk:
                    alertEkranCoz.show();
                    return true;
                case R.id.action_resimCozunurluk:
                    alertResimCoz.show();
                    return true;
                case R.id.action_modelDegistir:
                    alertModelSecici.show();
                    return true;
                case R.id.action_resimYukle:
                    showFileChooser();
                    return true;
                default:
                    return false;
            }
        }
    };


    private boolean modelYukle()
    {
        try {
            classifier = TensorFlowObjectDetectionAPIModel.create(getAssets(),secilenModel,secilenModelLabels,resizeGenislik,resizeUzunluk);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return  false;
        }
    }

    public void popupGecis(boolean deger1, boolean deger2)
    {
        toolbar.getMenu().findItem(R.id.action_esik).setEnabled(deger1);
        toolbar.getMenu().findItem(R.id.action_maksTespit).setEnabled(deger1);
        toolbar.getMenu().findItem(R.id.action_nesneSecimi).setEnabled(deger1);
        toolbar.getMenu().findItem(R.id.action_ekranCozunurluk).setEnabled(deger1);
        toolbar.getMenu().findItem(R.id.action_resimCozunurluk).setEnabled(deger1);
        toolbar.getMenu().findItem(R.id.action_esik).setVisible(deger1);
        toolbar.getMenu().findItem(R.id.action_maksTespit).setVisible(deger1);
        toolbar.getMenu().findItem(R.id.action_nesneSecimi).setVisible(deger1);
        toolbar.getMenu().findItem(R.id.action_ekranCozunurluk).setVisible(deger1);
        toolbar.getMenu().findItem(R.id.action_resimCozunurluk).setVisible(deger1);
        toolbar.getMenu().findItem(R.id.action_modelDegistir).setVisible(deger1);
        toolbar.getMenu().findItem(R.id.action_resimYukle).setVisible(deger1);

        toolbar.getMenu().findItem(R.id.action_iptal).setEnabled(deger2);
        toolbar.getMenu().findItem(R.id.action_resmiKaydet).setEnabled(deger2);
        toolbar.getMenu().findItem(R.id.action_sonuclariKaydet).setEnabled(deger2);
        toolbar.getMenu().findItem(R.id.action_iptal).setVisible(deger2);
        toolbar.getMenu().findItem(R.id.action_resmiKaydet).setVisible(deger2);
        toolbar.getMenu().findItem(R.id.action_sonuclariKaydet).setVisible(deger2);

        fabKamera.setEnabled(deger1);
        if(deger1)
            fabKamera.show();
        else
            fabKamera.hide();
    }

    private void modelSeciciIcinOlustur()
    {
        builderModelSecici = new AlertDialog.Builder(MainActivity.this);
        alertModelSecici = builderModelSecici.create();
        List<String> temp = new ArrayList<>();
        temp.add("SSD MobileNet v2 - SCUT HEAD DataSet Part A");
        temp.add("SSD MobileNet v2 - Open Image Dataset v4");
        temp.add("SSD MobileNet v2 - COCO Dataset");
        temp.add("SSD MobileNet v1 Fpn - COCO Dataset");
        modelSeciciAdapter = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_list_item_single_choice,android.R.id.text1,temp);
        listeModelSecici = new ListView(MainActivity.this);
        listeModelSecici.setAdapter(modelSeciciAdapter);
        listeModelSecici.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        alertModelSecici.setView(listeModelSecici);
        alertModelSecici.setTitle("Kullanmak istediğiniz modeli seçiniz.");
        alertModelSecici.setCancelable(false);
        listeModelSecici.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                if(oncekiModelId == -1)
                    oncekiModelId = position;
                else
                    oncekiModelId = secilenModelId;
                secilenModelId = position;
            }
        });

        alertModelSecici.setButton(AlertDialog.BUTTON_POSITIVE, "Seç", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.e("secilen",secilenModelId + "");
                if(secilenModel != null && oncekiModelId == secilenModelId)
                    return;
                switch(secilenModelId)
                {
                    case 0:
                        secilenModel = benimModel;
                        secilenModelLabels = benimLabels;
                        resizeGenislik = resizeUzunluk = 300;
                        break;
                    case 1:
                        secilenModel = ssdMnetv2oidv4Model;
                        secilenModelLabels = ssdMnetv2oidv4Labels;
                        resizeUzunluk = resizeGenislik = 300;
                        break;
                    case 2:
                        secilenModel = ssdMnetv2cocoModel;
                        secilenModelLabels = ssdMnetv2cocoLabels;
                        resizeUzunluk = resizeGenislik = 300;
                        break;
                    case 3:
                        secilenModel = ssdMnetv1fpncocoModel;
                        secilenModelLabels = ssdMnetv1fpncocoLabels;
                        resizeUzunluk = resizeGenislik = 640;
                        break;
                    default:
                        Toast.makeText(MainActivity.this,"Seçilen model bulunamadı.",Toast.LENGTH_SHORT).show();
                        return;
                }
                alertModelSecici.getButton(AlertDialog.BUTTON_POSITIVE).setClickable(false);
                listeModelSecici.setClickable(false);
                if(modelYukle())
                {
                    Toast.makeText(MainActivity.this,modelSeciciAdapter.getItem(secilenModelId) + " modeli başarıyla yüklendi.",Toast.LENGTH_SHORT).show();
                    nesneSecimiIcinOlustur();
                }
                else
                    Toast.makeText(MainActivity.this,modelSeciciAdapter.getItem(secilenModelId) + " modeli yüklenirken hata oluştu..",Toast.LENGTH_SHORT).show();

                alertModelSecici.getButton(AlertDialog.BUTTON_POSITIVE).setClickable(true);
            }
        });
    }


    public void kamerayaDon()
    {
        imageView.setVisibility(View.GONE);
        imageView.setClickable(false);
        imageView.setEnabled(false);

        mOpenCvCameraView.setClickable(true);
        mOpenCvCameraView.setEnabled(true);
        mOpenCvCameraView.setVisibility(View.VISIBLE);
    }

    public void nesneSecimiIcinOlustur()
    {
        siniflariOku();
        siniflarAdaptor = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_list_item_multiple_choice,android.R.id.text1,siniflar);
        listeNesne = new ListView(MainActivity.this);
        listeNesne.setAdapter(siniflarAdaptor);
        listeNesne.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        builderNesne = new AlertDialog.Builder(MainActivity.this);
        alertNesne = builderNesne.create();
        alertNesne.setTitle("Nesne Seçimi");
        alertNesne.setView(listeNesne);
        alertNesne.setButton(AlertDialog.BUTTON_NEGATIVE, "İptal", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        alertNesne.setButton(AlertDialog.BUTTON_POSITIVE, "Tamam", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SparseBooleanArray boolSecilenler = listeNesne.getCheckedItemPositions();
                secilenSiniflar.clear();

                for(int i=0;i<boolSecilenler.size();i++)
                {
                    if(boolSecilenler.valueAt(i))
                        secilenSiniflar.add(siniflar.get(boolSecilenler.keyAt(i)));
                }
            }
        });
    }

    public void maksTespitSeciciOlustur()
    {
        builderTespit = new AlertDialog.Builder(MainActivity.this);
        alertTespit = builderTespit.create();

        alertTespit.setTitle("Maksimum Tespit Sayısı");
        alertTespit.setMessage("50");
        seekBar = new SeekBar(MainActivity.this);
        seekBar.setLeft(1);
        seekBar.setRight(100);
        seekBar.setProgress(50);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                alertTespit.setMessage(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        alertTespit.setButton(AlertDialog.BUTTON_POSITIVE, "Tamam", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                Log.e("max",""+seekBar.getProgress());
            }
        });
        alertTespit.setView(seekBar);
    }

    public void esikGirisiIcinOlustur()
    {
        builderEsik = new AlertDialog.Builder(MainActivity.this);
        alertEsik = builderEsik.create();
        alertEsik.setTitle("Eşik Değeri");
        alertEsik.setMessage("0.5");
        seekBarEsik = new SeekBar(MainActivity.this);
        seekBarEsik.setProgress(50);
        seekBarEsik.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                alertEsik.setMessage(String.valueOf(progress/100.0f));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        alertEsik.setButton(AlertDialog.BUTTON_POSITIVE, "Tamam", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        alertEsik.setView(seekBarEsik);
    }

    public void ekranCozIcinOlustur()
    {
        builderEkranCoz = new AlertDialog.Builder(MainActivity.this);
        alertEkranCoz = builderEkranCoz.create();
        alertEkranCoz.setTitle("Ekran Çözünürlüğü");
        alertEkranCoz.setButton(AlertDialog.BUTTON_POSITIVE, "Tamam", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this,"Ekran çözünürlüğü " + mOpenCvCameraView.getResolution().width
                        + "x" + mOpenCvCameraView.getResolution().height + " olarak atandı.",Toast.LENGTH_SHORT).show();
            }
        });
        alertEkranCoz.setView(listeEkranCoz);
        listeEkranCoz.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mOpenCvCameraView.setResolution(ekranCozSizeList.get(position).width,ekranCozSizeList.get(position).height);
            }
        });
    }

    public void resimCozIcinOlustur()
    {
        builderResimCoz = new AlertDialog.Builder(MainActivity.this);
        alertResimCoz = builderResimCoz.create();
        alertResimCoz.setTitle("Ekran Çözünürlüğü");
        alertResimCoz.setButton(AlertDialog.BUTTON_POSITIVE, "Tamam", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this,"Resim çözünürlüğü " + mOpenCvCameraView.getResimCozunurluk().width
                        + "x" + mOpenCvCameraView.getResimCozunurluk().height + " olarak atandı.",Toast.LENGTH_SHORT).show();
            }
        });
        listeResimCoz.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mOpenCvCameraView.ResimBoyutuAta(resimCozSizeList.get(position).width,resimCozSizeList.get(position).height);
            }
        });
        alertResimCoz.setView(listeResimCoz);
    }


    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mCameraStarted = true;
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        return inputFrame.rgba();
    }


    public static Classifier getClassifier()
    {
        return classifier;
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        View decor_View = getWindow().getDecorView();

        int ui_Options = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        decor_View.setSystemUiVisibility(ui_Options);
        super.onWindowFocusChanged(hasFocus);
    }

    public ArrayList<String> getSecilenSiniflar() {
        return secilenSiniflar;
    }

    void resmiAktifEt()
    {
        imageView.setEnabled(true);
        imageView.setClickable(true);
        imageView.setVisibility(View.VISIBLE);
    }

    public int getResizeGenislik() {
        return resizeGenislik;
    }

    public int getResizeUzunluk() {
        return resizeUzunluk;
    }

    private void showFileChooser() {
        Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.setType("*/*");
        chooseFile = Intent.createChooser(chooseFile, "Choose a file");
        startActivityForResult(chooseFile, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mOpenCvCameraView.resimIslemleri(bitmap);
        }
    }

    public SeekBar getSeekBar() {
        return seekBar;
    }

    public SeekBar getSeekBarEsik() {
        return seekBarEsik;
    }

    public ImageView getImageView() {
        return imageView;
    }
}
