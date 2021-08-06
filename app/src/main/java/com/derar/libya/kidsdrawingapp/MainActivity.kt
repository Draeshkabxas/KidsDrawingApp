package com.derar.libya.kidsdrawingapp

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_brush_size.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private var mImageButtonCurrentPaint: ImageButton? = null

    // A variable for current color is picked from color pallet.
    private lateinit var photoGalleryLauncher: ActivityResultLauncher<String>

    private lateinit var drawing_view: DrawingView

    private lateinit var mProgressDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializePhotoGalleryLauncher()

        drawing_view = findViewById(R.id.drawing_view)

        initializeProgressDialog()

        drawing_view.setSizeForBrush(20.toFloat()) // Setting the default brush size to drawing view.

        /**
         * This is to select the default Image button which is
         * active and color is already defined in the drawing view class.
         * As the default color is black so in our color pallet it is on 2 position.
         * But the array list start position is 0 so the black color is at position 1.
         */
        mImageButtonCurrentPaint = ll_paint_colors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.pallet_pressed
            )
        )

        ib_brush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        //TODO(Step 8 : Adding an click event to image button for selecting the image from gallery.)
        //START
        ib_gallery.setOnClickListener {
            //Very firstly we will check the app required a storage permission.
            // So we will add a permission in the Android.xml for storage.

            //First checking if the app is already having the permission
            if (isReadStorageAllowed()) {
                openGalleryLauncher()
                // If the permission is granted we will code here. But now let us just ask for the permission.
            } else {

                //If the app don't have storage access permission we will ask for it.
                requestStoragePermission()
            }
        }

        ib_undo.setOnClickListener {
            drawing_view.onClickUndo()
            Log.i(this.localClassName, "Undo work")
        }

        ib_save.setOnClickListener {
            if (isReadStorageAllowed()) {
                val bitmap = getBitmapFromView(fl_drawing_view_container)
                saveImageInBackground(bitmap)
            } else {
                requestStoragePermission()
            }
        }

        // END
    }

    private fun initializeProgressDialog() {
        mProgressDialog = Dialog(this)
        mProgressDialog.setContentView(R.layout.dialog_custom_progress)
    }

    private fun openGalleryLauncher() {
        photoGalleryLauncher.launch("image/*")
    }

    private fun initializePhotoGalleryLauncher() {
        photoGalleryLauncher = registerForActivityResult(GetContent()) { uri: Uri? ->
            // Handle the returned Uri
            if (uri != null) {
                iv_background.visibility = View.VISIBLE
                iv_background.setImageURI(uri)
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Error in parsing the image or its corrupted",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    //TODO(Step 6 - This method is override method which be executed when we update the status of the permission when we are ask to allow or deny.)
    //START
    /**
     * This is override method and the method will be called when the user will tap on allow or deny
     *
     * Determines whether the delegate should handle
     * {@link ActivityCompat#requestPermissions(Activity, String[], int)}, and request
     * permissions if applicable. If this method returns true, it means that permission
     * request is successfully handled by the delegate, and platform should not perform any
     * further requests for permission.
     *
     * @param activity The target activity.
     * @param permissions The requested permissions. Must me non-null and not empty.
     * @param requestCode Application specific request code to match with a result reported to
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        //Checking the request code of our request
        if (requestCode == STORAGE_PERMISSION_CODE) {

            //If permission is granted
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this@MainActivity,
                    "Permission granted now you can read the storage files.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                //Displaying another toast if permission is not granted
                Toast.makeText(
                    this@MainActivity,
                    "Oops you just denied the permission.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    //END

    /**
     * Method is used to launch the dialog to select different brush sizes.
     */
    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size :")
        val smallBtn = brushDialog.ib_small_brush
        smallBtn.setOnClickListener(View.OnClickListener {
            drawing_view.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        })
        val mediumBtn = brushDialog.ib_medium_brush
        mediumBtn.setOnClickListener(View.OnClickListener {
            drawing_view.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        })

        val largeBtn = brushDialog.ib_large_brush
        largeBtn.setOnClickListener(View.OnClickListener {
            drawing_view.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        })
        brushDialog.show()
    }

    /**
     * Method is called when color is clicked from pallet_normal.
     *
     * @param view ImageButton on which click took place.
     */
    fun paintClicked(view: View) {
        if (view !== mImageButtonCurrentPaint) {
            // Update the color
            val imageButton = view as ImageButton
            // Here the tag is used for swaping the current color with previous color.
            // The tag stores the selected view
            val colorTag = imageButton.tag.toString()
            // The color is set as per the selected tag here.
            drawing_view.setColor(colorTag)
            // Swap the backgrounds for last active and currently active image button.
            imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))
            mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.pallet_normal
                )
            )

            //Current view is updated with selected view in the form of ImageButton.
            mImageButtonCurrentPaint = view
        }
    }

    //TODO(Step 4 - For the first time you need to ask for the permission
    // for selecting the image from your phone or when it is not allowed it is when you are about to select an image from phone storage.)
    //START
    /**
     * Requesting permission
     */
    private fun requestStoragePermission() {

        /**
         * Gets whether you should show UI with rationale for requesting a permission.
         * You should do this only if you do not have the permission and the context in
         * which the permission is requested does not clearly communicate to the user
         * what would be the benefit from granting this permission.
         * <p>
         * For example, if you write a camera app, requesting the camera permission
         * would be expected by the user and no rationale for why it is requested is
         * needed. If however, the app needs location for tagging photos then a non-tech
         * savvy user may wonder how location is related to taking photos. In this case
         * you may choose to show UI with rationale of requesting this permission.
         * </p>
         *
         * @param activity The target activity.
         * @param permission A permission your app wants to request.
         * @return Whether you can show permission rationale UI.
         *
         */
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).toString()
            )
        ) {
            //If the user has denied the permission previously your code will come to this block
            //Here you can explain why you need this permission
            //Explain here why you need this permission
        }

        /**
         * Requests permissions to be granted to this application. These permissions
         * must be requested in your manifest, otherwise they will not be granted to your app.
         */

        //And finally ask for the permission
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            STORAGE_PERMISSION_CODE
        )
    }
    //END

    //TODO(Step 7 - After giving an permission in Manifest file check that is it allowed or not for selecting the image from your phone)
    //START
    /**
     * We are calling this method to check the permission status
     */
    private fun isReadStorageAllowed(): Boolean {
        //Getting the permission status
        // Here the checkSelfPermission is
        /**
         * Determine whether <em>you</em> have been granted a particular permission.
         *
         * @param permission The name of the permission being checked.
         *
         */
        val result = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE
        )

        /**
         *
         * @return {@link android.content.pm.PackageManager#PERMISSION_GRANTED} if you have the
         * permission, or {@link android.content.pm.PackageManager#PERMISSION_DENIED} if not.
         *
         */
        //If permission is granted returning true and If permission is not granted returning false
        return result == PackageManager.PERMISSION_GRANTED
    }
    //END

    //TODO(Step 5 - A unique code for asking the storage permission is declared in Companion Object.)
    //START
    companion object {


        /**
         * Permission code that will be checked in the method onRequestPermissionsResult
         *
         * For more Detail visit : https://developer.android.com/training/permissions/requesting#kotlin
         */
        private const val STORAGE_PERMISSION_CODE = 1
        //END
    }

    /**
     * Create bitmap from view and returns it
     */
    private fun getBitmapFromView(view: View): Bitmap {

        //Define a bitmap with the same size as the view.
        // CreateBitmap : Returns a mutable bitmap with the specified width and height
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        //Bind a canvas to it
        val canvas = Canvas(returnedBitmap)
        //Get the view's background
        val bgDrawable = view.background
        if (bgDrawable != null) {
            //has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas)
        } else {
            //does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.WHITE)
        }
        // draw the view on the canvas
        view.draw(canvas)
        //return the bitmap
        return returnedBitmap
    }

    private fun saveImageInBackground(mBitmap: Bitmap?) {
        CoroutineScope(IO).launch {
            showProgressDialog()
            var result: String = ""
            val saveJob = launch {
                if (mBitmap != null) {
                    try {
                        val bytes =
                            ByteArrayOutputStream() // Creates a new byte array output stream.
                        // The buffer capacity is initially 32 bytes, though its size increases if necessary.

                        mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                        /**
                         * Write a compressed version of the bitmap to the specified outputstream.
                         * If this returns true, the bitmap can be reconstructed by passing a
                         * corresponding inputstream to BitmapFactory.decodeStream(). Note: not
                         * all Formats support all bitmap configs directly, so it is possible that
                         * the returned bitmap from BitmapFactory could be in a different bitdepth,
                         * and/or may have lost per-pixel alpha (e.g. JPEG only supports opaque
                         * pixels).
                         *
                         * @param format   The format of the compressed image
                         * @param quality  Hint to the compressor, 0-100. 0 meaning compress for
                         *                 small size, 100 meaning compress for max quality. Some
                         *                 formats, like PNG which is lossless, will ignore the
                         *                 quality setting
                         * @param stream   The outputstream to write the compressed data.
                         * @return true if successfully compressed to the specified stream.
                         */

                        val f = File(
                            externalCacheDir!!.absoluteFile.toString()
                                    + File.separator + "KidDrawingApp_" + System.currentTimeMillis() / 1000 + ".jpg"
                        )
                        // Here the Environment : Provides access to environment variables.
                        // getExternalStorageDirectory : returns the primary shared/external storage directory.
                        // absoluteFile : Returns the absolute form of this abstract pathname.
                        // File.separator : The system-dependent default name-separator character. This string contains a single character.

                        val fo =
                            FileOutputStream(f) // Creates a file output stream to write to the file represented by the specified object.
                        fo.write(bytes.toByteArray()) // Writes bytes from the specified byte array to this file output stream.
                        fo.close() // Closes this file output stream and releases any system resources associated with this stream. This file output stream may no longer be used for writing bytes.
                        result = f.absolutePath // The file absolute path is return as a result.
                    } catch (e: Exception) {
                        result = ""
                        e.printStackTrace()
                    }
                }
            }
            saveJob.invokeOnCompletion {
                //Cancel ProgressDialog
                cancelProgressDialog()
                //Check if have any error 
                if (result.isEmpty()) {
                    showToastOnMainThread(
                        "Something went wrong while saving the file."
                    )
                } else {
                    showToastOnMainThread(
                        "File saved successfully :$result"
                    )
                }

                //Share the image
                MediaScannerConnection.scanFile(
                    this@MainActivity,
                    arrayOf(result),
                    null
                ) { path, uri ->
                    val shareIntent = Intent()
                    shareIntent.action = Intent.ACTION_SEND
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                    shareIntent.type = "image/png"
                    startActivity(
                        Intent.createChooser(shareIntent, "Share")
                    )
                }

            }
        }
    }

    private suspend fun showProgressDialog() {
        withContext(Main) {
            mProgressDialog.show()
        }

    }

    private fun cancelProgressDialog() {
        CoroutineScope(Main).launch {
            mProgressDialog.dismiss()
        }
    }

    private fun showToastOnMainThread(result: String) {
        CoroutineScope(Main).launch {
            Toast.makeText(
                this@MainActivity,
                result,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}