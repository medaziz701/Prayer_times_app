package com.mosque.prayer.ui

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import com.mosque.prayer.data.AppPreferences
import com.mosque.prayer.databinding.DialogDelayTimesBinding

object DelayTimesDialog {

    fun show(context: Context, onApplied: () -> Unit) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogDelayTimesBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.setCancelable(true)
        dialog.window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.45f)
        }

        val activity = context as? Activity
        val rootToBlur = activity?.window?.decorView?.findViewById<ViewGroup>(android.R.id.content)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && rootToBlur != null) {
            val blurEffect = RenderEffect.createBlurEffect(40f, 40f, Shader.TileMode.CLAMP)
            rootToBlur.setRenderEffect(blurEffect)
            dialog.setOnDismissListener { rootToBlur.setRenderEffect(null) }
        }

        val prefs = AppPreferences(context)
        fun intToText(v: Int) = v.toString()
        binding.edtFajrOffset.setText(intToText(prefs.getAdhanOffsetMinutes("fajr")))
        binding.edtSunriseOffset.setText(intToText(prefs.getAdhanOffsetMinutes("sunrise")))
        binding.edtDhuhrOffset.setText(intToText(prefs.getAdhanOffsetMinutes("dhuhr")))
        binding.edtAsrOffset.setText(intToText(prefs.getAdhanOffsetMinutes("asr")))
        binding.edtMaghribOffset.setText(intToText(prefs.getAdhanOffsetMinutes("maghrib")))
        binding.edtIshaOffset.setText(intToText(prefs.getAdhanOffsetMinutes("isha")))

        fun parse(text: String?): Int {
            return text?.trim()?.toIntOrNull() ?: 0
        }

        binding.btnApply.setOnClickListener {
            prefs.setAdhanOffsetMinutes("fajr", parse(binding.edtFajrOffset.text?.toString()))
            prefs.setAdhanOffsetMinutes("sunrise", parse(binding.edtSunriseOffset.text?.toString()))
            prefs.setAdhanOffsetMinutes("dhuhr", parse(binding.edtDhuhrOffset.text?.toString()))
            prefs.setAdhanOffsetMinutes("asr", parse(binding.edtAsrOffset.text?.toString()))
            prefs.setAdhanOffsetMinutes("maghrib", parse(binding.edtMaghribOffset.text?.toString()))
            prefs.setAdhanOffsetMinutes("isha", parse(binding.edtIshaOffset.text?.toString()))
            onApplied()
            dialog.dismiss()
        }

        binding.btnCancel.setOnClickListener { dialog.dismiss() }
        binding.btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
}
