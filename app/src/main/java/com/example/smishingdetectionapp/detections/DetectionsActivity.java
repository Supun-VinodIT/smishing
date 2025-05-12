package com.example.smishingdetectionapp.detections;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smishingdetectionapp.MainActivity;
import com.example.smishingdetectionapp.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.FileOutputStream;
import java.io.File;
import android.os.Environment;
import android.media.MediaScannerConnection;

public class DetectionsActivity extends AppCompatActivity {

    private ListView detectionLV;
    DatabaseAccess databaseAccess;

    public void searchDB(String search) {
        String searchQuery = "SELECT * FROM Detections WHERE Phone_Number LIKE '%" + search + "%' OR Message LIKE '%" + search + "%' OR Date LIKE '%" + search + "%'";
        Cursor cursor = DatabaseAccess.db.rawQuery(searchQuery, null);
        DisplayDataAdapterView adapter = new DisplayDataAdapterView(this, cursor);
        detectionLV.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    public void sortONDB() {
        String searchQuery = "SELECT * FROM Detections ORDER BY Date ASC";
        Cursor cursor = DatabaseAccess.db.rawQuery(searchQuery, null);
        DisplayDataAdapterView adapter = new DisplayDataAdapterView(this, cursor);
        detectionLV.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    public void sortNODB() {
        String searchQuery = "SELECT * FROM Detections ORDER BY Date DESC";
        Cursor cursor = DatabaseAccess.db.rawQuery(searchQuery, null);
        DisplayDataAdapterView adapter = new DisplayDataAdapterView(this, cursor);
        detectionLV.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    public void refreshList() {
        String searchQuery = "SELECT * FROM Detections";
        Cursor cursor = DatabaseAccess.db.rawQuery(searchQuery, null);
        DisplayDataAdapterView adapter = new DisplayDataAdapterView(this, cursor);
        detectionLV.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    public void DeleteRow(String id) {
        DatabaseAccess.db.delete("Detections", "_id = ?", new String[]{id});
    }

    private void saveRadioButtonState(String key, boolean isChecked) {
        SharedPreferences sharedPreferences = getSharedPreferences("RadioPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, isChecked);
        editor.apply();
    }

    private void clearRadioButtonState() {
        SharedPreferences sharedPreferences = getSharedPreferences("RadioPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    @Override
    protected void onStop() {
        super.onStop();
        clearRadioButtonState();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detections);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton detections_back = findViewById(R.id.detections_back);
        detections_back.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            clearRadioButtonState();
        });

        detectionLV = findViewById(R.id.lvDetectionsList);
        databaseAccess = new DatabaseAccess(getApplicationContext());
        databaseAccess.open();

        Cursor cursor = DatabaseAccess.db.rawQuery("SELECT * FROM Detections", null);
        DisplayDataAdapterView adapter = new DisplayDataAdapterView(this, cursor);
        detectionLV.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        EditText detSearch = findViewById(R.id.searchTextBox);
        detSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchDB(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        SharedPreferences sharedPreferences = getSharedPreferences("RadioPrefs", MODE_PRIVATE);
        ImageView filterBtn = findViewById(R.id.filterBtn);
        filterBtn.setOnClickListener(v -> {
            View bottomSheet = getLayoutInflater().inflate(R.layout.popup_filter, null);
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(DetectionsActivity.this);
            bottomSheetDialog.setContentView(bottomSheet);
            bottomSheetDialog.show();

            RadioButton OldToNewRB = bottomSheet.findViewById(R.id.OldToNewRB);
            RadioButton NewToOldRB = bottomSheet.findViewById(R.id.NewToOldRB);

            OldToNewRB.setChecked(sharedPreferences.getBoolean("OldToNewRB", false));
            NewToOldRB.setChecked(sharedPreferences.getBoolean("NewToOldRB", false));

            OldToNewRB.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (OldToNewRB.isChecked()) {
                    NewToOldRB.setChecked(false);
                    sortONDB();
                }
                saveRadioButtonState("OldToNewRB", isChecked);
            });

            NewToOldRB.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (NewToOldRB.isChecked()) {
                    OldToNewRB.setChecked(false);
                    sortNODB();
                }
                saveRadioButtonState("NewToOldRB", isChecked);
            });
        });

        detectionLV.setOnItemLongClickListener((parent, view, position, id) -> {
            View bottomSheetDel = getLayoutInflater().inflate(R.layout.popup_deleteitem, null);
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(DetectionsActivity.this);
            bottomSheetDialog.setContentView(bottomSheetDel);
            bottomSheetDialog.show();

            Button Cancel = bottomSheetDel.findViewById(R.id.delItemCancel);
            Button Confirm = bottomSheetDel.findViewById(R.id.DelItemConfirm);

            Cancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

            Confirm.setOnClickListener(v -> {
                DeleteRow(String.valueOf(id));
                refreshList();
                bottomSheetDialog.dismiss();
                Toast.makeText(getApplicationContext(), "Detection Deleted!", Toast.LENGTH_SHORT).show();
            });

            return true;
        });
        //export pdf
        Button exportBtn = findViewById(R.id.exportPdfBtn);
        exportBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportDetectionsToPDF();
            }
        });


    }

    private void exportDetectionsToPDF() {
        Cursor cursor = DatabaseAccess.db.rawQuery("SELECT * FROM Detections", null);
        if (cursor.getCount() == 0) {
            Toast.makeText(this, "No detections to export", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create the document
        Document document = new Document();
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "detections_report.pdf");
        String filePath = file.getAbsolutePath();

        try {
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();
            document.add(new Paragraph("Smishing Detections Report\n\n"));

            while (cursor.moveToNext()) {
                String phone = cursor.getString(cursor.getColumnIndexOrThrow("Phone_Number"));
                String message = cursor.getString(cursor.getColumnIndexOrThrow("Message"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("Date"));

                document.add(new Paragraph("Phone: " + phone));
                document.add(new Paragraph("Message: " + message));
                document.add(new Paragraph("Date: " + date));
                document.add(new Paragraph("\n"));
            }

            document.close();

            MediaScannerConnection.scanFile(
                    this,
                    new String[] { file.getAbsolutePath() },
                    new String[] { "application/pdf" },
                    null
            );

            Toast.makeText(this, "PDF exported to: " + filePath, Toast.LENGTH_LONG).show();

            Toast.makeText(this, "PDF exported to: " + filePath, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to export PDF", Toast.LENGTH_SHORT).show();
        }
    }

}
