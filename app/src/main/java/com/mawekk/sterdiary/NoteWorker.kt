package com.mawekk.sterdiary

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.res.Configuration
import android.content.res.ColorStateList
import android.icu.util.Calendar
import android.view.View
import android.view.Gravity
import android.util.TypedValue
import android.graphics.drawable.GradientDrawable
import android.graphics.Color
import android.view.ViewGroup
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.view.View as AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.mawekk.sterdiary.databinding.FragmentNewNoteBinding
import com.mawekk.sterdiary.db.DiaryViewModel
import com.mawekk.sterdiary.db.entities.Note
import com.mawekk.sterdiary.db.entities.Emotion
import com.mawekk.sterdiary.fragments.EmotionsFragment
import java.text.SimpleDateFormat
import kotlin.math.roundToInt

class NoteWorker(
    private val fragmentActivity: FragmentActivity,
    private val activity: MainActivity,
    private val viewModel: DiaryViewModel,
    private val owner: LifecycleOwner,
    private val binding: FragmentNewNoteBinding
) {
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", java.util.Locale("ru"))
    private val timeFormat = SimpleDateFormat("HH:mm")
    lateinit var boxes: List<CheckBox>

    // Группы эмоций для раскраски и разбиения на хорошие/плохие
    private val positiveEmotions = setOf(
        "Радость", "Восторг", "Гордость", "Восхищение", "Умиротворение", "Воодушевление",
        "Спокойствие", "Интерес", "Любовь",
        // дополнительные положительные эмоции
        "Удовлетворение", "Вдохновение", "Благодарность", "Надежда", "Оптимизм"
    )
    private val negativeEmotions = setOf(
        "Гнев", "Раздражение", "Отвращение", "Презрение", "Обида", "Досада", "Зависть", "Ревность",
        "Грусть", "Разочарование", "Тоска", "Отчаяние", "Сожаление", "Страх", "Тревога", "Ужас",
        "Паника", "Стыд", "Вина"
    )

    // Wizard state
    private var currentStep = 0
    private lateinit var steps: List<List<View>>
    private var wizardProgress: LinearProgressIndicator? = null
    private var wizardNav: LinearLayout? = null
    private var prevButton: MaterialButton? = null
    private var nextButton: MaterialButton? = null
    private var discomfortBadge: View? = null
    private var discomfortDial: FrameLayout? = null
    private var modeContainer: View? = null
    private var selectedMode: String? = null
    private var descriptorText: TextView? = null
    private var seekbarLabels: LinearLayout? = null
    private var dayBtn: MaterialButton? = null
    private var momentBtn: MaterialButton? = null
    private var modeTopSpacer: AndroidView? = null
    private var modeBottomSpacer: AndroidView? = null
    private var topSummary: TextView? = null
    private var afterDial: FrameLayout? = null
    private var afterDescriptorText: TextView? = null
    private var afterSeekbarLabels: LinearLayout? = null
    private var finalSummary: TextView? = null
    private var finalContainer: LinearLayout? = null
    private var finalColorBadge: View? = null
    private var currentMoodColor: Int = Color.parseColor("#C7C7CC")
    private var finalTopSpacer: AndroidView? = null
    private var finalBottomSpacer: AndroidView? = null

    fun initWizard() {
        // 0 — выбор режима, 1 — общий уровень, 2 — эмоции, 3 — факторы, 4 — рефлексия
        val screens = mutableListOf<List<View>>()

        // 0: Выбор режима (за день / сиюминутные)
        createModeChoice()
        createModeSpacers()
        createTopSummary()
        val step0 = mutableListOf<View>()
        modeTopSpacer?.let { step0 += it }
        modeContainer?.let { step0 += it }
        modeBottomSpacer?.let { step0 += it }
        screens += step0

        // 1: Общие ощущения (круг + шкала)
        updateQuestionByMode()
        val step1 = mutableListOf<View>()
        step1 += binding.levelBefore
        discomfortDial?.let { step1 += it }
        descriptorText?.let { step1 += it }
        step1 += binding.levelBeforeLayout
        seekbarLabels?.let { step1 += it }
        screens += step1

        // 2: Эмоции (две явные секции: негативные и позитивные)
        val step2 = mutableListOf<View>()
        step2 += binding.textView10
        step2 += binding.emotionsNegativeTitle
        step2 += binding.selectedEmotions
        step2 += binding.emotionsPositiveTitle
        step2 += binding.positiveEmotionsGroup
        screens += step2

        // 3: Факторы влияния (чекбоксы, стилизованные как кнопки)
        val step3 = mutableListOf<View>()
        step3 += binding.textView11
        step3 += binding.checkBox1
        step3 += binding.checkBox2
        step3 += binding.checkBox3
        step3 += binding.checkBox4
        step3 += binding.checkBox5
        step3 += binding.checkBox6
        step3 += binding.checkBox7
        step3 += binding.checkBox8
        step3 += binding.checkBox9
        step3 += binding.checkBox10
        step3 += binding.checkBox11
        step3 += binding.checkBox12
        screens += step3

        // 4: Рефлексия (заголовок + почти полноэкранное поле ввода)
        binding.feelingsLayout.hint = ""
        binding.feelingsText.hint = fragmentActivity.getString(R.string.dear_diary_placeholder)
        val step4 = mutableListOf<View>()
        step4 += binding.textView10
        step4 += binding.feelingsLayout
        screens += step4

        steps = screens
        currentStep = 0
        updateStepUI()
    }

    fun isLastStep(): Boolean = ::steps.isInitialized && currentStep == steps.size - 1

    fun nextStep(): Boolean {
        if (!::steps.isInitialized) return false
        if (!validateCurrentStep()) return false
        return if (currentStep < steps.size - 1) {
            currentStep++
            updateStepUI()
            true
        } else false
    }

    fun prevStep(): Boolean {
        if (!::steps.isInitialized) return false
        return if (currentStep > 0) {
            currentStep--
            updateStepUI()
            true
        } else false
    }

    private fun updateStepUI() {
        if (!::steps.isInitialized) return
        // Перед показом шага с эмоциями (шаг 2) меняем порядок секций в зависимости от настроения
        if (currentStep == 2) {
            reorderEmotionSectionsByMood()
        }

        // Show only current step views
        val allViews = steps.flatten()
        allViews.forEach { it.isVisible = false }
        steps[currentStep].forEach { it.isVisible = true }

        // Проценты рядом с полосками не показываем — только цвет и длина
        binding.percentsBefore.isVisible = false
        binding.percentsAfter.isVisible = false
        // Подписи под шкалой нужны только на экране со шкалой (шаг 1)
        seekbarLabels?.isVisible = currentStep == 1
        afterSeekbarLabels?.isVisible = false

        // Заголовок сверху показываем только на первом шаге (выбор режима)
        topSummary?.isVisible = currentStep == 0

        // Заголовок textView10 используем и для эмоций, и для рефлексии
        when (currentStep) {
            2 -> binding.textView10.text = fragmentActivity.getString(R.string.wizard_emotions_title)
            4 -> binding.textView10.text = fragmentActivity.getString(R.string.wizard_reflection_title)
        }

        // Update top bar title to show step progress
        val total = steps.size
        activity.binding.newNoteTopBar.title = "Новая запись • ${currentStep + 1}/$total"

        // Update progress indicator and nav buttons text/state
        wizardProgress?.let {
            val pct = (((currentStep + 1) * 100f) / total).roundToInt()
            it.setProgressCompat(pct, true)
        }
        prevButton?.isEnabled = currentStep > 0
        // Одна основная кнопка: "Продолжить" или "Записать" на последнем шаге
        nextButton?.text = if (currentStep == total - 1) fragmentActivity.getString(R.string.wizard_finish) else fragmentActivity.getString(R.string.wizard_continue)
        nextButton?.isEnabled = validateCurrentStep()
    }

    /**
     * Меняет порядок блоков "Негативные эмоции" / "Позитивные эмоции" на шаге эмоций
     * в зависимости от текущего уровня настроения (ползунок на шаге 1).
     * Если настроение хорошее (progress >= 50) — наверху показываем позитивные эмоции,
     * если плохое — сначала негативные.
     */
    private fun reorderEmotionSectionsByMood() {
        val parent = binding.emotionsNegativeTitle.parent as? ViewGroup ?: return

        val negTitle = binding.emotionsNegativeTitle
        val negGroup = binding.selectedEmotions
        val posTitle = binding.emotionsPositiveTitle
        val posGroup = binding.positiveEmotionsGroup

        // Удаляем секции из родителя, чтобы затем добавить их в нужном порядке
        parent.removeView(negTitle)
        parent.removeView(negGroup)
        parent.removeView(posTitle)
        parent.removeView(posGroup)

        val goodMood = binding.seekBarBefore.progress >= 50
        if (goodMood) {
            // Сначала позитивные, затем негативные
            parent.addView(posTitle)
            parent.addView(posGroup)
            parent.addView(negTitle)
            parent.addView(negGroup)
        } else {
            // Сначала негативные, затем позитивные (как было по умолчанию)
            parent.addView(negTitle)
            parent.addView(negGroup)
            parent.addView(posTitle)
            parent.addView(posGroup)
        }
    }

    private fun validateCurrentStep(): Boolean {
        // По требованию: разрешить переход дальше без обязательных выборов
        return true
    }

    private fun createModeSpacers() {
        if (modeTopSpacer != null || modeBottomSpacer != null) return
        val parent = binding.dateLayout.parent as ViewGroup
        val idx = parent.indexOfChild(modeContainer)
        modeTopSpacer = AndroidView(fragmentActivity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        modeBottomSpacer = AndroidView(fragmentActivity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        parent.addView(modeTopSpacer, idx)
        parent.addView(modeBottomSpacer, idx + 2)
        // Ensure the content area expands to screen height so spacers can center vertically
        ((binding.root as FrameLayout).getChildAt(0) as? ScrollView)?.post {
            (parent as? LinearLayout)?.minimumHeight = (binding.root as FrameLayout).height
        }
    }

    private fun createTopSummary() {
        if (topSummary != null) return
        val parent = binding.dateLayout.parent as ViewGroup
        val summary = TextView(fragmentActivity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                val mlp = this as ViewGroup.MarginLayoutParams
                mlp.topMargin = dp(16)
                mlp.bottomMargin = dp(8)
            }
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(fragmentActivity, R.color.dark_blue))
            textSize = 20f
            text = "Вы бы хотели описать"
        }
        parent.addView(summary, 0)
        topSummary = summary
    }


    private fun showDatePickerDialog() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(Calendar.YEAR, selectedYear)
                calendar.set(Calendar.MONTH, selectedMonth)
                calendar.set(Calendar.DAY_OF_MONTH, selectedDay)
                binding.dateText.setText(dateFormat.format(calendar.time))
            }

        DatePickerDialog(
            fragmentActivity,
            R.style.Dialog_Theme,
            dateSetListener,
            year,
            month,
            day
        ).show()

    }

    private fun showTimePickerDialog() {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timeSetListener =
            TimePickerDialog.OnTimeSetListener { _, selectedHour, selectedMinute ->
                calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                calendar.set(Calendar.MINUTE, selectedMinute)
                binding.timeText.setText(timeFormat.format(calendar.time))
            }

        TimePickerDialog(
            fragmentActivity,
            R.style.Dialog_Theme,
            timeSetListener,
            hour,
            minute,
            true
        )
            .show()
    }

    fun setDateAndTime() {
        binding.apply {
            dateText.setText(dateFormat.format(System.currentTimeMillis()))
            timeText.setText(timeFormat.format(System.currentTimeMillis()))
        }
    }

    fun setActions() {
        binding.apply {
            // Клики по дате и времени
            dateText.setOnClickListener { showDatePickerDialog() }
            timeText.setOnClickListener { showTimePickerDialog() }

            // Инициализируем шкалу ощущений (до записи)
            showSeekBarProgress(seekBarBefore, percentsBefore)
            // Шкалу "после" в новом мастере не показываем, но значение нужно для БД
            showSeekBarProgress(seekBarAfter, percentsAfter)

            // Стартовые значения: ползунок по умолчанию по центру (50%)
            seekBarBefore.progress = 50
            seekBarAfter.progress = 50
            percentsBefore.text = "50%"
            percentsAfter.text = "50%"

            // Базовая структура шагов (оставляем только то, что нужно мастеру)
            setStructure()
            setDistortions()

            // Переименовываем заголовки под новый флоу
            textView10.text = fragmentActivity.getString(R.string.wizard_emotions_title)
            textView11.text = fragmentActivity.getString(R.string.wizard_influence_title)

            // Убираем лишние поля с экрана
            dateLayout.isVisible = false
            timeLayout.isVisible = false
            situationLayout.isVisible = false
            thoughtsLayout.isVisible = false
            actionsLayout.isVisible = false
            answerLayout.isVisible = false
            levelAfter.isVisible = false
            levelAfterLayout.isVisible = false
            distortionsText.isVisible = false
            addEmotionButton.isVisible = false

            // Круг и подписи под шкалой общего уровня ощущений
            createDiscomfortDial()
            createSeekbarLabels()
        }

        // Настраиваем верхнюю панель и нижнюю кнопку мастера
        hookTopBar()
        setupWizardUI()
        initWizard()
    }

    private fun showSeekBarProgress(seekBar: SeekBar, textView: TextView) {
        // Явно задаём диапазон 0–100, чтобы длина полоски соответствовала процентам
        seekBar.max = 100
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(bar: SeekBar) {}
            override fun onStartTrackingTouch(bar: SeekBar) {}
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                // Храним значение в таком же текстовом формате, как раньше, для БД
                textView.text = "$progress%"

                if (seekBar === binding.seekBarBefore) {
                    updateDiscomfortBadgeColor(progress)
                }
                if (seekBar === binding.seekBarAfter) {
                    updateDiscomfortAfterDialColor(progress)
                }
            }
        })
    }

    private fun createModeChoice() {
        if (modeContainer != null) return
        val parent = binding.dateLayout.parent as ViewGroup
        val insertIndex = parent.indexOfChild(binding.dateLayout)

        // Контейнер для двух больших круглых кнопок в углах
        val container = FrameLayout(fragmentActivity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(260)
            ).apply {
                val lp = this as ViewGroup.MarginLayoutParams
                lp.topMargin = dp(24)
                lp.bottomMargin = dp(16)
                lp.marginStart = dp(24)
                lp.marginEnd = dp(24)
            }
        }

        fun selectMode(modeText: String) {
            selectedMode = modeText
            binding.situationText.setText(modeText)
            updateQuestionByMode()
            updateNextEnabled()
        }

        // Левая верхняя кнопка — ощущения за день
        dayBtn = MaterialButton(fragmentActivity).apply {
            layoutParams = FrameLayout.LayoutParams(
                dp(140),
                dp(140),
                Gravity.TOP or Gravity.START
            ).apply {
                marginStart = dp(8)
                topMargin = dp(8)
            }
            text = fragmentActivity.getString(R.string.mode_day)
            isAllCaps = false
            cornerRadius = dp(70)
            textSize = 14f
            rippleColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#330A84FF"))
            setOnClickListener { selectMode(fragmentActivity.getString(R.string.mode_day)) }
        }

        // Правая нижняя кнопка — сиюминутные ощущения
        momentBtn = MaterialButton(fragmentActivity).apply {
            layoutParams = FrameLayout.LayoutParams(
                dp(140),
                dp(140),
                Gravity.BOTTOM or Gravity.END
            ).apply {
                marginEnd = dp(8)
                bottomMargin = dp(8)
            }
            text = fragmentActivity.getString(R.string.mode_moment)
            isAllCaps = false
            cornerRadius = dp(70)
            textSize = 14f
            rippleColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#330A84FF"))
            setOnClickListener { selectMode(fragmentActivity.getString(R.string.mode_moment)) }
        }

        container.addView(dayBtn)
        container.addView(momentBtn)

        parent.addView(container, insertIndex)
        modeContainer = container
        updateModeButtonsStyle()
    }

    private fun updateQuestionByMode() {
        val q = if (selectedMode == fragmentActivity.getString(R.string.mode_day)) fragmentActivity.getString(R.string.question_day)
        else fragmentActivity.getString(R.string.question_moment)
        binding.levelBefore.text = q
        binding.levelBefore.textSize = 22f
        updateModeButtonsStyle()
    }

    private fun updateModeButtonsStyle() {
        val selDay = selectedMode == fragmentActivity.getString(R.string.mode_day)
        dayBtn?.let {
            it.backgroundTintList = android.content.res.ColorStateList.valueOf(if (selDay) Color.parseColor("#0A84FF") else Color.parseColor("#200A84FF"))
            it.strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#0A84FF"))
            it.setTextColor(if (selDay) ContextCompat.getColor(fragmentActivity, R.color.white) else Color.parseColor("#0A84FF"))
        }
        momentBtn?.let {
            val sel = selectedMode == fragmentActivity.getString(R.string.mode_moment)
            it.backgroundTintList = android.content.res.ColorStateList.valueOf(if (sel) Color.parseColor("#0A84FF") else Color.parseColor("#200A84FF"))
            it.setTextColor(if (sel) ContextCompat.getColor(fragmentActivity, R.color.white) else Color.parseColor("#0A84FF"))
        }
    }

    private fun updateNextEnabled() {
        nextButton?.isEnabled = validateCurrentStep()
    }

    private fun createDiscomfortBadge() {
        if (discomfortBadge != null) return
        val badge = View(fragmentActivity).apply {
            layoutParams = LinearLayout.LayoutParams(dp(20), dp(20)).apply {
                (this as ViewGroup.MarginLayoutParams).marginEnd = dp(8)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#C7C7CC")) // iOS separator gray
            }
            contentDescription = "Индикатор уровня дискомфорта"
        }
        // add to the end of the levelBeforeLayout
        binding.levelBeforeLayout.addView(badge)
        discomfortBadge = badge
        updateDiscomfortBadgeColor(binding.seekBarBefore.progress)
    }

    private fun updateDiscomfortBadgeColor(progress: Int) {
        val color = when {
            progress >= 67 -> Color.parseColor("#FF3B30") // iOS red
            progress >= 34 -> Color.parseColor("#FF9500") // iOS orange
            progress > 0 -> Color.parseColor("#34C759") // iOS green
            else -> Color.parseColor("#C7C7CC") // iOS gray
        }
        (discomfortBadge?.background as? GradientDrawable)?.setColor(color)
        updateDiscomfortDialColor(progress)
    }

    private fun createDiscomfortDial() {
        if (discomfortDial != null) return
        val parent = binding.levelBeforeLayout.parent as ViewGroup
        val index = parent.indexOfChild(binding.levelBeforeLayout)

        val container = FrameLayout(fragmentActivity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(240)
            ).apply { (this as ViewGroup.MarginLayoutParams).bottomMargin = dp(8) }
        }

        val aura = View(fragmentActivity).apply {
            layoutParams = FrameLayout.LayoutParams(dp(220), dp(220), Gravity.CENTER)
            background = GradientDrawable().apply {
                gradientType = GradientDrawable.RADIAL_GRADIENT
                shape = GradientDrawable.OVAL
                gradientRadius = dp(140).toFloat()
                colors = intArrayOf(Color.parseColor("#660A84FF"), Color.TRANSPARENT)
            }
        }
        val core = View(fragmentActivity).apply {
            layoutParams = FrameLayout.LayoutParams(dp(140), dp(140), Gravity.CENTER)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#C7C7CC"))
            }
            contentDescription = "Индикатор уровня дискомфорта (круг)"
        }
        container.addView(aura)
        container.addView(core)

        parent.addView(container, index)
        discomfortDial = container
        // descriptor label under dial
        val descriptor = TextView(fragmentActivity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { (this as ViewGroup.MarginLayoutParams).bottomMargin = dp(8) }
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(fragmentActivity, R.color.dark_blue))
            textSize = 20f
            text = ""
        }
        parent.addView(descriptor, index + 1)
        descriptorText = descriptor
        updateDiscomfortDialColor(binding.seekBarBefore.progress)
    }

    private fun createAfterDial() {
        if (afterDial != null) return
        val parent = binding.levelAfterLayout.parent as ViewGroup
        val index = parent.indexOfChild(binding.levelAfterLayout)

        val container = FrameLayout(fragmentActivity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(200)
            ).apply { (this as ViewGroup.MarginLayoutParams).bottomMargin = dp(8) }
        }

        val aura = View(fragmentActivity).apply {
            layoutParams = FrameLayout.LayoutParams(dp(180), dp(180), Gravity.CENTER)
            background = GradientDrawable().apply {
                gradientType = GradientDrawable.RADIAL_GRADIENT
                shape = GradientDrawable.OVAL
                gradientRadius = dp(120).toFloat()
                colors = intArrayOf(Color.parseColor("#6634C759"), Color.TRANSPARENT)
            }
        }
        val core = View(fragmentActivity).apply {
            layoutParams = FrameLayout.LayoutParams(dp(120), dp(120), Gravity.CENTER)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#C7C7CC"))
            }
            contentDescription = "Индикатор уровня дискомфорта (после)"
        }
        container.addView(aura)
        container.addView(core)
        parent.addView(container, index)
        afterDial = container

        val descriptor = TextView(fragmentActivity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { (this as ViewGroup.MarginLayoutParams).bottomMargin = dp(8) }
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(fragmentActivity, R.color.dark_blue))
            textSize = 18f
            text = ""
        }
        parent.addView(descriptor, index + 1)
        afterDescriptorText = descriptor
        updateDiscomfortAfterDialColor(binding.seekBarAfter.progress)
    }

    private fun createAfterSeekbarLabels() {
        if (afterSeekbarLabels != null) return
        val parent = binding.levelAfterLayout.parent as ViewGroup
        val afterIndex = parent.indexOfChild(binding.levelAfterLayout) + 1

        val labels = LinearLayout(fragmentActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { (this as ViewGroup.MarginLayoutParams).bottomMargin = dp(12) }
        }
        val left = TextView(fragmentActivity).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = "ОЧЕНЬ ПЛОХО"
            setTextColor(Color.parseColor("#7C8086"))
            textSize = 12f
        }
        val right = TextView(fragmentActivity).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = "ОЧЕНЬ ХОРОШО"
            gravity = Gravity.END
            setTextColor(Color.parseColor("#7C8086"))
            textSize = 12f
        }
        labels.addView(left)
        labels.addView(right)
        parent.addView(labels, afterIndex)
        afterSeekbarLabels = labels
    }

    private fun createSeekbarLabels() {
        if (seekbarLabels != null) return
        val parent = binding.levelBeforeLayout.parent as ViewGroup
        val afterIndex = parent.indexOfChild(binding.levelBeforeLayout) + 1

        val labels = LinearLayout(fragmentActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { (this as ViewGroup.MarginLayoutParams).bottomMargin = dp(12) }
        }
        val left = TextView(fragmentActivity).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = "ОЧЕНЬ ПЛОХО"
            setTextColor(Color.parseColor("#7C8086"))
            textSize = 12f
        }
        val right = TextView(fragmentActivity).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = "ОЧЕНЬ ХОРОШО"
            gravity = Gravity.END
            setTextColor(Color.parseColor("#7C8086"))
            textSize = 12f
        }
        labels.addView(left)
        labels.addView(right)
        parent.addView(labels, afterIndex)
        seekbarLabels = labels
    }

    private fun updateDiscomfortDialColor(progress: Int) {
        val color = when {
            progress >= 67 -> Color.parseColor("#34C759") // green = very good
            progress >= 34 -> Color.parseColor("#FF9500") // orange = medium
            progress > 0 -> Color.parseColor("#FF3B30") // red = very bad
            else -> Color.parseColor("#C7C7CC")
        }
        currentMoodColor = color

        // Цвет самой полоски и бегунка: чем меньше, тем краснее; чем больше, тем зеленее
        binding.seekBarBefore.progressTintList = android.content.res.ColorStateList.valueOf(color)
        binding.seekBarBefore.thumbTintList = android.content.res.ColorStateList.valueOf(color)

        // Меняем цвет самой полоски и бегунка: чем меньше, тем краснее; чем больше, тем зеленее
        binding.seekBarBefore.progressTintList = android.content.res.ColorStateList.valueOf(color)
        binding.seekBarBefore.thumbTintList = android.content.res.ColorStateList.valueOf(color)
        val auraColor = when {
            progress >= 67 -> intArrayOf(Color.parseColor("#6634C759"), Color.TRANSPARENT)
            progress >= 34 -> intArrayOf(Color.parseColor("#66FF9500"), Color.TRANSPARENT)
            progress > 0 -> intArrayOf(Color.parseColor("#66FF3B30"), Color.TRANSPARENT)
            else -> intArrayOf(Color.parseColor("#66C7C7CC"), Color.TRANSPARENT)
        }
        // core is second child
        (discomfortDial?.getChildAt(1)?.background as? GradientDrawable)?.setColor(color)
        ((discomfortDial?.getChildAt(0)?.background) as? GradientDrawable)?.apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            colors = auraColor
        }
        val label = when {
            progress == 0 -> ""
            progress <= 20 -> "Очень неприятные"
            progress <= 40 -> "Отчасти неприятные"
            progress <= 60 -> "Нейтральные"
            progress <= 80 -> "Отчасти приятные"
            else -> "Очень приятные"
        }
        descriptorText?.text = label
        updateBackgroundTint(color)
        updateNextEnabled()
    }

    private fun updateDiscomfortAfterDialColor(progress: Int) {
        val color = when {
            progress >= 67 -> Color.parseColor("#34C759")
            progress >= 34 -> Color.parseColor("#FF9500")
            progress > 0 -> Color.parseColor("#FF3B30")
            else -> Color.parseColor("#C7C7CC")
        }
        val auraColors = intArrayOf(Color.argb(36, Color.red(color), Color.green(color), Color.blue(color)), Color.TRANSPARENT)
        (afterDial?.getChildAt(1)?.background as? GradientDrawable)?.setColor(color)
        ((afterDial?.getChildAt(0)?.background) as? GradientDrawable)?.apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            colors = auraColors
        }
        afterDescriptorText?.text = when {
            progress == 0 -> ""
            progress <= 20 -> "Почти не упал"
            progress <= 40 -> "Слегка упал"
            progress <= 60 -> "Умеренно упал"
            progress <= 80 -> "Заметно упал"
            else -> "Сильно упал"
        }
        updateBackgroundTint(color)
    }

    private fun updateBackgroundTint(color: Int) {
        val root = binding.root as FrameLayout
        val bg = GradientDrawable().apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = dp(520).toFloat()
            setGradientCenter(0.5f, 0.35f)
            val isNight = (fragmentActivity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val base = if (isNight) Color.parseColor("#0F1115") else Color.parseColor("#FFF8F3")
            colors = intArrayOf(Color.argb(44, Color.red(color), Color.green(color), Color.blue(color)), base)
        }
        root.background = bg
    }

    private fun setupWizardUI() {
        if (wizardNav != null && wizardProgress != null) return

        val root = binding.root as FrameLayout

        // Верхняя полоска прогресса
        wizardProgress = LinearProgressIndicator(fragmentActivity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(6)
            ).apply { gravity = Gravity.TOP }
            trackThickness = dp(6)
            setIndicatorColor(Color.parseColor("#0A84FF"))
            setTrackColor(ContextCompat.getColor(fragmentActivity, R.color.light_gray))
        }
        root.addView(wizardProgress)

        // Нижняя панель только с одной центральной кнопкой
        wizardNav = LinearLayout(fragmentActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.BOTTOM }
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(12), dp(24), dp(24))
            background = null
            elevation = dp(8).toFloat()
        }

        nextButton = MaterialButton(fragmentActivity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = fragmentActivity.getString(R.string.wizard_continue)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#0A84FF"))
            setTextColor(ContextCompat.getColor(fragmentActivity, R.color.white))
            setOnClickListener {
                if (!isLastStep()) {
                    nextStep()
                } else {
                    performSave()
                }
            }
        }

        wizardNav?.addView(nextButton)
        root.addView(wizardNav)
        wizardNav?.bringToFront()

        // Add extra bottom padding so content isn't obscured by bottom bar
        val scroll = (root.getChildAt(0) as? ScrollView)
        val content = scroll?.getChildAt(0)
        content?.setPadding(
            content.paddingLeft,
            content.paddingTop,
            content.paddingRight,
            content.paddingBottom + dp(96)
        )

        // Initialize UI state
        updateStepUI()
    }

    private fun dp(v: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        v.toFloat(),
        fragmentActivity.resources.displayMetrics
    ).toInt()

    private fun createFinalSpacers() {
        if (finalTopSpacer != null && finalBottomSpacer != null) return
        val parent = binding.levelBeforeLayout.parent as? LinearLayout ?: return
        val container = finalContainer ?: return
        // Insert top spacer, final container, bottom spacer in that order
        finalTopSpacer = AndroidView(fragmentActivity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
            isVisible = false
        }
        finalBottomSpacer = AndroidView(fragmentActivity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
            isVisible = false
        }
        val containerIndex = parent.indexOfChild(container)
        val insertTop = if (containerIndex >= 0) containerIndex else 0
        parent.addView(finalTopSpacer, insertTop)
        val insertBottom = parent.indexOfChild(container) + 1
        parent.addView(finalBottomSpacer, insertBottom)
    }

    private fun hookTopBar() {
        // Настраиваем верхнюю панель мастера: слева "назад", справа — синий крестик "закрыть"
        activity.binding.newNoteTopBar.post {
            val toolbar = activity.binding.newNoteTopBar

            // Навигационная кнопка: синий back
            toolbar.navigationIcon = androidx.core.content.ContextCompat.getDrawable(
                fragmentActivity,
                R.drawable.ic_back
            )
            toolbar.navigationIcon?.setTint(
                androidx.core.content.ContextCompat.getColor(
                    fragmentActivity,
                    R.color.primary_blue
                )
            )

            // Кнопка "закрыть" справа
            toolbar.menu.clear()
            toolbar.inflateMenu(R.menu.new_note_top_bar)
            toolbar.menu.findItem(R.id.close_item)?.icon?.setTint(
                androidx.core.content.ContextCompat.getColor(
                    fragmentActivity,
                    R.color.primary_blue
                )
            )

            // Логика навигации по шагам мастера
            toolbar.setNavigationOnClickListener {
                // Если можем уйти на предыдущий шаг — делаем это, иначе спрашиваем, выходить ли из мастера
                val moved = prevStep()
                if (!moved) {
                    showBackDialog(fragmentActivity.layoutInflater.inflate(R.layout.back_dialog, null))
                }
            }

            toolbar.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.close_item -> {
                        // Всегда спрашиваем подтверждение выхода из мастера
                        showBackDialog(fragmentActivity.layoutInflater.inflate(R.layout.back_dialog, null))
                        true
                    }

                    else -> false
                }
            }
        }
    }

    private fun performSave() {
        val emotions = viewModel.selectedEmotions.value ?: emptyList()
        val distortions = checkSelectedDistortions().joinToString(separator = ";")
        var id: Long
        viewModel.getMaxId().observe(owner) {
            id = it ?: 0L
            if (viewModel.saveNote(
                    viewModel.assembleNote(
                        binding,
                        id + 1,
                        distortions
                    ),
                    emotions
                )
            ) {
                activity.onBackPressed()
            } else {
                android.widget.Toast.makeText(
                    fragmentActivity,
                    R.string.all_fields_must_be_filled,
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun createFinalSummary() {
        if (finalContainer != null) return
        val parent = binding.levelBeforeLayout.parent as ViewGroup
        val container = LinearLayout(fragmentActivity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                val mlp = this as ViewGroup.MarginLayoutParams
                mlp.topMargin = dp(24)
                mlp.bottomMargin = dp(24)
            }
        }
        val badge = View(fragmentActivity).apply {
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply {
                (this as ViewGroup.MarginLayoutParams).bottomMargin = dp(12)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(currentMoodColor)
            }
            contentDescription = "Цвет настроения"
        }
        val summary = TextView(fragmentActivity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setTextColor(ContextCompat.getColor(fragmentActivity, R.color.dark_blue))
            textSize = 17f
            setLineSpacing(0f, 1.2f)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            gravity = Gravity.CENTER_HORIZONTAL
        }
        container.addView(badge)
        container.addView(summary)
        parent.addView(container, 0)
        finalContainer = container
        finalSummary = summary
        finalColorBadge = badge
    }

    private fun updateFinalSummary() {
        val mode = binding.situationText.text?.toString()?.ifBlank { "—" } ?: "—"
        val level = "До: ${binding.seekBarBefore.progress}%"
        val emotions = viewModel.selectedEmotions.value?.joinToString { it.name }.orEmpty().ifBlank { "—" }
        val reflection = binding.feelingsText.text?.toString()?.ifBlank { "—" } ?: "—"
        val influence = checkSelectedDistortions().joinToString().ifBlank { "—" }
        finalSummary?.text = "Режим: $mode\n$level\nЭмоции: $emotions\nРефлексия: $reflection\nВлияние: $influence"
        (finalColorBadge?.background as? GradientDrawable)?.setColor(currentMoodColor)
    }

    private fun setAddEmotionButton() {
        binding.apply {
            addEmotionButton.setOnClickListener {
                activity.apply {
                    binding.emotionsTopBar.isVisible = true
                    binding.editNoteTopBar.isVisible = false
                    binding.newNoteTopBar.isVisible = false
                    showFragment(
                        EmotionsFragment.newInstance(),
                        R.id.addEmotionButton
                    )
                }
            }
        }
    }

    fun setDistortions() {
        binding.apply {
            boxes = listOf(
                checkBox1,
                checkBox2,
                checkBox3,
                checkBox4,
                checkBox5,
                checkBox6,
                checkBox7,
                checkBox8,
                checkBox9,
                checkBox10,
                checkBox11,
                checkBox12
            )
            val distortionsNames =
                fragmentActivity.resources.getStringArray(R.array.influence_categories)
            boxes.forEachIndexed { index, checkBox ->
                if (index < distortionsNames.size) {
                    checkBox.text = distortionsNames[index]
                }
                // визуальный feedback: синий текст в обычном состоянии и белый при выборе
                checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                    val color = if (isChecked)
                        ContextCompat.getColor(fragmentActivity, R.color.white)
                    else
                        ContextCompat.getColor(fragmentActivity, R.color.dark_blue)
                    buttonView.setTextColor(color)
                }
            }
        }
    }

    fun showSelectedEmotions(context: Context?) {
        binding.addEmotionButton.isVisible = false
        viewModel.getAllEmotions().observe(owner) { all ->
            val selected = viewModel.selectedEmotions.value ?: emptyList()

            // Очищаем обе группы перед перерисовкой
            binding.selectedEmotions.removeAllViews()      // негативные
            binding.positiveEmotionsGroup.removeAllViews() // позитивные и прочие

            // Вообще не показываем эмоцию «Азарт», даже если она есть в БД
            val filtered = all.filter { it.name != "Азарт" }

            // Сначала негативные, затем позитивные, потом остальные
            val negatives = filtered.filter { negativeEmotions.contains(it.name) }
            val positives = filtered.filter { positiveEmotions.contains(it.name) }
            val others = filtered.filter { !negativeEmotions.contains(it.name) && !positiveEmotions.contains(it.name) }

            fun addChipToGroup(e: Emotion, group: ViewGroup) {
                val chip = Chip(context)
                chip.text = e.name
                chip.isCheckable = true
                chip.isChecked = selected.any { it.name == e.name }

                val isNight = (fragmentActivity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

                val (accent, uncheckedBg) = when {
                    negativeEmotions.contains(e.name) -> {
                        val acc = Color.parseColor("#FF3B30") // красный
                        val bg = if (isNight) Color.parseColor("#331C0B0A") else Color.parseColor("#FFF0EB")
                        acc to bg
                    }
                    positiveEmotions.contains(e.name) -> {
                        val acc = Color.parseColor("#34C759") // зелёный
                        val bg = if (isNight) Color.parseColor("#33132615") else Color.parseColor("#EAF9EE")
                        acc to bg
                    }
                    else -> {
                        val acc = Color.parseColor("#0A84FF") // синий по умолчанию
                        val bg = if (isNight) Color.parseColor("#1E2630") else Color.parseColor("#E9F2FF")
                        acc to bg
                    }
                }

                val textNormal = if (isNight) Color.parseColor("#E9EEF5") else ContextCompat.getColor(fragmentActivity, R.color.dark_blue)
                val checkedBg = accent

                chip.setTextColor(ColorStateList.valueOf(textNormal))
                chip.rippleColor = ColorStateList.valueOf(Color.parseColor("#330A84FF"))
                chip.chipStrokeWidth = dp(1).toFloat()
                chip.chipStrokeColor = ColorStateList.valueOf(accent)
                chip.chipBackgroundColor = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(checkedBg, uncheckedBg)
                )
                chip.isCheckedIconVisible = false

                chip.setOnCheckedChangeListener { _, isChecked ->
                    chip.setTextColor(if (isChecked) ContextCompat.getColor(fragmentActivity, R.color.white) else textNormal)
                }
                chip.setOnClickListener {
                    val cur = viewModel.selectedEmotions.value ?: emptyList()
                    if (chip.isChecked) {
                        viewModel.selectEmotions(cur + e)
                    } else {
                        viewModel.selectEmotions(cur.filter { it.name != e.name })
                    }
                    updateNextEnabled()
                }
                group.addView(chip)
            }

            negatives.forEach { addChipToGroup(it, binding.selectedEmotions) }
            positives.forEach { addChipToGroup(it, binding.positiveEmotionsGroup) }
            // "Прочие" эмоции показываем после позитивных, с синим цветом
            others.forEach { addChipToGroup(it, binding.positiveEmotionsGroup) }
        }
    }

    fun checkSelectedDistortions(): List<String> =
        boxes.map { if (it.isChecked) it.text.toString() else "#" }.filter { it != "#" }

    fun showBackDialog(dialogLayout: View) {
        val builder = AlertDialog.Builder(activity, R.style.Dialog_Theme)
        builder.setView(dialogLayout)
        builder.setTitle(R.string.back_dialog_title)

        val dialog = builder.create()
        val okButton = dialogLayout.findViewById<Button>(R.id.okButton)
        val cancelButton = dialogLayout.findViewById<Button>(R.id.cancelBackButton)

        okButton.setOnClickListener {
            activity.onBackPressed()
            viewModel.changeLoadMode(false)
            dialog.dismiss()
        }
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            val titleId = fragmentActivity.resources.getIdentifier("alertTitle", "id", "android")
            val dialogTitle = dialog.findViewById<View>(titleId) as TextView
            dialogTitle.setTextColor(ContextCompat.getColor(activity, R.color.dark_blue))
        }
        dialog.show()
    }

    fun showHelpDialog(dialogLayout: View) {
        val builder = AlertDialog.Builder(activity, R.style.Dialog_Theme)
        builder.setView(dialogLayout)
        builder.setTitle(R.string.help)

        val dialog = builder.create()
        val okButton = dialogLayout.findViewById<Button>(R.id.closeHelpButton)

        okButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            val titleId = fragmentActivity.resources.getIdentifier("alertTitle", "id", "android")
            val dialogTitle = dialog.findViewById<View>(titleId) as TextView
            dialogTitle.setTextColor(ContextCompat.getColor(activity, R.color.dark_blue))
        }
        dialog.show()
    }

    private fun setStructure() {
        val settings = activity.getSharedPreferences(TAG, Context.MODE_PRIVATE)

        val level = settings.getBoolean(STRUCTURE[0], true)
        val feelings = settings.getBoolean(STRUCTURE[1], true)
        val actions = settings.getBoolean(STRUCTURE[2], true)
        binding.apply {
            levelBefore.isVisible = level
            levelBeforeLayout.isVisible = level
            levelAfter.isVisible = level
            levelAfterLayout.isVisible = level
            feelingsLayout.isVisible = feelings
            actionsLayout.isVisible = actions
            // Answer removed from flow per request
            answerLayout.isVisible = false
        }
    }

    fun clearStructure() {
        binding.apply {
            levelBefore.isVisible = false
            levelBeforeLayout.isVisible = false
            levelAfter.isVisible = false
            levelAfterLayout.isVisible = false
            feelingsLayout.isVisible = false
            actionsLayout.isVisible = false
            answerLayout.isVisible = false
        }
    }

    fun showDeleteDialog(note: Note, dialogLayout: View) {
        val builder = AlertDialog.Builder(activity, R.style.Dialog_Theme)
        builder.setView(dialogLayout)
        builder.setTitle(R.string.delete_dialog_title)


        val dialog = builder.create()
        val deleteButton = dialogLayout.findViewById<Button>(R.id.deleteButton)
        val cancelButton = dialogLayout.findViewById<Button>(R.id.cancelDelButton)

        deleteButton.setOnClickListener {
            activity.binding.noteTopBar.isVisible = false
            viewModel.deleteNote(note)
            activity.onBackPressed()
            dialog.dismiss()
        }
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            val titleId = fragmentActivity.resources.getIdentifier("alertTitle", "id", "android")
            val dialogTitle = dialog.findViewById<View>(titleId) as TextView
            dialogTitle.setTextColor(ContextCompat.getColor(activity, R.color.dark_blue))
        }
        dialog.show()
    }

    fun showFilledFields(note: Note) {
        binding.apply {
            note.apply {
                if (discomfortBefore != "0%") {
                    seekBarBefore.max = 100
                    val progress = discomfortBefore.dropLast(1).toInt()
                    seekBarBefore.progress = progress
                    percentsBefore.text = discomfortBefore
                    levelBefore.isVisible = true
                    levelBeforeLayout.isVisible = true
                    // Поле "после записи" больше не показываем
                    levelAfter.isVisible = false
                    levelAfterLayout.isVisible = false
                    // Окрасить индикатор в соответствии с сохранённым уровнем
                    updateDiscomfortDialColor(progress)
                }
                if (feelings != "") {
                    feelingsText.setText(feelings)
                    feelingsLayout.isVisible = true
                }
                if (actions != "") {
                    actionsText.setText(actions)
                    actionsLayout.isVisible = true
                }
                if (answer != "") {
                    answerText.setText(answer)
                    answerLayout.isVisible = true
                }
            }
        }
    }
}
