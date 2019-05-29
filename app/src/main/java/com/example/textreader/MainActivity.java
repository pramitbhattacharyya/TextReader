package com.example.textreader;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.support.annotation.Nullable;

import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    static int k=0;
    private  static final int RESULT_LOAD_IMAGE=1, RESULT_CAP_IMG=0;
    ImageView imgupl;
    Button btnupl,capture,send;
    TextView res;
    File photofile;
    String Path="";
    String currentPhotoPath;
    Bitmap Imagetotake;
    boolean save=false;
    public static final String TESS_DATA = "/tessdata";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/Android/data/com.example.textreader/files";
    private TessBaseAPI tessBaseAPI;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgupl=(ImageView)findViewById(R.id.imgupl);
        btnupl=(Button)findViewById(R.id.btnupl);
        capture=(Button)findViewById(R.id.capture);
        res=(TextView)findViewById(R.id.result);
        send=(Button)findViewById(R.id.send);
        if(Build.VERSION.SDK_INT>=23){
            requestPermissions(new String[]
                            {Manifest.permission.CAMERA,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    2);
        }
        setOnClickListener();
    }

    public  void setOnClickListener()
    {
        btnupl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gallaryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(gallaryIntent, RESULT_LOAD_IMAGE);

//                    Toast.makeText(MainActivity.this,"Path= "+Path,Toast.LENGTH_LONG).show();
            }
        });
        imgupl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call any function here to execute when the image is clicked.
                if(k==0)
                    Toast.makeText(MainActivity.this,"Insert Appropriate Image",Toast.LENGTH_SHORT).show();
            }
        });

        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                alertDialogBuilder.setMessage("Are you sure, " +
                        "Do you want to save the image");
                alertDialogBuilder.setPositiveButton("yes",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                save=true;
                                Toast.makeText(MainActivity.this,"The Image Will be Saved"
                                        ,Toast.LENGTH_LONG).show();
                                    captureImage();
                            }
                        });

                alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        save=false;
                        Intent captureImg=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(captureImg,RESULT_CAP_IMG);
                    }
                });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(k==0)
                    Toast.makeText(MainActivity.this,
                            "No image has been selected",
                            Toast.LENGTH_SHORT)
                            .show();
                else {
                    Toast.makeText(MainActivity.this,"The Image is getting processed. Please wait",Toast.LENGTH_LONG).show();
                    prepareTessData();
                    ProcessImage pm=new ProcessImage();
                    pm.execute();
                }
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==RESULT_LOAD_IMAGE && resultCode==RESULT_OK)
        {
            k=1;
            Uri selectedImage=data.getData();
            imgupl.setImageURI(selectedImage);
            Path=selectedImage.getPath();
            try {
                Imagetotake=BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        if(requestCode==RESULT_CAP_IMG && resultCode==RESULT_OK)
        {
            k=1;
            if(!save) {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                imgupl.setImageBitmap(bitmap);
                Imagetotake=bitmap;
            }
            else{
                Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                imgupl.setImageBitmap(bitmap);
                Imagetotake=bitmap;
            }
        }
    }

    private  void captureImage(){
         Intent captureImg=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(captureImg.resolveActivity(getPackageManager())!=null) {
            photofile = null;
            try {
                photofile = saveImage();
                if (photofile != null) {
//                    captureImg = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                    if (captureImg.resolveActivity(getPackageManager()) != null)
//                        startActivityForResult(captureImg, RESULT_CAP_IMG);
                    currentPhotoPath = photofile.getAbsolutePath();

                    Uri photoUri = FileProvider.getUriForFile(MainActivity.this,
                            "com.example.textreader.fileprovider",
                            photofile);
                    captureImg.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    startActivityForResult(captureImg,RESULT_CAP_IMG);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private File saveImage() {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "TR" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }
    private void prepareTessData(){
        try{
            File dir = new File(DATA_PATH+TESS_DATA);
            if(!dir.exists()){
                if (!dir.mkdir()) {
                    Toast.makeText(MainActivity.this, "The folder " + dir.getPath() + "was not created", Toast.LENGTH_SHORT).show();
                }
            }
            String fileList[] = getAssets().list("");
            for(String fileName : fileList){
                String pathToDataFile = dir + "/" + fileName;

                if(!((new File(pathToDataFile)).exists())){
//                    Toast.makeText(MainActivity.this, "Dir" + dir , Toast.LENGTH_SHORT).show();
//                    Toast.makeText(MainActivity.this, "Filename: " + fileName , Toast.LENGTH_SHORT).show();
                    InputStream in = getAssets().open(fileName);
//                    InputStream in=new FileInputStream("app/assets/"+fileName);
                    OutputStream out = new FileOutputStream(pathToDataFile);

                    byte [] buff = new byte[1024];
                    int len ;
                    while(( len = in.read(buff)) > 0){
                        out.write(buff,0,len);
                    }
                    in.close();
                    out.close();
                }
            }
        } catch (Exception e) {
//            Toast.makeText(MainActivity.this,"Error in prepareTessData",Toast.LENGTH_LONG).show();
            Log.e(TAG, e.getMessage());
        }
    }

    private String startOCR(){
        String result="Not found.";
        try{
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = false;
            options.inSampleSize = 6;
//            Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, options);
                result = this.getText(Imagetotake);
            res.setText(result);
        }catch (Exception e){
            Toast.makeText(MainActivity.this,"Error in startOCR",Toast.LENGTH_LONG).show();
            Log.e(TAG, e.getMessage());
        }finally {
            return result;
        }
    }

    private String getText(Bitmap bitmap){
        try{
            tessBaseAPI = new TessBaseAPI();
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
        tessBaseAPI.init(DATA_PATH, "eng");
        tessBaseAPI.setImage(bitmap);
        String retStr = "No result";
        try{
            retStr = tessBaseAPI.getUTF8Text();
        }catch (Exception e){
            Toast.makeText(MainActivity.this,"Error in getText",Toast.LENGTH_LONG).show();
            Log.e(TAG, e.getMessage());
        }
        tessBaseAPI.end();
        return retStr;
    }

    public class ProcessImage extends AsyncTask<Void,Void,Void>
    {
        Handler h=new Handler();
        @Override
        protected Void doInBackground(Void... strings) {
            final String str=startOCR();
            h.post(new Runnable() {
                @Override
                public void run() {
                    res.setText(str);
                    Toast.makeText(MainActivity.this,"The Processing is Done",Toast.LENGTH_SHORT).show();
                }
            });
            return null;
        }
    }
}