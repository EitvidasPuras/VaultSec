package com.vaultsec.vaultsec

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_registration.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import java.util.regex.Pattern

class RegistrationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Remove these comments to prevent taking screenshots
//        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
//            WindowManager.LayoutParams.FLAG_SECURE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        openLoginActivity()
        registerUser()
        clearInputErrors()

        // Google bug:
        // After clicking the password visibility toggling icon, textfield's helper text disappears
//        textfield_registration_password_layout.setEndIconOnClickListener {
//            textfield_registration_password_layout.helperText =
//                getString(R.string.registration_password_helper_text)
//        }
//        textfield_registration_password_retype_layout.setEndIconOnClickListener {
//            textfield_registration_password_retype_layout.helperText =
//                getString(R.string.registration_password_helper_text_retype)
//        }
    }

    private fun clearInputErrors() {
        textfield_registration_firstname.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                textfield_registration_firstname_layout.error = null
            }
        })
        textfield_registration_lastname.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                textfield_registration_lastname_layout.error = null
            }
        })
        textfield_registration_email.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                textfield_registration_email_layout.error = null
            }
        })
        textfield_registration_password.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                textfield_registration_password_layout.error = null
            }
        })
        textfield_registration_password_retype.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                textfield_registration_password_retype_layout.error = null
            }
        })
    }

    private fun registerUser() {
        button_registration.setOnClickListener() {
            val firstNameInput: String = textfield_registration_firstname.text.toString()
            val lastNameInput: String = textfield_registration_lastname.text.toString()
            val emailInput: String = textfield_registration_email.text.toString()
            val passInput: String = textfield_registration_password.text.toString()
            val passRetypeInput: String = textfield_registration_password_retype.text.toString()

            GlobalScope.async(Dispatchers.Main) {
                if (registrationCredentialsValidation(
                        firstNameInput,
                        lastNameInput,
                        emailInput,
                        passInput,
                        passRetypeInput
                    )
                ) {
                    Toast.makeText(
                        this@RegistrationActivity,
                        "IT IS REGISTER",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                GlobalScope.cancel()
            }
        }
    }

    private fun registrationCredentialsValidation(
        firstNameInput: String, lastNameInput: String,
        emailInput: String, passInput: String,
        passRetypeInput: String
    ): Boolean {
        if (firstNameInput.length > 30
            || lastNameInput.length > 30
            || passInput.length > 40
            || passRetypeInput.length > 40
        ) {
            return false
        }

        val letterLowercase = Pattern.compile("[a-z]")
        val letterUppercase = Pattern.compile("[A-Z]")
        val digit = Pattern.compile("[0-9]")
        val specialChar = Pattern.compile("[!@#$%^&*()_+=|<>?{}\\[\\]~`-]")

        if (firstNameInput.isEmpty()) {
            textfield_registration_firstname_layout.error = "First name is required"
        } else if (!firstNameInput.chars().allMatch(Character::isLetter)) {
            textfield_registration_firstname_layout.error =
                "First name must only contain letters"
        }
        if (lastNameInput.isEmpty()) {
            textfield_registration_lastname_layout.error = "Last name is required"
        } else if (!lastNameInput.chars().allMatch(Character::isLetter)) {
            textfield_registration_lastname_layout.error =
                "Last name must only contain letters"
        }
        if (emailInput.isEmpty()) {
            textfield_registration_email_layout.error = "Email is required"
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            textfield_registration_email_layout.error = "Invalid email address"
        }
        if (passInput.isEmpty()) {
            textfield_registration_password_layout.error = "Password is required"
        } else if (passInput.length < 10) {
            textfield_registration_password_layout.error =
                "Must be at least 10 characters long"
        } else if (!letterLowercase.matcher(passInput).find()
            || !letterUppercase.matcher(passInput).find()
            || !digit.matcher(passInput).find()
            || !specialChar.matcher(passInput).find()
        ) {
            textfield_registration_password_layout.error = "A stronger password is required"
        }
        if (!passRetypeInput.equals(passInput) && passInput.isNotEmpty()) {
            textfield_registration_password_retype_layout.error = "Passwords do not match"
        }

        return textfield_registration_firstname_layout.error.isNullOrEmpty()
                && textfield_registration_lastname_layout.error.isNullOrEmpty()
                && textfield_registration_email_layout.error.isNullOrEmpty()
                && textfield_registration_password_layout.error.isNullOrEmpty()
                && textfield_registration_password_retype_layout.error.isNullOrEmpty()
    }

    private fun openLoginActivity() {
        textview_registration_login.setOnClickListener {
            val loginIntent = Intent(this, MainActivity::class.java)
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(loginIntent)
            finish()
        }
    }
}
