package com.vaultsec.vaultsec

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/*
* @HiltAndroidApp triggers Hilt's code generation, including a base class for your application that
* servers as the application-level dependency container
* */
@HiltAndroidApp
class VaultSecApplication : Application()

// TODO: 2021-01-25 Explore Keystore to store token or user password
// TODO: 2021-01-30 Room should have a server-side id for notes for updating it
// TODO: 2021-01-30 "deleted: Boolean" Room column? For when the note is deleted in case no internet
// TODO: 2021-01-30 The goal is to keep both, client-side and server-side databases as up to date as possible