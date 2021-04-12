package com.vaultsec.vaultsec.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.databinding.ActivityBottomNavigationBinding
import com.vaultsec.vaultsec.util.hideKeyboard
import com.vaultsec.vaultsec.util.setProgressBarDrawable
import com.vaultsec.vaultsec.viewmodel.BottomNavigationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlin.math.hypot

@AndroidEntryPoint
class BottomNavigationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBottomNavigationBinding

    private val bottomNavigationViewModel: BottomNavigationViewModel by viewModels()

    private var wasDoubleBackToExitPressed = false
    private lateinit var backToast: Toast
    private lateinit var navController: NavController

    private var defaultStatusBarColor: Int = 0

    companion object {
        const val EXTRA_LOGIN = "com.vaultsec.vaultsec.ui.BottomNavigationActivity.EXTRA_LOGIN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBottomNavigationBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setProgressBarDrawable(binding.progressbarBottomNavigation)

        bottomNavigationViewModel.printTokenToConsoleForTesting()

        if (intent.hasExtra(EXTRA_LOGIN) && intent.getBooleanExtra(EXTRA_LOGIN, false)) {
            binding.fragmentContainerView.visibility = View.INVISIBLE
            bottomNavigationViewModel.onLogIn()
        }

        val toolbar: androidx.appcompat.widget.Toolbar? =
            findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav_view)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
        navController = navHostFragment.findNavController()

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.fragment_notes,
                R.id.fragment_passwords,
                R.id.fragment_files,
                R.id.fragment_generator
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        bottomNavigationView.setupWithNavController(navController)
        bottomNavigationView.setOnNavigationItemReselectedListener {}

        supportFragmentManager.setFragmentResultListener(
            "com.vaultsec.vaultsec.ui.note.AddEditNoteFragment.openCamera",
            this
        ) { _, bundle ->
            val result = bundle.getBoolean("OpenCamera")
            if (result) {
                defaultStatusBarColor = window.statusBarColor
                window.statusBarColor = getColor(R.color.color_black)
                window.decorView.systemUiVisibility += View.SYSTEM_UI_FLAG_FULLSCREEN
                window.decorView.systemUiVisibility += View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                binding.toolbar.isVisible = false
                val params =
                    binding.fragmentContainerView.layoutParams as ViewGroup.MarginLayoutParams
                params.setMargins(0, 0, 0, 0)
                binding.fragmentContainerView.requestLayout()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    window.attributes.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        }

        supportFragmentManager.setFragmentResultListener(
            "com.vaultsec.vaultsec.ui.CameraFragment.closeCamera",
            this
        ) { _, bundle ->
            val result = bundle.getBoolean("CloseCamera")
            if (result) {
                supportFragmentManager.setFragmentResult(
                    "com.vaultsec.vaultsec.ui.CameraFragment.restoreSettings",
                    bundleOf(
                        "RestoreSettings" to true
                    )
                )
                window.statusBarColor = defaultStatusBarColor
                /*
                * These are apparently deprecated in Android 12 ... Oh well
                * */
                window.decorView.systemUiVisibility -= View.SYSTEM_UI_FLAG_FULLSCREEN
                window.decorView.systemUiVisibility -= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                binding.toolbar.isVisible = true
                val actionBarSize = calculateActionBarSize()
                val params =
                    binding.fragmentContainerView.layoutParams as ViewGroup.MarginLayoutParams
                params.setMargins(0, actionBarSize, 0, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    window.attributes.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                }
            }
        }

        playOpeningAnimation(view)

        lifecycleScope.launchWhenStarted {
            bottomNavigationViewModel.bottomNavigationEvent.collect { event ->
                when (event) {
                    BottomNavigationViewModel.BottomNavigationEvent.SuccessfulLogout -> {
                        /*
                        * To prevent the empty recycler view message from briefly flashing on the screen
                        * */
                        binding.fragmentContainerView.visibility = View.INVISIBLE
                        openStartActivity()
                    }
                    is BottomNavigationViewModel.BottomNavigationEvent.ShowProgressBar -> {
                        binding.progressbarBottomNavigation.isVisible = event.doShow
                    }
                    is BottomNavigationViewModel.BottomNavigationEvent.ShowHttpError -> {
                        Snackbar.make(view, event.message, Snackbar.LENGTH_LONG)
                            .setBackgroundTint(getColor(R.color.color_error_snackbar))
                            .show()
                    }
                    is BottomNavigationViewModel.BottomNavigationEvent.ShowRequestError -> {
                        Snackbar.make(view, event.message, Snackbar.LENGTH_LONG)
                            .setBackgroundTint(getColor(R.color.color_error_snackbar))
                            .show()
                    }
                    BottomNavigationViewModel.BottomNavigationEvent.SuccessfulLogin -> {
                        supportFragmentManager.setFragmentResult(
                            "com.vaultsec.vaultsec.ui.BottomNavigationActivity.seedDatabase",
                            bundleOf(
                                "seedDatabase" to true
                            )
                        )
                    }
                }
            }
        }
    }

    private fun calculateActionBarSize(): Int {
        val tv = TypedValue()
        if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
        }
        return -1
    }

    override fun onSupportNavigateUp(): Boolean {
        hideKeyboard(this)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (navController.currentDestination?.id == R.id.fragment_camera) {
            supportFragmentManager.setFragmentResult(
                "com.vaultsec.vaultsec.ui.CameraFragment.restoreSettings",
                bundleOf(
                    "RestoreSettings" to true
                )
            )
            window.statusBarColor = defaultStatusBarColor
            /*
            * These are apparently deprecated in Android 12 ... Oh well
            * */
            window.decorView.systemUiVisibility -= View.SYSTEM_UI_FLAG_FULLSCREEN
            window.decorView.systemUiVisibility -= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            binding.toolbar.isVisible = true
            val actionBarSize = calculateActionBarSize()
            val params =
                binding.fragmentContainerView.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(0, actionBarSize, 0, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            }
        }
        super.onBackPressed()
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
                val xAxis = view.right
                val yAxis = view.bottom / 2
                val radius = hypot(xAxis.toDouble(), yAxis.toDouble()).toFloat()
                val anim =
                    ViewAnimationUtils.createCircularReveal(view, xAxis, yAxis, 0f, radius)
                view.visibility = View.VISIBLE
                anim.duration = 1000
                anim.start()
            }
        })
    }


    // TODO: Implement soft logout?
//    override fun onBackPressed() {
//        if (wasDoubleBackToExitPressed) {
//            backToast.cancel()
//            super.onBackPressed()
//            return
//        }
//        wasDoubleBackToExitPressed = true
//        backToast = Toast.makeText(this, "Press back again to quit", Toast.LENGTH_SHORT)
//        backToast.show()
//
//        Handler().postDelayed({ wasDoubleBackToExitPressed = false }, 2000)
//    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_logout -> {
                bottomNavigationViewModel.onLogoutClick(this.applicationInfo.dataDir + "/databases")
                true
            }
            R.id.item_settings -> {
                Snackbar.make(binding.root, "Soon to be implemented", Snackbar.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openStartActivity() {
        val startIntent = Intent(this, StartActivity::class.java)
        startIntent.putExtra(StartActivity.EXTRA_LOGOUT, true)
        startActivity(startIntent)
        finish()
    }
}