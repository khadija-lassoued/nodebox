package tn.insat.nodebox;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class ResetPasswordActivity extends AppCompatActivity {

    private Button btnreset;
    private TextView btnSignin, inputEmail;
    private ProgressBar progressBar;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        btnreset = findViewById(R.id.btnReset);
        btnSignin = findViewById(R.id.signin_link);
        progressBar = findViewById(R.id.progress_reset);
        inputEmail = findViewById(R.id.email);

        auth = FirebaseAuth.getInstance();

        btnreset.setOnClickListener(new ResetOnClickListener());
        btnSignin.setOnClickListener(new SigninOnClickListener());

    }

    private class ResetOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(final View view) {

            String email = inputEmail.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                Snackbar.make(view, "Enter your registered email address!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                return;
            }
            btnreset.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Snackbar.make(view, "We have sent you instructions to reset your password!", Snackbar.LENGTH_LONG);
                            } else {
                                Snackbar.make(view, "Failed to send reset email!", Snackbar.LENGTH_LONG);
                            }
                            progressBar.setVisibility(View.GONE);
                            btnreset.setVisibility(View.VISIBLE);
                        }
                    });
        }

    }


    private class SigninOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            finish();
        }
    }
}
