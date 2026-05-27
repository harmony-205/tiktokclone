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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class EmailSignupActivity extends Activity {

    private static final String TAG = "EmailSignUpActivity";
    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Ensure a fresh login attempt
        mAuth.signOut();

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
            }
        }
    }

    private void handleSignUp(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            Log.d(TAG, "Firebase Auth success. UID: " + firebaseUser.getUid());
                            // After auth success, verify existence in Firestore
                            checkUserInFirestore(firebaseUser);
                        }
                    } else {
                        dialog.dismiss();
                        Log.e(TAG, "Firebase auth failed", task.getException());
                        Toast.makeText(this, "Xác thực Firebase thất bại: " + (task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định"), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkUserInFirestore(FirebaseUser firebaseUser) {
        // Explicitly get a new instance to ensure the token is updated
        FirebaseFirestore.getInstance().collection("users").document(firebaseUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            // User already exists in Firestore. 
                            // In a SIGNUP flow, this means they should be using the Sign-In screen instead.
                            dialog.dismiss();
                            Log.d(TAG, "User exists in Firestore. Notifying user to sign in.");
                            Toast.makeText(EmailSignupActivity.this, "Email này đã được đăng ký, vui lòng Đăng nhập", Toast.LENGTH_LONG).show();
                            
                            // Sign out and send them to the Sign-In choice screen
                            mAuth.signOut();
                            moveToAnotherActivity(SigninChoiceActivity.class);
                        } else {
                            Log.d(TAG, "New user or missing Firestore data. Creating...");
                            createNewUserRecords(firebaseUser);
                        }
                    } else {
                        dialog.dismiss();
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Permission Denied";
                        Log.e(TAG, "Firestore check failed: " + errorMsg);
                        
                        Toast.makeText(EmailSignupActivity.this, "Lỗi phân quyền Firestore: Vui lòng kiểm tra Rules trên Firebase Console.", Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                    }
                });
    }

    private void createNewUserRecords(FirebaseUser firebaseUser) {
        String id = firebaseUser.getUid();
        String username = id.substring(0, Math.min(id.length(), 6));
        User user = new User(id, username, "", firebaseUser.getEmail());
        
        writeNewUser(user, aVoid -> {
            Profile profile = new Profile(id, username);
            writeNewProfile(profile, aVoid1 -> {
                dialog.dismiss();
                Log.d(TAG, "All records created successfully.");
                moveToAnotherActivity(HomeScreenActivity.class);
            });
        });
    }

    private void signUp() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void writeNewUser(User user, OnSuccessListener<Void> successListener) {
        Map<String, Object> userValues = user.toMap();
        db.collection("users").document(user.getUserId())
                .set(userValues)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Log.w(TAG, "Error writing user", e);
                    Toast.makeText(EmailSignupActivity.this, "Lỗi tạo thông tin người dùng", Toast.LENGTH_SHORT).show();
                });
    }

    private void writeNewProfile(Profile profile, OnSuccessListener<Void> successListener) {
        WriteBatch batch = db.batch();
        DocumentReference profileRef = db.collection("profiles").document(profile.getUserId());
        
        batch.set(profileRef, profile.toMap());
        
        // Use separate document IDs to avoid sub-collection creation issues in batch if needed
        Map<String, Object> data = new HashMap<>();
        data.put("userID", "initial");
        batch.set(profileRef.collection("following").document("initial"), data);
        batch.set(profileRef.collection("followers").document("initial"), data);

        batch.commit()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Log.e(TAG, "Error writing profile Batch: " + e.getMessage(), e);
                    if (e.getMessage() != null && e.getMessage().contains("PERMISSION_DENIED")) {
                        Toast.makeText(EmailSignupActivity.this, "Lỗi phân quyền Firestore khi tạo hồ sơ. Hãy kiểm tra lại Rules.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(EmailSignupActivity.this, "Lỗi tạo hồ sơ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void moveToAnotherActivity(Class<?> cls) {
        Intent intent = new Intent(EmailSignupActivity.this, cls);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
