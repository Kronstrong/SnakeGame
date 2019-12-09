package com.eternal_search.snakegame

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Spinner
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.activity_menu.*
import kotlinx.android.synthetic.main.dialog_options.*

class OptionsDialog: DialogFragment() {
	private lateinit var preferences: SharedPreferences
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		preferences = PreferenceManager.getDefaultSharedPreferences(context!!)
	}
	
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? = inflater.inflate(R.layout.dialog_options, container, false)
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		bindSpinnerPreference(speed_spinner, "game_speed", R.array.speeds, 2) {
			(context as? MenuActivity)?.game_view?.setSpeed(it)
		}
		bindCheckBoxPreference(sound_check_box, "enable_sound", true)
		bindCheckBoxPreference(music_check_box, "enable_music", true) {
			(context as? MenuActivity)?.mediaPlayer?.let { player ->
				if (it) {
					player.start()
				} else {
					player.pause()
				}
			}
		}
		bindCheckBoxPreference(vibration_check_box, "enable_vibration", true)
	}
	
	private fun bindCheckBoxPreference(
		checkBox: CheckBox, key: String, defValue: Boolean,
		callback: ((value: Boolean) -> Unit)? = null
	) {
		checkBox.isChecked = preferences.getBoolean(key, defValue)
		checkBox.setOnCheckedChangeListener { _, isChecked ->
			preferences.edit {
				putBoolean(key, isChecked)
			}
			callback?.invoke(isChecked)
		}
	}
	
	private fun bindSpinnerPreference(
		spinner: Spinner, key: String, arrayRes: Int, defValueIndex: Int,
		callback: ((value: Int) -> Unit)? = null
	) {
		spinner.adapter = ArrayAdapter.createFromResource(
			spinner.context, arrayRes, R.layout.spinner_item
		)
		speed_spinner.setSelection(preferences.getInt(key, defValueIndex))
		speed_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
			override fun onItemSelected(
				parent: AdapterView<*>,
				view: View,
				position: Int,
				id: Long
			) {
				preferences.edit {
					putInt(key, position)
				}
				callback?.invoke(position)
			}
			
			override fun onNothingSelected(parent: AdapterView<*>) {
			}
		}
	}
	
	companion object {
		@JvmStatic
		fun newInstance() = OptionsDialog()
	}
}
