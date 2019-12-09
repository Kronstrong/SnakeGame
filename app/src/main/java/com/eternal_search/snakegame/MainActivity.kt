package com.eternal_search.snakegame

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.KeyEvent
import android.view.Window
import android.view.WindowManager
import kotlinx.android.synthetic.main.activity_main.*
import java.security.MessageDigest

class MainActivity : AppCompatActivity(), GameView.GameCallback {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		val preferences = PreferenceManager.getDefaultSharedPreferences(this)
		
		requestWindowFeature(Window.FEATURE_NO_TITLE)
		window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

		setContentView(R.layout.activity_main)
		game_view.callback = this
		game_view.enableVibration = preferences.getBoolean("enable_vibration", game_view.enableVibration)
		game_view.enableSound = preferences.getBoolean("enable_sound", game_view.enableSound)
		game_view.setSpeed(preferences.getInt("game_speed", 2))
		game_view.noWalls = intent.getBooleanExtra("noWalls", false)
		
		val playerCount = intent.getIntExtra("playerCount", game_view.playerCount)
		val aiCount = intent.getIntExtra("aiCount", game_view.aiCount)
		val levelData = intent.getStringExtra("levelData")
		
		if (playerCount == 1) {
			game_view.maxScorePreferenceKey =
				"max_score:" + MessageDigest.getInstance("MD5").digest((levelData ?: "null").toByteArray()).joinToString("") {
					String.format("%02x", it)
				} + ":" + aiCount + ":" + game_view.noWalls
		}
		game_view.playerCount = playerCount
		game_view.aiCount = aiCount
		
		intent.getStringExtra("levelData")?.let {
			game_view.reset(it)
		}
		game_view.reset()
	}
	
	override fun onPause() {
		super.onPause()
		game_view.pause()
	}

	override fun onResume() {
		super.onResume()
		game_view.resume()
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
		if (game_view.isEnabled) {
			when (keyCode) {
				KeyEvent.KEYCODE_DPAD_UP -> game_view.go(0, -1)
				KeyEvent.KEYCODE_DPAD_DOWN -> game_view.go(0, 1)
				KeyEvent.KEYCODE_DPAD_LEFT -> game_view.go(-1, 0)
				KeyEvent.KEYCODE_DPAD_RIGHT -> game_view.go(1, 0)
			}
		}
		return super.onKeyDown(keyCode, event)
	}

	override fun onGameStarted() {
	}
	
	override fun onGameFailed(score: Int) {
		game_view.isEnabled = false
		game_view.postDelayed({
			game_view.isEnabled = true
		}, 1000)
	}

	override fun onScoreUpdate(score: Int) {
	}
}
