package com.mawekk.sterdiary

import android.app.Application
import androidx.room.Room
import com.mawekk.sterdiary.db.DiaryDatabase
import com.mawekk.sterdiary.db.entities.Emotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class DiaryApp : Application() {
    lateinit var diaryDatabase: DiaryDatabase

    override fun onCreate() {
        super.onCreate()
        diaryDatabase = Room.databaseBuilder(
            applicationContext,
            DiaryDatabase::class.java, "diary-db"
        )
            .fallbackToDestructiveMigration().build()

        fillEmotionsDb()
    }

    private fun fillEmotionsDb() {
        MainScope().launch(Dispatchers.IO) {
            val defaultEmotions = resources.getStringArray(R.array.default_emotions)
            if (diaryDatabase.emotionDao().getEmotionsCount() == 0) {
                defaultEmotions.forEach {
                    diaryDatabase.emotionDao().addEmotion(Emotion(it))
                }
            }
        }
    }
}