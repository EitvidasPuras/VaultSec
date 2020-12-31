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
import kotlinx.coroutines.launch
import java.util.*

@Database(entities = [Token::class, Note::class], version = 5)
@TypeConverters(DateConverter::class)
abstract class PasswordManagerDatabase : RoomDatabase() {

    abstract fun tokenDao(): TokenDao
    abstract fun noteDao(): NoteDao

    private class ManagerDatabaseCallback(private val scope: CoroutineScope) :
        RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { passwordManagerDatabase ->
                CoroutineScope(Dispatchers.IO).launch {
                    val tokenDao = passwordManagerDatabase.tokenDao()
                    val noteDao = passwordManagerDatabase.noteDao()

//                    tokenDao.deleteAll()
//                    val token = Token(0, "aispipevnwpindksnsda")
//                    tokenDao.insert(token)

                    noteDao.deleteAll()
                    var note = Note(
                        0,
                        "Title1",
                        "Description1",
                        "#ffffff",
                        12,
                        Date(System.currentTimeMillis()),
                        Date(System.currentTimeMillis())
                    )
                    noteDao.insert(note)
                    note = Note(
                        0,
                        "Title1",
                        "Description1",
                        "#303f9f",
                        10,
                        Date(System.currentTimeMillis()),
                        Date(System.currentTimeMillis())
                    )
                    noteDao.insert(note)
                    note = Note(
                        0,
                        "Title2",
                        "Description2",
                        "#9ccc65",
                        14,
                        Date(System.currentTimeMillis()),
                        Date(System.currentTimeMillis())
                    )
                    noteDao.insert(note)
                    note = Note(
                        0,
                        "Title4",
                        "Description3",
                        "#f57c00",
                        16,
                        Date(System.currentTimeMillis()),
                        Date(System.currentTimeMillis())
                    )
                    noteDao.insert(note)
                    note = Note(
                        0,
                        "Title11",
                        "Description5",
                        "#ffffff",
                        12,
                        Date(System.currentTimeMillis()),
                        Date(System.currentTimeMillis())
                    )
                    noteDao.insert(note)
                    note = Note(
                        0,
                        "Title1000Title1000Title1000Title1000",
                        "Description5Description5Description5",
                        "#ffcc80",
                        18,
                        Date(System.currentTimeMillis()),
                        Date(System.currentTimeMillis())
                    )
                    noteDao.insert(note)
                    note = Note(
                        0,
                        "Title10101",
                        "Descriasdsadasdasd5",
                        "#ba68c8",
                        10,
                        Date(System.currentTimeMillis()),
                        Date(System.currentTimeMillis())
                    )
                    noteDao.insert(note)
                    note = Note(
                        0,
                        "Title1001",
                        "Description5Description5Description5Description5Description5Description5",
                        "#ffffff",
                        18,
                        Date(System.currentTimeMillis()),
                        Date(System.currentTimeMillis())
                    )
                    noteDao.insert(note)
                    note = Note(
                        0,
                        "Title11",
                        "Description5",
                        "#43a047",
                        16,
                        Date(System.currentTimeMillis()),
                        Date(System.currentTimeMillis())
                    )
                    noteDao.insert(note)
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
                    // The line below populates the DB with random data
                    // ON DATABASE CREATION
                    .addCallback(ManagerDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}