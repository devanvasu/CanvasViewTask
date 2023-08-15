package com.example.canvasviewtask

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {
    private var canvas: CanvasView? = null
    lateinit var paint: Paint
    lateinit var canvasNew: Canvas
    private val REQUEST_STORAGE_PERMISSION = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
   paint = Paint()
        canvasNew = Canvas()
        canvas = findViewById(R.id.canvas) as CanvasView

       storagePermission()

        val btnUndo = findViewById<Button>(R.id.btnUndo)
        btnUndo.setOnClickListener {

           canvas!!.undo()



        }

        val btnRedo = findViewById<Button>(R.id.btnRedo)
        btnRedo.setOnClickListener {
            canvas!!.redo()
        }

        val btnClear = findViewById<Button>(R.id.btnClear)
        btnClear.setOnClickListener {
            canvas!!.clear()
        }

        val btnShape = findViewById<Button>(R.id.btShape)
        btnShape.setOnClickListener {
            showAlertDialog()

        }

        val btnPen = findViewById<Button>(R.id.btnPen)
        btnPen.setOnClickListener {
            penAlertDialog()
            canvas!!.setDrawer(CanvasView.Drawer.PEN)

        }

        val btnSave = findViewById<Button>(R.id.btSave)
        btnSave.setOnClickListener {
            val bitmap:Bitmap= canvas!!.getBitmap()
           saveImage(bitmap)
        }

        val btnShare = findViewById<Button>(R.id.btShare)
        btnShare.setOnClickListener {
            val bitmap:Bitmap= canvas!!.getBitmap()

            shareImage(bitmap)
        }
    }


    private fun showAlertDialog() {
        val alertDialog: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
        alertDialog.setItems(arrayOf<CharSequence>(
            "Draw the Rectangle",
            "Draw the CIRCLE",
            "Draw the ELLIPSE",
            "Draw the QUADRATIC_BEZIER",
            "Draw the Line"
        ),
            DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    0 -> canvas!!.setDrawer(CanvasView.Drawer.RECTANGLE)
                    1 -> canvas!!.setDrawer(CanvasView.Drawer.CIRCLE)
                    2 -> canvas!!.setDrawer(CanvasView.Drawer.ELLIPSE)
                    3 -> canvas!!.setDrawer(CanvasView.Drawer.QUADRATIC_BEZIER)
                    4 -> canvas!!.setDrawer(CanvasView.Drawer.LINE)

                }
            })
        val alert: AlertDialog = alertDialog.create()
        alert.setCanceledOnTouchOutside(false)
        alert.show()
    }
     fun saveImage(finalBitmap: Bitmap) {
        val root = Environment.getExternalStorageDirectory().absolutePath
        val myDir = File(root + "/saved_images")
        myDir.mkdirs()
        val fname = myDir.name+".jpg"
        val file = File(myDir, fname)
        if (file.exists()) file.delete()
        try {
            val out = FileOutputStream(file)
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
            Toast.makeText(this,"saved success fully",Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun storagePermission(){
        if (checkPermissions()) {
        } else {
            requestPermissions()
        }


    }

    private fun checkPermissions(): Boolean {
        val writePermission = ContextCompat.checkSelfPermission(
            this,
            WRITE_EXTERNAL_STORAGE
        )
        val readPermission = ContextCompat.checkSelfPermission(
            this,
            READ_EXTERNAL_STORAGE
        )

        return writePermission == PackageManager.PERMISSION_GRANTED &&
                readPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                WRITE_EXTERNAL_STORAGE,
                READ_EXTERNAL_STORAGE
            ),
            REQUEST_STORAGE_PERMISSION
        )
    }

    fun shareImage(bitmap: Bitmap){
        val root = Environment.getExternalStorageDirectory().absolutePath
        val myDir = File(root+"/saved_images")
        myDir.mkdirs()
        val fname = myDir.name+".jpg"
        val file = File(myDir, fname)
        if (file.exists()) file.delete()
        try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "image/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(file.toString()))
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(shareIntent, "Share Image"))

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            } else {
                Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun penAlertDialog(){
            val alertDialog: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
            alertDialog.setItems(arrayOf<CharSequence>(
                "Click the Black Pen",
                "Click the Red Pen",
                "Click the Blue Pen"
            ),
                DialogInterface.OnClickListener { dialog, which ->
                    when (which) {
                        0 -> {
                           canvas!!.blackPaint()
                            canvas!!.color = "black"
                        }
                        1 -> {
                            canvas!!.redPaint()
                            canvas!!.color = "red"

                        }
                        2 -> {
                            canvas!!.bluePaint()
                            canvas!!.color = "blue"

                        }

                    }
                })
            val alert: AlertDialog = alertDialog.create()
            alert.setCanceledOnTouchOutside(false)
            alert.show()
    }
}