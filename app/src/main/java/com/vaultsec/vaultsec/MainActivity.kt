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
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Remove these comments to prevent taking screenshots
//        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
//            WindowManager.LayoutParams.FLAG_SECURE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        openRegistrationActivity()
        logUserIn()
        clearInputErrors()
    }

    private fun clearInputErrors() {
        textfield_login_email.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                textfield_login_email_layout.error = null
            }
        })

        textfield_login_password.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                textfield_login_password_layout.error = null
            }
        })
    }

    private fun openRegistrationActivity() {
        textview_login_create.setOnClickListener {
            val registrationIntent = Intent(this, RegistrationActivity::class.java)
            startActivity(registrationIntent)
        }
    }

    private fun logUserIn() {
        button_login.setOnClickListener {
            val emailInput: String = textfield_login_email.text.toString()
            val passInput: String = textfield_login_password.text.toString()

            if (loginCredentialsValidation(emailInput, passInput))
                Toast.makeText(
                    this, "email: $emailInput password: $passInput", Toast.LENGTH_SHORT
                ).show()
        }
    }

    private fun loginCredentialsValidation(emailInput: String, passInput: String): Boolean {
        if (emailInput.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            textfield_login_email_layout.error = "Email is required"
        }
        if (passInput.isEmpty()) {
            textfield_login_password_layout.error = "Password is required"
        }
        return textfield_login_email_layout.error.isNullOrEmpty()
                && textfield_login_password_layout.error.isNullOrEmpty()
    }
}
