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
import com.mosque.prayer.databinding.DialogHadithIntervalBinding

object HadithIntervalDialog {

    fun show(
        context: Context,
        currentMinutes: Int,
        onSelected: (Int) -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogHadithIntervalBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.setCancelable(true)

        dialog.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.45f)
        }

        val activity = context as? Activity
        val rootToBlur = activity?.window?.decorView?.findViewById<ViewGroup>(android.R.id.content)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && rootToBlur != null) {
            val blurEffect = RenderEffect.createBlurEffect(40f, 40f, Shader.TileMode.CLAMP)
            rootToBlur.setRenderEffect(blurEffect)
            dialog.setOnDismissListener {
                rootToBlur.setRenderEffect(null)
            }
        }

        binding.edtMinutes.setText(currentMinutes.toString())

        binding.btnOk.setOnClickListener {
            val v = binding.edtMinutes.text?.toString()?.trim()?.toIntOrNull()
            val chosen = when {
                v == null -> currentMinutes
                v < 1 -> 1
                v > 720 -> 720
                else -> v
            }
            onSelected(chosen)
            dialog.dismiss()
        }
        binding.btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
        binding.edtMinutes.requestFocus()
    }
}
