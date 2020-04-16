package com.vaultsec.vaultsec

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
//        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
//            WindowManager.LayoutParams.FLAG_SECURE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        openRegistrationActivity()
    }

    private fun openRegistrationActivity() {
        textview_login_create.setOnClickListener {
            val registrationIntent = Intent(this, RegistrationActivity::class.java)
            startActivity(registrationIntent)
        }
    }
}
