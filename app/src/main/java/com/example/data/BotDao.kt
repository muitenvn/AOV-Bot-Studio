package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BotDao {
    @Query("SELECT * FROM bot_profiles ORDER BY updatedAt DESC")
    fun getAllProfiles(): Flow<List<BotProfileEntity>>

    @Query("SELECT COUNT(*) FROM bot_profiles")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: BotProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: BotProfileEntity)

    @Query("DELETE FROM bot_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Int)
}
