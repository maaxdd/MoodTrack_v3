package com.mawekk.sterdiary.db

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mawekk.sterdiary.DiaryApp
import com.mawekk.sterdiary.STRUCTURE
import com.mawekk.sterdiary.TAG
import com.mawekk.sterdiary.databinding.FragmentNewNoteBinding
import com.mawekk.sterdiary.db.dao.EmotionDao
import com.mawekk.sterdiary.db.dao.NoteDao
import com.mawekk.sterdiary.db.entities.Emotion
import com.mawekk.sterdiary.db.entities.Note
import com.mawekk.sterdiary.db.entities.NoteEmotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

open class DiaryViewModel(application: Application) : AndroidViewModel(application) {
    private val noteDao: NoteDao
    private val mutableSelectedNote = MutableLiveData<Note>()
    val selectedNote: LiveData<Note> get() = mutableSelectedNote

    private val emotionDao: EmotionDao
    private val mutableSelectedEmotions = MutableLiveData<List<Emotion>>()
    private val mutableEditMode = MutableLiveData(false)
    private val mutableLoadMode = MutableLiveData(false)
    private val mutablePinMode = MutableLiveData<String>()
    val selectedEmotions: LiveData<List<Emotion>> get() = mutableSelectedEmotions
    val editMode: LiveData<Boolean> get() = mutableEditMode
    val loadMode: LiveData<Boolean> get() = mutableLoadMode
    val pinMode: LiveData<String> get() = mutablePinMode

    init {
        val app = application as DiaryApp
        noteDao = app.diaryDatabase.noteDao()
        emotionDao = app.diaryDatabase.emotionDao()
    }

    fun changeLoadMode(value: Boolean) {
        mutableLoadMode.value = value
    }

    fun selectNote(note: Note) {
        mutableSelectedNote.value = note
    }

    fun changePinMode(value: String) {
        mutablePinMode.value = value
    }

    fun getAllNotes(): LiveData<List<Note>> {
        return noteDao.getAllNotes()
    }

    suspend fun getAllNotesAsync(): List<Note> {
        return noteDao.getAllNotesAsync()
    }

    private fun addNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            noteDao.addNote(note)
        }
    }

    private fun addEmotionToNote(noteEmotion: NoteEmotion) {
        viewModelScope.launch(Dispatchers.IO) {
            noteDao.addEmotionToNote(noteEmotion)
        }
    }

    fun deleteNote(note: Note) {
        deleteNoteEmotionsById(note.id)
        viewModelScope.launch(Dispatchers.IO) {
            noteDao.deleteNote(note)
        }
    }

    private fun checkNote(note: Note, emotions: List<Emotion>): Boolean {
        // Разрешаем сохранение даже при незаполненных полях по требованию пользователя
        return true
    }

    fun assembleNote(
        binding: FragmentNewNoteBinding, id: Long, distortions: String
    ): Note {
        binding.apply {
            val date = dateText.text.toString()
            val time = timeText.text.toString()
            val situation = situationText.text.toString()
            val discomfortBefore = percentsBefore.text.toString()
            val thoughts = thoughtsText.text.toString()
            val feelings = feelingsText.text.toString()
            val actions = actionsText.text.toString()
            val answer = answerText.text.toString()
            val discomfortAfter = percentsAfter.text.toString()

            return Note(
                id,
                date,
                time,
                situation,
                discomfortBefore,
                thoughts,
                feelings,
                actions,
                distortions,
                answer,
                discomfortAfter
            )
        }
    }

    private fun updateNoteEmotions(noteId: Long, emotions: List<Emotion>) {
        deleteNoteEmotionsById(noteId)
        emotions.forEach {
            addEmotionToNote(NoteEmotion(0, noteId, it.name))
        }
    }

    fun saveNote(note: Note, emotions: List<Emotion>): Boolean {
        return if (checkNote(note, emotions)) {
            addNote(note)
            updateNoteEmotions(note.id, emotions)
            true
        } else {
            false
        }
    }

    fun updateNote(note: Note, emotions: List<Emotion>): Boolean {
        return if (checkNote(note, emotions)) {
            viewModelScope.launch(Dispatchers.IO) {
                noteDao.updateNote(note)
            }
            updateNoteEmotions(note.id, emotions)
            true
        } else {
            false
        }
    }

    fun getNoteEmotionsById(id: Long): LiveData<List<Emotion>> {
        return noteDao.getNoteEmotionsById(id)
    }

    suspend fun getNoteEmotionsByIdAsync(id: Long): List<Emotion> {
        return noteDao.getNoteEmotionsByIdAsync(id)
    }

    private fun deleteNoteEmotionsById(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            noteDao.deleteNoteEmotionsById(id)
        }
    }

    fun getMaxId(): LiveData<Long> {
        return noteDao.getMaxId()
    }

    fun selectEmotions(emotions: List<Emotion>) {
        mutableSelectedEmotions.value = emotions
    }

    fun changeEditMode(value: Boolean) {
        mutableEditMode.value = value
    }

    fun deselectEmotion(emotion: Emotion) {
        mutableSelectedEmotions.value =
            mutableSelectedEmotions.value?.filter { it.name != emotion.name }
    }

    fun isEmotionSelected(emotion: Emotion): Boolean {
        return mutableSelectedEmotions.value?.contains(emotion) ?: false
    }

    fun getAllEmotions(): LiveData<List<Emotion>> {
        return emotionDao.getAllEmotions()
    }

    fun getNotesEmotions(): LiveData<List<NoteEmotion>> {
        return noteDao.getNotesEmotions()
    }

    fun addEmotion(emotion: Emotion) {
        viewModelScope.launch(Dispatchers.IO) {
            emotionDao.addEmotion(emotion)
        }
    }

    fun deleteEmotion(emotion: Emotion) {
        viewModelScope.launch(Dispatchers.IO) {
            emotionDao.deleteEmotion(emotion)
            noteDao.deleteNoteEmotionsByName(emotion.name)
        }
    }

    fun getEmotionsCount(): Int {
        return emotionDao.getEmotionsCount()
    }

    fun sortNotes(notes: List<Note>): List<Note> {
        // В БД могут жить заметки с датами, сохранёнными как с английскими, так и с русскими месяцами.
        // Поэтому пробуем сначала русский формат, а при ошибке — английский.
        val ruFormatter = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("ru"))
        val enFormatter = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.ENGLISH)

        return notes.sortedBy { note ->
            val dateAndTimeString = note.date + ' ' + note.time
            val parsed: Date = try {
                ruFormatter.parse(dateAndTimeString)
            } catch (_: ParseException) {
                try {
                    enFormatter.parse(dateAndTimeString)
                } catch (_: ParseException) {
                    // Если совсем не смогли распарсить, отправляем в самый низ списка
                    Date(0)
                }
            }
            parsed
        }
    }
}
