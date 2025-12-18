package com.mawekk.sterdiary.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mawekk.sterdiary.db.entities.Emotion

@Dao
interface EmotionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addEmotion(emotion: Emotion)

    @Delete
    suspend fun deleteEmotion(emotion: Emotion)

    @Query("SELECT * FROM emotions_table")
    fun getAllEmotions(): LiveData<List<Emotion>>

    @Query("SELECT COUNT(*) FROM emotions_table")
    fun getEmotionsCount(): Int
}