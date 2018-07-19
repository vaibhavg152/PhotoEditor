package com.example.vaibhav.photoeditor;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Permissions;

import static java.lang.Math.floorDiv;
import static java.lang.Math.min;

public class EditActivity extends AppCompatActivity {
    private static final String TAG = "EditActivity";

    private static final short MAX_DIMENSION = 1920, REQUEST_IMAGE = 1, REQUEST_BG = 2;
    private ImageView imgSample;
    private Spinner spinnerAlignment;
    private FloatingActionButton fabAddImage;
    private float blurScale=0.4f;
    private String image_Name;
    private Bitmap bitmapImage,bmpBg,bmpBgCurrent;
    private Button btnChangeBg,btnSave;
    private int progressBlur,imageHeight,imageWidth,align=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        Log.d(TAG, "onCreate: ");

        fabAddImage = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        imgSample = (ImageView) findViewById(R.id.imgSample);
        btnChangeBg = (Button) findViewById(R.id.btnChangebg);
        btnSave = (Button) findViewById(R.id.btnSaveImage);
        spinnerAlignment = (Spinner) findViewById(R.id.SpinnerAlignment);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.align_options,android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAlignment.setAdapter(adapter);

        spinnerAlignment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (bitmapImage!=null)
                    alignBg(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        fabAddImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageChooser(REQUEST_IMAGE);
            }
        });

        btnChangeBg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeBgDialog();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveImage();
            }
        });
    }

    private void saveImage() {
        Log.d(TAG, "saveImage: ");

        FileOutputStream stream;
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()+"/GreatPhotosEdited";
        File myDir = new File(filePath);
        if (!myDir.exists()) myDir.mkdirs();
        File file = new File(myDir,image_Name+"edited.jpg");
        try {
            Log.d(TAG, "saveImage: fone");
            stream = new FileOutputStream(file);
            BitmapDrawable drawable = (BitmapDrawable) imgSample.getDrawable();
            Bitmap src = drawable.getBitmap();

            src.compress(Bitmap.CompressFormat.JPEG,100,stream);
            stream.close();

            toastMessage("Image Saved Successfully!");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            toastMessage("Error Couldnt Save the photo");
            if (Build.VERSION.SDK_INT >= 23){
                Log.d(TAG, "saveImage: moreBuild");
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                    Log.d(TAG, "saveImage: Not granted");
                    ActivityCompat.requestPermissions(EditActivity.this,new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                }
            }
            else Log.d(TAG, "saveImage: lessBuild");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void alignBg(int position) {
        Log.d(TAG, "alignBg: ");

        align = position;
        Bitmap finalImage = getSquareImage(bitmapImage);
        imgSample.setImageBitmap(finalImage);
    }

    private void changeBgDialog() {
        Log.d(TAG, "changeBgDialog: ");

        AlertDialog.Builder builder = new AlertDialog.Builder(EditActivity.this);
        String[] array = getResources().getStringArray(R.array.bg_options);
        builder.setSingleChoiceItems(array, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichPos) {
                if (whichPos==0)
                    changeBlur();
                else if (whichPos==1)
                    choosePattern();
                else if (whichPos==2)
                    openImageChooser(REQUEST_BG);
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void choosePattern() {
        Log.d(TAG, "choosePattern: ");

        AlertDialog.Builder builder = new AlertDialog.Builder(EditActivity.this);
        builder.setTitle("Select a Pattern");
        String[] array = new String[5];
        for (int i=1; i<=array.length; i++){
            array[i-1] = "Pattern"+i;
        }

        builder.setSingleChoiceItems(array, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                Log.d(TAG, "onClick: "+which);
                     if (which==0) bmpBg = BitmapFactory.decodeResource(EditActivity.this.getResources(),R.drawable.pattern1);
                else if (which==1) bmpBg = BitmapFactory.decodeResource(EditActivity.this.getResources(),R.drawable.pattern2);
                else if (which==2) bmpBg = BitmapFactory.decodeResource(EditActivity.this.getResources(),R.drawable.pattern3);
                else if (which==3) bmpBg = BitmapFactory.decodeResource(EditActivity.this.getResources(),R.drawable.pattern4);
                else if (which==4) bmpBg = BitmapFactory.decodeResource(EditActivity.this.getResources(),R.drawable.pattern5);

                createBlurBackground(bmpBg,blurScale);
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void changeBlur() {
        Log.d(TAG, "changeBlur: ");

        final Dialog dialog = new Dialog(EditActivity.this);
        dialog.setContentView(R.layout.seekbar);

        final TextView txtTitle = (TextView) dialog.findViewById(R.id.txtSeekBar);
        final SeekBar seekBarBlur = (SeekBar) dialog.findViewById(R.id.seekBar);
        final Button btnDone = (Button) dialog.findViewById(R.id.btnDoneSeekbar);

        seekBarBlur.setProgress(((int) (100 * blurScale)));
        txtTitle.setText("Blur: "+seekBarBlur.getProgress()+ "%");

        seekBarBlur.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressBlur = progress;
                txtTitle.setText("Blur: "+progress+ "/" +seekBar.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                txtTitle.setText("Blur: "+seekBarBlur.getProgress()+ "%");
            }
        });

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                blurScale = (float) progressBlur/seekBarBlur.getMax();
                createBlurBackground(bmpBg,blurScale);

                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void openImageChooser(int request_code) {
        Log.d(TAG, "openImageChooser: "+request_code);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, request_code);
    }

    private Bitmap getSquareImage(Bitmap oldImage) {
        Log.d(TAG, "getSquareImage: ");

        Bitmap result, background = bmpBgCurrent;
        int size = background.getWidth();
        result = Bitmap.createBitmap(size, size, background.getConfig());
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(background, new Matrix(), null);

        float extraSpace;
        if (imageHeight>imageWidth) {

            if (align == 0)
                extraSpace = (float)(size-imageWidth)/2;
            else if (align == 1)
                extraSpace = (float)(size-imageWidth);
            else
                extraSpace = 0f;

            canvas.drawBitmap(oldImage,extraSpace,0f,null);
        }else if (imageHeight<imageWidth){

            if (align == 'C')
                extraSpace = (float)(size-imageHeight)/2;
            else if (align == 'E')
                extraSpace = (float)(size-imageHeight);
            else
                extraSpace = 0f;

            canvas.drawBitmap(oldImage,0f,extraSpace,null);
        }
//
////
////            if (imageHeight>MAX_DIMENSION){
////                Log.d(TAG, "getSquareImage: Scaling down "+imageHeight);
////
////                float scaleDown = (float) MAX_DIMENSION/imageHeight;
////                imageHeight *= scaleDown;
////                imageWidth  *= scaleDown;
////                oldImage = Bitmap.createScaledBitmap(oldImage,imageHeight,imageWidth,false);
////            }
//            final int extraSpace = (imageHeight-imageWidth)/2;
//            Log.d(TAG, "getSquareImage: "+imageHeight+" "+ imageWidth+" "+ extraSpace+":::"+background.getHeight()+":"+background.getWidth());
//
//            Bitmap left  = Bitmap.createBitmap(background,0,0,extraSpace,imageHeight);
//            Bitmap right = Bitmap.createBitmap(background,background.getWidth() - extraSpace,0,extraSpace,imageHeight);
//            result = Bitmap.createBitmap(imageHeight, imageHeight, Bitmap.Config.ARGB_8888);
//
//            Canvas comboImage = new Canvas(result);
//
//            comboImage.drawBitmap(left,0f,0f,null);
//            comboImage.drawBitmap(oldImage, extraSpace, 0f, null);
//            comboImage.drawBitmap(right, imageWidth+extraSpace, 0f, null);
//
//        }
////
////            if (imageWidth > MAX_DIMENSION){
////                Log.d(TAG, "getSquareImage: Scaling down "+imageHeight);
////
////                float scaleDown = (float) MAX_DIMENSION/imageWidth;
////                imageHeight *= scaleDown;
////                imageWidth  *= scaleDown;
////                oldImage = Bitmap.createScaledBitmap(oldImage,imageHeight,imageWidth,false);
////            }
//
//            final int extraSpace = (imageWidth-imageHeight)/2;
//
//            Log.d(TAG, "getSquareImage: "+background.getHeight()+":"+background.getWidth()+":::"+imageHeight+":"+imageWidth+" "+extraSpace);
//
//            Bitmap top    = Bitmap.createBitmap(background,0,0,imageWidth,extraSpace);
//            Bitmap bottom = Bitmap.createBitmap(background,0,background.getHeight()-extraSpace,imageWidth,extraSpace);
//            result = Bitmap.createBitmap(imageWidth, imageWidth, Bitmap.Config.ARGB_8888);
//
//            Canvas comboImage = new Canvas(result);
//
//            comboImage.drawBitmap(top,0f,0f,null);
//            comboImage.drawBitmap(oldImage, 0f, top.getHeight(),null);
//            comboImage.drawBitmap(bottom,0f, imageHeight+extraSpace, null);
//
//        }
//        else result = oldImage;
        return result;
    }

    private void createBlurBackground(Bitmap oldImage, float blurSc) {
        Log.d(TAG, "createBlurBackground: ");

//
//        if (imageHeight>MAX_DIMENSION){
//            Log.d(TAG, "getSquareImage: Scaling down");
//
//            float scaleDown = (float) MAX_DIMENSION/(imageHeight>imageWidth ? imageHeight : imageWidth);
//            imageHeight *= scaleDown;
//            imageWidth  *= scaleDown;
//            oldImage = Bitmap.createScaledBitmap(oldImage,imageHeight,imageWidth,true);
//        }
        int startX=0,startY=0,size;
        if (imageHeight==imageWidth){
            size=imageWidth;
        }
        else if (imageHeight>imageWidth) {

            startY = (imageHeight-imageWidth)/2;
            size = imageHeight;
        }
        else {
            startX = (imageWidth-imageHeight)/2;
            size = imageWidth;
        }
        oldImage = Bitmap.createScaledBitmap(oldImage, size,size, true);
        Bitmap temp = Bitmap.createBitmap(oldImage, startX, startY,size-startX,size-startY);

        if (blurSc == 1f)
            blurSc = 0.99f;
        if (blurSc != 0f)
            temp = blur(this, temp, blurSc, 7);

        bmpBgCurrent = Bitmap.createScaledBitmap(temp, size, size, true);

        Bitmap img = getSquareImage(bitmapImage);
        imgSample.setImageBitmap(img);

    }

    public void toastMessage(String s){
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_SHORT).show();
    }

    public static Bitmap blur(Context context, Bitmap image, float scale, int radius) {
        Log.d(TAG, "blur: ");

        scale = (1-scale)*0.75f;
        if (scale==0f)
            scale=0.00001f;
        int width = Math.round(image.getWidth() * scale);
        int height = Math.round(image.getHeight() * scale);

        Bitmap inputBitmap = Bitmap.createScaledBitmap(image, width, height, false);
        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
        Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);
        theIntrinsic.setRadius(radius);
        theIntrinsic.setInput(tmpIn);
        theIntrinsic.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);

        return outputBitmap;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: ");

        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Log.d(TAG, "onActivityResult: OK");

            if (requestCode == REQUEST_IMAGE) {
                Uri uriImage = data.getData();
                try {
                    File file = new File(uriImage.toString());
                    image_Name = file.getName();

                    bitmapImage = MediaStore.Images.Media.getBitmap(getContentResolver(), uriImage);
                    bmpBg = bitmapImage;
                    imageHeight = bitmapImage.getHeight();
                    imageWidth = bitmapImage.getWidth();
                    createBlurBackground(bitmapImage, blurScale);

                    fabAddImage.setVisibility(View.INVISIBLE);
                    btnChangeBg.setVisibility(View.VISIBLE);

                } catch (IOException e) {
                    toastMessage("Error! choose Another Image");
                    e.printStackTrace();
                }
            } else if (requestCode == REQUEST_BG) {

                Uri uriImage = data.getData();
                Log.d(TAG, "onActivityResult: 2");
                try {
                    bmpBg = MediaStore.Images.Media.getBitmap(getContentResolver(), uriImage);
                    createBlurBackground(bmpBg, blurScale);
                } catch (IOException e) {
                    e.printStackTrace();
                    toastMessage("Error! choose Another Image");
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
            //resume tasks needing this permission

            FileOutputStream stream;
            String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()+"/GreatPhotosEdited";
            File myDir = new File(filePath);
            if (!myDir.exists()) myDir.mkdirs();
            File file = new File(myDir,image_Name+"edited.jpg");
            try {
                Log.d(TAG, "saveImage: fone");
                stream = new FileOutputStream(file);
                BitmapDrawable drawable = (BitmapDrawable) imgSample.getDrawable();
                Bitmap src = drawable.getBitmap();

                src.compress(Bitmap.CompressFormat.JPEG,100,stream);
                stream.close();

                toastMessage("Image Saved Successfully!");

            }catch (FileNotFoundException e){
                e.printStackTrace();
                toastMessage("Could not load image");
            }catch (IOException e){
                e.printStackTrace();
                toastMessage("Could not load image");
            }
        }
        else Log.d(TAG, "onRequestPermissionsResult: "+grantResults[0]);
    }
}
