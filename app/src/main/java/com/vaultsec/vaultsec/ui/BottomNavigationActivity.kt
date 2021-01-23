package com.vaultsec.vaultsec.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.database.SortOrder
import com.vaultsec.vaultsec.databinding.ActivityBottomNavigationBinding
import com.vaultsec.vaultsec.network.entity.ErrorTypes
import com.vaultsec.vaultsec.util.hideKeyboard
import com.vaultsec.vaultsec.viewmodel.NoteViewModel
import com.vaultsec.vaultsec.viewmodel.TokenViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.hypot

@AndroidEntryPoint
class BottomNavigationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBottomNavigationBinding

    private val tokenViewModel: TokenViewModel by viewModels()
    private val noteViewModel: NoteViewModel by viewModels()

    private var wasDoubleBackToExitPressed = false
    private lateinit var backToast: Toast
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBottomNavigationBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

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
//        NavigationUI.setupWithNavController(bottomNavigationView, navController)
        setupActionBarWithNavController(navController, appBarConfiguration)
        bottomNavigationView.setupWithNavController(navController)

        playOpeningAnimation(view)
    }


    override fun onSupportNavigateUp(): Boolean {
        hideKeyboard(this)
        return navController.navigateUp() || super.onSupportNavigateUp()
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
                binding.progressbarBottomNavActivity.visibility = View.VISIBLE
                tokenViewModel.postLogout()
                    .observe(this) {
                        binding.progressbarBottomNavActivity.visibility = View.INVISIBLE
                        if (!it.isError) {
                            noteViewModel.onSortOrderSelected(SortOrder.BY_TITLE)
                            noteViewModel.onSortDirectionSelected(true)
                            openLogInActivity()
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
                    }
                true
            }
            R.id.item_settings -> {
                Snackbar.make(binding.root, "Soon to be implemented", Snackbar.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openLogInActivity() {
        val startIntent = Intent(this, StartActivity::class.java)
        startIntent.putExtra(StartActivity.EXTRA_LOGOUT, true)
        startActivity(startIntent)
        finish()
    }
}
