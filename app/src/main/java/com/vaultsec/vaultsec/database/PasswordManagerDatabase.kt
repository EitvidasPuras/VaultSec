package com.vaultsec.vaultsec.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vaultsec.vaultsec.database.converter.DateConverter
import com.vaultsec.vaultsec.database.dao.NoteDao
import com.vaultsec.vaultsec.database.dao.PasswordDao
import com.vaultsec.vaultsec.database.dao.PaymentCardDao
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.database.entity.Password
import com.vaultsec.vaultsec.database.entity.PaymentCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.sql.Timestamp
import javax.inject.Inject
import javax.inject.Provider

@Database(entities = [Note::class, Password::class, PaymentCard::class], version = 8)
@TypeConverters(DateConverter::class)
abstract class PasswordManagerDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun passwordDao(): PasswordDao
    abstract fun paymentCardDao(): PaymentCardDao

    /*
    * @Inject tells Dagger how it can create this class. Also that this object can be injected
    * into other objects. Also it passes the necessary dependencies inside the constructor()
    * */
    class Callback @Inject constructor(
        private val database: Provider<PasswordManagerDatabase>
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
//            val tokenDao = database.get().tokenDao()
            val noteDao = database.get().noteDao()

            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {

                val arrayOfFontSizes = (12..22 step 2).toList().toTypedArray()
                val arrayOfColors =
                    arrayOf("#ffffff", "#ef5350", "#ffa726", "#66bb6a", "#42a5f5")

                noteDao.deleteAll()
                var note: Note
                for (i in 0 until 50) {
                    if (i % 3 == 0 && i % 4 == 0) { // No title, short text
                        note = Note(
                            "",
                            "Description $i",
                            arrayOfColors.random(),
                            arrayOfFontSizes.random(),
                            Timestamp(System.currentTimeMillis()),
                            Timestamp(System.currentTimeMillis())
                        )
                    } else if (i % 2 == 0 && i % 4 == 0) { // No title, long text
                        note = Note(
                            "",
                            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                                    "Morbi ac augue pellentesque, egestas urna id, sagittis risus. " +
                                    "Nunc dignissim pellentesque massa mattis viverra. Vestibulum " +
                                    "turpis felis, elementum ut ligula sed, consequat suscipit " +
                                    "purus. Proin dictum nisi et porttitor vestibulum. " +
                                    "Ut euismod enim at tortor auctor, at volutpat nisi imperdiet. " +
                                    "Vestibulum in lectus vitae augue consequat faucibus eget " +
                                    "a lacus. Phasellus quis dolor ac est fermentum eleifend. " +
                                    "Nullam ac orci eu arcu euismod aliquet elementum at erat. " +
                                    "Fusce a fringilla odio. Phasellus mattis hendrerit nisl eu consequat. " +
                                    "Aliquam nec massa felis. Quisque at urna sapien. " +
                                    "Donec rhoncus augue vitae tristique iaculis. " +
                                    "Nullam vehicula ligula nec urna rhoncus, " +
                                    "in sodales eros tincidunt. Suspendisse potenti. " +
                                    "Integer scelerisque ipsum nec ullamcorper commodo. $i",
                            arrayOfColors.random(),
                            arrayOfFontSizes.random(),
                            Timestamp(System.currentTimeMillis()),
                            Timestamp(System.currentTimeMillis())
                        )
                    } else if (i % 5 == 0) { // With title, medium text
                        note = Note(
                            "Title $i",
                            "Nulla bibendum accumsan purus et euismod. In sed elementum massa, " +
                                    "et tempor velit. Duis lacus urna, egestas sed diam eget, " +
                                    "fermentum luctus erat. Quisque tempus lorem sit amet libero " +
                                    "fringilla, sit amet lacinia velit interdum. Morbi eleifend $i",
                            arrayOfColors.random(),
                            arrayOfFontSizes.random(),
                            Timestamp(System.currentTimeMillis()),
                            Timestamp(System.currentTimeMillis())
                        )
                    } else { // With title, short text
                        note = Note(
                            "Title $i",
                            "Description $i",
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

    /*
    * This is how to create and populate the database without Dagger Hilt.
    * Keep the code below as an example
    * */
//    private class ManagerDatabaseCallback :
//        RoomDatabase.Callback() {
//        override fun onCreate(db: SupportSQLiteDatabase) {
//            super.onCreate(db)
//            INSTANCE?.let { passwordManagerDatabase ->
//                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
//                    val tokenDao = passwordManagerDatabase.tokenDao()
//                    val noteDao = passwordManagerDatabase.noteDao()
//
//                    val arrayOfFontSizes = (12..22 step 2).toList().toTypedArray()
//                    val arrayOfColors =
//                        arrayOf("#ffffff", "#ff5252", "#ffab00", "#43a047", "#0091ea")
////                    tokenDao.deleteAll()
////                    val token = Token(0, "aispipevnwpindksnsda")
////                    tokenDao.insert(token)
//
//                    noteDao.deleteAll()
//                    var note: Note
//                    for (i in 0 until 50) {
//                        if (i % 3 == 0 && i % 4 == 0) {
//                            note = Note(
//                                "",
//                                "Description${i}",
//                                arrayOfColors.random(),
//                                arrayOfFontSizes.random(),
//                                Timestamp(System.currentTimeMillis()),
//                                Timestamp(System.currentTimeMillis())
//                            )
//                        } else {
//                            note = Note(
//                                "Title${i}",
//                                "Description${i}",
//                                arrayOfColors.random(),
//                                arrayOfFontSizes.random(),
//                                Timestamp(System.currentTimeMillis()),
//                                Timestamp(System.currentTimeMillis())
//                            )
//                        }
//                        noteDao.insert(note)
//                    }
//                }
//            }
//        }
//    }
//
//    companion object {
//        @Volatile
//        private var INSTANCE: PasswordManagerDatabase? = null
//
//        fun getInstance(context: Context): PasswordManagerDatabase {
//            return INSTANCE ?: synchronized(this) {
//                val instance = Room.databaseBuilder(
//                    context.applicationContext,
//                    PasswordManagerDatabase::class.java,
//                    "vaultsec-database"
//                )
//                    .fallbackToDestructiveMigration()
//                    // The line below populates the DB with random data
//                    // ON DATABASE CREATION
//                    .addCallback(ManagerDatabaseCallback())
//                    .build()
//                INSTANCE = instance
//                instance
//            }
//        }
//    }
}