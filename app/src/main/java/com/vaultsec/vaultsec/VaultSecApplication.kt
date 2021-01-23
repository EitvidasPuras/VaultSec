package com.vaultsec.vaultsec

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/*
* @HiltAndroidApp triggers Hitl's code generation, including a base class for your application that
* servers as the application-level dependency container
* */
@HiltAndroidApp
class VaultSecApplication : Application()