package com.cozy.apps.imagetool;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import me.shaohui.advancedluban.Luban;
import me.shaohui.advancedluban.OnCompressListener;

public class MainActivity extends AppCompatActivity {
    private static int SELECT_PHOTO = 100;
    private static int REQUEST_PERMISSION = 133;
    private static int EDIT_IMAGE_CODE = 122;
    private LinearLayout chooseBtn;
    private LinearLayout dialogSave;
    private LinearLayout dialogShare;
    private LinearLayout ConvertLayout;
    private ImageView selectImage;
    private ImageView closeSelected;
    private TextView fileName;
    private TextView fileSize;
    private TextView resolution;
    private TextView seekbarMinimum;
    private TextView seekbarMaximum;
    private TextView seekbarCurrentSize;
    private TextView mainBtnText;
    private SeekBar seekBar;
    long fileSizeInBytes;
    long convertFileSizeInBytes;
    long minimumFileSize;
    boolean imageSelected = false;
    File file = null;
    AlertDialog alertDialog;
    ImageView dialogImageView;
    File compressedFile;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        selectImage = findViewById(R.id.selectedImage);
        fileName = findViewById(R.id.selectedImageName);
        fileSize = findViewById(R.id.selectedImageSize);
        resolution = findViewById(R.id.selectedImageResolution);
        ConvertLayout = findViewById(R.id.convertLayout);
        chooseBtn = findViewById(R.id.chooseBtn);
        seekBar = findViewById(R.id.seekbar);
        seekbarMaximum = findViewById(R.id.maximumSize);
        seekbarMinimum = findViewById(R.id.minimumSize);
        seekbarCurrentSize = findViewById(R.id.currentSize);
        closeSelected = findViewById(R.id.closeSelected);
        mainBtnText = findViewById(R.id.mainBtnText);
        closeSelected.setOnClickListener(v -> {
            reset();
        });

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_imageview, null);
        dialogBuilder.setView(dialogView);
        dialogImageView =  dialogView.findViewById(R.id.dialogImageView);
        dialogSave =  dialogView.findViewById(R.id.save);
        dialogShare =  dialogView.findViewById(R.id.share);
        dialogSave.setOnClickListener(view -> {
            saveBitmapInStorage(compressedFile,this);
            alertDialog.dismiss();
        });
        dialogShare.setOnClickListener(view -> {
            Intent i =  new Intent(Intent.ACTION_SEND_MULTIPLE);
            i.setType("*/*");
            i.putExtra(Intent.EXTRA_TEXT, "Draw with Hand Drawer download now\n rb.gy/vgohuo");
            ArrayList<Uri> files = new ArrayList<>();
            files.add( FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID,compressedFile));
            i.putParcelableArrayListExtra(Intent.EXTRA_STREAM,  files);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(i);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
         alertDialog = dialogBuilder.create();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("loading...");
        chooseBtn.setOnClickListener(v -> {
            if (imageSelected) {
                String extension = "";
                int i = file.getName().lastIndexOf('.');
                if (i > 0) {
                    extension = file.getName().substring(i + 1);
                }
                Luban.compress(this, file)
                        .setMaxSize((int) (convertFileSizeInBytes / 1024))
                        .putGear(Luban.CUSTOM_GEAR)
                        .setCompressFormat(extension.equals("png") ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG)
                        .launch(new OnCompressListener() {
                            @Override
                            public void onStart() {
                                progressDialog.show();
                            }

                            @Override
                            public void onSuccess(File file) {
                                compressedFile = file;
                                Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
                                dialogImageView.setImageBitmap(bitmap);
                                progressDialog.dismiss();
                                alertDialog.show();
                            }

                            @Override
                            public void onError(Throwable e) {
                                progressDialog.dismiss();
                                Toast.makeText(MainActivity.this,"please try again",Toast.LENGTH_LONG).show();
                            }
                        });
            } else {
                openGallery();
            }
        });

//        seekBar.setMax((int) fileSizeInBytes);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            seekBar.setMin((int) (fileSizeInBytes/20));
//        }

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                convertFileSizeInBytes = Math.max(getFileSizeByPercentage(i), minimumFileSize);
                seekbarCurrentSize.setText(getFileSize(convertFileSizeInBytes));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showMessageDialog(R.string.permission_warning_quit,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        REQUEST_PERMISSION);
                            }
                        });
                return;
            }
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
            return;
        }


    }


    private void showMessageDialog(int str, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(str)
                .setPositiveButton("OK", okListener)
                .create()
                .show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
//                showMessageDialog(R.string.permission_warning_quit,
//                        new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                MainActivity.this.finish();
//                            }
//                        });
                return;
            }
        }
    }



    private void openGallery() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, SELECT_PHOTO);
    }


    private void saveBitmapInStorage(File file, Context context) {
        String filename = "QR_" + System.currentTimeMillis() + ".jpg";
        OutputStream fos = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();

            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

            Uri imageUri =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            try {
                fos = resolver.openOutputStream(imageUri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            File imagesDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File image = new File(imagesDir, filename);
            try {
                fos = new FileOutputStream(image);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        try {
            InputStream fis =  new FileInputStream(file);
            byte[] b = new byte[(int) file.length()];
            fis.read(b);
            fos.write(b);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == SELECT_PHOTO && resultCode == RESULT_OK) {
            Uri selectedImage = intent.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            if (cursor == null) {
                super.onActivityResult(requestCode, resultCode, intent);
                return;
            }
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String filePath = cursor.getString(columnIndex);
            cursor.close();
            setSelectedPhoto(filePath);

        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    void reset() {
        fileSizeInBytes = 0;
        ConvertLayout.setVisibility(View.GONE);
        mainBtnText.setText("Choose Image");
        imageSelected = false;
    }


    void setSelectedPhoto(String filePath) {
        imageSelected = true;
        mainBtnText.setText("Compress");
        file = new File(filePath);
        fileName.setText(file.getName());
        fileSizeInBytes = file.length();
        fileSize.setText(getFileSize(fileSizeInBytes));
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        resolution.setText(bitmap.getWidth() + "x" + bitmap.getHeight() + "px");
        selectImage.setImageBitmap(bitmap);
        ConvertLayout.setVisibility(View.VISIBLE);
        seekbarMaximum.setText(getFileSize(fileSizeInBytes));
        minimumFileSize = 50*1024;
        seekbarMinimum.setText(getFileSize(minimumFileSize));
        convertFileSizeInBytes = getFileSizeByPercentage(50);
        seekbarCurrentSize.setText(getFileSize(convertFileSizeInBytes));
    }

    String getFileSize(Long fileSizeInBytes) {
        long fileSizeInKB = fileSizeInBytes / 1024;
        if (fileSizeInKB > 1024) {
            long fileSizeInMB = fileSizeInKB / 1024;
            return fileSizeInMB + "mb";
        } else {
            return fileSizeInKB + "kb";
        }
    }

    long getFileSizeByPercentage(int percentage) {
        return (fileSizeInBytes / 100) * percentage;
    }
}