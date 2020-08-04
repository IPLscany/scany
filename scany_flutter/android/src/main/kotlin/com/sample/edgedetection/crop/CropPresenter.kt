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
            val pic = enhancedPicture
            if (null != pic) {
                val file1 = File(dir, "enhance_${SystemClock.currentThreadTimeMillis()}.pdf")
                val outStream = FileOutputStream(file1)
                pic.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                outStream.flush()
                outStream.close()

                val file2 = File(dir, "enhance_${SystemClock.currentThreadTimeMillis()}.png")
                val outStream2 = FileOutputStream(file2)
                //pic.compress(Bitmap.CompressFormat.PNG, 100, outStream2)
                outStream2.flush()
                outStream2.close()

                val file3 = File(dir, "enhance_${SystemClock.currentThreadTimeMillis()}.jpeg")
                val outStream3 = FileOutputStream(file3)
                //pic.compress(Bitmap.CompressFormat.JPEG, 100, outStream3)
                outStream3.flush()
                outStream3.close()

                // Create a PdfDocument with a page of the same size as the image
                val document: PdfDocument = PdfDocument()
                val pageInfo: PdfDocument.PageInfo  = PdfDocument.PageInfo.Builder(pic.width, pic.height, 1).create()
                val page: PdfDocument.Page  = document.startPage(pageInfo)

                // Draw the bitmap onto the page
                val canvas: Canvas = page.canvas
                canvas.drawBitmap(pic, 0f, 0f, null)
                document.finishPage(page)
                document.writeTo(FileOutputStream(file1))
                document.close()

                addImageToGallery(file2.absolutePath, this.context) //Commented as we don't want the images in the gallery.
                addImageToGallery(file3.absolutePath, this.context) //Commented as we don't want the images in the gallery.
                //Toast.makeText(context, "picture saved, path: ${file1.absolutePath}", Toast.LENGTH_SHORT).show()
                return file1.absolutePath + "," + file2.absolutePath + "," + file3.absolutePath

            } else {
                val cropPic = croppedBitmap
                val cropPic2 = croppedBitmap
                val cropPic3 = croppedBitmap
                if (null != cropPic && null != cropPic2 && null != cropPic3) {
                    val file1 = File(dir, "crop_${SystemClock.currentThreadTimeMillis()}.pdf")
                    val outStream = FileOutputStream(file1)
                    cropPic.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                    outStream.flush()
                    outStream.close()

                    val file2 = File(dir, "crop_${SystemClock.currentThreadTimeMillis()}.png")
                    val outStream2 = FileOutputStream(file2)
                    cropPic2.compress(Bitmap.CompressFormat.PNG, 100, outStream2)
                    outStream2.flush()
                    outStream2.close()

                    val file3 = File(dir, "crop_${SystemClock.currentThreadTimeMillis()}.jpeg")
                    val outStream3 = FileOutputStream(file3)
                    cropPic3.compress(Bitmap.CompressFormat.JPEG, 100, outStream3)
                    outStream3.flush()
                    outStream3.close()

                    // Create a PdfDocument with a page of the same size as the image
                    val document: PdfDocument = PdfDocument()
                    val pageInfo: PdfDocument.PageInfo  = PdfDocument.PageInfo.Builder(cropPic.width, cropPic.height, 1).create()
                    val page: PdfDocument.Page  = document.startPage(pageInfo)

                    // Draw the bitmap onto the page
                    val canvas: Canvas = page.canvas
                    canvas.drawBitmap(cropPic, 0f, 0f, null)
                    document.finishPage(page)
                    document.writeTo(FileOutputStream(file1))
                    document.close()
                    cropPic.recycle()
                    cropPic2.recycle()
                    cropPic3.recycle()

                    addImageToGallery(file2.absolutePath, this.context) //Commented as we don't want the images in the gallery.
                    addImageToGallery(file3.absolutePath, this.context) //Commented as we don't want the images in the gallery.
                    //addImageToGallery(file.absolutePath, this.context) //Commented as we don't want the images in the gallery.
                    //Toast.makeText(context, "picture saved, path: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
                    
                    return file1.absolutePath + "," + file2.absolutePath + "," + file3.absolutePath
                }
            }
        }
        return null
    }
}