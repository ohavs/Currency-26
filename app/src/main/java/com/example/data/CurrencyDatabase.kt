package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey val currencyCode: String,
    val rateRelativeToUSD: Double,
    val timestamp: Long
)

@Entity(tableName = "currency_usage")
data class CurrencyUsageEntity(
    @PrimaryKey val currencyCode: String,
    val lastUsedTimestamp: Long
)

@Dao
interface CurrencyDao {
    @Query("SELECT * FROM exchange_rates WHERE currencyCode = :code")
    suspend fun getRate(code: String): ExchangeRateEntity?

    @Query("SELECT * FROM exchange_rates")
    fun getAllRates(): Flow<List<ExchangeRateEntity>>

    @Query("SELECT MAX(timestamp) FROM exchange_rates")
    suspend fun getLastUpdateTimestamp(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<ExchangeRateEntity>)

    @Query("SELECT * FROM currency_usage ORDER BY lastUsedTimestamp DESC")
    fun getRecentCurrencies(): Flow<List<CurrencyUsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateCurrencyUsage(usage: CurrencyUsageEntity)
}

@Database(entities = [ExchangeRateEntity::class, CurrencyUsageEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun currencyDao(): CurrencyDao
}
