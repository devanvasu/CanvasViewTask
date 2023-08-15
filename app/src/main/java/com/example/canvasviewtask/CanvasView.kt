package com.example.canvasviewtask

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathEffect
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.io.ByteArrayOutputStream


class CanvasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    enum class Mode {
        DRAW, TEXT, ERASER
    }

    // Enumeration for Drawer
    enum class Drawer {
        PEN, LINE, RECTANGLE, CIRCLE, ELLIPSE, QUADRATIC_BEZIER, QUBIC_BEZIER
    }

    private var canvas: Canvas? = null
    private var bitmap: Bitmap? = null

    private val pathLists: MutableList<Path> = ArrayList()
    private val paintLists: MutableList<Paint> = ArrayList()

    private val emptyPaint = Paint()
    private val eraserPaint = Paint()

    // for Eraser
     private var baseColor = Color.WHITE
    private var eraserColor = Color.BLACK

    // for Undo, Redo
    private var historyPointer = 0

    // Flags
    private var mode = Mode.DRAW
    private var drawer = Drawer.PEN
    private var isDown = false

    // for Paint
    private var paintStyle = Paint.Style.STROKE
    private var paintStrokeColor = Color.BLACK
    private var paintFillColor = Color.BLACK
    private var paintStrokeWidth = 3f
    private var opacity = 255
    private var blur = 0f
    private var lineCap = Paint.Cap.ROUND
    private var drawPathEffect: PathEffect? = null

    // for Text
    private var text = ""
    private var fontFamily = Typeface.DEFAULT
    private var fontSize = 32f
    private val textAlign = Paint.Align.RIGHT // fixed

    private var textPaint = Paint()
    private var blackPaint = Paint()
    private var redPaint = Paint()
    private var bluePaint = Paint()

    private var textX = 0f
    private var textY = 0f

    // for Drawer
    private var startX = 0f
    private var startY = 0f
    private var controlX = 0f
    private var controlY = 0f

    private var eraserMode = false

     var color=""

    /**
     * Copy Constructor
     *
     * @param context
     * @param attrs
     * @param defStyle
     */
    fun CanvasView(context: Context?, attrs: AttributeSet?, defStyle: Int) {
        setup()
    }

    /**
     * Copy Constructor
     *
     * @param context
     * @param attrs
     */
    fun CanvasView(context: Context?, attrs: AttributeSet?) {
        setup()
    }

    /**
     * Copy Constructor
     *
     * @param context
     */
    fun CanvasView(context: Context?) {
        setup()
    }

    /**
     * Common initialization.
     *
     */




    private fun setup() {
        pathLists.add(Path())
        paintLists.add(createPaint())
        historyPointer++
        textPaint.setARGB(0, 255, 255, 255)
    }

    /**
     * This method creates the instance of Paint.
     * In addition, this method sets styles for Paint.
     *
     * @return paint This is returned as the instance of Paint
     */
     fun createPaint(): Paint {
        val paint = Paint()
        paint.isAntiAlias = true
        paint.style = paintStyle
        paint.strokeWidth = paintStrokeWidth
        paint.strokeCap = lineCap
        paint.strokeJoin = Paint.Join.MITER // fixed

        // for Text
        if (mode == Mode.TEXT) {
            paint.typeface = fontFamily
            paint.textSize = fontSize
            paint.textAlign = textAlign
            paint.strokeWidth = 0f
        }
        if (mode == Mode.ERASER) {
            // Eraser
            paint.color = Color.TRANSPARENT
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            paint.setARGB(0, 0, 0, 0)
            // paint.setColor(this.baseColor);
            // paint.setShadowLayer(this.blur, 0F, 0F, this.baseColor);
        } else {
            // Otherwise
            paint.color = paintStrokeColor
            paint.setShadowLayer(blur, 0f, 0f, paintStrokeColor)
            paint.alpha = opacity
            paint.pathEffect = drawPathEffect
        }
        return paint
    }

    /**
     * This method initialize Path.
     * Namely, this method creates the instance of Path,
     * and moves current position.
     *
     * @param event This is argument of onTouchEvent method
     * @return path This is returned as the instance of Path
     */
    private fun createPath(event: MotionEvent): Path {
        val path = Path()

        // Save for ACTION_MOVE
        startX = event.x
        startY = event.y
        path.moveTo(startX, startY)
        return path
    }

    /**
     * This method updates the lists for the instance of Path and Paint.
     * "Undo" and "Redo" are enabled by this method.
     *
     * @param path the instance of Path
     */
    private fun updateHistory(path: Path) {
        if (historyPointer == pathLists.size) {
            pathLists.add(path)
            paintLists.add(createPaint())
            historyPointer++
        } else {
            // On the way of Undo or Redo
            pathLists[historyPointer] = path
            paintLists[historyPointer] = createPaint()
            historyPointer++
            var i = historyPointer
            val size = paintLists.size
            while (i < size) {
                pathLists.removeAt(historyPointer)
                paintLists.removeAt(historyPointer)
                i++
            }
        }
    }

    /**
     * This method gets the instance of Path that pointer indicates.
     *
     * @return the instance of Path
     */
    private fun getCurrentPath(): Path {
        return pathLists[historyPointer - 1]
    }

    /**
     * This method draws text.
     *
     * @param canvas the instance of Canvas
     */
    private fun drawText(canvas: Canvas) {
        if (text.length <= 0) {
            return
        }
        if (mode == Mode.TEXT) {
            textX = startX
            textY = startY
            textPaint = createPaint()
        }
        val textX = textX
        val textY = textY
        val paintForMeasureText = Paint()

        // Line break automatically
        val textLength = paintForMeasureText.measureText(text)
        val lengthOfChar = textLength / text.length.toFloat()
        val restWidth = this.canvas!!.width - textX // text-align : right
        val numChars =
            if (lengthOfChar <= 0) 1 else Math.floor((restWidth / lengthOfChar).toDouble())
                .toInt() // The number of characters at 1 line
        val modNumChars = if (numChars < 1) 1 else numChars
        var y = textY
        var i = 0
        val len = text.length
        while (i < len) {
            var substring = ""
            substring = if (i + modNumChars < len) {
                text.substring(i, i + modNumChars)
            } else {
                text.substring(i, len)
            }
            y += fontSize
            canvas.drawText(substring, textX, y, textPaint)
            i += modNumChars
        }
    }

    /**
     * This method defines processes on MotionEvent.ACTION_DOWN
     *
     * @param event This is argument of onTouchEvent method
     */
    private fun onActionDown(event: MotionEvent) {
        when (mode) {
            Mode.DRAW, Mode.ERASER -> if (drawer != Drawer.QUADRATIC_BEZIER && drawer != Drawer.QUBIC_BEZIER) {
                // Oherwise
                updateHistory(createPath(event))
                isDown = true
            } else {
                // Bezier
                if (startX == 0f && startY == 0f) {
                    // The 1st tap
                    updateHistory(createPath(event))
                } else {
                    // The 2nd tap
                    controlX = event.x
                    controlY = event.y
                    isDown = true
                }
            }

            Mode.TEXT -> {
                startX = event.x
                startY = event.y
            }

            else -> {}
        }
    }

    /**
     * This method defines processes on MotionEvent.ACTION_MOVE
     *
     * @param event This is argument of onTouchEvent method
     */
    private fun onActionMove(event: MotionEvent) {
        val x = event.x
        val y = event.y
        when (mode) {
            Mode.DRAW, Mode.ERASER -> if (drawer != Drawer.QUADRATIC_BEZIER && drawer != Drawer.QUBIC_BEZIER) {
                if (!isDown) {
                    return
                }
                val path = getCurrentPath()


                when (drawer) {
                    Drawer.PEN -> path.lineTo(x, y)
                    Drawer.LINE -> {
                        path.reset()
                        path.moveTo(startX, startY)
                        path.lineTo(x, y)
                    }

                    Drawer.RECTANGLE -> {
                        path.reset()
                        val left = Math.min(startX, x)
                        val right = Math.max(startX, x)
                        val top = Math.min(startY, y)
                        val bottom = Math.max(startY, y)
                        path.addRect(left, top, right, bottom, Path.Direction.CCW)
                    }

                    Drawer.CIRCLE -> {
                        val distanceX = Math.abs((startX - x).toDouble())
                        val distanceY = Math.abs((startX - y).toDouble())
                        val radius = Math.sqrt(Math.pow(distanceX, 2.0) + Math.pow(distanceY, 2.0))
                        path.reset()
                        path.addCircle(startX, startY, radius.toFloat(), Path.Direction.CCW)
                    }

                    Drawer.ELLIPSE -> {
                        val rect = RectF(startX, startY, x, y)
                        path.reset()
                        path.addOval(rect, Path.Direction.CCW)
                    }

                    else -> {}
                }
            } else {
                if (!isDown) {
                    return
                }
                val path = getCurrentPath()
                path.reset()
                path.moveTo(startX, startY)
                path.quadTo(controlX, controlY, x, y)
            }

            Mode.TEXT -> {
                startX = x
                startY = y
            }

            else -> {}
        }
    }

    /**
     * This method defines processes on MotionEvent.ACTION_DOWN
     *
     * @param event This is argument of onTouchEvent method
     */
    private fun onActionUp(event: MotionEvent) {

        if (isDown) {
            startX = 0f
            startY = 0f
            isDown = false
        }
    }

    /**
     * This method updates the instance of Canvas (View)
     *
     * @param canvas the new instance of Canvas
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Before "drawPath"
        canvas.drawColor(baseColor)

        if (bitmap != null) {
            canvas.drawBitmap(bitmap!!, 0f, 0f, emptyPaint)
        }
        for (i in 0 until historyPointer) {
            val path = pathLists[i]
            val paint = paintLists[i]

            canvas.drawPath(path, paint)

        }

        drawText(canvas)
        this.canvas = canvas
    }

    /**
     * This method set event listener for drawing.
     *
     * @param event the instance of MotionEvent
     * @return
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> onActionDown(event)
                MotionEvent.ACTION_MOVE -> onActionMove(event)
                MotionEvent.ACTION_UP -> onActionUp(event)
                else -> {}

            }
            // Re draw
            this.invalidate()

            return true

    }

    /**
     * This method is getter for mode.
     *
     * @return
     */
    fun getMode(): Mode? {
        return mode
    }

    /**
     * This method is setter for mode.
     *
     * @param mode
     */
    fun setMode(mode: Mode) {
        this.mode = mode
    }

    /**
     * This method is getter for drawer.
     *
     * @return
     */
    fun getDrawer(): Drawer? {
        return drawer
    }

    /**
     * This method is setter for drawer.
     *
     * @param drawer
     */
    fun setDrawer(drawer: Drawer) {
        this.drawer = drawer
    }

    /**
     * This method checks if Undo is available
     *
     * @return If Undo is available, this is returned as true. Otherwise, this is returned as false.
     */
    fun canUndo(): Boolean {
        return historyPointer > 1
    }

    /**
     * This method checks if Redo is available
     *
     * @return If Redo is available, this is returned as true. Otherwise, this is returned as false.
     */
    fun canRedo(): Boolean {
        return historyPointer < pathLists.size
    }

    /**
     * This method draws canvas again for Undo.
     *
     * @return If Undo is enabled, this is returned as true. Otherwise, this is returned as false.
     */
    fun undo(): Boolean {
        return if (canUndo()) {
            historyPointer--
            this.invalidate()
            true
        } else {
            false
        }
    }

    /**
     * This method draws canvas again for Redo.
     *
     * @return If Redo is enabled, this is returned as true. Otherwise, this is returned as false.
     */
    fun redo(): Boolean {
        return if (canRedo()) {
            historyPointer++
            this.invalidate()
            true
        } else {
            false
        }
    }

    /**
     * This method initializes canvas.
     *
     * @return
     */
    fun clear() {
        val path = Path()
        path.moveTo(0f, 0f)
        path.addRect(0f, 0f, 1000f, 1000f, Path.Direction.CCW)
        path.close()
        val paint = Paint()
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        if (historyPointer == pathLists.size) {
            pathLists.add(path)
            paintLists.add(paint)
            historyPointer++
        } else {
            // On the way of Undo or Redo
            pathLists[historyPointer] = path
            paintLists[historyPointer] = paint
            historyPointer++
            var i = historyPointer
            val size = paintLists.size
            while (i < size) {
                pathLists.removeAt(historyPointer)
                paintLists.removeAt(historyPointer)
                i++
            }
        }
        text = ""

        // Clear
        this.invalidate()
    }

    /**
     * This method is getter for canvas background color
     *
     * @return
     */
    fun getBaseColor(): Int {
        return baseColor
    }

    /**
     * This method is setter for canvas background color
     *
     * @param color
     */
    fun setBaseColor(color: Int) {
        baseColor = color
    }

    /**
     * This method is getter for drawn text.
     *
     * @return
     */
    fun getText(): String? {
        return text
    }

    /**
     * This method is setter for drawn text.
     *
     * @param text
     */
    fun setText(text: String) {
        this.text = text
    }

    /**
     * This method is getter for stroke or fill.
     *
     * @return
     */
    fun getPaintStyle(): Paint.Style? {
        return paintStyle
    }

    /**
     * This method is setter for stroke or fill.
     *
     * @param style
     */
    fun setPaintStyle(style: Paint.Style) {
        paintStyle = style
    }

    /**
     * This method is getter for stroke color.
     *
     * @return
     */
    fun getPaintStrokeColor(): Int {
        return paintStrokeColor
    }

    /**
     * This method is setter for stroke color.
     *
     * @param color
     */
    fun setPaintStrokeColor(color: Int) {
        paintStrokeColor = color
    }

    /**
     * This method is getter for fill color.
     * But, current Android API cannot set fill color (?).
     *
     * @return
     */
    fun getPaintFillColor(): Int {
        return paintFillColor
    }

    /**
     * This method is setter for fill color.
     * But, current Android API cannot set fill color (?).
     *
     * @param color
     */
    fun setPaintFillColor(color: Int) {
        paintFillColor = color
    }

    /**
     * This method is getter for stroke width.
     *
     * @return
     */
    fun getPaintStrokeWidth(): Float {
        return paintStrokeWidth
    }

    /**
     * This method is setter for stroke width.
     *
     * @param width
     */
    fun setPaintStrokeWidth(width: Float) {
        if (width >= 0) {
            paintStrokeWidth = width
        } else {
            paintStrokeWidth = 3f
        }
    }

    /**
     * This method is getter for alpha.
     *
     * @return
     */
    fun getOpacity(): Int {
        return opacity
    }

    /**
     * This method is setter for alpha.
     * The 1st argument must be between 0 and 255.
     *
     * @param opacity
     */
    fun setOpacity(opacity: Int) {
        if (opacity >= 0 && opacity <= 255) {
            this.opacity = opacity
        } else {
            this.opacity = 255
        }
    }

    /**
     * This method is getter for amount of blur.
     *
     * @return
     */
    fun getBlur(): Float {
        return blur
    }

    /**
     * This method is setter for amount of blur.
     * The 1st argument is greater than or equal to 0.0.
     *
     * @param blur
     */
    fun setBlur(blur: Float) {
        if (blur >= 0) {
            this.blur = blur
        } else {
            this.blur = 0f
        }
    }

    /**
     * This method is getter for line cap.
     *
     * @return
     */
    fun getLineCap(): Paint.Cap? {
        return lineCap
    }

    /**
     * This method is setter for line cap.
     *
     * @param cap
     */
    fun setLineCap(cap: Paint.Cap) {
        lineCap = cap
    }

    /**
     * This method is getter for path effect of drawing.
     *
     * @return drawPathEffect
     */
    fun getDrawPathEffect(): PathEffect? {
        return drawPathEffect
    }

    /**
     * This method is setter for path effect of drawing.
     *
     * @param drawPathEffect
     */
    fun setDrawPathEffect(drawPathEffect: PathEffect?) {
        this.drawPathEffect = drawPathEffect
    }

    /**
     * This method is getter for font size,
     *
     * @return
     */
    fun getFontSize(): Float {
        return fontSize
    }

    /**
     * This method is setter for font size.
     * The 1st argument is greater than or equal to 0.0.
     *
     * @param size
     */
    fun setFontSize(size: Float) {
        if (size >= 0f) {
            fontSize = size
        } else {
            fontSize = 32f
        }
    }

    /**
     * This method is getter for font-family.
     *
     * @return
     */
    fun getFontFamily(): Typeface? {
        return fontFamily
    }

    /**
     * This method is setter for font-family.
     *
     * @param face
     */
    fun setFontFamily(face: Typeface) {
        fontFamily = face
    }

    /**
     * This method gets current canvas as bitmap.
     *
     * @return This is returned as bitmap.
     */
    fun getBitmap(): Bitmap {
        this.isDrawingCacheEnabled = false
        this.isDrawingCacheEnabled = true
        return Bitmap.createBitmap(this.drawingCache)
    }

    /**
     * This method gets current canvas as scaled bitmap.
     *
     * @return This is returned as scaled bitmap.
     */
    fun getScaleBitmap(w: Int, h: Int): Bitmap? {
        this.isDrawingCacheEnabled = false
        this.isDrawingCacheEnabled = true
        return Bitmap.createScaledBitmap(this.drawingCache, w, h, true)
    }

    /**
     * This method draws the designated bitmap to canvas.
     *
     * @param bitmap
     */
    fun drawBitmap(bitmap: Bitmap?) {
        this.bitmap = bitmap
        this.invalidate()
    }

    /**
     * This method draws the designated byte array of bitmap to canvas.
     *
     * @param byteArray This is returned as byte array of bitmap.
     */
    fun drawBitmap(byteArray: ByteArray) {
        this.drawBitmap(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size))
    }

    /**
     * This static method gets the designated bitmap as byte array.
     *
     * @param bitmap
     * @param format
     * @param quality
     * @return This is returned as byte array of bitmap.
     */
    fun getBitmapAsByteArray(bitmap: Bitmap, format: Bitmap.CompressFormat?, quality: Int): ByteArray? {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(format, quality, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    /**
     * This method gets the bitmap as byte array.
     *
     * @param format
     * @param quality
     * @return This is returned as byte array of bitmap.
     */
    fun getBitmapAsByteArray(format: Bitmap.CompressFormat?, quality: Int): ByteArray? {
        val byteArrayOutputStream = ByteArrayOutputStream()
        getBitmap().compress(format, quality, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    /**
     * This method gets the bitmap as byte array.
     * Bitmap format is PNG, and quality is 100.
     *
     * @return This is returned as byte array of bitmap.
     */
    fun getBitmapAsByteArray(): ByteArray? {
        return this.getBitmapAsByteArray(Bitmap.CompressFormat.PNG, 100)
    }

    fun blackPaint(){
        blackPaint = Paint()
        blackPaint.setColor(Color.BLACK)
        color=""
    }
    fun redPaint(){
        redPaint = Paint()
        redPaint.setColor(Color.RED)
        color=""

    }
    fun bluePaint(){
        bluePaint = Paint()
        bluePaint.color = Color.BLUE
        color=""

    }
}