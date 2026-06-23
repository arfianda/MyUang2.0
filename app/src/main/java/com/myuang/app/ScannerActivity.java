package com.myuang.app;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;

public class ScannerActivity extends BaseActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 71;
    private static final int REQUEST_IMAGE_CAPTURE = 72;
    private static final int REQUEST_IMAGE_PICK = 73;

    private View resultPanel;
    private TextView scanContentText;
    private TextView cameraPreviewText;
    private ImageView receiptPreviewImage;
    private String latestRecognizedText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getColorCompat(R.color.camera_dark));
        setContentView(R.layout.activity_scanner);
        if (!requireAuthenticated()) {
            return;
        }

        resultPanel = findViewById(R.id.panelScanResult);
        scanContentText = findViewById(R.id.textScanContent);
        cameraPreviewText = findViewById(R.id.textCameraPreview);
        receiptPreviewImage = findViewById(R.id.imageReceiptPreview);

        findViewById(R.id.btnScannerClose).setOnClickListener(v -> finish());
        findViewById(R.id.btnScannerFlash).setOnClickListener(v -> toast(getString(R.string.toast_scanner_flash)));
        findViewById(R.id.btnScannerGallery).setOnClickListener(v -> openGallery());
        findViewById(R.id.btnScannerManual).setOnClickListener(v -> finish());
        findViewById(R.id.btnScannerCapture).setOnClickListener(v -> openCamera());
        findViewById(R.id.btnUseScanResult).setOnClickListener(v -> {
            if (latestRecognizedText.isEmpty()) {
                toast(getString(R.string.scan_text_empty));
                return;
            }
            ReceiptParser.Result receipt = ReceiptParser.parse(latestRecognizedText);
            Intent intent = new Intent(ScannerActivity.this, AddTransactionActivity.class);
            intent.putExtra(AddTransactionActivity.EXTRA_SCAN_NOTE, latestRecognizedText);
            intent.putExtra(AddTransactionActivity.EXTRA_SCAN_MERCHANT, receipt.merchant);
            intent.putExtra(AddTransactionActivity.EXTRA_SCAN_AMOUNT, receipt.totalAmount);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
    }

    private void openCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        } catch (ActivityNotFoundException e) {
            toast(getString(R.string.scanner_no_camera));
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, getString(R.string.scanner_select_image)), REQUEST_IMAGE_PICK);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                toast(getString(R.string.scanner_camera_permission_denied));
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        if (requestCode == REQUEST_IMAGE_CAPTURE && data.getExtras() != null) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            if (bitmap != null) {
                receiptPreviewImage.setImageBitmap(bitmap);
                showPreview();
                recognizeText(InputImage.fromBitmap(bitmap, 0));
            }
        } else if (requestCode == REQUEST_IMAGE_PICK && data.getData() != null) {
            Uri imageUri = data.getData();
            receiptPreviewImage.setImageURI(imageUri);
            showPreview();
            try {
                recognizeText(InputImage.fromFilePath(this, imageUri));
            } catch (IOException e) {
                toast(getString(R.string.scanner_ocr_failed));
            }
        }
    }

    private void showPreview() {
        receiptPreviewImage.setVisibility(View.VISIBLE);
        cameraPreviewText.setVisibility(View.GONE);
        resultPanel.setVisibility(View.VISIBLE);
        scanContentText.setText(getString(R.string.scanner_processing));
    }

    private void recognizeText(InputImage image) {
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(this::showRecognizedText)
                .addOnFailureListener(e -> {
                    latestRecognizedText = "";
                    scanContentText.setText(getString(R.string.scanner_ocr_failed));
                })
                .addOnCompleteListener(task -> recognizer.close());
    }

    private void showRecognizedText(Text result) {
        latestRecognizedText = result.getText() == null ? "" : result.getText().trim();
        if (latestRecognizedText.isEmpty()) {
            scanContentText.setText(getString(R.string.scan_text_empty));
        } else {
            scanContentText.setText(latestRecognizedText);
        }
    }
}
