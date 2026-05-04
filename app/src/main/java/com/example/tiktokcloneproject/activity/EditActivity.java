package com.example.tiktokcloneproject.activity;

import androidx.annotation.NonNull;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.example.tiktokcloneproject.helper.Validator;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class EditActivity extends Activity implements View.OnClickListener {

    TextInputLayout layoutInput;
    TextInputEditText edtInput;
    TextView tvLabel;
    ImageButton imbBack;
    Button btnSave;
    String mode;
    String content;
    Validator validator;

    FirebaseFirestore db;
    FirebaseUser user;

    final String TAG = "EditActivity";
    Handler handler = new Handler();
    String msg;
    final Calendar myCalendar= Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.overridePendingTransition(R.anim.slide_left_to_right, R.anim.slide_right_to_left);
        setContentView(R.layout.activity_edit);

        layoutInput = (TextInputLayout) findViewById(R.id.layoutInput);
        edtInput = (TextInputEditText) findViewById(R.id.edtInput);
        tvLabel = (TextView) findViewById(R.id.tvLabel);
        imbBack = (ImageButton) findViewById(R.id.imbBack);
        btnSave = (Button) findViewById(R.id.btnSave);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            mode = bundle.getString("mode");
            content = bundle.getString("content");
        }

        validator = Validator.getInstance();
        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();


        switch (mode) {
            case StaticVariable.USERNAME:
                handleUsername();
                break;
            case StaticVariable.BIRTHDATE:
                handleBirthdate();
                break;
        }

        imbBack.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == imbBack.getId()) {
            backToEditProfile();
        }

        if(view.getId() == btnSave.getId()) {
            if (mode.equals(StaticVariable.BIRTHDATE)) {
                updateBirthdate();
            }
        }
    }

    private void handleUsername() {
        tvLabel.setText(R.string.username_label);
        edtInput.setHint("admin");
        edtInput.setText(content);

        edtInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                setEnableSave(true);
            }
            @Override
            public void afterTextChanged(Editable editable) {
                if(!validator.isValidUsername(editable.toString())) {
                    layoutInput.setError(getString(R.string.error_username));
                    setEnableSave(false);
                }
                else {
                    layoutInput.setError("");
                    if(content != null && content.equals(editable.toString())) {
                        setEnableSave(false);
                    } else {
                        setEnableSave(true);
                    }
                }
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newUsername = edtInput.getText().toString();
                if (content != null && content.equals(newUsername)) {
                    backToEditProfile();
                    return;
                }

                setEnableSave(false);
                db.collection("users")
                        .whereEqualTo("username", newUsername)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                boolean exists = false;
                                for (DocumentSnapshot document : task.getResult()) {
                                    if (document.exists()) {
                                        exists = true;
                                        break;
                                    }
                                }

                                if (!exists) {
                                    updateUsername();
                                } else {
                                    setEnableSave(true);
                                    layoutInput.setError(getString(R.string.exist_username));
                                }
                            } else {
                                setEnableSave(true);
                                Log.d(TAG, "Error getting documents: ", task.getException());
                            }
                        });
            }
        });
    }

    private void handleBirthdate() {
        tvLabel.setText(R.string.birthdate_label);
        edtInput.setHint("01/01/2000");
        edtInput.setText(content);
        layoutInput.setStartIconDrawable(R.drawable.ic_calendar);

        layoutInput.setStartIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                        myCalendar.set(Calendar.YEAR, year);
                        myCalendar.set(Calendar.MONTH, month);
                        myCalendar.set(Calendar.DAY_OF_MONTH, day);
                        updateLabel();
                    }
                };
                DatePickerDialog datePickerDialog = new DatePickerDialog(EditActivity.this, date,myCalendar.get(Calendar.YEAR),myCalendar.get(Calendar.MONTH),myCalendar.get(Calendar.DAY_OF_MONTH));
                datePickerDialog.show();
                datePickerDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(getColor(R.color.tiktok_red));
                datePickerDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(getColor(R.color.tiktok_red));

            }
        });

        edtInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                if(!validator.isValidBirthdate(editable.toString())) {
                    layoutInput.setError(getString(R.string.error_birthdate));
                    setEnableSave(false);
                }
                else {
                    layoutInput.setError("");
                    if(content != null && content.equals(editable.toString())) {
                        setEnableSave(false);
                    } else {
                        setEnableSave(true);
                    }
                }
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateBirthdate();
            }
        });
    }

    private void setEnableSave(boolean value) {
        if(value) {
            btnSave.setEnabled(true);
            btnSave.setTextColor(getResources().getColor(R.color.tiktok_red));
        } else {
            btnSave.setEnabled(false);
            btnSave.setTextColor(getResources().getColor(R.color.tiktok_grey_50));
        }
    }

    private void updateUsername() {
        String username = edtInput.getText().toString();
        if (user == null) return;

        DocumentReference userDoc = db.collection("users").document(user.getUid());
        DocumentReference profileDoc = db.collection("profiles").document(user.getUid());

        userDoc.update("username", username)
                .addOnSuccessListener(aVoid -> {
                    profileDoc.update("username", username)
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(EditActivity.this, "Username updated", Toast.LENGTH_SHORT).show();
                                backToEditProfile();
                            });
                })
                .addOnFailureListener(e -> {
                    setEnableSave(true);
                    Toast.makeText(EditActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateBirthdate() {
        String birthdate = edtInput.getText().toString();
        if (user == null) return;

        setEnableSave(false);
        db.collection("users").document(user.getUid())
                .update("birthdate", birthdate)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditActivity.this, "Birthdate updated", Toast.LENGTH_SHORT).show();
                    backToEditProfile();
                })
                .addOnFailureListener(e -> {
                    setEnableSave(true);
                    Toast.makeText(EditActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void backToEditProfile() {
        finish();
    }

    private void updateLabel()
    {
        String myFormat="dd/MM/yyyy";
        SimpleDateFormat dateFormat=new SimpleDateFormat(myFormat, Locale.US);
        edtInput.setText(dateFormat.format(myCalendar.getTime()));
    }
}