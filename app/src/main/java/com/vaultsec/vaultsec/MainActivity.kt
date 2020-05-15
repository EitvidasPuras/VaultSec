package com.vaultsec.vaultsec

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val REGISTRATION_REQUEST_CODE = 1
    }

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

    private fun hasInternetConnection(): Boolean {
        val connectivityManager =
            this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnected == true
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
            startActivityForResult(registrationIntent, REGISTRATION_REQUEST_CODE)
        }
    }

    private fun logUserIn() {
        button_login.setOnClickListener {
            val emailInput: String = textfield_login_email.text.toString()
            val passInput: String = textfield_login_password.text.toString()

            if (hasInternetConnection()) {
                if (loginCredentialsValidation(emailInput, passInput))
                    Toast.makeText(
                        this, "email: $emailInput password: $passInput", Toast.LENGTH_SHORT
                    ).show()
            } else {
                Toast.makeText(
                    this,
                    R.string.error_no_internet_connection,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loginCredentialsValidation(emailInput: String, passInput: String): Boolean {
        if (emailInput.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            textfield_login_email_layout.error = getString(R.string.error_email_required)
        }
        if (passInput.isEmpty()) {
            textfield_login_password_layout.error = getString(R.string.error_password_required)
        }
        return textfield_login_email_layout.error.isNullOrEmpty()
                && textfield_login_password_layout.error.isNullOrEmpty()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REGISTRATION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val contextView = findViewById<View>(R.id.constraintlayout_main_activity)
            Snackbar.make(contextView, R.string.successful_registration, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getColor(R.color.color_successful_registration)).show()
        }
    }
}
