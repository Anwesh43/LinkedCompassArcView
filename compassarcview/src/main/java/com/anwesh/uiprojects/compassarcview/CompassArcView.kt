package com.anwesh.uiprojects.compassarcview

/**
 * Created by anweshmishra on 29/09/18.
 */

import android.app.Activity
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
        save()
        translate((w/2 - size) * sc2, 0f)
        rotate(-deg)
        drawLine(0f, 0f, 0f, size, paint)
        restore()
        drawArc(RectF(-r, -r, r, r), 90f - deg, deg, false, paint)
        restore()
    }
    restore()
}

class CompassArcView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
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

    data class CompassArc(var i : Int) {
        private var root : CANode = CANode(0)
        private var curr : CANode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : CompassArcView) {

        private val ca : CompassArc = CompassArc(0)
        private val animator : Animator = Animator(view)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(Color.parseColor("#BDBDBD"))
            ca.draw(canvas, paint)
            animator.animate {
                ca.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            ca.startUpdating {
                animator.start()
            }
        }
    }

    companion object {
        fun create(activity : Activity) : CompassArcView {
            val view : CompassArcView = CompassArcView(activity)
            activity.setContentView(view)
            return view
        }
    }
}