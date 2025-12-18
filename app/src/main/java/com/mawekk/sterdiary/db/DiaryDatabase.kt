package com.mawekk.sterdiary.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mawekk.sterdiary.db.dao.EmotionDao
import com.mawekk.sterdiary.db.dao.NoteDao
import com.mawekk.sterdiary.db.entities.Emotion
import com.mawekk.sterdiary.db.entities.Note
import com.mawekk.sterdiary.db.entities.NoteEmotion

@Database(entities = [Note::class, Emotion::class, NoteEmotion::class], version = 9)
abstract class DiaryDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun emotionDao(): EmotionDao
}