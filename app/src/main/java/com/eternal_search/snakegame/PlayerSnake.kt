package com.eternal_search.snakegame

import com.eternal_search.snakesim.Point
import com.eternal_search.snakesim.Snake
import com.eternal_search.snakesim.SnakeMap

class PlayerSnake(
	gameView: SnakeMap,
	x: Int, y: Int, length: Int = 4,
	horizontal: Boolean = true, reversed: Boolean = true
): Snake(gameView, x, y, length, horizontal, reversed) {
	private var dx: Int = 0
	private var dy: Int = 0
	val isMoving: Boolean get() = dx != 0 || dy != 0
	
	override fun step(): Point {
		return Point(dx, dy)
	}
	
	fun go(dx: Int, dy: Int) {
		this.dx = dx
		this.dy = dy
	}
}
