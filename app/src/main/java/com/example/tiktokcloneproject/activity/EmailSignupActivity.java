package com.example.tiktokcloneproject.activity;

import androidx.annotation.NonNull;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.model.Profile;
import com.example.tiktokcloneproject.model.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EmailSignupActivity extends Activity{

    EditText edtEmail;
    Button btnEmail;

    private static final String TAG = "EmailSignUpActivity";
    private static final int RC_SIGN_IN = 9001;

    private String msg;

    // [START declare_auth]
    private FirebaseAuth mAuth;
    private  FirebaseFirestore db;
    // [END declare_auth]

    private GoogleSignInClient mGoogleSignInClient;

    private Dialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Sử dụng ID từ strings.xml để đảm bảo tính nhất quán
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mGoogleSignInClient.signOut();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setView(R.layout.dialog_progress);
        dialog = builder.create();

        signUp();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    dialog.show();
                    handleSignUp(account);
                }
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed, code: " + e.getStatusCode(), e);
                Toast.makeText(this, "Lỗi đăng nhập Google: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            dialog.dismiss();
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                String id = firebaseUser.getUid();
                                String username = id.substring(0, Math.min(id.length(), 6));
                                User user = new User(id, username, "", firebaseUser.getEmail());
                                writeNewUser(user);
                                Profile profile = new Profile(id, username);
                                writeNewProfile(profile);
                                moveToAnotherActivity(HomeScreenActivity.class);
                            }

                        } else {
                            dialog.dismiss();
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(EmailSignupActivity.this, "Xác thực Firebase thất bại", Toast.LENGTH_SHORT).show();
                            moveToAnotherActivity(SignupChoiceActivity.class);
                        }
                    }
                });
    }

    private void signUp() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void writeNewUser(User user) {
        Map<String, Object> userValues = user.toMap();
        db.collection("users").document(user.getUserId())
                .set(userValues)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User written!"))
                .addOnFailureListener(e -> Log.w(TAG, "Error writing user", e));
    }

    private void writeNewProfile(Profile profile) {
        Map<String, Object> userValues = profile.toMap();
        db.collection("profiles").document(profile.getUserId())
                .set(userValues)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Profile written!"))
                .addOnFailureListener(e -> Log.w(TAG, "Error writing profile", e));
        
        Map<String, Object> Data1 = new HashMap<>();
        Data1.put("userID","dump");

        db.collection("profiles").document(profile.getUserId()).collection("following").document("dump").set(Data1);
        db.collection("profiles").document(profile.getUserId()).collection("followers").document("dump").set(Data1);
    }

    private void moveToAnotherActivity(Class<?> cls) {
        Intent intent = new Intent(EmailSignupActivity.this, cls);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void handleSignUp(GoogleSignInAccount account) {
        db.collection("users")
                .whereEqualTo("email", account.getEmail())
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean exists = false;
                        for (DocumentSnapshot document : task.getResult()) {
                            if (document.exists()) {
                                exists = true;
                                break;
                            }
                        }

                        if (!exists) {
                            firebaseAuthWithGoogle(account.getIdToken());
                        } else {
                            dialog.dismiss();
                            Toast.makeText(EmailSignupActivity.this, "Email này đã được đăng ký, vui lòng Đăng nhập", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    } else {
                        dialog.dismiss();
                        Log.d(TAG, "Error getting documents: ", task.getException());
                        Toast.makeText(EmailSignupActivity.this, "Lỗi kiểm tra dữ liệu: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        firebaseAuthWithGoogle(account.getIdToken());
                    }
                });

    }
}