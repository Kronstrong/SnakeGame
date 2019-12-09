package com.eternal_search.snakegame

import android.graphics.Canvas
import androidx.core.content.edit

class GameJoystick(
	private val gameView: GameView,
	private val x: Int,
	private val y: Int,
	internal val snake: PlayerSnake,
	private val mirrorY: Boolean
) {
	private val buttons = mutableListOf<ControlButton>()
	internal var failed: Boolean = false
	private var maxScore: Int = 0
	var maxScorePreferenceKey: String? = null
		set(value) {
			field = value
			if (value != null) {
				maxScore = gameView.preferences.getInt(value, 0)
			}
		}
	internal var pointerId: Int? = null
	
	init {
		buttons.addAll(listOf(
			ControlButton(
				x,
				y - 3,
				3.0f,
				{ canvas, button ->
					val paint = if (button.pressed) gameView.pressedButtonPaint else gameView.fieldBorderPaint
					gameView.drawCell(canvas, button.x, button.y, 0.45f, paint)
					gameView.drawCell(canvas, button.x - 1, button.y, 0.45f, paint)
					gameView.drawCell(canvas, button.x + 1, button.y, 0.45f, paint)
					gameView.drawCell(canvas, button.x, button.y - 1, 0.45f, paint)
				},
				{
					go(0, -1)
				}
			),
			ControlButton(
				x,
				y + 3,
				3.0f,
				{ canvas, button ->
					val paint = if (button.pressed) gameView.pressedButtonPaint else gameView.fieldBorderPaint
					gameView.drawCell(canvas, button.x, button.y, 0.45f, paint)
					gameView.drawCell(canvas, button.x - 1, button.y, 0.45f, paint)
					gameView.drawCell(canvas, button.x + 1, button.y, 0.45f, paint)
					gameView.drawCell(canvas, button.x, button.y + 1, 0.45f, paint)
				},
				{
					go(0, 1)
				}
			),
			ControlButton(
				x - 3,
				y,
				2.5f,
				{ canvas, button ->
					val paint = if (button.pressed) gameView.pressedButtonPaint else gameView.fieldBorderPaint
					gameView.drawCell(canvas, button.x, button.y, 0.45f, paint)
					gameView.drawCell(canvas, button.x, button.y - 1, 0.45f, paint)
					gameView.drawCell(canvas, button.x, button.y + 1, 0.45f, paint)
					gameView.drawCell(canvas, button.x - 1, button.y, 0.45f, paint)
				},
				{
					go(-1, 0)
				}
			),
			ControlButton(
				x + 3,
				y,
				2.5f,
				{ canvas, button ->
					val paint = if (button.pressed) gameView.pressedButtonPaint else gameView.fieldBorderPaint
					gameView.drawCell(canvas, button.x, button.y, 0.45f, paint)
					gameView.drawCell(canvas, button.x, button.y - 1, 0.45f, paint)
					gameView.drawCell(canvas, button.x, button.y + 1, 0.45f, paint)
					gameView.drawCell(canvas, button.x + 1, button.y, 0.45f, paint)
				},
				{
					go(1, 0)
				}
			)
		))
	}
	
	fun draw(canvas: Canvas) {
		buttons.forEach {
			it.onDraw(canvas, it)
		}
		
		if (failed) {
			canvas.drawRect(
				gameView.cellX1(x - 5, y - 5),
				gameView.cellY1(x - 5, y - 5),
				gameView.cellX2(x + 5, y + 5),
				gameView.cellY2(x + 5, y + 5),
				gameView.gameOverBackgroundPaint
			)
		}
		
		canvas.save()
		
		canvas.translate(gameView.width / 2.0f, gameView.cellY1(x, y, 0.0f))
		if (mirrorY) {
			canvas.rotate(180.0f)
		}
		
		if (failed) {
			canvas.drawText(
				gameView.gameOverLabel,
				(gameView.width - gameView.gameOverLabelTextPaint.measureText(gameView.gameOverLabel)) / 2 - gameView.cellX1(x, y, 0.0f),
				gameView.cellY1(0, y, 1.0f) + gameView.cellSize * 2 - gameView.cellY1(x, y, 0.0f),
				gameView.gameOverLabelTextPaint
			)
		}
		
		val scoreText = String.format("%05d", snake.score)
		val x1 = gameView.cellX1(0, 0, 1.0f) - gameView.cellX1(x, y, 1.0f)
		val y1 = -5 * gameView.cellSize - gameView.cellSize / 2 +
				gameView.scoreTextPaint.fontMetrics.descent - gameView.scoreTextPaint.fontMetrics.ascent
		canvas.drawText(scoreText, x1, y1, gameView.scoreTextPaint)
		val y2 = y1 + gameView.scoreLabelTextPaint.fontMetrics.descent - gameView.scoreLabelTextPaint.fontMetrics.ascent
		canvas.drawText(gameView.scoreLabel, x1, y2, gameView.scoreLabelTextPaint)
		val maxScoreText = String.format("%05d", maxScore)
		val x2 = -x1 - gameView.scoreTextPaint.measureText(maxScoreText)
		canvas.drawText(maxScoreText, x2, y1, gameView.scoreTextPaint)
		val x3 = -x1 - gameView.scoreLabelTextPaint.measureText(gameView.maxScoreLabel)
		canvas.drawText(gameView.maxScoreLabel, x3, y2, gameView.scoreLabelTextPaint)
		
		canvas.restore()
	}
	
	fun touchStart(x: Int, y: Int): Boolean {
		return buttons.filter {
			/* val dx = x - it.x
		val dy = y - it.y
		val d2 = dx * dx + dy * dy
		d2 <= it.r * it.r */
			true
		}.minBy {
			val dx = x - it.x
			val dy = y - it.y
			dx * dx + dy * dy
		}?.let {
			it.pressed = true
			it.onClick(it)
			gameView.postDelayed(it.repeatCallback, 500)
			true
		} == true
	}
	
	fun touchEnd() {
		buttons.forEach {
			it.pressed = false
			gameView.removeCallbacks(it.repeatCallback)
		}
	}
	
	fun updateMaxScore() {
		if (snake.score > maxScore) {
			maxScore = snake.score
			maxScorePreferenceKey?.let { key ->
				gameView.preferences.edit {
					putInt(key, maxScore)
				}
			}
		}
	}
	
	private fun go(dx: Int, dy: Int) {
		if (failed) {
			if (gameView.joysticks.all { it.failed }) {
				gameView.reset()
			}
		} else {
			snake.go(dx, dy)
			gameView.mayBeSpeedUp()
		}
	}
	
	private inner class ControlButton(
		val x: Int,
		val y: Int,
		val r: Float,
		val onDraw: (canvas: Canvas, button: ControlButton) -> Unit,
		val onClick: (button: ControlButton) -> Unit
	) {
		var pressed: Boolean = false
		val repeatCallback = object : Runnable {
			override fun run() {
				if (failed) return
				onClick(this@ControlButton)
				gameView.postDelayed(this, gameView.stepDelay / 2)
			}
		}
	}
}
