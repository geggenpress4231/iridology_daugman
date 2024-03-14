package com.example.seiesdemoapp.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.BitmapFactory;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.graphics.drawable.Drawable;
import okhttp3.OkHttpClient;


import android.os.Bundle;
import android.Manifest;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.seiesdemoapp.R;
import com.example.seiesdemoapp.network.ImageProcessingService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.*;
public class ImageHelperActivity extends AppCompatActivity {

    private ImageView inputImageView;
    private TextView outputTextView;
    private Button buttonCreateLayers;
    private int REQUEST_PICK_IMAGE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_helper);

        inputImageView = findViewById(R.id.imageViewInput);
        outputTextView = findViewById(R.id.textViewOutput);
        buttonCreateLayers = findViewById(R.id.buttonIrisRecognition);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            }
        }

        buttonCreateLayers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Drawable drawable = inputImageView.getDrawable();
                if (drawable instanceof BitmapDrawable) {
                    Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                    if (bitmap != null) {
                        callImageProcessingAPI(bitmap);
                    } else {
                        outputTextView.setText("No image to process");
                    }
                } else {
                    outputTextView.setText("No valid image found");
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(ImageHelperActivity.class.getSimpleName(), "grant result for" + permissions[0] + "is" + grantResults[0]);
    }

    public void onPickImage(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    public void onStartCamera(View view) {
        // Camera start logic
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PICK_IMAGE) {
                Uri uri = data.getData();
                Bitmap bitmap = loadFromURI(uri);
                inputImageView.setImageBitmap(bitmap);
            }
        }
    }

    private Bitmap loadFromURI(Uri uri) {
        Bitmap bitmap = null;
        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                bitmap = ImageDecoder.decodeBitmap(source);
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            }
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }



    private void callImageProcessingAPI(Bitmap bitmap) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        // Setting up Retrofit with the OkHttpClient
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://localhost:8080/") // Use your server URL
                .addConverterFactory(GsonConverterFactory.create())
                .client(client) // Use the custom OkHttpClient
                .build();
        ImageProcessingService service = retrofit.create(ImageProcessingService.class);
        MultipartBody.Part imagePart = convertBitmapToMultiPart(bitmap, "image.png");

        service.processImage(imagePart).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Bitmap processedBitmap = BitmapFactory.decodeStream(response.body().byteStream());
                        runOnUiThread(() -> {
                            if (processedBitmap != null) {
                                inputImageView.setImageBitmap(processedBitmap);
                            } else {
                                Log.e("ImageDownload", "Failed to decode image");
                            }
                        });
                    } catch (Exception e) {
                        Log.e("ImageDownload", "Error processing image response: " + e.getMessage());
                    }
                } else {
                    Log.e("API Call", "Response not successful or empty response body");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("API Call", "Error: " + t.getMessage());
            }
        });
    }

    private MultipartBody.Part convertBitmapToMultiPart(Bitmap bitmap, String fileName) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        RequestBody requestBody = RequestBody.create(MediaType.parse("image/png"), baos.toByteArray());
        return MultipartBody.Part.createFormData("file", fileName, requestBody);
    }
}

