package tn.insat.nodebox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import androidx.annotation.NonNull;

public class SignupActivity extends Activity {

    private EditText inputname, inputEmail, inputPassword;
    private Button btnSignUp;
    private TextView btnSignin;
    private ProgressBar progressBar;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance();

        btnSignUp = (Button) findViewById(R.id.btnsignup);
        btnSignin = (TextView) findViewById(R.id.signin_link);
        inputname = (EditText) findViewById(R.id.name);
        inputEmail = (EditText) findViewById(R.id.email);
        inputPassword = (EditText) findViewById(R.id.password);
        progressBar = (ProgressBar) findViewById(R.id.progress_signup);

        btnSignin.setOnClickListener(new SigninOnClickListener());
        btnSignUp.setOnClickListener(new SignupOnClickListener());
    }

    private class SignupOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {

            final String name = inputname.getText().toString().trim();
            final String email = inputEmail.getText().toString().trim();
            final String password = inputPassword.getText().toString().trim();
            final View view = v;

            if (TextUtils.isEmpty(email)) {
                Snackbar.make(view, "Enter email address!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                return;
            }

            if (TextUtils.isEmpty(password)) {
                Snackbar.make(view, "Enter password!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }

            if (password.length() < 6) {
                Snackbar.make(view, "Password too short, enter minimum 6 characters!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            btnSignUp.setVisibility(View.GONE);

            //create user
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(SignupActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            progressBar.setVisibility(View.GONE);
                            btnSignUp.setVisibility(View.VISIBLE);

                            if (!task.isSuccessful()) {
                                Snackbar.make(view, "Authentication failed.", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            } else {
                                FirebaseUser user = auth.getCurrentUser();

                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(name).build();

                                user.updateProfile(profileUpdates);

                                Intent i = new Intent(getApplicationContext(), HomeActivity.class);
                                startActivity(new Intent(SignupActivity.this, HomeActivity.class).putExtra("name", name));
                                finish();
                            }
                        }
                    });

        }
    }

    private class SigninOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            intent.putExtra("return","true");
            startActivity(intent);
            finish();
        }
    }

}
