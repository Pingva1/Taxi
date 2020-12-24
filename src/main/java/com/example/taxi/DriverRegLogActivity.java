package com.example.taxi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverRegLogActivity extends AppCompatActivity {

    TextView driverStatus, question;
    Button singInBtn, regDriverBtn;
    EditText emailET, passwordET;

    FirebaseAuth mAuth;
    DatabaseReference DriverDbRef;
    String OnlineDriverID;

    ProgressDialog loadingbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_reg_log);

        driverStatus = (TextView)findViewById(R.id.statusDriver);
        question = (TextView)findViewById(R.id.accountCreate);
        singInBtn = (Button) findViewById(R.id.signInDriver);
        regDriverBtn = (Button)findViewById(R.id.regDriver);
        emailET = (EditText) findViewById(R.id.driverEmail);
        passwordET = (EditText)findViewById(R.id.driverPassword);



        mAuth = FirebaseAuth.getInstance();

        loadingbar = new ProgressDialog(this);
        regDriverBtn.setVisibility(View.INVISIBLE);
        regDriverBtn.setEnabled(false);

        question.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                singInBtn.setVisibility(View.INVISIBLE);
                singInBtn.setEnabled(false);
                question.setVisibility(View.INVISIBLE);
                regDriverBtn.setVisibility(View.VISIBLE);
                regDriverBtn.setEnabled(true);
                driverStatus.setText("Регистрация водителя");
            }
        });
        
        regDriverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailET.getText().toString();
                String password = passwordET.getText().toString();
                RegisterDriver(email,password);
            }
        });
        singInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailET.getText().toString();
                String password = passwordET.getText().toString();

                SingInDriver(email,password);
            }
        });
    }

    private void SingInDriver(String email, String password) {
        loadingbar.setTitle("Выполняется вход");
        loadingbar.setMessage("Пожалуйста, подождите");
        loadingbar.show();
        mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    Toast.makeText(DriverRegLogActivity.this, "Вход выполнен", Toast.LENGTH_SHORT).show();
                    loadingbar.dismiss();
                    Intent driverIntent = new Intent(DriverRegLogActivity.this, DriversMapsActivity.class);
                    startActivity(driverIntent);
                }
                else{
                    Toast.makeText(DriverRegLogActivity.this, "Произошла ошибка, попробуйте снова", Toast.LENGTH_SHORT).show();
                    loadingbar.dismiss();
                }
            }
        });
    }

    private void RegisterDriver(String email, String password) {
        loadingbar.setTitle("Регистрация водителя");
        loadingbar.setMessage("Пожалуйста, подождите");
        loadingbar.show();
        mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){

                    OnlineDriverID = mAuth.getCurrentUser().getUid();
                    DriverDbRef = FirebaseDatabase.getInstance().getReference()
                            .child("Users").child("Drivers").child(OnlineDriverID);
                    DriverDbRef.setValue(true);

                    Intent driverIntent = new Intent(DriverRegLogActivity.this, DriversMapsActivity.class);
                    startActivity(driverIntent);

                    Toast.makeText(DriverRegLogActivity.this, "Регистрация прошла успешно", Toast.LENGTH_SHORT).show();
                    loadingbar.dismiss();

                }
                else{
                    Toast.makeText(DriverRegLogActivity.this, "Ошибка", Toast.LENGTH_SHORT).show();
                    loadingbar.dismiss();
                }
            }
        });
    }
}