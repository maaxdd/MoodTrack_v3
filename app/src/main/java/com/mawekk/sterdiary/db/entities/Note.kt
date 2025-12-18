package com.mawekk.sterdiary.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes_table")
data class Note(
    @PrimaryKey var id: Long,
    @ColumnInfo(name = "date") var date: String,
    @ColumnInfo(name = "time") var time: String,
    @ColumnInfo(name = "situation") var situation: String,
    @ColumnInfo(name = "discomfort_before") var discomfortBefore: String,
    @ColumnInfo(name = "thoughts") var thoughts: String,
    @ColumnInfo(name = "feelings") var feelings: String,
    @ColumnInfo(name = "actions") var actions: String,
    @ColumnInfo(name = "distortions")var distortions: String,
    @ColumnInfo(name = "answer") var answer: String,
    @ColumnInfo(name = "discomfort_after") var discomfortAfter: String
)
