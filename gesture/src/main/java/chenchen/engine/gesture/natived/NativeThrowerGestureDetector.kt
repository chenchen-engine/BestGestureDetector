package chenchen.engine.gesture.natived

import android.content.Context
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.OverScroller
import androidx.core.view.ViewCompat
import chenchen.engine.gesture.OnThrowerListener
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 投掷手势
 * 都是模板代码，套路固定，没有任何变数，基于RecyclerView的滑动小改
 * @author: chenchen
 * @since: 2024/6/25 10:12
 */
class NativeThrowerGestureDetector(context: Context) {

    private companion object {
        private fun obtain() = VelocityTracker.obtain()
    }

    private val viewConfig = ViewConfiguration.get(context)
    private val minFlingVelocity = viewConfig.scaledMinimumFlingVelocity.toFloat()
    private val maxFlingVelocity = viewConfig.scaledMaximumFlingVelocity.toFloat()
    private val velocity = obtain()
    private val glider = Glider(context)
    private var listener: OnThrowerListener? = null

    fun onTouchEvent(view: View, event: MotionEvent) {
        velocity?.addMovement(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_UP -> {
                val time = listener?.onBeginThrow() ?: 0
                if (time <= 0) {
                    velocity.clear()
                    return
                }
                velocity.computeCurrentVelocity(time, maxFlingVelocity)
                val xVel = -velocity.xVelocity
                val yVel = -velocity.yVelocity
                if (xVel != 0f || yVel != 0f) {
                    start(view, xVel, yVel)
                }
                velocity.clear()
            }
        }
    }

    private fun start(view: View, xVel: Float, yVel: Float) {
        var velocityX = xVel
        var velocityY = yVel
        if (abs(xVel) < minFlingVelocity) {
            velocityX = 0f
        }
        if (abs(yVel) < minFlingVelocity) {
            velocityY = 0f
        }
        if (velocityX == 0f && velocityY == 0f) {
            return
        }
        velocityX = max(-maxFlingVelocity, min(velocityX, maxFlingVelocity))
        velocityY = max(-maxFlingVelocity, min(velocityY, maxFlingVelocity))
        glider.start(view, velocityX.toInt(), velocityY.toInt())
    }

    private fun onFling(x: Int, y: Int) {
        if (listener?.onThrow(x, y) == false) {
            glider.stop()
        }
    }

    fun setOnThrowListener(listener: OnThrowerListener) {
        this.listener = listener
    }

    private inner class Glider(context: Context) : Runnable {
        private var lastFlingX = 0
        private var lastFlingY = 0
        private var viewRef: WeakReference<View>? = null
        private val overScroller = OverScroller(context)

        override fun run() {
            if (!overScroller.computeScrollOffset()) {
                stop()
                return
            }
            if (overScroller.currVelocity <= minFlingVelocity || overScroller.currVelocity >= maxFlingVelocity) {
                stop()
                return
            }
            val currX = overScroller.currX
            val currY = overScroller.currY
            onFling(lastFlingX - currX, lastFlingY - currY)
            lastFlingX = currX
            lastFlingY = currY
            postOnAnimation()
        }

        fun start(view: View, velocityX: Int, velocityY: Int) {
            stop()
            lastFlingX = 0
            lastFlingY = 0
            viewRef = WeakReference(view)
            overScroller.fling(0, 0, velocityX, velocityY, Int.MIN_VALUE, Int.MAX_VALUE, Int.MIN_VALUE, Int.MAX_VALUE)
            postOnAnimation()
        }

        fun stop() {
            listener?.onEndThrow()
            viewRef?.get()?.removeCallbacks(this)
            viewRef?.clear()
            overScroller.abortAnimation()
            lastFlingX = 0
            lastFlingY = 0
        }

        private fun postOnAnimation() {
            val view = viewRef?.get() ?: return stop()
            view.removeCallbacks(this)
            ViewCompat.postOnAnimation(view, this)
        }
    }
}