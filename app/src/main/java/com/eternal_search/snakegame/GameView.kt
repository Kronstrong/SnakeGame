package com.eternal_search.snakegame

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random
import com.eternal_search.snakesim.AiSnake
import com.eternal_search.snakesim.Point
import com.eternal_search.snakesim.Snake
import com.eternal_search.snakesim.SnakeMap

class GameView(
	context: Context, attrs: AttributeSet?, defStyleAttr: Int): View(context, attrs, defStyleAttr
), SnakeMap {
	internal val preferences = PreferenceManager.getDefaultSharedPreferences(context)
	override val foods = mutableListOf<SnakeMap.Food>()
	override val snakes = mutableListOf<Snake>()
	private val foodPaint = Paint()
	private val snakePaint = Paint()
	private val snakeHeadPaint = Paint()
	private val snakeBorderPaint = Paint()
	private val aiSnakePaint = Paint()
	private val aiSnakeHeadPaint = Paint()
	private val teleportPaint = Paint()
	internal val fieldBorderPaint = Paint()
	internal val pressedButtonPaint = Paint()
	internal val scoreTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
	internal val scoreLabelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
	internal val gameOverLabelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
	internal val gameOverBackgroundPaint = Paint()
	internal val scoreLabel = context.getString(R.string.score)
	internal val maxScoreLabel = context.getString(R.string.max)
	internal val gameOverLabel = context.getString(R.string.game_over)
	private val fixedCellSize: Float = resources.getDimension(R.dimen.snakeChunkSize)
	var callback: GameCallback? = null
	@Volatile var stepDelay: Long = 250
		private set
	/* private val stepRunnable = object : Runnable {
		override fun run() {
			if (joysticks.isEmpty() || !joysticks.all { it.failed }) {
				val playerSnakes = snakes.mapNotNull { it as? PlayerSnake }
				if (playerSnakes.isEmpty() || playerSnakes.any { it.isMoving }) {
					snakes.toList().forEach {
						it.makeStep()
					}
					if (snakes.isNotEmpty()) {
						if (Random.nextInt(256) == 0) {
							regenerateFood()
						}
						invalidate()
					}
				}
			}
			postDelayed(this, stepDelay)
		}
	} */
	var playerCount: Int = 1
	var aiCount: Int = Int.MAX_VALUE
	override val walls = mutableListOf<Point>()
	private var levelSizeX: Int = 0
	private var levelSizeY: Int = 0
	override val cellCountX: Int get() =
		if (levelSizeX < 1) (width / cellSize).toInt() else levelSizeX
	override val cellCountY: Int get() =
		(if (levelSizeY < 1) ((height / cellSize).toInt() - marginTop - marginBottom) else levelSizeY)
	private val offsetX
		get() = (width - cellCountX * cellSize) / 2.0f + cellSize * 0.5f
	private val offsetY
		get() = (height - (cellCountY + marginTop + marginBottom) * cellSize) / 2 + cellSize * 0.5f
	internal val cellSize: Float get() =
		if (levelSizeX == 0 || levelSizeY == 0)
			fixedCellSize
		else {
			min(width.toFloat() / cellCountX,
				height.toFloat() / (cellCountY + marginTop + marginBottom))
		}
	private var levelData: String = ""
	var maxScorePreferenceKey: String? = null
	internal val joysticks = mutableListOf<GameJoystick>()
	private val marginTop: Int get() = if (playerCount >= 2) 10 else 0
	private val marginBottom: Int get() = if (playerCount >= 1) 10 else 0
	private val noFoodCells = mutableListOf<Point>()
	var enableVibration: Boolean = true
	override val allowIdle: Boolean get() = stepDelay <= 16L
	private var thread: GameThread? = null
	private val resetMutex = Object()
	val soundPool = SoundPool(5, AudioManager.STREAM_MUSIC, 0)
	val stepSoundId = soundPool.load(context, R.raw.step, 1)
	private val pickupSoundId = soundPool.load(context, R.raw.pickup, 2)
	private val deathSoundId = soundPool.load(context, R.raw.death, 3)
	var enableSound: Boolean = true
	override var noWalls: Boolean = false
	
	constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)
	
	constructor(context: Context): this(context, null)
	
	init {
		foodPaint.color = resources.getColor(R.color.foodColor)
		foodPaint.style = Paint.Style.FILL
		snakePaint.color = resources.getColor(R.color.snakeColor)
		snakePaint.style = Paint.Style.FILL
		snakeHeadPaint.color = resources.getColor(R.color.snakeHeadColor)
		snakeHeadPaint.style = Paint.Style.FILL
		aiSnakePaint.color = resources.getColor(R.color.aiSnakeColor)
		aiSnakePaint.style = Paint.Style.FILL
		aiSnakeHeadPaint.color = resources.getColor(R.color.aiSnakeHeadColor)
		aiSnakeHeadPaint.style = Paint.Style.FILL
		snakeBorderPaint.color = (background as ColorDrawable).color
		snakeBorderPaint.style = Paint.Style.STROKE
		snakeBorderPaint.strokeWidth = cellSize / 10.0f
		fieldBorderPaint.color = resources.getColor(R.color.fieldBorderColor)
		fieldBorderPaint.style = Paint.Style.FILL
		teleportPaint.color = resources.getColor(R.color.teleportBorderColor)
		teleportPaint.style = Paint.Style.FILL
		pressedButtonPaint.color = resources.getColor(R.color.pressedButtonColor)
		pressedButtonPaint.style = Paint.Style.FILL
		scoreTextPaint.color = resources.getColor(R.color.scoreTextColor)
		scoreTextPaint.textSize = resources.getDimension(R.dimen.scoreTextSize)
		scoreLabelTextPaint.color = resources.getColor(R.color.scoreTextColor)
		scoreLabelTextPaint.textSize = resources.getDimension(R.dimen.scoreLabelTextSize)
		gameOverLabelTextPaint.color = resources.getColor(R.color.scoreTextColor)
		gameOverLabelTextPaint.textSize = resources.getDimension(R.dimen.gameOverTextSize)
		gameOverBackgroundPaint.color = resources.getColor(R.color.gameOverBackgroundColor)
		gameOverBackgroundPaint.style = Paint.Style.FILL

		reset()
	}
	
	@Synchronized
	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		
		if (snakes.isEmpty()) return
		
		joysticks.forEach {
			it.draw(canvas)
		}
		
		walls.forEach {
			drawCell(canvas, it.x, it.y, 0.45f, fieldBorderPaint)
		}
		
		foods.forEach {
			drawCell(canvas, it.x, it.y, 0.45f, foodPaint)
		}

		snakes.asReversed().forEach { snake ->
			snake.chunks.forEachIndexed { index, it ->
				drawCell(
					canvas,
					it.x, it.y,
					if (it.fat) 0.6f else 0.45f,
					if (playerCount == 0 || snake === joysticks.firstOrNull()?.snake)
						if (index > 0) snakePaint else snakeHeadPaint
					else
						if (index > 0) aiSnakePaint else aiSnakeHeadPaint
				)
			}
			snake.chunks.forEach {
				if (!it.fat) return@forEach
				drawCell(canvas, it.x, it.y, 0.6f, snakeBorderPaint)
			}
		}

		val borderPaint = if (noWalls) teleportPaint else fieldBorderPaint
		for (x in 0 until cellCountX) {
			drawCell(canvas, x, 0, 0.45f, borderPaint)
			drawCell(canvas, x, cellCountY - 1, 0.45f, borderPaint)
		}
		for (y in 0 until cellCountY) {
			drawCell(canvas, 0, y, 0.45f, borderPaint)
			drawCell(canvas, cellCountX - 1, y, 0.45f, borderPaint)
		}
	}
	
	internal fun cellX1(x: Int, y: Int, r: Float = 1.0f) = offsetX + x * cellSize - r * cellSize
	
	internal fun cellY1(x: Int, y: Int, r: Float = 1.0f) = offsetY + (y + marginTop) * cellSize - r * cellSize
	
	internal fun cellX2(x: Int, y: Int, r: Float = 1.0f) = offsetX + x * cellSize + r * cellSize
	
	internal fun cellY2(x: Int, y: Int, r: Float = 1.0f) = offsetY + (y + marginTop) * cellSize + r * cellSize
	
	internal fun drawCell(canvas: Canvas, x: Int, y: Int, r: Float, paint: Paint) {
		canvas.drawRect(cellX1(x, y, r), cellY1(x, y, r), cellX2(x, y, r), cellY2(x, y, r), paint)
	}
	
	fun reset(levelData: String? = null) {
		if (width == 0 || height == 0) {
			postDelayed({ reset(levelData) }, 100)
			return
		}
		
		synchronized(resetMutex) {
			if (levelData != null) {
				this.levelData = levelData
			}
			
			val elements = (if (this.levelData.isNotEmpty()) this.levelData else null)
				?.split(';')?.map { it.split(',') }
			val objects = elements?.drop(1)
			
			levelSizeX = elements?.get(0)?.get(0)?.toInt()?.plus(2) ?: 0
			levelSizeY = elements?.get(0)?.get(1)?.toInt()?.plus(2) ?: 0
			
			walls.clear()
			walls.addAll(objects?.filter {
				it[2] == "wall"
			}?.map {
				Point(it[0].toInt() + 1, it[1].toInt() + 1)
			} ?: emptyList())
			
			noFoodCells.clear()
			noFoodCells.addAll(objects?.filter {
				it[2] == "no-food"
			}?.map {
				Point(it[0].toInt() + 1, it[1].toInt() + 1)
			} ?: emptyList())
			
			snakes.clear()
			joysticks.clear()
			if (playerCount >= 1) {
				val info = objects?.firstOrNull { it[2] == "player-spawn" }
				val p = info?.let {
					Point(
						it[0].toInt(),
						it[1].toInt()
					)
				} ?: Point(
					4,
					1
				)
				val snake = PlayerSnake(
					this, p.x + 1, p.y + 1, 4,
					info?.get(3)?.toInt()?.let { it == 0 || it == 2 } ?: true,
					info?.get(3)?.toInt()?.let { it == 0 || it == 1 } ?: true
				)
				val joystick = GameJoystick(
					this@GameView,
					cellCountX / 2,
					((cellCountY + 5) + ((height - offsetY) / cellSize).toInt() - 4 - marginTop) / 2,
					snake,
					false
				)
				addPlayerSnake(snake, joystick)
				
				if (playerCount >= 2) {
					val info2 = objects?.firstOrNull { it[2] == "ai-spawn" }
					val p2 = info2?.let {
						Point(
							it[0].toInt(),
							it[1].toInt()
						)
					} ?: Point(4, cellCountY - 4)
					val snake2 = PlayerSnake(
						this, p2.x + 1, p2.y + 1, 4,
						info2?.get(3)?.toInt()?.let { it == 0 || it == 2 } ?: true,
						info2?.get(3)?.toInt()?.let { it == 0 || it == 1 } ?: true
					)
					val joystick2 = GameJoystick(
						this@GameView,
						cellCountX / 2,
						(-6 - (offsetY / cellSize).toInt() - 6) / 2,
						snake2,
						true
					)
					addPlayerSnake(snake2, joystick2)
				}
			}
			(objects?.filter { it[2] == "ai-spawn" } ?: listOf(
				listOf(
					4.toString(),
					(cellCountY - 4).toString(),
					"ai-spawn",
					0.toString()
				)
			))
				.drop((playerCount - 1).coerceAtLeast(0))
				.take(aiCount)
				.forEach { info ->
					val p = Point(info[0].toInt(), info[1].toInt())
					
					snakes.add(AiSnake(
						this, p.x + 1, p.y + 1, 4,
						info[3].toInt().let { it == 0 || it == 2 },
						info[3].toInt().let { it == 0 || it == 1 }
					).apply {
						dieCallback = {
							snakes.remove(this)
							foods.addAll(
								this.chunks
									.filterIndexed { index, _ -> index % 2 == 1 }
									.map { SnakeMap.Food(it.x, it.y, false) }
							)
							if (snakes.isEmpty()) {
								callback?.onGameFailed(score)
							} else {
								invalidate()
							}
						}
					})
				}
			foods.clear()
			snakes.forEach { _ ->
				generateFood()
			}
			
			invalidate()
			
			callback?.onGameStarted()
			callback?.onScoreUpdate(0)
		}
	}
	
	private fun addPlayerSnake(snake: PlayerSnake, joystick: GameJoystick) {
		snakes.add(snake)
		joysticks.add(joystick)
		joystick.maxScorePreferenceKey = maxScorePreferenceKey
		snake.eatCallback = {
			val score = snake.score
			post {
				if (enableVibration) {
					performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
				}
				if (enableSound) {
					soundPool.play(pickupSoundId, 1.0f, 1.0f, 2, 0, 1.0f)
				}
				callback?.onScoreUpdate(score)
			}
		}
		snake.dieCallback = {
			joysticks.forEach {
				it.updateMaxScore()
			}
			joystick.failed = true
			if (snakes.count { it is PlayerSnake } > 1) {
				snakes.remove(snake)
				snake.chunks.forEachIndexed { index, chunk ->
					if (index % 2 == 1) {
						foods.add(SnakeMap.Food(chunk.x, chunk.y, false))
					}
				}
			} else {
				val score = snake.score
				post {
					callback?.onGameFailed(score)
				}
			}
			post {
				if (enableVibration) {
					(context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).let {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
							it.vibrate(
								VibrationEffect.createOneShot(500, 255)
							)
						} else {
							it.vibrate(500)
						}
					}
				}
				if (enableSound) {
					soundPool.play(deathSoundId, 1.0f, 1.0f, 3, 0, 1.0f)
				}
				invalidate()
			}
		}
	}
	
	override fun generateFood() = generateFood(true)
	
	private fun generateFood(markAsGenerated: Boolean) {
		var attemptIndex = 0
		while (attemptIndex < 65536) {
			val p = Point(
				Random.nextInt(1, cellCountX - 1),
				Random.nextInt(1, cellCountY - 1)
			)
			val blacklist = snakes.flatMap { it.chunks.map {
				Point(
					it.x,
					it.y
				)
			} } +
					foods.map { Point(it.x, it.y) } + walls + noFoodCells
			if (blacklist.firstOrNull {
				p.x == it.x && p.y == it.y
			} == null) {
				if (attemptIndex > 128) {
					foods.add(SnakeMap.Food(p.x, p.y, true))
					break
				} else {
					if (blacklist.firstOrNull {
						abs(p.x - it.x) <= 1 && abs(p.y - it.y) <= 1
					} == null) {
						foods.add(SnakeMap.Food(p.x, p.y, markAsGenerated))
						break
					}
				}
			}
			attemptIndex++
		}
		//invalidate()
	}
	
	override fun eatFood(x: Int, y: Int): SnakeMap.Food? {
		val foodIndex = foods.indexOfFirst {
			it.x == x && it.y == y
		}
		if (foodIndex >= 0) {
			return foods.removeAt(foodIndex)
		}
		return null
	}
	
	private fun regenerateFood() {
		generateFood(if (foods.isNotEmpty()) {
			foods.removeAt(Random.nextInt(foods.size)).autoGenerated
		} else true)
	}
	
	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(event: MotionEvent): Boolean {
		if (isEnabled) {
			when (event.actionMasked) {
				MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
					val x = ((event.getX(event.actionIndex) - offsetX) / cellSize).toInt()
					val y = ((event.getY(event.actionIndex) - offsetY) / cellSize).toInt() - marginTop
					//Log.i(this::class.java.simpleName, "onTouchEvent(x=$x,y=$y,index=${event.actionIndex})")
					if (y >= cellCountY && joysticks.size > 0 && joysticks[0].pointerId == null) {
						if (joysticks[0].touchStart(x, y)) {
							joysticks[0].pointerId = event.getPointerId(event.actionIndex)
							invalidate()
							if (enableVibration) {
								performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
							}
							return true
						}
					} else if (y < 0 && joysticks.size > 1 && joysticks[1].pointerId == null) {
						if (joysticks[1].touchStart(x, y)) {
							joysticks[1].pointerId = event.getPointerId(event.actionIndex)
							invalidate()
							if (enableVibration) {
								performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
							}
							return true
						}
					}
				}
				MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP -> {
					joysticks.forEach {
						if (it.pointerId == event.getPointerId(event.actionIndex)) {
							it.pointerId = null
							it.touchEnd()
						}
					}
					invalidate()
					if (event.actionMasked == MotionEvent.ACTION_UP) {
						performClick()
					}
					return true
				}
			}
		}
		return super.onTouchEvent(event)
	}
	
	fun mayBeSpeedUp() {
		if (playerCount == 1) {
			val startTime = thread?.startTime ?: 0L
			postDelayed({
				if (startTime == thread?.startTime ?: 0L) {
					thread?.interrupt()
				}
			}, 50)
		}
	}
	
	fun go(dx: Int, dy: Int) {
		(snakes.firstOrNull { it is PlayerSnake } as? PlayerSnake)?.go(dx, dy)
		mayBeSpeedUp()
	}
	
	fun resume() {
		//postDelayed(stepRunnable, stepDelay)
		if (thread != null) {
			pause()
		}
		thread = GameThread()
		thread?.start()
	}
	
	fun pause() {
		//removeCallbacks(stepRunnable)
		thread?.running = false
		thread?.interrupt()
		//thread?.join()
		thread = null
	}
	
	fun setSpeed(speed: Int) {
		stepDelay = when (speed) {
			0 -> 1000
			1 -> 500
			2 -> 250
			3 -> 125
			4 -> 62
			5 -> 16
			else -> 0
		}
	}
	
	override fun setEnabled(enabled: Boolean) {
		super.setEnabled(enabled)
		if (!enabled) {
			joysticks.forEach {
				it.touchEnd()
				it.pointerId = null
			}
		}
	}
	
	private inner class GameThread: Thread() {
		@Volatile var running: Boolean = true
		@Volatile var startTime = 0L
		
		override fun run() {
			while (running) {
				startTime = System.currentTimeMillis()
				synchronized(resetMutex) {
					if (joysticks.isEmpty() || !joysticks.all { it.failed }) {
						val playerSnakes = snakes.mapNotNull { it as? PlayerSnake }
						if (playerSnakes.isEmpty() || playerSnakes.any { it.isMoving }) {
							snakes.toList().forEach {
								it.makeStep()
							}
							if (snakes.isNotEmpty()) {
								if (Random.nextInt(256) == 0) {
									regenerateFood()
								}
								post {
									if (!isDirty) {
										if (enableSound) {
											soundPool.play(stepSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
										}
										invalidate()
									}
								}
							}
						}
					}
				}
				val endTime = System.currentTimeMillis()
				val timeDelta = endTime - startTime
				try {
					sleep((stepDelay - timeDelta).coerceAtLeast(0))
				} catch (e: InterruptedException) {
				}
			}
		}
	}
	
	interface GameCallback {
		fun onGameStarted()
		fun onGameFailed(score: Int)
		fun onScoreUpdate(score: Int)
	}
}
