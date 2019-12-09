package com.eternal_search.snakegame

import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import kotlinx.android.synthetic.main.activity_menu.*
import java.io.InputStreamReader

class MenuActivity : AppCompatActivity(), GameView.GameCallback {
	lateinit var mediaPlayer: MediaPlayer
		private set
	private lateinit var preferences: SharedPreferences
	private lateinit var levels: Array<String>
	private var currentLevel = 0
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		preferences = PreferenceManager.getDefaultSharedPreferences(this)
		
		mediaPlayer = MediaPlayer.create(this, R.raw.music)
		mediaPlayer.isLooping = true
		
		levels = InputStreamReader(resources.openRawResource(R.raw.levels)).readLines().filter {
			it.isNotBlank()
		}.toTypedArray()
		
		requestWindowFeature(Window.FEATURE_NO_TITLE)
		window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
		
		setContentView(R.layout.activity_menu)
		game_view.callback = this
		game_view.playerCount = 0
		game_view.enableVibration = false
		game_view.enableSound = false
		game_view.setSpeed(preferences.getInt("game_speed", 2))
		currentLevel = savedInstanceState?.getInt("currentLevel") ?: 0
		game_view.reset(if (currentLevel > 0) levels[currentLevel - 1] else null)
		
		classic_mode_text_view.setOnClickListener {
			playStepSound()
			val intent = Intent(this, MainActivity::class.java)
			intent.putExtra("aiCount", 0)
			intent.putExtra("noWalls", currentLevel == 1)
			if (currentLevel > 1) {
				intent.putExtra("levelData", levels[currentLevel - 2])
			}
			startActivity(intent)
		}
		pvp_mode_text_view.setOnClickListener {
			playStepSound()
			val intent = Intent(this, MainActivity::class.java)
			intent.putExtra("playerCount", 2)
			intent.putExtra("aiCount", 0)
			intent.putExtra("noWalls", currentLevel == 1)
			if (currentLevel > 1) {
				intent.putExtra("levelData", levels[currentLevel - 2])
			}
			startActivity(intent)
		}
		ai_mode_text_view.setOnClickListener {
			playStepSound()
			val intent = Intent(this, MainActivity::class.java)
			intent.putExtra("noWalls", currentLevel == 1)
			if (currentLevel > 1) {
				intent.putExtra("levelData", levels[currentLevel - 2])
			}
			startActivity(intent)
		}
		next_map_text_view.setOnClickListener {
			playStepSound()
			currentLevel = (currentLevel + 1) % (levels.size + 2)
			resetGameView()
		}
		prev_map_text_view.setOnClickListener {
			playStepSound()
			currentLevel = if (currentLevel > 0) currentLevel - 1 else levels.size + 1
			resetGameView()
		}
		options_text_view.setOnClickListener {
			playStepSound()
			OptionsDialog.newInstance().show(supportFragmentManager, null)
		}
		
		/* game_view.setOnClickListener {
			for (child in root.children) {
				if (child !is TextView) continue
				child.visibility =
					if (child.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
			}
			resetGameView()
		} */
	}
	
	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putInt("currentLevel", currentLevel)
	}

	override fun onPause() {
		super.onPause()
		game_view.pause()
		mediaPlayer.pause()
	}

	override fun onResume() {
		super.onResume()
		game_view.resume()
		if (preferences.getBoolean("enable_music", true)) {
			mediaPlayer.start()
		}
	}

	override fun onGameStarted() {
	}
	
	override fun onGameFailed(score: Int) {
		game_view.postDelayed({
			game_view.reset()
		}, 1000)
	}

	override fun onScoreUpdate(score: Int) {
	}
	
	private fun resetGameView() {
		game_view.noWalls = currentLevel == 1
		game_view.reset(if (currentLevel > 1) levels[currentLevel - 2] else "")
	}
	
	private fun playStepSound() {
		if (preferences.getBoolean("enable_sound", true)) {
			game_view.postDelayed({
				game_view.soundPool.play(game_view.stepSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
			}, 50)
		}
	}
}
