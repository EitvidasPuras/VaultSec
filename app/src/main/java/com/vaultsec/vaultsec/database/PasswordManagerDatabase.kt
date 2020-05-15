package com.vaultsec.vaultsec.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vaultsec.vaultsec.database.dao.TokenDao
import com.vaultsec.vaultsec.database.entity.Token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Token::class], version = 2)
abstract class PasswordManagerDatabase : RoomDatabase() {

    abstract fun tokenDao(): TokenDao

    private class ManagerDatabaseCallback(private val scope: CoroutineScope) :
        RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { passwordManagerDatabase ->
                CoroutineScope(Dispatchers.IO).launch {
                    val tokenDao = passwordManagerDatabase.tokenDao()

                    tokenDao.deleteAll()

                    val token = Token(0, "aispipevnwpindksnsda")
                    tokenDao.insert(token)
                }
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: PasswordManagerDatabase? = null
        private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

        fun getInstance(context: Context): PasswordManagerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PasswordManagerDatabase::class.java,
                    "vaultsec-database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(ManagerDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}