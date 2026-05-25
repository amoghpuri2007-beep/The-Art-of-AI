package com.example.speechtotext;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    EditText etName, etEmail, etPhone;
    Button btnContinue;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etName      = findViewById(R.id.etName);
        etEmail     = findViewById(R.id.etEmail);
        etPhone     = findViewById(R.id.etPhone);
        btnContinue = findViewById(R.id.btnContinue);

        int hintColor = Color.parseColor("#BBBBBB");
        etName.setHintTextColor(hintColor);
        etEmail.setHintTextColor(hintColor);
        etPhone.setHintTextColor(hintColor);

        db = FirebaseFirestore.getInstance();

        btnContinue.setOnClickListener(v -> {

            String name  = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();

            if (name.isEmpty()) { etName.setError("Enter name"); return; }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { etEmail.setError("Invalid email"); return; }
            if (phone.length() != 10) { etPhone.setError("Enter valid 10-digit phone"); return; }

            btnContinue.setEnabled(false);
            btnContinue.setText("Saving...");

            Map<String, Object> user = new HashMap<>();
            user.put("name",      name);
            user.put("email",     email);
            user.put("phone",     phone);
            user.put("timestamp", System.currentTimeMillis());

            db.collection("users")
                    .document(email)
                    .set(user)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Welcome, " + name + "!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, OtpActivity.class);
                        intent.putExtra("name", name);
                        intent.putExtra("email", email);
                        startActivity(intent);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnContinue.setEnabled(true);
                        btnContinue.setText("Continue");
                    });
        });
    }
}
