package com.vaultsec.vaultsec.database.dao

import androidx.room.*
import com.vaultsec.vaultsec.database.PaymentCardsSortOrder
import com.vaultsec.vaultsec.database.entity.PaymentCard
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentCardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: PaymentCard)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @JvmSuppressWildcards
    suspend fun insertList(cardList: ArrayList<PaymentCard>)

    @Update
    suspend fun update(card: PaymentCard)

    @Query("""DELETE FROM vault_cards""")
    fun deleteAll()

    @Query("""DELETE FROM vault_cards WHERE id IN (:idList)""")
    fun deleteSelectedPaymentCards(idList: ArrayList<Int>)

    @Query("""SELECT * FROM vault_cards WHERE sync_state = 1""")
    fun getUnsyncedPaymentCards(): Flow<List<PaymentCard>>

    @Query("""SELECT id FROM vault_cards WHERE sync_state = 2""")
    fun getSyncedDeletedPaymentCardsIds(): Flow<List<Int>>

    @Query("""SELECT * FROM vault_cards WHERE sync_state = 3""")
    fun getSyncedUpdatedPaymentCards(): Flow<List<PaymentCard>>

    fun getPaymentCards(
        searchQuery: String,
        paymentCardsSortOrder: PaymentCardsSortOrder,
        isAsc: Boolean
    ): Flow<List<PaymentCard>> =
        when (paymentCardsSortOrder) {
            PaymentCardsSortOrder.BY_TITLE -> getPaymentCardsSortedByTitle(
                searchQuery,
                isAsc
            )
            PaymentCardsSortOrder.BY_DATE_CREATED -> getPaymentCardsSortedByDateCreated(
                searchQuery,
                isAsc
            )
            PaymentCardsSortOrder.BY_DATE_UPDATED -> getPaymentCardsSortedByDateUpdated(
                searchQuery,
                isAsc
            )
            PaymentCardsSortOrder.BY_DATE_EXPIRATION -> getPaymentCardsSortedByDateExpiration(
                searchQuery,
                isAsc
            )
            PaymentCardsSortOrder.BY_TYPE -> getPaymentCardsSortedByType(searchQuery, isAsc)
        }

    @Query(
        """SELECT * FROM vault_cards WHERE sync_state != 2 AND (title LIKE '%' || :searchQuery || '%' OR pin LIKE '%' || :searchQuery || '%') ORDER BY
            CASE WHEN :isAsc = 1 THEN title END ASC,
            CASE WHEN :isAsc = 1 AND title IS NULL THEN expiration_yy END ASC,
            CASE WHEN :isAsc = 0 THEN title END DESC,
            CASE WHEN :isAsc = 0 AND title IS NULL THEN expiration_yy END DESC"""
    )
    fun getPaymentCardsSortedByTitle(searchQuery: String, isAsc: Boolean): Flow<List<PaymentCard>>

    @Query(
        """SELECT * FROM vault_cards WHERE sync_state != 2 AND (title LIKE '%' || :searchQuery || '%' OR pin LIKE '%' || :searchQuery || '%') ORDER BY
            CASE WHEN :isAsc = 1 THEN created_at_local END ASC,
            CASE WHEN :isAsc = 0 THEN created_at_local END DESC"""
    )
    fun getPaymentCardsSortedByDateCreated(
        searchQuery: String,
        isAsc: Boolean
    ): Flow<List<PaymentCard>>

    @Query(
        """SELECT * FROM vault_cards WHERE sync_state != 2 AND (title LIKE '%' || :searchQuery || '%' OR pin LIKE '%' || :searchQuery || '%') ORDER BY
            CASE WHEN :isAsc = 1 THEN updated_at_local END ASC,
            CASE WHEN :isAsc = 0 THEN updated_at_local END DESC"""
    )
    fun getPaymentCardsSortedByDateUpdated(
        searchQuery: String,
        isAsc: Boolean
    ): Flow<List<PaymentCard>>

    @Query(
        """SELECT * FROM vault_cards WHERE sync_state != 2 AND (title LIKE '%' || :searchQuery || '%' OR pin LIKE '%' || :searchQuery || '%') ORDER BY
            CASE WHEN :isAsc = 1 THEN expiration_yy + expiration_mm END ASC,
            CASE WHEN :isAsc = 0 THEN expiration_yy + expiration_mm END DESC"""
    )
    fun getPaymentCardsSortedByDateExpiration(
        searchQuery: String,
        isAsc: Boolean
    ): Flow<List<PaymentCard>>

    @Query(
        """SELECT * FROM vault_cards WHERE sync_state != 2 AND (title LIKE '%' || :searchQuery || '%' OR pin LIKE '%' || :searchQuery || '%') ORDER BY
            CASE WHEN :isAsc = 1 THEN type END ASC,
            CASE WHEN :isAsc = 0 THEN type END DESC"""
    )
    fun getPaymentCardsSortedByType(searchQuery: String, isAsc: Boolean): Flow<List<PaymentCard>>
}