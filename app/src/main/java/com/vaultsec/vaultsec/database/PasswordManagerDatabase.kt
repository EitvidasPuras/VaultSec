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

@Database(entities = [Token::class, Note::class], version = 7)
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

//                    tokenDao.deleteAll()
//                    val token = Token(0, "aispipevnwpindksnsda")
//                    tokenDao.insert(token)

                    noteDao.deleteAll()
                    var note = Note(
                        "Title1",
                        "Description1",
                        "#ffffff",
                        12,
                        Timestamp(System.currentTimeMillis()),
                        Timestamp(System.currentTimeMillis())
                    )
                    noteDao.insert(note)
                    note = Note(
                        "Title1",
                        "Description1",
                        "#303f9f",
                        10,
                        Timestamp(System.currentTimeMillis()),
                        Timestamp(System.currentTimeMillis())
                    )
                    noteDao.insert(note)
                    note = Note(
                        "Title2",
                        "Description2",
                        "#9ccc65",
                        14,
                        Timestamp(System.currentTimeMillis()),
                        Timestamp(System.currentTimeMillis())
                    )
                    noteDao.insert(note)
                    note = Note(
                        "Title4",
                        "Description3",
                        "#f57c00",
                        16,
                        Timestamp(System.currentTimeMillis()),
                        Timestamp(System.currentTimeMillis())
                    )
                    noteDao.insert(note)
                    note = Note(
                        "Title11",
                        "Description5",
                        "#ffffff",
                        12,
                        Timestamp(System.currentTimeMillis()),
                        Timestamp(System.currentTimeMillis())
                    )
                    noteDao.insert(note)
                    note = Note(
                        "Title1000Title1000Title1000Title1000",
                        "Description5Description5Description5",
                        "#ffcc80",
                        18,
                        Timestamp(System.currentTimeMillis()),
                        Timestamp(System.currentTimeMillis())
                    )
                    noteDao.insert(note)
                    note = Note(
                        "Title10101",
                        "Descriasdsadasdasd5",
                        "#ba68c8",
                        10,
                        Timestamp(System.currentTimeMillis()),
                        Timestamp(System.currentTimeMillis())
                    )
                    noteDao.insert(note)
                    note = Note(
                        "Title1001",
                        "Description5Description5Description5Description5Description5Description5",
                        "#ffffff",
                        18,
                        Timestamp(System.currentTimeMillis()),
                        Timestamp(System.currentTimeMillis())
                    )
                    noteDao.insert(note)
                    note = Note(
                        "Title11",
                        "Description5",
                        "#43a047",
                        16,
                        Timestamp(System.currentTimeMillis()),
                        Timestamp(System.currentTimeMillis())
                    )
                    noteDao.insert(note)
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