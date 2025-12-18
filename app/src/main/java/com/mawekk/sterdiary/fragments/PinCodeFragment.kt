package com.mawekk.sterdiary.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.mawekk.sterdiary.MainActivity
import com.mawekk.sterdiary.PIN_CHANGE
import com.mawekk.sterdiary.PIN_CODE
import com.mawekk.sterdiary.PIN_CREATE
import com.mawekk.sterdiary.PIN_ENTER
import com.mawekk.sterdiary.R
import com.mawekk.sterdiary.TAG
import com.mawekk.sterdiary.databinding.FragmentPinCodeBinding
import com.mawekk.sterdiary.db.DiaryViewModel

class PinCodeFragment : Fragment() {
    private lateinit var binding: FragmentPinCodeBinding
    private lateinit var keys: List<TextView>
    private lateinit var dots: List<ImageView>
    private val viewModel: DiaryViewModel by activityViewModels()
    private var savedPin = ""
    private var pinCode = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPinCodeBinding.inflate(inflater, container, false)
        binding.apply {
            keys = listOf(key0, key1, key2, key3, key4, key5, key6, key7, key8, key9)
            dots = listOf(dot1, dot2, dot3, dot4)
        }

        setDeleteButton()

        viewModel.pinMode.observe(viewLifecycleOwner) {
            when (it) {
                PIN_CREATE -> {
                    binding.pinText.setText(R.string.create_pin)
                    setKeysOnClickListener(it)
                }

                else -> setKeysOnClickListener(it)

            }
        }

        return binding.root
    }

    private fun setKeysOnClickListener(mode: String) {
        keys.forEach { textView ->
            textView.setOnClickListener {
                if (pinCode.length < 4) {
                    pinCode += textView.text
                    dots[pinCode.length - 1].setColorFilter(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.green
                        ), android.graphics.PorterDuff.Mode.SRC_IN
                    )
                    if (pinCode.length == 4) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            checkPinCode(mode)
                        }, 200)
                    }
                }
            }
        }
    }

    private fun setDeleteButton() {
        binding.deleteLastButton.setOnClickListener {
            if (pinCode.isNotEmpty()) {
                dots[pinCode.length - 1].setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.gray
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
                pinCode = pinCode.dropLast(1)
            }
        }
    }

    private fun checkPinCode(mode: String) {
        val activity = activity as MainActivity
        val settings = activity.getSharedPreferences(TAG, Context.MODE_PRIVATE)

        if (settings != null) {
            val editor = settings.edit()
            when (mode) {
                PIN_CREATE -> {
                    if (savedPin.isEmpty()) {
                        savedPin = pinCode
                        binding.pinText.setText(R.string.repeat_pin)
                        clearPin()
                    } else {
                        if (savedPin == pinCode) {
                            editor.putString(PIN_CODE, savedPin)
                            editor.apply()
                            activity.onBackPressed()
                        } else {
                            showIncorrectPinText(binding.pinText.text)
                            clearPin()
                        }
                    }
                }

                PIN_CHANGE -> {
                    if (settings.getString(PIN_CODE, "") == pinCode) {
                        clearPin()
                        viewModel.changePinMode(PIN_CREATE)
                    } else {
                        showIncorrectPinText(binding.pinText.text)
                        clearPin()
                    }
                }

                PIN_ENTER -> {
                    if (settings.getString(PIN_CODE, "") == pinCode) {
                        activity.showBottomNavigation()
                        activity.binding.topAppBar.isVisible = true
                        activity.showFragment(ArchiveFragment.newInstance(), R.id.archive_item)
                    } else {
                        showIncorrectPinText(binding.pinText.text)
                        clearPin()
                    }
                }
            }
        }
    }

    private fun clearPin() {
        pinCode = ""
        dots.forEach {
            it.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.gray
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
        }
    }

    private fun showIncorrectPinText(text: CharSequence) {
        binding.pinText.apply {
            setText(R.string.incorrect_pin)
            Handler(Looper.getMainLooper()).postDelayed({
                setText(text)
            }, 1000)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = PinCodeFragment()
    }
}