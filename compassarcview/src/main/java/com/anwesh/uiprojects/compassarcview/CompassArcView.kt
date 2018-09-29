package com.anwesh.uiprojects.compassarcview

/**
 * Created by anweshmishra on 29/09/18.
 */

import android.view.View
import android.view.MotionEvent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.content.Context
import android.graphics.Color

val nodes : Int = 5

fun Canvas.drawCANode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / 3
    val r : Float = gap / 7
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = Math.min(w, h) / 60
    paint.strokeCap = Paint.Cap.ROUND
    paint.color = Color.parseColor("#9C27B0")
    save()
    translate(w/2, gap * i + gap)
    for (j in 0..1) {
        val sf : Float = 1f - 2 * j
        val sc : Float = Math.min(0.5f, Math.max(0f, scale - 0.5f * j)) * 2
        val sc1 : Float = Math.min(0.5f, sc) * 2
        val sc2 : Float = Math.min(0.5f, Math.max(0f, sc - 0.5f)) * 2
        val oDeg : Float = 30f
        val destDeg = 90f
        val deg : Float = oDeg + (destDeg - oDeg) * sc1
        save()
        scale(sf, 1f)
        rotate(-deg)
        save()
        translate((w - size) * sc2, 0f)
        drawLine(0f, 0f, 0f, size, paint)
        restore()
        drawArc(RectF(-r, -r, r, r), 90f - oDeg, oDeg, false, paint)
        restore()
    }
    restore()
}

class CompassArcView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas : Canvas) {

    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {

            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var prevScale : Float = 0f, var dir : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += 0.025f * dir
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class CANode(var i : Int, var state : State = State()) {
        private var next : CANode? = null
        private var prev : CANode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = CANode(i + 1)
                next?.prev = this
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun getNext(dir : Int, cb : () -> Unit) : CANode {
            var curr : CANode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawCANode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }
    }
}