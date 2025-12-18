package com.mawekk.sterdiary

import android.content.Context
import android.graphics.Rect
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.content.res.Configuration
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.google.android.material.textfield.TextInputEditText
import com.mawekk.sterdiary.databinding.ActivityMainBinding
import com.mawekk.sterdiary.db.DiaryViewModel
import com.mawekk.sterdiary.fragments.ArchiveFragment
import com.mawekk.sterdiary.fragments.NewNoteFragment
import com.mawekk.sterdiary.fragments.PinCodeFragment
import com.mawekk.sterdiary.fragments.SearchFragment
import com.mawekk.sterdiary.fragments.SettingsFragment
import com.mawekk.sterdiary.fragments.StatisticsFragment
import java.util.Stack


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private val idStack = Stack<Int>()
    private val viewModel: DiaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme before inflating views
        val prefs = getSharedPreferences(TAG, Context.MODE_PRIVATE)
        when (prefs.getString(THEME, "system")) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreateOptionsMenu(binding.noteTopBar.menu)
        setContentView(binding.root)

        // Soften backgrounds with theme awareness
        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val softBg = if (isNight) Color.parseColor("#0F1115") else Color.parseColor("#F6F8FB")
        val softSurface = if (isNight) Color.parseColor("#1A1E24") else Color.parseColor("#FAFBFE")
        binding.root.setBackgroundColor(softBg)
        binding.appBarLayout.setBackgroundColor(softSurface)
        binding.topAppBar.setBackgroundColor(softSurface)
        binding.newNoteTopBar.setBackgroundColor(softSurface)
        binding.editNoteTopBar.setBackgroundColor(softSurface)
        binding.emotionsTopBar.setBackgroundColor(softSurface)
        binding.settingsTopBar.setBackgroundColor(softSurface)
        binding.noteTopBar.setBackgroundColor(softSurface)
        binding.exportTopBar.setBackgroundColor(softSurface)
        binding.bottomNavigation.setBackgroundColor(softSurface)

        val settings = getSharedPreferences(TAG, Context.MODE_PRIVATE)
        val pinCode = settings.getString(PIN_CODE, "") ?: ""

        if (pinCode.isNotEmpty()) {
            hideBottomNavigation()
            binding.topAppBar.isVisible = false
            viewModel.changePinMode(PIN_ENTER)
            showFragment(PinCodeFragment.newInstance(), R.id.pin_text)
            idStack.pop()
        } else {
            // Стартуем с архива и подчёркиваем соответствующую вкладку
            binding.bottomNavigation.selectedItemId = R.id.archive_item
            showFragment(ArchiveFragment.newInstance(), R.id.archive_item)
        }

        setBottomBarNavigation()
        setTopBarNavigation()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val id = idStack.pop()
        if (idStack.isNotEmpty()) {
            when (id) {
                R.id.archive_item, R.id.statistics_item -> {
                    binding.bottomNavigation.setOnItemSelectedListener(backListenerBottom)
                    binding.bottomNavigation.selectedItemId = idStack.peek()
                    binding.bottomNavigation.setOnItemSelectedListener(mainListenerBottom)
                }

                R.id.search_item -> {
                    binding.topAppBar.isVisible = true
                    showBottomNavigation()
                }

                R.id.topAppBar -> {
                    binding.topAppBar.isVisible = true
                    binding.settingsTopBar.isVisible = false
                    showBottomNavigation()
                }

                R.id.addEmotionButton -> {
                    if (idStack.peek() == R.id.newNoteTopBar) {
                        binding.newNoteTopBar.isVisible = true
                    } else if (idStack.peek() == R.id.topAppBar) {
                        binding.settingsTopBar.isVisible = true
                    } else {
                        binding.editNoteTopBar.isVisible = true
                    }
                    binding.emotionsTopBar.isVisible = false
                }

                R.id.newNoteTopBar, R.id.noteTopBar -> {
                    // Возврат либо из экрана просмотра записи, либо из экрана новой записи
                    if (binding.newNoteTopBar.isVisible) {
                        binding.newNoteTopBar.isVisible = false
                    } else {
                        binding.noteTopBar.isVisible = false
                    }
                    if (idStack.peek() != R.id.search_item) {
                        binding.topAppBar.isVisible = true
                        showBottomNavigation()
                    }
                }

                R.id.edit -> {
                    binding.noteTopBar.isVisible = true
                    binding.editNoteTopBar.isVisible = false
                }

                R.id.pin_text ->
                    if (idStack.peek() == R.id.topAppBar) {
                        binding.settingsTopBar.isVisible = true
                    }
            }
        } else {
            finish()
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            if (view is TextInputEditText || view is EditText) {
                val outRect = Rect()
                view.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    view.clearFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }


    fun showFragment(fragment: Fragment, @IdRes buttonId: Int): Boolean {
        supportFragmentManager.commit {
            replace(R.id.place_holder, fragment)
            setReorderingAllowed(true)
            addToBackStack(null)
            idStack.push(buttonId)
        }
        return true
    }

    fun hideBottomNavigation() {
        binding.apply {
            bottomNavigation.isVisible = false
            bottomShadow.isVisible = false
        }
    }

    fun showBottomNavigation() {
        binding.apply {
            bottomNavigation.isVisible = true
            bottomShadow.isVisible = true
        }
    }

    private val mainListenerBottom: (item: MenuItem) -> Boolean = {
        when (it.itemId) {
            R.id.archive_item -> {
                binding.topAppBar.setTitle(R.string.archive)
                binding.settingsTopBar.isVisible = false
                binding.topAppBar.isVisible = true
                showBottomNavigation()
                showFragment(ArchiveFragment.newInstance(), it.itemId)
            }

            R.id.statistics_item -> {
                binding.topAppBar.setTitle(R.string.statistics)
                binding.settingsTopBar.isVisible = false
                binding.topAppBar.isVisible = true
                showBottomNavigation()
                showFragment(
                    StatisticsFragment.newInstance(),
                    it.itemId
                )
            }

            R.id.settings_item -> {
                showBottomNavigation()
                binding.topAppBar.isVisible = false
                binding.settingsTopBar.isVisible = true
                showFragment(SettingsFragment.newInstance(), it.itemId)
            }

            else -> false
        }
    }

    private val backListenerBottom: (item: MenuItem) -> Boolean = {
        when (it.itemId) {
            R.id.archive_item -> {
                binding.topAppBar.setTitle(R.string.archive)
                true
            }

            R.id.statistics_item -> {
                binding.topAppBar.setTitle(R.string.statistics)
                true
            }

            R.id.settings_item -> {
                binding.topAppBar.setTitle(R.string.settings)
                true
            }

            else -> false
        }
    }

    private fun setBottomBarNavigation() {
        binding.apply {
            bottomNavigation.setOnItemSelectedListener(mainListenerBottom)
        }
    }

    fun openNewNote() {
        hideBottomNavigation()
        binding.topAppBar.isVisible = false
        binding.newNoteTopBar.isVisible = true
        showFragment(
            NewNoteFragment.newInstance(),
            R.id.newNoteTopBar
        )
    }

    private fun setTopBarNavigation() {
        binding.apply {
            topAppBar.setNavigationOnClickListener {
                // Навигационная кнопка больше не открывает настройки
            }
            topAppBar.setOnMenuItemClickListener {
                hideBottomNavigation()
                showFragment(SearchFragment.newInstance(), R.id.search_item)
                topAppBar.isVisible = false
                true
            }
        }
    }
}
