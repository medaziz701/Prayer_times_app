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
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.TextView
import com.mosque.prayer.R
import com.mosque.prayer.data.CitiesRepository
import com.mosque.prayer.data.City
import com.mosque.prayer.databinding.DialogCityPickerBinding

object CityPickerDialog {

    fun show(
        context: Context,
        selectedCityId: String,
        onSelected: (City) -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogCityPickerBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.setCancelable(true)
        dialog.window?.apply {
            // Fullscreen transparent window; the white card inside controls its own width/margins
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

        // Ensure repository loads from res/raw/cities.txt (may be large)
        CitiesRepository.ensureLoaded(context)
        var list = CitiesRepository.all()

        fun updateAdapter(items: List<City>) {
            val names = items.map { it.nameAr }
            val adapter = object : ArrayAdapter<String>(context, R.layout.item_city, R.id.txtCity, names) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    val tv = view.findViewById<TextView>(R.id.txtCity)
                    val cityItem = items.getOrNull(position)
                    tv.text = cityItem?.nameAr ?: ""
                    if (cityItem?.id == selectedCityId) {
                        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_city_selected, 0)
                    } else {
                        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
                    }
                    return view
                }
            }
            binding.listCities.adapter = adapter
            binding.listCities.setOnItemClickListener { _, _, position, _ ->
                onSelected(items[position])
                dialog.dismiss()
            }
        }

        updateAdapter(list)

        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString() ?: ""
                list = CitiesRepository.search(q)
                updateAdapter(list)
            }
        })

        binding.btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
}
