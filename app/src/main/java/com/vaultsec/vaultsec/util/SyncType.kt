package com.vaultsec.vaultsec.util

class SyncType {
    companion object {
        const val NOTHING_REQUIRED = 0
        const val CREATE_REQUIRED = 1
        const val DELETE_REQUIRED = 2
        const val UPDATE_REQUIRED = 3
    }
}

/*
* 0    -   Item doesn't require any action                 (Item is in the server)
* 1    -   Item needs to be created inside the server      (Item isn't in the server)
* 2    -   Item needs to be deleted from the server        (Item is in the server)
* 3    -   Item needs to be updated inside the server      (Item is in the server)
* */

/*
* Decided not to use Enums because they slow down Room Database operations, which degrades app performance
* */