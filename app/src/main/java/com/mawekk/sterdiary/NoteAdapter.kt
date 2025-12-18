package com.mawekk.sterdiary

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mawekk.sterdiary.databinding.DividerBinding
import com.mawekk.sterdiary.databinding.NoteBinding
import com.mawekk.sterdiary.db.entities.Note

open class RecyclerViewItem
class NoteItem(val note: Note) : RecyclerViewItem() 
class DividerItem(val text: String) : RecyclerViewItem()

class NoteAdapter(val listener: (NoteItem) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var data = emptyList<RecyclerViewItem>()



    override fun getItemViewType(position: Int): Int {
        if (data[position] is NoteItem) {
            return VIEW_TYPE_NOTE
        }
        return VIEW_TYPE_DIVIDER
    }

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = NoteBinding.bind(itemView)

        private fun moodBaseColor(progress: Int): Int = when {
            progress >= 67 -> Color.parseColor("#34C759") // green
            progress >= 34 -> Color.parseColor("#FF9500") // orange
            progress > 0 -> Color.parseColor("#FF3B30")   // red
            else -> Color.parseColor("#C7C7CC")           // neutral
        }

        fun bind(item: NoteItem) {
            binding.apply {
                val rawDate = item.note.date.trimStart('0')
                val localizedDate = localizeMonth(rawDate)
                date.text = localizedDate.dropLast(5) + ", " + item.note.time
                situation.text = item.note.situation

                // Мягкая подсветка карточки по настроению (дискомфорт до записи)
                // В БД хранится строка вида "76%" — извлекаем только цифры
                val progress = item.note.discomfortBefore
                    .filter { it.isDigit() }
                    .toIntOrNull()
                    ?.coerceIn(0, 100)
                    ?: 0
                val base = moodBaseColor(progress)
                val soft = Color.argb(
                    40, // чуть заметнее, но всё ещё мягко
                    Color.red(base),
                    Color.green(base),
                    Color.blue(base)
                )
                noteCard.setCardBackgroundColor(soft)
            }
        }

        private fun localizeMonth(source: String): String {
            val map = mapOf(
                "January" to "января",
                "February" to "февраля",
                "March" to "марта",
                "April" to "апреля",
                "May" to "мая",
                "June" to "июня",
                "July" to "июля",
                "August" to "августа",
                "September" to "сентября",
                "October" to "октября",
                "November" to "ноября",
                "December" to "декабря"
            )
            var result = source
            map.forEach { (en, ru) ->
                result = result.replace(en, ru)
            }
            return result
        }
    }

    class DividerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = DividerBinding.bind(itemView)
        fun bind(item: DividerItem) {
            binding.divider.text = item.text
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_NOTE -> {
                val itemView =
                    LayoutInflater.from(parent.context).inflate(R.layout.note, parent, false)
                NoteViewHolder(itemView)
            }

            else -> {
                val itemView =
                    LayoutInflater.from(parent.context).inflate(R.layout.divider, parent, false)
                DividerViewHolder(itemView)
            }
        }
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = data[position]
        if (holder is NoteViewHolder && item is NoteItem) {
            holder.itemView.setOnClickListener { listener(item) }
            holder.bind(item)
        }
        if (holder is DividerViewHolder && item is DividerItem) {
            holder.bind(item)
        }
    }
    
    fun setList(list: List<RecyclerViewItem>) {
        data = list.reversed()
        notifyDataSetChanged()
    }
}
