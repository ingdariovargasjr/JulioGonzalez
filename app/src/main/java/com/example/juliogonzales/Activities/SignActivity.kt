package com.example.juliogonzales.Activities

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.andrognito.flashbar.Flashbar
import com.bumptech.glide.Glide
import com.example.juliogonzales.Models.ReportModel
import com.example.juliogonzales.R
import com.example.juliogonzales.Utils.DrawingView
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_images_report.*
import kotlinx.android.synthetic.main.activity_question.*
import kotlinx.android.synthetic.main.activity_send_report.*
import kotlinx.android.synthetic.main.activity_sign.*
import java.io.File
import java.io.FileOutputStream
import java.util.*

class SignActivity : AppCompatActivity() {

    internal var storage: FirebaseStorage ?=null
    private var sharePath: Uri? = null
    lateinit var pic: File
    var UriMail: Uri? = null
    internal var storageReference: StorageReference?= null
    private lateinit var Question1Reference: DatabaseReference
    private val images: MutableList<String> = ArrayList()



    //Data to Send Email
    var reportId: String = ""
    var date: String = ""
    var mail: String = ""
    var imageUri: Uri? = null
    var imageUrl: String = ""
    var personArrival: String = ""
    var sealNumber: String = ""
    var personAfixed: String = ""
    var personVerified: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign)

        val drawingView = findViewById<DrawingView>(R.id.drawingView)
        val btn_ready = findViewById<LinearLayout>(R.id.btn_listo)
        val btn_trash = findViewById<ImageView>(R.id.btn_trash)

        getImages()
        imageUri

        // Get the id of the Report
        val bundle = intent.extras
        reportId = bundle?.get("reportID") as String
        date = bundle.get("date") as String
        mail = bundle.get("mail") as String
        personArrival = bundle.get("personArrival") as String
        sealNumber = bundle.get("sealNumber") as String
        personAfixed = bundle.get("personAfixed") as String
        personVerified = bundle.get("personVer") as String

        Log.d("DEBUGIDSIGN",reportId)

        storage = FirebaseStorage.getInstance()
        storageReference = storage!!.reference



        drawingView.setOnTouchListener { _, _ ->
            btn_ready.visibility = View.VISIBLE
            /* show dialog here */
            false
        }

        btn_trash.setOnClickListener {
            drawingView.clearCanvas()
            btn_ready.visibility = View.GONE
        }

        btn_ready?.setOnClickListener {
            btn_ready.visibility = View.GONE
            takeScreenshot()
        }
    }

    override fun onRestart() {
        val intent = Intent(this@SignActivity, WelcomeActivity::class.java)
        startActivity(intent)
        finish()
        super.onRestart()
    }

    private fun takeScreenshot() {
        layoutLine.visibility = View.GONE
        layoutUp.visibility = View.GONE
        val now = Date()
        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now)
        try {
            // image naming and path  to include sd card  appending name you choose for file
            val mPath = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpeg"

            // create bitmap screen capture
            val v1 = window.decorView.rootView
            v1.isDrawingCacheEnabled = true
            val bitmap = Bitmap.createBitmap(v1.drawingCache)
            v1.isDrawingCacheEnabled = false

            val imageFile = File(mPath)

            val outputStream = FileOutputStream(imageFile)
            val quality = 100
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.flush()
            outputStream.close()

            //setting screenshot in imageview
            val filePath = Uri.fromFile(imageFile)
            UriMail = filePath

            val ssbitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            iv!!.setImageBitmap(ssbitmap)
            sharePath = filePath
            uploadFile()
        } catch (e: Throwable) {
            // Several error may come out with file handling or DOM
            e.printStackTrace()
        }
    }

    private fun uploadFile() {
        if (sharePath != null){
            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle("Uploading...")
            progressDialog.show()

            val imageRef = storageReference!!.child("images/$reportId/Signature/"+UUID.randomUUID().toString())
            imageRef.putFile(sharePath!!)
                .addOnSuccessListener {
                    progressDialog.dismiss()
                    sendEmail()
                    Toast.makeText(this,"Sign Uploaded",Toast.LENGTH_SHORT).show()
//                    val intent = Intent(this@SignActivity, WelcomeActivity::class.java)
//                    startActivity(intent)
//                    finish()
                }
                .addOnFailureListener{
                    progressDialog.dismiss()

                    Toast.makeText(this,"Failed",Toast.LENGTH_SHORT).show()
                }
                .addOnProgressListener { taskSnapshot ->
                    val progress = 100.0 * taskSnapshot.bytesTransferred/taskSnapshot.totalByteCount
                    progressDialog.setMessage("Uploaded "+progress.toInt() + "%...")
                }
        }
    }


    fun getImages(){

        // Get the id of the Report
        val bundle = intent.extras
        val id = bundle?.get("reportID") as String

        val storage = FirebaseStorage.getInstance()
        val listRef = storage.reference.child("images/$id/extras")
        // WORKING CODE!
        // Create a reference to a file from a Google Cloud Storage URI

        val storageReference = FirebaseStorage.getInstance().reference

        listRef.listAll()
            .addOnSuccessListener { listResult ->
                listResult.prefixes.forEach { prefix ->
                    Log.d("Buenoprefix", prefix.toString())
                }

                listResult.items.forEach { item ->

                    images.add(item.toString())
                    val gsReference = storage.getReferenceFromUrl(item.toString())
                    gsReference.downloadUrl.addOnSuccessListener {
                        if(it != null){
                            imageUrl = it.toString()
                        }
                    }
                    Log.d("Bueno1", images.toString())
                    Log.d("Bueno2", item.toString())
                    Log.d("Bueno3", imageUrl)
                }

            }
            .addOnFailureListener {
                Log.d("Bueno", "Error")
            }

    }

    fun sendEmail(){
        // Initialize Database
        Question1Reference = FirebaseDatabase.getInstance().reference
            .child("Reportes")
            .child(reportId)
            .child("form")
        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get Post object and use the values to update the UI
                val post = dataSnapshot.getValue(ReportModel::class.java)
                // [START_EXCLUDE]
                post?.let {

                    var str = images.toString()
                    var delimiter1 = ","
                    val parts = str.split(delimiter1)

                    Log.d("DEBUGPARTS", parts.toString())

                    val listImages = images.toString()
                    val icon = getDrawable(R.drawable.image)
                    val propertyOf = it.propertyName
                    val vehicleNumber = it.vehicleNumber
                    val licence = it.licence
                    val personSecurity = it.securityPerson
                    val email = it.email
                    val uri = Uri.parse(sharePath.toString())

                    //Intent to send the email with the information
                    val emailintent = Intent(Intent.ACTION_SEND)
                    emailintent.data = Uri.parse("mailto:")
                    emailintent.type = "plain/text"
                    var strTo = arrayOf(mail)
                    emailintent.putExtra(Intent.EXTRA_EMAIL, strTo)
                    emailintent.putExtra(Intent.EXTRA_SUBJECT,"Reporte No. $reportId")
                    emailintent.putExtra(Intent.EXTRA_TEXT,
                            "Load Property of: \n" +
                                    " $propertyOf\n" +
                                    "\n"+
                                    "Vehicle's Economic Number\n" +
                                    " $vehicleNumber\n" +
                                    "\n"+
                                    "Licence plate\n" +
                                    " $licence\n" +
                                    "\n"+
                                    "Printed name of person who conducted security inspection upon arrival:\n" +
                                    " $personSecurity\n" +
                                    "\n"+
                                    "Email where the report will send\n" +
                                    " $email\n" +
                                    "\n"+
                                    "Date\n" +
                                    " $date\n" +
                                    "\n"+
                                    "Printed name of person who conducted follow up security inspection:\n" +
                                    " $personArrival\n" +
                                    "\n"+
                                    "Seal number(s) that was on container when it arrived at this facility:\n" +
                                    " $sealNumber\n" +
                                    "\n"+
                                    "Printed name of person who affixed seal(s):\n" +
                                    " $personAfixed\n" +
                                    "\n"+
                                    "Printed name of person who verified physical integrity of seal(s):\n" +
                                    " $personVerified\n" +
                                    "\n"
//                                    "Images: \n" +
//                                    " $imageUrl\n" +
//                                    "\n"
                        )
//                    emailintent.putExtra(Intent.EXTRA_STREAM,sharePath)
                    intent.putExtra(Intent.EXTRA_STREAM, sharePath)
                    Log.d("BuenoImageUrl", imageUrl)

                    if (emailintent.resolveActivity(this@SignActivity.packageManager) != null) {
                            startActivity(Intent.createChooser(emailintent,""))
                        }

                }


            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
            }
        }
        Question1Reference.addValueEventListener(postListener)
    }


}
