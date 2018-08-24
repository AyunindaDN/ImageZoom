package com.example.woi.elo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.OnTouchListener
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewParent
import android.widget.ImageView
import android.widget.ImageView.ScaleType.FIT_XY
import kotlinx.android.synthetic.main.activity_elo.imageView1
import kotlinx.android.synthetic.main.activity_elo.imageView2
import kotlinx.android.synthetic.main.activity_elo.imageView3


class EloActivity : AppCompatActivity() {
  val LOG_TAG = "kektai"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_elo)

    val customOnTouchListener = CustomOnTouchListener(this)
    imageView1.setOnTouchListener(customOnTouchListener)
    imageView2.setOnTouchListener(customOnTouchListener)
    imageView3.setOnTouchListener(customOnTouchListener)

  }
}


class CustomOnTouchListener(private val context : Context) : OnTouchListener, ScaleGestureDetector.OnScaleGestureListener {

  companion object {
    const val MIN_SCALE_FACTOR = 1f
    const val MAX_SCALE_FACTOR = 5f
    const val STATE_IDLE = 0
    const val STATE_POINTER_DOWN = 1
    const val STATE_ZOOMING = 2
  }

  private val scaleGestureDetector by lazy { ScaleGestureDetector(context, this) }
  private var scaleFactor = 1f

  private lateinit var targetView: View
  private var imageToZoom: ImageView? = null
  //  private var pointerX: Float = 0f
//  private var pointerY: Float = 0f
//  private var currentX: Float = 0f
//  private var currentY: Float = 0f

  private lateinit var initialCoordinate: Coordinate
  private var currentCoordinate: Coordinate = Coordinate()
  private var state: Int = 0

  private val targetViewCoordinate = IntArray(2)

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouch(v: View?, event: MotionEvent?): Boolean {
    if (event!!.pointerCount >= 2) {
      scaleGestureDetector.onTouchEvent(event)

      targetView = v!!

      val action = event?.action?.and(MotionEvent.ACTION_MASK)

//    if (scaleGestureDetector?.isInProgress == true) {
      when (action) {
        MotionEvent.ACTION_POINTER_DOWN,
        MotionEvent.ACTION_DOWN -> {
          Log.e("move", "down $action state: $state")
          detachParentFocus(v!!.parent, event!!)
          setInitialState(event, v)
        }

        MotionEvent.ACTION_MOVE -> {
          moveImage(event)
        }

//        MotionEvent.ACTION_UP,
//        MotionEvent.ACTION_POINTER_UP,
//        MotionEvent.ACTION_CANCEL -> {
//          Log.e("move", "up $action state: $state")
//          if (state == STATE_ZOOMING) {
//            resetState(v)
//          }
//        }
      }
    }
//    }

    return true
  }

  private fun setInitialState(event: MotionEvent, v: View?) {
//    when (state) {
//      STATE_IDLE -> {
//        state = STATE_POINTER_DOWN
//      }
//      STATE_POINTER_DOWN -> {
    if (state == STATE_IDLE) {
      if (event.pointerCount == 2) {
        initialCoordinate = getCurrentCoordinate(event)
        state = STATE_ZOOMING
      }

      Log.e("move", "down state: $state")

//      startZoomingAction(v)
    }
//      }
//    }
  }

  private fun moveImage(event: MotionEvent) {
    Log.e("move", "move state: $state")
    if (state == STATE_ZOOMING) {

      if (scaleGestureDetector?.isInProgress == true) {
        moveDetector(event)
      } else {
        if (currentCoordinate.x != 0f && currentCoordinate.y != 0f) {
          moveDetector(event)
        }
      }
    }
  }

  private fun resetState(v: View) {
    ((v.context as Activity).window.decorView as ViewGroup).removeView(imageToZoom)
    initialCoordinate.reset()
    currentCoordinate.reset()
    v.visibility = VISIBLE
    state = STATE_IDLE

//    v.parent?.let {
    enableParentTouch(v.parent)
//    }
  }

  private fun moveDetector(event: MotionEvent) {
    if (event.pointerCount == 2) {
      currentCoordinate = getCurrentCoordinate(event)

      currentCoordinate.x = (currentCoordinate.x - initialCoordinate.x) + targetViewCoordinate[0]
      currentCoordinate.y = (currentCoordinate.y - initialCoordinate.y) + targetViewCoordinate[1]

      imageToZoom?.x = currentCoordinate.x
      imageToZoom?.y = currentCoordinate.y
    }
  }

  private fun getCurrentCoordinate(
      event: MotionEvent): Coordinate {
    val currentX = (event.getX(0) + event.getX(1)) / 2
    val currentY = (event.getY(0) + event.getY(1)) / 2

    return Coordinate(currentX, currentY)
  }

  private fun startZoomingAction(v: View?) {
    v?.let {
      val root = it.rootView
      imageToZoom = ImageView(it.context)
      imageToZoom?.adjustViewBounds = true
      imageToZoom?.scaleType = FIT_XY
      imageToZoom?.layoutParams = LayoutParams(it.width, it.height)
      imageToZoom?.setImageDrawable((it as ImageView).drawable)

      it.getLocationInWindow(targetViewCoordinate)

      imageToZoom?.x = targetViewCoordinate[0].toFloat()
      imageToZoom?.y = targetViewCoordinate[1].toFloat()

      disableParentTouch(it.parent)
      it.visibility = INVISIBLE

      ((it.context as Activity).window.decorView as ViewGroup).addView(imageToZoom)
    }
  }

  override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
    Log.e("scale", "scale begin")
    startZoomingAction(targetView)
    return true
  }

  override fun onScaleEnd(detector: ScaleGestureDetector?) {
    Log.e("scale", "scale end")
    scaleFactor = 1f
    resetState(targetView)
  }

  override fun onScale(detector: ScaleGestureDetector?): Boolean {
    if (imageToZoom == null) return false

    scaleFactor *= detector!!.scaleFactor
    scaleFactor = Math.max(MIN_SCALE_FACTOR, Math.min(scaleFactor, MAX_SCALE_FACTOR))

    imageToZoom?.scaleX = scaleFactor
    imageToZoom?.scaleY = scaleFactor

    return true
  }

  private fun disableParentTouch(view: ViewParent) {
    view.requestDisallowInterceptTouchEvent(true)
    if (view.parent != null) disableParentTouch(view.parent)
  }

  private fun enableParentTouch(view: ViewParent) {
    view.requestDisallowInterceptTouchEvent(false)
    if (view.parent != null) enableParentTouch(view.parent)
  }

  private fun detachParentFocus(view: ViewParent, touchEvent: MotionEvent) {
    if (view is View){
      view.let { it as View }.dispatchTouchEvent(touchEvent)
    }
    if (view.parent != null) detachParentFocus(view.parent, touchEvent)
  }

  data class Coordinate(
      var x: Float = 0f,
      var y: Float = 0f
  ) {
    fun reset() {
      x = 0f
      y = 0f
    }
  }
}

