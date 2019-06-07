package tn.insat.nodebox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.LinearLayoutCompat;

public class LoginActivity extends Activity {

    private EditText inputEmail, inputPassword;
    private ProgressBar progressBar,loading;
    private Button btnLogin;
    private TextView btnSignup,btnReset,loadingState;
    private CheckBox remeber;
    LinearLayoutCompat content;

    private FirebaseAuth auth;

    private static int SPLASH_TIME_OUT = 5000;
    int progressStatus = 0;

    Handler handler,handler_load;
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            loading.setVisibility(View.GONE);
            loadingState.setVisibility(View.GONE);
            content.setVisibility(View.VISIBLE);
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        isAlreadyLogeddIn();

        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance();

        content = (LinearLayoutCompat) findViewById(R.id.loginForm);
        inputEmail = (EditText) findViewById(R.id.email);
        inputPassword = (EditText) findViewById(R.id.password);
        progressBar = (ProgressBar) findViewById(R.id.progress_login);
        loading = (ProgressBar) findViewById(R.id.loading_pg);
        loadingState = (TextView) findViewById(R.id.loading_text);
        btnSignup = (TextView) findViewById(R.id.signup_link);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnReset = (TextView) findViewById(R.id.reset_password);
        remeber = (CheckBox) findViewById(R.id.remeber_me);

        Intent intent = getIntent();
        if (intent.getStringExtra("return")!=null && intent.getStringExtra("return").contains("true")) {
            loading.setVisibility(View.GONE);
            loadingState.setVisibility(View.GONE);
            content.setVisibility(View.VISIBLE);
        }

        handler = new Handler();
        handler_load = new Handler();

        handler.postDelayed(runnable,SPLASH_TIME_OUT);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (progressStatus<100) {
                    progressStatus++;
                    try{
                        Thread.sleep(50);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                    handler_load.post(new Runnable() {
                        @Override
                        public void run() {
                            loading.setProgress(progressStatus);
                            loadingState.setText(progressStatus+" %");
                        }
                    });

                }
            }
        }).start();

        btnLogin.setOnClickListener(new LoginOnClickListener());
        btnSignup.setOnClickListener(new SignupOnClickListener());
        btnReset.setOnClickListener(new ResetOnClickListener());
    }


    private void isAlreadyLogeddIn()
    {
        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
            finish();
        }
    }

    private class LoginOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(final View view) {
            String email = inputEmail.getText().toString();
            final String password = inputPassword.getText().toString();

            if (TextUtils.isEmpty(email)) {
                Snackbar.make(view, "Enter email address!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                return;
            }

            if (TextUtils.isEmpty(password)) {
                Snackbar.make(view, "Enter password!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }

            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.GONE);

            //authenticate user
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            progressBar.setVisibility(View.GONE);
                            if (!task.isSuccessful()) {
                                // there was an error
                                btnLogin.setVisibility(View.VISIBLE);
                                if (password.length() < 6) {
                                    inputPassword.setError("minimum_password");
                                } else {
                                    Snackbar.make(view, "authentification failed", Snackbar.LENGTH_LONG)
                                            .setAction("Action", null).show();
                                }
                            } else {
                                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                if (remeber.isChecked())
                                    intent.putExtra("remeber_me","true");
                                startActivity(intent);
                                finish();
                            }
                        }
                    });
        }
    }

    private class SignupOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            finish();
        }
    }

    private class ResetOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class));
        }
    }

}
