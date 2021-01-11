package com.vaultsec.vaultsec.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vaultsec.vaultsec.database.converter.DateConverter
import com.vaultsec.vaultsec.database.dao.NoteDao
import com.vaultsec.vaultsec.database.dao.TokenDao
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.database.entity.Token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.sql.Timestamp

@Database(entities = [Token::class, Note::class], version = 8)
@TypeConverters(DateConverter::class)
abstract class PasswordManagerDatabase : RoomDatabase() {

    abstract fun tokenDao(): TokenDao
    abstract fun noteDao(): NoteDao

    private class ManagerDatabaseCallback :
        RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { passwordManagerDatabase ->
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    val tokenDao = passwordManagerDatabase.tokenDao()
                    val noteDao = passwordManagerDatabase.noteDao()

                    val arrayOfFontSizes = (12..22 step 2).toList().toTypedArray()
                    val arrayOfColors =
                        arrayOf("#ffffff", "#ff5252", "#ffab00", "#43a047", "#0091ea")
//                    tokenDao.deleteAll()
//                    val token = Token(0, "aispipevnwpindksnsda")
//                    tokenDao.insert(token)

                    noteDao.deleteAll()
                    var note: Note
                    for (i in 0 until 50) {
                        if (i % 3 == 0 && i % 4 == 0) {
                            note = Note(
                                "",
                                "Description${i}",
                                arrayOfColors.random(),
                                arrayOfFontSizes.random(),
                                Timestamp(System.currentTimeMillis()),
                                Timestamp(System.currentTimeMillis())
                            )
                        } else {
                            note = Note(
                                "Title${i}",
                                "Description${i}",
                                arrayOfColors.random(),
                                arrayOfFontSizes.random(),
                                Timestamp(System.currentTimeMillis()),
                                Timestamp(System.currentTimeMillis())
                            )
                        }
                        noteDao.insert(note)
                    }
                }
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: PasswordManagerDatabase? = null

        fun getInstance(context: Context): PasswordManagerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PasswordManagerDatabase::class.java,
                    "vaultsec-database"
                )
                    .fallbackToDestructiveMigration()
                    // The line below populates the DB with random data
                    // ON DATABASE CREATION
                    .addCallback(ManagerDatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}