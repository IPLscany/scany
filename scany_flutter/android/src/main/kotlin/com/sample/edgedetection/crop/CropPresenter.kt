package com.sample.edgedetection.crop

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Toast
import com.sample.edgedetection.SourceManager
import com.sample.edgedetection.processor.Corners
import com.sample.edgedetection.processor.TAG
import com.sample.edgedetection.processor.cropPicture
import com.sample.edgedetection.processor.enhancePicture
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.opencv.android.Utils
import android.graphics.pdf.PdfDocument
import android.graphics.Canvas
import org.opencv.core.Mat
import java.io.File
import java.io.FileOutputStream
import android.provider.MediaStore
import android.content.ContentValues
import androidx.core.app.ActivityCompat
import android.annotation.SuppressLint

const val IMAGES_DIR = "smart_scanner"

class CropPresenter(val context: Context, private val iCropView: ICropView.Proxy) {
    private val picture: Mat? = SourceManager.pic

    private val corners: Corners? = SourceManager.corners
    private var croppedPicture: Mat? = null
    private var enhancedPicture: Bitmap? = null
    private var croppedBitmap: Bitmap? = null

    init {
        iCropView.getPaperRect().onCorners2Crop(corners, picture?.size())
        val bitmap = Bitmap.createBitmap(picture?.width() ?: 1080, picture?.height()
                ?: 1920, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(picture, bitmap, true)
        iCropView.getPaper().setImageBitmap(bitmap)
    }

    fun addImageToGallery(filePath: String, context: Context) {

        val values = ContentValues()

        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        values.put(MediaStore.MediaColumns.DATA, filePath)

        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }
    fun crop() {
        if (picture == null) {
            Log.i(TAG, "picture null?")
            return
        }

        if (croppedBitmap != null) {
            Log.i(TAG, "already cropped")
            return
        }

        Observable.create<Mat> {
            it.onNext(cropPicture(picture, iCropView.getPaperRect().getCorners2Crop()))
        }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { pc ->
                    Log.i(TAG, "cropped picture: " + pc.toString())
                    croppedPicture = pc
                    croppedBitmap = Bitmap.createBitmap(pc.width(), pc.height(), Bitmap.Config.ARGB_8888)
                    Utils.matToBitmap(pc, croppedBitmap)
                    iCropView.getCroppedPaper().setImageBitmap(croppedBitmap)
                    iCropView.getPaper().visibility = View.GONE
                    iCropView.getPaperRect().visibility = View.GONE
                }
    }
    fun enhance() {
        if (croppedBitmap == null) {
            Log.i(TAG, "picture null?")
            return
        }

        Observable.create<Bitmap> {
            it.onNext(enhancePicture(croppedBitmap))
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { pc ->
                    enhancedPicture = pc
                    iCropView.getCroppedPaper().setImageBitmap(pc)
                }
    }

    fun save(): String? {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "please grant write file permission and trya gain", Toast.LENGTH_SHORT).show()
        } else {
            val dir = File(Environment.getExternalStorageDirectory(), IMAGES_DIR)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            //first save enhanced picture, if picture is not enhanced, save cropped picture, otherwise nothing to do
            val pic_pdf = enhancedPicture
            val pic_png = enhancedPicture
            //val pic_jpg = enhancedPicture

            if (null != pic_pdf && null != pic_png) { //&& null != pic_jpg) {
                val file_pdf = File(dir, "enhance_${SystemClock.currentThreadTimeMillis()}.pdf")
                val outStream_pdf = FileOutputStream(file_pdf)
                pic_pdf.compress(Bitmap.CompressFormat.PNG, 100, outStream_pdf)
                outStream_pdf.flush()
                outStream_pdf.close()

                val file_png = File(dir, "enhance_${SystemClock.currentThreadTimeMillis()}.png")
                val outStream_png = FileOutputStream(file_png)
                pic_png.compress(Bitmap.CompressFormat.PNG, 100, outStream_png)
                outStream_png.flush()
                outStream_png.close()

                /*val file_jpg = File(dir, "enhance_${SystemClock.currentThreadTimeMillis()}.jpeg")
                val outStream_jpg = FileOutputStream(file_jpg)
                pic_jpg.compress(Bitmap.CompressFormat.JPEG, 100, outStream_jpg)
                outStream_jpg.flush()
                outStream_jpg.close()*/

                // Create a PdfDocument with a page of the same size as the image
                val document: PdfDocument = PdfDocument()
                val pageInfo: PdfDocument.PageInfo  = PdfDocument.PageInfo.Builder(pic_pdf.width, pic_pdf.height, 1).create()
                val page: PdfDocument.Page  = document.startPage(pageInfo)

                // Draw the bitmap onto the page
                val canvas: Canvas = page.canvas
                canvas.drawBitmap(pic_pdf, 0f, 0f, null)
                document.finishPage(page)
                document.writeTo(FileOutputStream(file_pdf))
                document.close()

                addImageToGallery(file_png.absolutePath, this.context) //Commented as we don't want the images in the gallery.
                //addImageToGallery(file_jpg.absolutePath, this.context) //Commented as we don't want the images in the gallery.
                //Toast.makeText(context, "picture saved, path: ${file_pdf.absolutePath}", Toast.LENGTH_SHORT).show()
                return file_pdf.absolutePath + "," + file_png.absolutePath// + "," + file_jpg.absolutePath

            } else {
                val cropPic_pdf = croppedBitmap
                val cropPic_png = croppedBitmap
                //val cropPic_jpg = croppedBitmap
                if (null != cropPic_pdf && null != cropPic_png){// && null != cropPic_jpg) {
                    val file_pdf = File(dir, "crop_${SystemClock.currentThreadTimeMillis()}.pdf")
                    val outStream_pdf = FileOutputStream(file_pdf)
                    cropPic_pdf.compress(Bitmap.CompressFormat.PNG, 100, outStream_pdf)
                    outStream_pdf.flush()
                    outStream_pdf.close()

                    val file_png = File(dir, "crop_${SystemClock.currentThreadTimeMillis()}.png")
                    val outStream_png = FileOutputStream(file_png)
                    cropPic_png.compress(Bitmap.CompressFormat.PNG, 100, outStream_png)
                    outStream_png.flush()
                    outStream_png.close()

                    /*val file_jpg = File(dir, "crop_${SystemClock.currentThreadTimeMillis()}.jpeg")
                    val outStream_jpg = FileOutputStream(file_jpg)
                    cropPic_jpg.compress(Bitmap.CompressFormat.JPEG, 100, outStream_jpg)
                    outStream_jpg.flush()
                    outStream_jpg.close()*/

                    // Create a PdfDocument with a page of the same size as the image
                    val document: PdfDocument = PdfDocument()
                    val pageInfo: PdfDocument.PageInfo  = PdfDocument.PageInfo.Builder(cropPic_pdf.width, cropPic_pdf.height, 1).create()
                    val page: PdfDocument.Page  = document.startPage(pageInfo)

                    // Draw the bitmap onto the page
                    val canvas: Canvas = page.canvas
                    canvas.drawBitmap(cropPic_pdf, 0f, 0f, null)
                    document.finishPage(page)
                    document.writeTo(FileOutputStream(file_pdf))
                    document.close()
                    
                    cropPic_pdf.recycle()
                    cropPic_png.recycle()
                    //cropPic_jpg.recycle()

                    addImageToGallery(file_png.absolutePath, this.context) //Commented as we don't want the images in the gallery.
                    //addImageToGallery(file_jpg.absolutePath, this.context) //Commented as we don't want the images in the gallery.
                    
                    //Toast.makeText(context, "picture saved, path: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
                    
                    //return file_pdf.absolutePath + "," + file_png.absolutePath + "," + file_jpg.absolutePath
                    return file_pdf.absolutePath + "," + file_png.absolutePath
                }
            }
        }
        return null
    }
}