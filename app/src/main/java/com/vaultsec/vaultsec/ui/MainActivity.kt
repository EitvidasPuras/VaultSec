package com.vaultsec.vaultsec.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.ViewAnimationUtils
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.databinding.ActivityMainBinding
import com.vaultsec.vaultsec.network.entity.ApiResponse
import com.vaultsec.vaultsec.network.entity.ApiUser
import com.vaultsec.vaultsec.network.entity.ErrorTypes
import com.vaultsec.vaultsec.viewmodel.TokenViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.hypot

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tokenViewModel: TokenViewModel

    companion object {
        const val REGISTRATION_REQUEST_CODE = 1

        const val EXTRA_LOGOUT = "com.vaultsec.vaultsec.EXTRA_LOGOUT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Remove these comments to prevent taking screenshots
//        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
//            WindowManager.LayoutParams.FLAG_SECURE)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        tokenViewModel =
            ViewModelProvider(this@MainActivity).get(TokenViewModel::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        if (intent.hasExtra(EXTRA_LOGOUT) && intent.getBooleanExtra(
                EXTRA_LOGOUT, false
            )
        ) {
            Snackbar.make(
                binding.root,
                R.string.successful_logout, Snackbar.LENGTH_LONG
            )
                .setBackgroundTint(getColor(R.color.color_successful_snackbar)).show()
        }
        playOpeningAnimation(view)
        isUserLoggedIn()
        openRegistrationActivity()
        logUserIn()

    }

    private fun playOpeningAnimation(view: View) {
        view.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                v.removeOnLayoutChangeListener(this)
                val rightX = view.right
                val bottomY = view.bottom
                val radius = hypot(rightX.toDouble(), bottomY.toDouble()).toFloat()
                val anim =
                    ViewAnimationUtils.createCircularReveal(view, rightX, bottomY, 0f, radius)
                view.visibility = View.VISIBLE
                anim.duration = 1500
                anim.start()
            }
        })
    }


    private fun isUserLoggedIn() {
        try {
            tokenViewModel.getToken().token
            openBottomNavigationActivity()
        } catch (e: NullPointerException) {
        }
    }

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
        if (!binding.textfieldLoginEmailLayout.error.isNullOrEmpty()) {
            binding.textfieldLoginEmailLayout.error = null
        }
        if (!binding.textfieldLoginPasswordLayout.error.isNullOrEmpty()) {
            binding.textfieldLoginPasswordLayout.error = null
        }
    }

    private fun openRegistrationActivity() {
        textview_login_create.setOnClickListener {
            val registrationIntent = Intent(this, RegistrationActivity::class.java)
            startActivityForResult(
                registrationIntent,
                REGISTRATION_REQUEST_CODE
            )
        }
    }

    private fun openBottomNavigationActivity() {
        val bottomNavIntent = Intent(this, BottomNavigationActivity::class.java)
        startActivity(bottomNavIntent)
        finish()
    }

    private fun logUserIn() {
        button_login.setOnClickListener {
            val emailInput: String = binding.textfieldLoginEmail.text.toString()
            val passInput: String = binding.textfieldLoginPassword.text.toString()
            clearInputErrors()
            if (hasInternetConnection()) {
                if (isLoginDataValid(emailInput, passInput)) {
                    binding.progressbarLogin.visibility = View.VISIBLE
                    val user = ApiUser(
                        email = emailInput,
                        password = passInput
                    )
                    tokenViewModel.postLogin(user).observe(this, Observer<ApiResponse> {
                        binding.progressbarLogin.visibility = View.INVISIBLE
                        if (!it.isError) {
                            openBottomNavigationActivity()
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
                    this,
                    R.string.error_no_internet_connection,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun isLoginDataValid(emailInput: String, passInput: String): Boolean {
        if (emailInput.isEmpty()) {
            binding.textfieldLoginEmailLayout.error = getString(R.string.error_email_required)
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            binding.textfieldLoginEmailLayout.error = getString(R.string.error_email_format)
        }
        if (passInput.isEmpty()) {
            binding.textfieldLoginPasswordLayout.error = getString(R.string.error_password_required)
        }
        return binding.textfieldLoginEmailLayout.error.isNullOrEmpty() &&
                binding.textfieldLoginPasswordLayout.error.isNullOrEmpty()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REGISTRATION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Snackbar.make(
                binding.root,
                R.string.successful_registration, Snackbar.LENGTH_LONG
            )
                .setBackgroundTint(getColor(R.color.color_successful_snackbar)).show()
        }
    }
}
