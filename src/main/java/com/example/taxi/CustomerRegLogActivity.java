package com.example.taxi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CustomerRegLogActivity extends AppCompatActivity {

    TextView customerStatus, question;
    Button singInBtn, regCustomerBtn;
    EditText emailET, passwordET;



    FirebaseAuth mAuth;
    DatabaseReference customerDbRef;
    String OnlineCustomerID;

    ProgressDialog loadingbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_reg_log);

        customerStatus = (TextView)findViewById(R.id.statusCustomer);
        question = (TextView)findViewById(R.id.accountCreateCustomer);
        singInBtn = (Button) findViewById(R.id.signInCustomer);
        regCustomerBtn = (Button)findViewById(R.id.regCustomer);
        emailET = (EditText) findViewById(R.id.customerEmail);
        passwordET = (EditText)findViewById(R.id.customerPassword);

        mAuth = FirebaseAuth.getInstance();

        loadingbar = new ProgressDialog(this);

        regCustomerBtn.setVisibility(View.INVISIBLE);
        regCustomerBtn.setEnabled(false);
        question.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                singInBtn.setVisibility(View.INVISIBLE);
                singInBtn.setEnabled(false);
                question.setVisibility(View.INVISIBLE);
                regCustomerBtn.setVisibility(View.VISIBLE);
                regCustomerBtn.setEnabled(true);
                customerStatus.setText("Регистрация пассажира");
            }
        });
        regCustomerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailET.getText().toString();
                String password = passwordET.getText().toString();
                RegisterCustomer(email,password);
            }
        });
        singInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailET.getText().toString();
                String password = passwordET.getText().toString();

                SingInCustomer(email,password);
            }
        });
    }

    private void SingInCustomer(String email, String password) {
        loadingbar.setTitle("Выполняется вход");
        loadingbar.setMessage("Пожалуйста, подождите");
        loadingbar.show();
        mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    Toast.makeText(CustomerRegLogActivity.this, "Вход выполнен", Toast.LENGTH_SHORT).show();
                    loadingbar.dismiss();
                    Intent customerIntent = new Intent(CustomerRegLogActivity.this, CustomersMapsActivity.class);
                    startActivity(customerIntent);
                }
                else{
                    Toast.makeText(CustomerRegLogActivity.this, "Произошла ошибка, попробуйте снова", Toast.LENGTH_SHORT).show();
                    loadingbar.dismiss();
                }
            }
        });
    }

    private void RegisterCustomer(String email, String password) {
        loadingbar.setTitle("Регистрация Пассажира");
        loadingbar.setMessage("Пожалуйста, подождите");
        loadingbar.show();
        mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){

                    OnlineCustomerID = mAuth.getCurrentUser().getUid();
                    customerDbRef = FirebaseDatabase.getInstance().getReference()
                            .child("Users").child("Customers").child(OnlineCustomerID);
                    customerDbRef.setValue(true);

                    Intent customerIntent = new Intent(CustomerRegLogActivity.this, CustomersMapsActivity.class);
                    startActivity(customerIntent);

                    Toast.makeText(CustomerRegLogActivity.this, "Регистрация прошла успешно", Toast.LENGTH_SHORT).show();
                    loadingbar.dismiss();

                }
                else{
                    Toast.makeText(CustomerRegLogActivity.this, "Ошибка", Toast.LENGTH_SHORT).show();
                    loadingbar.dismiss();
                }
            }
        });
    }
}
