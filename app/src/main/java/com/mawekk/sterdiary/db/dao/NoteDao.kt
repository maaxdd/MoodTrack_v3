package com.mawekk.sterdiary.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mawekk.sterdiary.db.entities.Emotion
import com.mawekk.sterdiary.db.entities.Note
import com.mawekk.sterdiary.db.entities.NoteEmotion

@Dao
interface NoteDao {
    @Insert
    suspend fun addNote(note: Note)
    @Update
    suspend fun updateNote(note: Note)
    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes_table")
    suspend fun getAllNotesAsync(): List<Note>

    @Query("SELECT name FROM note_emotion_table WHERE note_id = :id")
    suspend fun getNoteEmotionsByIdAsync(id: Long): List<Emotion>

    @Delete
    suspend fun deleteNoteEmotion(noteEmotion: NoteEmotion)
    @Query("SELECT * FROM notes_table")
    fun getAllNotes(): LiveData<List<Note>>

    @Query("SELECT * FROM note_emotion_table")
    fun getNotesEmotions(): LiveData<List<NoteEmotion>>

    @Query("SELECT MAX(id) FROM notes_table")
    fun getMaxId(): LiveData<Long>
    @Insert
    suspend fun addEmotionToNote(noteEmotion: NoteEmotion)

    @Query("SELECT name FROM note_emotion_table WHERE note_id = :id")
    fun getNoteEmotionsById(id: Long): LiveData<List<Emotion>>

    @Query("DELETE FROM note_emotion_table WHERE note_id = :id")
    fun deleteNoteEmotionsById(id: Long)

    @Query("DELETE FROM note_emotion_table WHERE name = :name")
    fun deleteNoteEmotionsByName(name: String)
}
