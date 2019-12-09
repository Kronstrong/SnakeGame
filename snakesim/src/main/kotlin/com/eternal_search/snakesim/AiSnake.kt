package com.eternal_search.snakesim

import java.util.*

class AiSnake(
	gameView: SnakeMap,
	x: Int, y: Int, length: Int = 4,
	horizontal: Boolean = true, reversed: Boolean = true
): Snake(gameView, x, y, length, horizontal, reversed) {
	override fun step(): Point {
		val foods = gameView.foods
		val obstacles = gameView.snakes.flatMap { it.chunks }.map {
			Point(
				it.x,
				it.y
			)
		} +
				gameView.walls
		val map = Array(maxX + 2) { x ->
			Array(maxY + 2) { y ->
				if (x == 0 || y == 0 || x == maxX + 1 || y == maxY + 1)
					-1
				else
					Int.MAX_VALUE
			}
		}
		obstacles.forEach {
			map[it.x][it.y] = -1
		}
		foods.forEach {
			map[it.x][it.y] = -2
		}

		map[chunks.first().x][chunks.first().y] = 0
		//map[chunks.last().x][chunks.last().y] = Int.MAX_VALUE

		val cells = ArrayDeque<Point>()
		cells.addLast(Point(
			chunks.first().x,
			chunks.first().y
		))
		while (cells.isNotEmpty()) {
			val cell = cells.pollFirst()!!
			val food = listOf(
				Point(cell.x + 1, cell.y),
				Point(cell.x - 1, cell.y),
				Point(cell.x, cell.y + 1),
				Point(cell.x, cell.y - 1)
			)
				.filter { it.x in 0 until map.size && it.y in 0 until map[0].size }
				.filter { map[it.x][it.y] != -1 && (map[it.x][it.y] == Int.MAX_VALUE || map[it.x][it.y] == -2) }
				.firstOrNull {
					val result = if (map[it.x][it.y] == -2) {
						true
					} else {
						cells.addLast(it)
						false
					}
					map[it.x][it.y] = map[cell.x][cell.y] + 1
					result
				}
            map[chunks.first().x][chunks.first().y] = -1
			if (food != null) {

				
				var target: Point = food
				for (i in 0 until map[food.x][food.y] - 1) {
					target = listOf(
						Point(target.x + 1, target.y),
						Point(target.x - 1, target.y),
						Point(target.x, target.y + 1),
						Point(target.x, target.y - 1)
					).shuffled().filter { map[it.x][it.y] >= 0 }.minBy {
						val hasWall = listOf(
							Point(it.x + 1, it.y),
							Point(it.x - 1, it.y),
							Point(it.x, it.y + 1),
							Point(it.x, it.y - 1)
						).firstOrNull { it2 ->
							it2.x < 0 || it2.x >= map.size ||
									it2.y < 0 || it2.x >= map[0].size ||
									map[it2.x][it2.y] == -1
						} != null
						map[it.x][it.y] + if (hasWall) 0.1f else 0.0f
					}!!
				}
				if (countReachableCells(target, map) == Int.MAX_VALUE) {
					/* println("x, y - " + chunks.first().x + ", " + chunks.first().y)
					println("eat - " + countReachableCells(target, map)) */
					return Point(
						target.x - chunks.first().x,
						target.y - chunks.first().y
					)
				} else {
					/* println("x, y - " + chunks.first().x + ", " + chunks.first().y)
					println("x+ - " + countReachableCells(Point(chunks.first().x + 1, chunks.first().y), map))
					println("x- - " + countReachableCells(Point(chunks.first().x - 1, chunks.first().y), map))
					println("y+ - " + countReachableCells(Point(chunks.first().x, chunks.first().y + 1), map))
					println("y- - " + countReachableCells(Point(chunks.first().x, chunks.first().y - 1), map)) */
					target = listOf(
						Point(chunks.first().x + 1, chunks.first().y),
						Point(chunks.first().x - 1, chunks.first().y),
						Point(chunks.first().x, chunks.first().y + 1),
						Point(chunks.first().x, chunks.first().y - 1)
					).maxBy { countReachableCells(it, map) }!!
					return Point(
						target.x - chunks.first().x,
						target.y - chunks.first().y
					)
				}
			}
		}
		
		map[chunks.first().x][chunks.first().y] = -1
		
		/* println("NO .. x, y - " + chunks.first().x + ", " + chunks.first().y)
		println("x+ - " + countReachableCells(Point(chunks.first().x + 1, chunks.first().y), map))
		println("x- - " + countReachableCells(Point(chunks.first().x - 1, chunks.first().y), map))
		println("y+ - " + countReachableCells(Point(chunks.first().x, chunks.first().y + 1), map))
		println("y- - " + countReachableCells(Point(chunks.first().x, chunks.first().y - 1), map)) */
		
		listOf(
			Point(chunks.first().x + 1, chunks.first().y),
			Point(chunks.first().x - 1, chunks.first().y),
			Point(chunks.first().x, chunks.first().y + 1),
			Point(chunks.first().x, chunks.first().y - 1)
		).filter {
			chunks.size == 1 || chunks[1].x != it.x || chunks[1].y != it.y
		}.maxBy { countReachableCells(it, map) }!!.let {
			return Point(
				it.x - chunks.first().x,
				it.y - chunks.first().y
			)
		}
	}
	
	private fun countReachableCells(p: Point, map: Array<Array<Int>>): Int {
		val m = Array(map.size) { x ->
			Array(map[0].size) { y ->
				map[x][y] == -1
			}
		}
		if (p.x == chunks.last().x && p.y == chunks.last().y){
			return Int.MAX_VALUE
		}
		if (m[p.x][p.y]) return 0
		val cells = ArrayDeque<Point>()
		cells.addLast(p)
		var count = 0
		while (cells.isNotEmpty()) {
			count++
			val cell = cells.pollFirst()!!
			cells.addAll(listOf(
				Point(cell.x + 1, cell.y),
				Point(cell.x - 1, cell.y),
				Point(cell.x, cell.y + 1),
				Point(cell.x, cell.y - 1)
			).filter {
				if (it.x == chunks.last().x && it.y == chunks.last().y){
					return Int.MAX_VALUE
				}
				val r = it.x >= 0 && it.y >= 0 && it.x < map.size && it.y < map[0].size && !m[it.x][it.y]
				if (r) {
					m[it.x][it.y] = true
				}
				r
			})
		}
		return count
	}
}
