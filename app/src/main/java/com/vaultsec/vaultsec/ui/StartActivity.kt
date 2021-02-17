package com.vaultsec.vaultsec.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.databinding.ActivityStartBinding
import com.vaultsec.vaultsec.util.hideKeyboard
import com.vaultsec.vaultsec.util.setProgressBarDrawable
import com.vaultsec.vaultsec.viewmodel.TokenViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.hypot

@AndroidEntryPoint
class StartActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStartBinding
    private lateinit var navController: NavController

    private val tokenViewModel: TokenViewModel by viewModels()

    companion object {
        const val EXTRA_LOGOUT = "com.vaultsec.vaultsec.EXTRA_LOGOUT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setProgressBarDrawable(binding.progressbarStart)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view_start) as NavHostFragment
        navController = navHostFragment.findNavController()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        if (intent.hasExtra(EXTRA_LOGOUT) && intent.getBooleanExtra(EXTRA_LOGOUT, false)
        ) {
            Snackbar.make(
                binding.root,
                R.string.successful_logout, Snackbar.LENGTH_LONG
            ).setBackgroundTint(getColor(R.color.color_successful_snackbar)).show()
        }
        isUserLoggedIn()
        playOpeningAnimation(view)
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

    private fun openBottomNavigationActivity() {
        val bottomNavIntent = Intent(this, BottomNavigationActivity::class.java)
        startActivity(bottomNavIntent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        hideKeyboard(this)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}