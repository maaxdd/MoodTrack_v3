package com.mawekk.sterdiary

import android.content.Context
import android.os.Environment
import androidx.lifecycle.viewModelScope
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.property.TextAlignment
import com.mawekk.sterdiary.db.DiaryViewModel
import com.mawekk.sterdiary.db.entities.Emotion
import com.mawekk.sterdiary.db.entities.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date


class ExportWorker(
    private val startDate: Date,
    private val endDate: Date,
    private val viewModel: DiaryViewModel,
    private val context: Context
) {
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy")
    private val fileName =
        "STERDiaryNotes_${dateFormat.format(startDate)}-${dateFormat.format(endDate)}".replace(
            " ",
            "_"
        )
    private val res = context.resources
    private val headers = listOf(
        res.getString(R.string.date),
        res.getString(R.string.time),
        res.getString(R.string.situation),
        res.getString(R.string.discomfort_before).dropLast(1),
        res.getString(R.string.thoughts),
        res.getString(R.string.emotions).dropLast(1),
        res.getString(R.string.feelings),
        res.getString(R.string.actions),
        res.getString(R.string.distortions).dropLast(1),
        res.getString(R.string.answer),
        res.getString(R.string.discomfort_after).dropLast(1)
    )

    private fun getNotesToExport(notes: List<Note>): MutableList<Note> {
        val notesToExport = mutableListOf<Note>()
        notes.forEach {
            val date = dateFormat.parse(it.date)
            if (startDate <= date && date <= endDate) {
                notesToExport.add(it)
            }
        }

        return notesToExport
    }

    private fun getFile(fileName: String): File {
        return File(
            context.getExternalFilesDir(
                Environment.DIRECTORY_DOCUMENTS
            ), fileName
        )
    }

    fun exportToCSV(): File {
        val file = getFile("$fileName.csv")

        viewModel.viewModelScope.launch(Dispatchers.IO) {
            val notes = getNotesToExport(viewModel.getAllNotesAsync())
            val emotions = mutableMapOf<Note, List<Emotion>>()
            notes.forEach { note ->
                emotions[note] = viewModel.getNoteEmotionsByIdAsync(note.id)
            }

            csvWriter().open(file) {
                writeRow(headers)
                notes.forEach { note ->
                    writeRow(noteToList(note, emotions[note]!!))
                }
            }
        }
        return file
    }

    fun exportToPDF(): File{
        val file = getFile("$fileName.pdf")

        viewModel.viewModelScope.launch(Dispatchers.IO) {
            val notes = getNotesToExport(viewModel.getAllNotesAsync())
            val emotions = mutableMapOf<Note, List<Emotion>>()
            notes.forEach { note ->
                emotions[note] = viewModel.getNoteEmotionsByIdAsync(note.id)
            }

            val font: PdfFont =
                PdfFontFactory.createFont("/assets/Arial.ttf", PdfEncodings.IDENTITY_H)
            val pdfDocument = PdfDocument(PdfWriter(file))
            val document = Document(pdfDocument)
            document.setFont(font).setFontSize(6f)

            val table = Table(11).useAllAvailableWidth()

            headers.forEach { header ->
                table.addHeaderCell(
                    Cell().add(
                        Paragraph(header).setTextAlignment(TextAlignment.CENTER).setBold()
                    )
                )
            }
            notes.forEach { note ->
                noteToList(note, emotions[note]!!).forEach { elem ->
                    table.addCell(Cell().add(Paragraph(elem).setTextAlignment(TextAlignment.LEFT)))

                }
            }

            document.add(table)
            document.close()
        }
        return file
    }

    private fun noteToList(note: Note, emotions: List<Emotion>): List<String> {
        var emotionsString = ""
        emotions.forEach {
            emotionsString += it.name + "\n"
        }

        note.apply {
            return listOf(
                date,
                time,
                situation,
                discomfortBefore,
                thoughts,
                emotionsString,
                feelings,
                actions,
                distortions.replace(";", "\n"),
                answer,
                discomfortAfter
            )
        }
    }
}