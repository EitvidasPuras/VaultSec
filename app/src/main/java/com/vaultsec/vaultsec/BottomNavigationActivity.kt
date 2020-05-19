package com.vaultsec.vaultsec

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.vaultsec.vaultsec.databinding.ActivityBottomNavigationBinding
import com.vaultsec.vaultsec.network.entity.ErrorTypes
import com.vaultsec.vaultsec.viewmodel.TokenViewModel

class BottomNavigationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBottomNavigationBinding
    private lateinit var tokenViewModel: TokenViewModel
    private var wasDoubleBackToExitPressed = false
    private lateinit var backToast: Toast

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBottomNavigationBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        tokenViewModel =
            ViewModelProvider(this@BottomNavigationActivity).get(TokenViewModel::class.java)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav_view)

        // Due to a bug on Google's side
        // If using <fragment> as a NavController
//        val navController = findNavController(R.id.fragment_container_view)

        // If using <FragmentContainerView> as a NavController
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
        val navController = navHostFragment.navController


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
    }

    override fun onBackPressed() {
        // TODO: Implement soft logout
        if (wasDoubleBackToExitPressed) {
            backToast.cancel()
            super.onBackPressed()
            return
        }
        wasDoubleBackToExitPressed = true
        backToast = Toast.makeText(this, "Press back again to quit", Toast.LENGTH_SHORT)
        backToast.show()

        Handler().postDelayed({ wasDoubleBackToExitPressed = false }, 2000)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.drop_down_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_logout -> {
                binding.progressbarBottomNavActivity.visibility = View.VISIBLE
                tokenViewModel.postLogout("Bearer " + tokenViewModel.getToken().token)
                    .observe(this, Observer {
                        if (!it.isError) {
                            binding.progressbarBottomNavActivity.visibility = View.INVISIBLE
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
                    })
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openLogInActivity() {
        val logInIntent = Intent(this, MainActivity::class.java)
        logInIntent.putExtra(MainActivity.EXTRA_LOGOUT, true)
        startActivity(logInIntent)
        finish()
    }
}