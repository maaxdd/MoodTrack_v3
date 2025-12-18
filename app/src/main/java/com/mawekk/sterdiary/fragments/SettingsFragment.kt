package com.mawekk.sterdiary.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.appcompat.app.AppCompatDelegate
import com.mawekk.sterdiary.ExportWorker
import com.mawekk.sterdiary.MainActivity
import com.mawekk.sterdiary.PIN_CHANGE
import com.mawekk.sterdiary.PIN_CODE
import com.mawekk.sterdiary.PIN_CREATE
import com.mawekk.sterdiary.R
import com.mawekk.sterdiary.STRUCTURE
import com.mawekk.sterdiary.TAG
import com.mawekk.sterdiary.THEME
import com.mawekk.sterdiary.NICKNAME
import com.mawekk.sterdiary.databinding.FragmentSettingsBinding
import com.mawekk.sterdiary.db.DiaryViewModel
import java.io.File
import java.text.SimpleDateFormat


class SettingsFragment : Fragment() {
    private lateinit var binding: FragmentSettingsBinding
    private lateinit var boxes: List<CheckBox>
    private val viewModel: DiaryViewModel by activityViewModels()
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy")

    private val sender: ((File, String) -> Unit) = { file, format ->
        val uri = FileProvider.getUriForFile(
            requireContext(),
            requireContext().applicationContext.packageName + ".provider",
            file
        )

        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = format
        }
        startActivity(Intent.createChooser(shareIntent, null))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        binding.apply {
            // Remove 'answer' from configurable structure
            boxes = listOf(levelBox, feelingsBox, actionsBox)
        }

        setSavedSettings()
        setupNickname()
        setTopAppBarActions()
        setEditEmotionsButton()
        setExportButton()
        setPinButtons()
        // Apply theme and structure changes immediately on user interaction
        binding.themeGroup.setOnCheckedChangeListener { _, _ -> saveChanges() }
        binding.levelBox.setOnCheckedChangeListener { _, _ -> saveChanges() }
        binding.feelingsBox.setOnCheckedChangeListener { _, _ -> saveChanges() }
        binding.actionsBox.setOnCheckedChangeListener { _, _ -> saveChanges() }
        return binding.root
    }

    override fun onPause() {
        super.onPause()
        saveChanges()
    }

    override fun onResume() {
        super.onResume()
        // Обновляем состояние PIN и никнейма при возврате на экран настроек
        setSavedSettings()
        setupNickname()
    }

    private fun setPinButtons() {
        val activity = activity as MainActivity
        binding.apply {
            createPINButton.setOnClickListener {
                activity.apply {
                    viewModel.changePinMode(PIN_CREATE)
                    binding.settingsTopBar.isVisible = false
                    showFragment(PinCodeFragment.newInstance(), R.id.pin_text)
                }
            }
            changePINButton.setOnClickListener {
                activity.apply {
                    viewModel.changePinMode(PIN_CHANGE)
                    binding.settingsTopBar.isVisible = false
                    showFragment(PinCodeFragment.newInstance(), R.id.pin_text)
                }
            }
            removePINButton.setOnClickListener {
                val settings = activity.getSharedPreferences(TAG, Context.MODE_PRIVATE)
                settings.edit().putString(PIN_CODE, "").apply()
                createPINButton.isVisible = true
                changePINButton.isVisible = false
                removePINButton.isVisible = false
                Toast.makeText(requireContext(), R.string.pin_removed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setSavedSettings() {
        val settings = activity?.getSharedPreferences(TAG, Context.MODE_PRIVATE)

        if (settings != null) {
            val theme = settings.getString(THEME, "system")
            val structure = mutableListOf<Boolean>()

            STRUCTURE.forEach {
                structure.add(settings.getBoolean(it, true))
            }

            binding.apply {
                val hasPin = settings.getString(PIN_CODE, "").orEmpty().isNotEmpty()
                createPINButton.isVisible = !hasPin
                changePINButton.isVisible = hasPin
                removePINButton.isVisible = hasPin

                when (theme) {
                    "system" -> systemTheme.isChecked = true
                    "light" -> lightTheme.isChecked = true
                    "dark" -> darkTheme.isChecked = true
                }

                val count = minOf(boxes.size, structure.size)
                for (i in 0 until count) {
                    boxes[i].isChecked = structure[i]
                }
            }
        }
    }

    private fun saveChanges() {
        val settings = activity?.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        if (settings != null) {
            val editor = settings.edit()

            val theme = when (binding.themeGroup.checkedRadioButtonId) {
                R.id.lightTheme -> "light"
                R.id.darkTheme -> "dark"
                else -> "system"
            }

            editor.apply {
                putString(THEME, theme)
                val count = minOf(boxes.size, STRUCTURE.size)
                for (i in 0 until count) {
                    putBoolean(STRUCTURE[i], boxes[i].isChecked)
                }
                apply()
            }

            when (theme) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            activity?.recreate()
        }
    }

    private fun setEditEmotionsButton() {
        val activity = activity as MainActivity
        binding.apply {
            editEmotionsButton.setOnClickListener {
                activity.apply {
                    binding.emotionsTopBar.isVisible = true
                    binding.settingsTopBar.isVisible = false
                    viewModel.changeEditMode(true)
                    showFragment(
                        EmotionsFragment.newInstance(),
                        R.id.addEmotionButton
                    )
                }
            }
        }
    }

    private fun setTopAppBarActions() {
        val activity = activity as MainActivity
        activity.binding.settingsTopBar.apply {
            setOnMenuItemClickListener {
                activity.onBackPressed()
                true
            }
        }
    }

    private fun setExportButton() {
        binding.exportButton.setOnClickListener {
            showExportDialog()
        }
    }

    private fun setupNickname() {
        val activity = activity as? MainActivity ?: return
        val settings = activity.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        val current = settings.getString(NICKNAME, getString(R.string.default_nickname)).orEmpty()

        val nicknameView = binding.nicknameText
        nicknameView.text = current
        nicknameView.setOnClickListener {
            val context = requireContext()
            val editText = android.widget.EditText(context).apply {
                setText(current)
                setSelection(text.length)
            }
            AlertDialog.Builder(context, R.style.Dialog_Theme)
                .setTitle(R.string.nickname_title)
                .setView(editText)
                .setPositiveButton(R.string.save) { _, _ ->
                    val newNick = editText.text.toString().ifBlank { getString(R.string.default_nickname) }
                    settings.edit().putString(NICKNAME, newNick).apply()
                    nicknameView.text = newNick
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun showExportDialog() {
        val builder = AlertDialog.Builder(activity, R.style.Dialog_Theme)
        val dialogLayout = layoutInflater.inflate(
            R.layout.export,
            null
        )
        builder.setView(dialogLayout)
        builder.setTitle(R.string.export_notes)
        val dialog = builder.create()

        val startText = dialogLayout.findViewById<TextView>(R.id.startText)
        val endText = dialogLayout.findViewById<TextView>(R.id.endText)
        val button = dialogLayout.findViewById<Button>(R.id.exportNotesButton)
        val radioGroup = dialogLayout.findViewById<RadioGroup>(R.id.radioGroup)

        setDefaultDates(startText, endText)

        startText.setOnClickListener { showDatePickerDialog(it as TextView) }
        endText.setOnClickListener { showDatePickerDialog(it as TextView) }
        button.setOnClickListener {
            if (exportNotes(startText, endText, radioGroup)) {
                dialog.dismiss()
            }
        }

        dialog.setOnShowListener {
            val titleId = resources.getIdentifier("alertTitle", "id", "android")
            val dialogTitle = dialog.findViewById<View>(titleId) as TextView
            dialogTitle.setTextColor(
                ContextCompat.getColor(
                    activity as MainActivity,
                    R.color.dark_blue
                )
            )
        }
        dialog.show()
    }

    private fun showDatePickerDialog(textView: TextView) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(Calendar.YEAR, selectedYear)
                calendar.set(Calendar.MONTH, selectedMonth)
                calendar.set(Calendar.DAY_OF_MONTH, selectedDay)
                textView.text = dateFormat.format(calendar.time)
            }

        DatePickerDialog(
            requireContext(),
            R.style.Dialog_Theme,
            dateSetListener,
            year,
            month,
            day
        ).show()

    }

    private fun setDefaultDates(startText: TextView, endText: TextView) {
        val endDefault = dateFormat.format(System.currentTimeMillis())
        endText.text = endDefault
        viewModel.getAllNotes()
            .observe(viewLifecycleOwner) {
                val sortedNotes = viewModel.sortNotes(it)
                var startDefault = endDefault
                if (sortedNotes.isNotEmpty()) {
                    startDefault = sortedNotes[0].date
                }
                startText.text = startDefault
            }
    }

    private fun exportNotes(
        startText: TextView,
        endText: TextView,
        radioGroup: RadioGroup
    ): Boolean {
        val startDate = dateFormat.parse(startText.text.toString())
        val endDate = dateFormat.parse(endText.text.toString())

        if (startDate > endDate) {
            setDefaultDates(startText, endText)
            Toast.makeText(
                requireContext(),
                R.string.incorrect_period,
                Toast.LENGTH_SHORT
            )
                .show()

            return false
        } else {
            val exportWorker =
                ExportWorker(startDate, endDate, viewModel, requireContext())

            when (radioGroup.checkedRadioButtonId) {
                R.id.csvButton -> sender(exportWorker.exportToCSV(), "text/csv")
                else -> sender(exportWorker.exportToPDF(), "application/pdf")
            }



            return true
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }
}
