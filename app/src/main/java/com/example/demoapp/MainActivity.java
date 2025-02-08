package com.example.demoapp;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.Call;
import retrofit2.Callback;
import okhttp3.ResponseBody;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    ImageView imageView;
    private Button buttonSendImage,clearbtn, downloadbtn;
    private FloatingActionButton mainbtn, camerabtn, gallerybtn;
    private boolean isFabOpen = false;
    private Uri imageUri;
    private Bitmap selectedBitmap;
    private Bitmap originalBitmap;  // Stores the input image
    private Bitmap processedBitmap; // Stores the output (dehazed) image

    private TextView dehazedText, originalText;


    private TextView textView;


    ActivityResultLauncher<Intent> galleryLauncher;
    ActivityResultLauncher<Uri> cameraLauncher;
    private RecyclerView historyRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        imageView = findViewById(R.id.imgView);
        mainbtn = findViewById(R.id.main_btn);
        camerabtn = findViewById(R.id.camerabtn);
        gallerybtn = findViewById(R.id.gallerybtn);
        buttonSendImage = findViewById(R.id.buttonSendImage);
        clearbtn=findViewById(R.id.clear_button);
        textView=findViewById(R.id.textView);
        dehazedText=findViewById(R.id.dehazedImageLabel);
        originalText=findViewById(R.id.originalImageLabel);
        downloadbtn=findViewById(R.id.download_button);

        //from here





        //till here

        // Hide send button initially
        buttonSendImage.setVisibility(View.GONE);
        textView.setVisibility(View.VISIBLE);

        mainbtn.setOnClickListener(v -> {
            ObjectAnimator rotateAnim = ObjectAnimator.ofFloat(mainbtn, "rotation", 0f, 360f);
            rotateAnim.setDuration(500);
            rotateAnim.start();
            toggleFab();
        });

        gallerybtn.setOnClickListener(v -> {
            toggleFab();
            openGallery();
        });

        camerabtn.setOnClickListener(v -> {
            toggleFab();
            openCamera();
        });
        clearbtn.setOnClickListener(v -> clearBtn());




        buttonSendImage.setOnClickListener(v -> {
            if (selectedBitmap != null) {
                sendImageToServer(selectedBitmap);

                //Remember if it not works
                clearbtn.setVisibility(View.VISIBLE);
                downloadbtn.setVisibility(View.VISIBLE);
                buttonSendImage.setVisibility(View.GONE);
                textView.setVisibility(View.GONE);

            } else {
                showToast("Please select or capture an image first.");
            }
        });

        downloadbtn.setOnClickListener(v -> saveImageToStorage(processedBitmap));

        // Initialize Gallery Picker
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri selectedImageUri = result.getData().getData();
                try {
                    selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                    imageView.setImageBitmap(selectedBitmap);
                    buttonSendImage.setVisibility(View.VISIBLE); // Show button when image is set
                } catch (IOException e) {
                    e.printStackTrace();
                    showToast("Failed to load image.");
                }
            }
        });

        // Initialize Camera Launcher
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success) {
                try {
                    selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    imageView.setImageBitmap(selectedBitmap);
                    buttonSendImage.setVisibility(View.VISIBLE); // Show button when image is set
                } catch (IOException e) {
                    e.printStackTrace();
                    showToast("Failed to capture image.");
                }
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }


    private void openCamera() {
        File imageFile = new File(getExternalCacheDir(), "captured_image.jpg");
        imageUri = FileProvider.getUriForFile(this, "com.example.demoapp.provider", imageFile);  // Ensure this matches AndroidManifest

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        cameraLauncher.launch(imageUri);
    }


    private void sendImageToServer(Bitmap imageBitmap) {

        originalBitmap = imageBitmap;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();

        RequestBody requestBody = RequestBody.create(imageBytes, MediaType.parse("image/png"));
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", "image.png", requestBody);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://dehazy.run.place/")  // Update with actual ngrok URL
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<ResponseBody> call = apiService.sendImage(imagePart);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseString);
                        String imgBase64 = jsonResponse.getString("image");

                        if (imgBase64.startsWith("data:image/png;base64,")) {
                            imgBase64 = imgBase64.substring("data:image/png;base64,".length());
                        }

                        byte[] decodedString = Base64.decode(imgBase64, Base64.DEFAULT);
                        processedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                        // Show processed image
                        imageView.setAdjustViewBounds(true);
                        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        imageView.setImageBitmap(processedBitmap);

                        // Show clear button and hide send button
                        clearbtn.setVisibility(View.VISIBLE);
                        mainbtn.setVisibility(View.GONE);
                        buttonSendImage.setVisibility(View.GONE);
                        textView.setVisibility(View.GONE);
                        downloadbtn.setVisibility(View.VISIBLE);

                        // Enable long press functionality
                        enableImageComparison();

                        showToast("Image Dehazed successfully!");


                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                        resetUIOnError("Error processing response image.");
                    }
                } else {
                    resetUIOnError("Error: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                resetUIOnError("Request failed: " + t.getMessage());
                showToast("Dehazing failed please try again: " + t.getMessage());
            }
        });
    }

    private void clearBtn(){
        imageView.setImageDrawable(null);
        clearbtn.setVisibility(View.GONE);
        mainbtn.setVisibility(View.VISIBLE);
        textView.setVisibility(View.VISIBLE);
        downloadbtn.setVisibility(View.GONE);

        originalBitmap = null;
        processedBitmap = null;
        imageView.setOnTouchListener(null);

        originalText.setVisibility(View.GONE);
        dehazedText.setVisibility(View.GONE);

    }

    private void toggleFab() {
        isFabOpen = !isFabOpen;
        camerabtn.setVisibility(isFabOpen ? View.VISIBLE : View.GONE);
        gallerybtn.setVisibility(isFabOpen ? View.VISIBLE : View.GONE);
    }

    private void showToast(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private void resetUIOnError(String errorMessage) {
        showToast(errorMessage);

        // Reset UI
        imageView.setImageDrawable(null);
        clearbtn.setVisibility(View.GONE);
        mainbtn.setVisibility(View.VISIBLE);
        textView.setVisibility(View.VISIBLE);
        downloadbtn.setVisibility(View.GONE);

        originalText.setVisibility(View.GONE);
        dehazedText.setVisibility(View.GONE);
    }

    private void enableImageComparison() {
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (processedBitmap == null || originalBitmap == null) {
                    return false; // Prevents any action if images are cleared
                }

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: // On press → Show original image
                        imageView.setImageBitmap(originalBitmap);
                        originalText.setVisibility(View.VISIBLE);
                        dehazedText.setVisibility(View.GONE);
                        break;

                    case MotionEvent.ACTION_UP: // On release → Restore processed image
                    case MotionEvent.ACTION_CANCEL:
                        imageView.setImageBitmap(processedBitmap);
                        dehazedText.setVisibility(View.VISIBLE);
                        originalText.setVisibility(View.GONE);
                        break;
                }
                return true;
            }
        });
    }

    private void saveImageToStorage(Bitmap bitmap) {
        if (bitmap == null) {
            showToast("No image to save.");
            return;
        }

        // Define the directory
        File directory = new File(getExternalFilesDir(null), "DemoAppImages");
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                showToast("Failed to create directory.");
                return;
            }
        }

        // Create file in the directory
        String fileName = "dehazed_image_" + System.currentTimeMillis() + ".png";
        File imageFile = new File(directory, fileName);

        try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            showToast("Image saved successfully: " + imageFile.getAbsolutePath());
            Log.d("PATH: ", "saveImageToStorage: "+ imageFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            showToast("Failed to save image.");
        }

        // Notify gallery about the new file
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(imageFile);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
    }

    @NonNull
    @Override
    public OnBackInvokedDispatcher getOnBackInvokedDispatcher() {
        //clearBtn();
        return super.getOnBackInvokedDispatcher();
    }
}