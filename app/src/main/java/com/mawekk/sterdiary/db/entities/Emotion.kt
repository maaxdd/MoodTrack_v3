package com.mawekk.sterdiary.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emotions_table")
data class Emotion(
    @PrimaryKey
    @ColumnInfo(name = "name") val name: String
)