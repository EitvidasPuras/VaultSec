package com.vaultsec.vaultsec

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.vaultsec.vaultsec.databinding.ActivityRegistrationBinding
import com.vaultsec.vaultsec.network.entity.ApiResponse
import com.vaultsec.vaultsec.network.entity.ApiUser
import com.vaultsec.vaultsec.network.entity.ErrorTypes
import com.vaultsec.vaultsec.viewmodel.TokenViewModel
import kotlinx.android.synthetic.main.activity_registration.*
import java.net.SocketException
import java.util.regex.Pattern

class RegistrationActivity : AppCompatActivity() {

    private lateinit var tokenViewModel: TokenViewModel
    private lateinit var binding: ActivityRegistrationBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        // Remove these comments to prevent taking screenshots
//        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
//            WindowManager.LayoutParams.FLAG_SECURE)
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        tokenViewModel =
            ViewModelProvider(this@RegistrationActivity).get(TokenViewModel::class.java)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        openLoginActivity()
        registerUser()
//        populateFormWithDataForTesting()
    }

//    private fun populateFormWithDataForTesting() {
//        binding.textfieldRegistrationFirstname.setText("Nu")
//        binding.textfieldRegistrationLastname.setText("Nu")
//        binding.textfieldRegistrationEmail.setText("nu@nu.com")
//        binding.textfieldRegistrationPassword.setText("123456789*aA")
//        binding.textfieldRegistrationPasswordRetype.setText("123456789*aA")
//    }

    private fun hasInternetConnection(): Boolean {
        val connectivityManager =
            this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }

    private fun clearInputErrors() {
        if (!binding.textfieldRegistrationFirstnameLayout.error.isNullOrEmpty()) {
            binding.textfieldRegistrationFirstnameLayout.error = null
        }
        if (!binding.textfieldRegistrationLastnameLayout.error.isNullOrEmpty()) {
            binding.textfieldRegistrationLastnameLayout.error = null
        }
        if (!binding.textfieldRegistrationEmailLayout.error.isNullOrEmpty()) {
            binding.textfieldRegistrationEmailLayout.error = null
        }
        if (!binding.textfieldRegistrationPasswordLayout.error.isNullOrEmpty()) {
            binding.textfieldRegistrationPasswordLayout.error = null
        }
        if (!binding.textfieldRegistrationPasswordRetypeLayout.error.isNullOrEmpty()) {
            binding.textfieldRegistrationPasswordRetypeLayout.error = null
        }
    }

    private fun registerUser() {
        button_registration.setOnClickListener {
            val firstNameInput: String = binding.textfieldRegistrationFirstname.text.toString()
            val lastNameInput: String = binding.textfieldRegistrationLastname.text.toString()
            val emailInput: String = binding.textfieldRegistrationEmail.text.toString()
            val passInput: String = binding.textfieldRegistrationPassword.text.toString()
            val passRetypeInput: String =
                binding.textfieldRegistrationPasswordRetype.text.toString()
            clearInputErrors()
            if (hasInternetConnection()) {
                if (isRegistrationDataValid(
                        firstNameInput,
                        lastNameInput,
                        emailInput,
                        passInput,
                        passRetypeInput
                    )
                ) {
                    binding.progressbarRegistration.visibility = View.VISIBLE
                    val user = ApiUser(
                        firstNameInput,
                        lastNameInput,
                        emailInput,
                        passInput,
                        passRetypeInput
                    )
                    tokenViewModel.postRegister(user).observe(this, Observer<ApiResponse> {
                        binding.progressbarRegistration.visibility = View.INVISIBLE
                        if (!it.isError) {
                            setResult(Activity.RESULT_OK)
                            finish()
                        } else {
                            when (it.type) {
                                ErrorTypes.HTTP_ERROR -> Toast.makeText(
                                    this,
                                    it.message,
                                    Toast.LENGTH_LONG
                                ).show()
                                ErrorTypes.SOCKET_TIMEOUT -> Toast.makeText(
                                    this,
                                    getString(R.string.error_connection_timed_out),
                                    Toast.LENGTH_LONG
                                ).show()
                                ErrorTypes.GENERAL -> Toast.makeText(
                                    this,
                                    getString(R.string.error_generic_connection),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    })
                }
            } else {
                Toast.makeText(
                    this@RegistrationActivity,
                    R.string.error_no_internet_connection,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun isRegistrationDataValid(
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

        try {
            val letterLowercase = Pattern.compile("[a-z]")
            val letterUppercase = Pattern.compile("[A-Z]")
            val digit = Pattern.compile("[0-9]")
            val specialChar = Pattern.compile("[!@#$%^&*()_+=|<>?{}\\[\\]~`-]")

            if (firstNameInput.isEmpty()) {
                binding.textfieldRegistrationFirstnameLayout.error =
                    getString(R.string.error_first_name_required)
            } else if (!firstNameInput.chars().allMatch(Character::isLetter)) {
                binding.textfieldRegistrationFirstnameLayout.error =
                    getString(R.string.error_first_name_format)
            }
            if (lastNameInput.isEmpty()) {
                binding.textfieldRegistrationLastnameLayout.error =
                    getString(R.string.error_last_name_required)
            } else if (!lastNameInput.chars().allMatch(Character::isLetter)) {
                binding.textfieldRegistrationLastnameLayout.error =
                    getString(R.string.error_last_name_format)
            }
            if (emailInput.isEmpty()) {
                binding.textfieldRegistrationEmailLayout.error =
                    getString(R.string.error_email_required)
            } else if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                binding.textfieldRegistrationEmailLayout.error =
                    getString(R.string.error_email_format)
            }
            if (passInput.isEmpty()) {
                binding.textfieldRegistrationPasswordLayout.error =
                    getString(R.string.error_password_required)
            } else if (passInput.length < 10) {
                binding.textfieldRegistrationPasswordLayout.error =
                    getString(R.string.error_password_length)
            } else if (!letterLowercase.matcher(passInput).find()
                || !letterUppercase.matcher(passInput).find()
                || !digit.matcher(passInput).find()
                || !specialChar.matcher(passInput).find()
            ) {
                binding.textfieldRegistrationPasswordLayout.error =
                    getString(R.string.error_password_format)
            }
            if (passRetypeInput != passInput && passInput.isNotEmpty()) {
                binding.textfieldRegistrationPasswordRetypeLayout.error =
                    getString(R.string.error_password_match)
            }
        } catch (e: SocketException) {
            Toast.makeText(
                this@RegistrationActivity,
                R.string.error_no_internet_connection,
                Toast.LENGTH_SHORT
            )
                .show()
            return false
        }

        return binding.textfieldRegistrationFirstnameLayout.error.isNullOrEmpty()
                && binding.textfieldRegistrationLastnameLayout.error.isNullOrEmpty()
                && binding.textfieldRegistrationEmailLayout.error.isNullOrEmpty()
                && binding.textfieldRegistrationPasswordLayout.error.isNullOrEmpty()
                && binding.textfieldRegistrationPasswordRetypeLayout.error.isNullOrEmpty()
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
