package com.vaultsec.vaultsec.database.dao

import androidx.room.*
import com.vaultsec.vaultsec.database.PasswordsSortOrder
import com.vaultsec.vaultsec.database.entity.Password
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(password: Password)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @JvmSuppressWildcards
    suspend fun insertList(passwordList: ArrayList<Password>)

    @Update
    suspend fun update(password: Password)

    @Query("""DELETE FROM vault_passwords""")
    fun deleteAll()

    @Query("""DELETE FROM vault_passwords WHERE id IN (:idList)""")
    fun deleteSelectedPasswords(idList: ArrayList<Int>)

    @Query("""SELECT * FROM vault_passwords WHERE sync_state = 1""")
    fun getUnsyncedPasswords(): Flow<List<Password>>

    @Query("""SELECT id FROM vault_passwords WHERE sync_state = 2""")
    fun getSyncedDeletedPasswordsIds(): Flow<List<Int>>

    @Query("""SELECT * FROM vault_passwords WHERE sync_state = 3""")
    fun getSyncedUpdatedPasswords(): Flow<List<Password>>

    fun getPasswords(
        searchQuery: String,
        passwordsSortOrder: PasswordsSortOrder,
        isAsc: Boolean
    ): Flow<List<Password>> =
        when (passwordsSortOrder) {
            PasswordsSortOrder.BY_TITLE -> getPasswordsSortedByTitle(searchQuery, isAsc)
            PasswordsSortOrder.BY_DATE_CREATED -> getPasswordsSortedByDateCreated(
                searchQuery,
                isAsc
            )
            PasswordsSortOrder.BY_DATE_UPDATED -> getPasswordsSortedByDateUpdated(
                searchQuery,
                isAsc
            )
            PasswordsSortOrder.BY_CATEGORY -> getPasswordsSortedByCategory(searchQuery, isAsc)
            PasswordsSortOrder.BY_COLOR -> getPasswordsSortedByColor(searchQuery, isAsc)
        }

    @Query(
        """SELECT * FROM vault_passwords WHERE sync_state != 2 AND (title LIKE '%' || :searchQuery || '%' OR login LIKE '%' || :searchQuery || '%' OR url LIKE '%' || :searchQuery || '%') ORDER BY
            CASE WHEN :isAsc = 1 THEN title END ASC,
            CASE WHEN :isAsc = 1 AND title IS NULL THEN category END ASC,
            CASE WHEN :isAsc = 0 THEN title END DESC,
            CASE WHEN :isAsc = 0 AND title IS NULL THEN category END DESC"""
    )
    fun getPasswordsSortedByTitle(searchQuery: String, isAsc: Boolean): Flow<List<Password>>

    @Query(
        """SELECT * FROM vault_passwords WHERE sync_state != 2 AND (title LIKE '%' || :searchQuery || '%' OR login LIKE '%' || :searchQuery || '%' OR url LIKE '%' || :searchQuery || '%') ORDER BY
            CASE WHEN :isAsc = 1 THEN created_at_local END ASC,
            CASE WHEN :isAsc = 0 THEN created_at_local END DESC"""
    )
    fun getPasswordsSortedByDateCreated(searchQuery: String, isAsc: Boolean): Flow<List<Password>>

    @Query(
        """SELECT * FROM vault_passwords WHERE sync_state != 2 AND (title LIKE '%' || :searchQuery || '%' OR login LIKE '%' || :searchQuery || '%' OR url LIKE '%' || :searchQuery || '%') ORDER BY
            CASE WHEN :isAsc = 1 THEN updated_at_local END ASC,
            CASE WHEN :isAsc = 0 THEN updated_at_local END DESC"""
    )
    fun getPasswordsSortedByDateUpdated(searchQuery: String, isAsc: Boolean): Flow<List<Password>>

    @Query(
        """SELECT * FROM vault_passwords WHERE sync_state != 2 AND (title LIKE '%' || :searchQuery || '%' OR login LIKE '%' || :searchQuery || '%' OR url LIKE '%' || :searchQuery || '%') ORDER BY
            CASE WHEN :isAsc = 1 THEN category END ASC,
            CASE WHEN :isAsc = 0 THEN category END DESC"""
    )
    fun getPasswordsSortedByCategory(searchQuery: String, isAsc: Boolean): Flow<List<Password>>

    @Query(
        """SELECT * FROM vault_passwords WHERE sync_state != 2 AND (title LIKE '%' || :searchQuery || '%' OR login LIKE '%' || :searchQuery || '%' OR url LIKE '%' || :searchQuery || '%') ORDER BY
            CASE WHEN :isAsc = 1 THEN color END ASC,
            CASE WHEN :isAsc = 0 THEN color END DESC"""
    )
    fun getPasswordsSortedByColor(searchQuery: String, isAsc: Boolean): Flow<List<Password>>
}