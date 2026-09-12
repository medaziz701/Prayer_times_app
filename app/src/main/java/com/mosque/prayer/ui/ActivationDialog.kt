package com.mosque.prayer.ui

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import com.mosque.prayer.R
import com.mosque.prayer.databinding.DialogActivationBinding

object ActivationDialog {

    fun show(
        context: Context,
        onActivated: (String) -> Unit,
        onExit: () -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogActivationBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.setCancelable(false)

        dialog.window?.apply {
            // Fullscreen transparent window; card inside controls its own size
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.45f)
        }

        // Apply real blur to the underlying activity content on Android 12+ (API 31)
        val activity = context as? Activity
        val rootToBlur = activity?.window?.decorView?.findViewById<ViewGroup>(android.R.id.content)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && rootToBlur != null) {
            val blurEffect = RenderEffect.createBlurEffect(40f, 40f, Shader.TileMode.CLAMP)
            rootToBlur.setRenderEffect(blurEffect)

            dialog.setOnDismissListener {
                rootToBlur.setRenderEffect(null)
            }
        }

        fun code(): String = "${binding.code1.text}${binding.code2.text}${binding.code3.text}${binding.code4.text}".trim()

        val edits = listOf(binding.code1, binding.code2, binding.code3, binding.code4)
        edits.forEachIndexed { idx, e ->
            e.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && idx < edits.size - 1) {
                        edits[idx + 1].requestFocus()
                    }
                }
            })
        }

        binding.btnActivate.setOnClickListener {
            val c = code()
            // N'activer l'application que si le code est bien un code valide
            // présent dans le fichier texte des codes secrets.
            if (isValidActivationCode(context, c)) {
                onActivated(c)
                dialog.dismiss()
            } else {
                // Code invalide : effacer la saisie pour réessayer
                binding.code1.text?.clear()
                binding.code2.text?.clear()
                binding.code3.text?.clear()
                binding.code4.text?.clear()
                binding.code1.requestFocus()
            }
        }
        binding.btnExit.setOnClickListener {
            dialog.dismiss()
            onExit()
        }

        dialog.show()
        binding.code1.requestFocus()
    }

    /**
     * Vérifie si le code saisi correspond à l'un des codes de 4 chiffres
     * présents dans le fichier texte des numéros secrets dans res/raw.
     */
    private fun isValidActivationCode(context: Context, code: String): Boolean {
        val trimmed = code.trim()
        if (trimmed.length != 4) return false

        val res = context.resources
        val rawId = R.raw.activation_codes

        var found = false
        res.openRawResource(rawId).bufferedReader().useLines { lines ->
            found = lines.any { line -> line.trim() == trimmed }
        }
        return found
    }
}
