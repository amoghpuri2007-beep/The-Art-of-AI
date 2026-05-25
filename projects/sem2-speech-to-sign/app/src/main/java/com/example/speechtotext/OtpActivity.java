package com.example.speechtotext;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class OtpActivity extends AppCompatActivity {

    EditText etOtp;
    Button btnVerify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        etOtp = findViewById(R.id.etOtp);
        btnVerify = findViewById(R.id.btnVerify);

        // Hint color set in Java (not XML)
        etOtp.setHintTextColor(Color.parseColor("#BBBBBB"));

        btnVerify.setOnClickListener(v -> {

            String otp = etOtp.getText().toString().trim();

            if (otp.equals("1234")) {
                startActivity(new Intent(this, Main2Activity.class));
                finish();
            } else {
                etOtp.setError("Invalid OTP");
            }
        });
    }
}
