package com.example.juliogonzales.Activities

import android.app.ProgressDialog
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.juliogonzales.Models.ReportModel
import com.example.juliogonzales.R
import com.example.juliogonzales.Utils.DrawingView
import com.example.juliogonzales.Utils.Utils
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.pspdfkit.document.PdfDocumentLoader
import com.pspdfkit.document.html.HtmlToPdfConverter
import kotlinx.android.synthetic.main.activity_images_report.*
import kotlinx.android.synthetic.main.activity_question.*
import kotlinx.android.synthetic.main.activity_send_report.*
import kotlinx.android.synthetic.main.activity_sign.*
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.thread


class SignActivity : AppCompatActivity() {

    internal var storage: FirebaseStorage ?=null
    private var sharePath: Uri? = null
    lateinit var pic: File
    var UriMail: Uri? = null
    internal var storageReference: StorageReference?= null
    private lateinit var Question1Reference: DatabaseReference
    private val images: MutableList<String> = ArrayList()
    private lateinit var hashMap: HashMap<String, Uri>
    private var canContinue = false




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
    var imagesURL: String = ""
    lateinit var utils: Utils


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign)


        val drawingView = findViewById<DrawingView>(R.id.drawingView)
        val btn_ready = findViewById<LinearLayout>(R.id.btn_listo)
        val btn_trash = findViewById<ImageView>(R.id.btn_trash)
        utils = Utils(this, this)
        utils.checkNeedPermissions()
        hashMap = HashMap()

        // Get the id of the Report
        val bundle = intent.extras
        reportId = bundle?.get("reportID") as String
        date = bundle.get("date") as String
        mail = bundle.get("mail") as String
        personArrival = bundle.get("personArrival") as String
        sealNumber = bundle.get("sealNumber") as String
        personAfixed = bundle.get("personAfixed") as String
        personVerified = bundle.get("personVer") as String

        Log.d("DEBUGIDSIGN", reportId)

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
            val permissions = utils.checkAndRequestPermissions()
        println("Are all the permissions granted ? --> $permissions")
        layoutLine.visibility = View.GONE
        layoutUp.visibility = View.GONE
        val now = Date()
        val signName = android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now).toString()

        try {
            // image naming and path  to include sd card  appending name you choose for file

            // create bitmap screen capture
            val v1 = window.decorView.rootView
            v1.isDrawingCacheEnabled = true
            val bitmap = Bitmap.createBitmap(v1.drawingCache)
            v1.isDrawingCacheEnabled = false
            val folder = File(Environment.getExternalStorageDirectory(), "JulioGonzales")
            folder.mkdirs()

            val cw = ContextWrapper(applicationContext)
            val dir = cw.getExternalFilesDir("images")

            val imageFile = File("${this.getExternalFilesDir("images")!!}${File.pathSeparator}$signName.jpg")
            if(!imageFile.exists()){
                val f = imageFile.createNewFile()
                println("Se crearon archivos: - $f")
            }


            //val imageFile = File("$mPath/JulioGonzales/$signName.jpg")
            println("------------Existe un nuevo archivo: ${imageFile.exists()} - ${imageFile.absolutePath}")
            if(imageFile.exists()){
                val del = imageFile.delete()
                val created = imageFile.createNewFile()
                println("IS Deleted? $del\t Is created? $created")
            }

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

            val imageRef = storageReference!!.child(
                "images/$reportId/Signature/" + UUID.randomUUID().toString() + ".jpeg"
            )
            imageRef.putFile(sharePath!!)
                .addOnSuccessListener {
                    getImages()
                    progressDialog.dismiss()
                    Toast.makeText(this, "Sign Uploaded", Toast.LENGTH_SHORT).show()
//                    val intent = Intent(this@SignActivity, WelcomeActivity::class.java)
//                    startActivity(intent)
//                    finish()
                    thread() {
                        Log.i("TAG", "this will be called after 3 seconds ${hashMap.isEmpty()}")
                        var i = 0
                        while(hashMap.isEmpty() && !canContinue){
                            println("Hashmap is empty -> ${hashMap.isEmpty()} - $i")
                            i++
                        }

                        sendEmail()
                    }

                }
                .addOnFailureListener{
                    progressDialog.dismiss()

                    Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                }
                .addOnProgressListener { taskSnapshot ->
                    val progress = 100.0 * taskSnapshot.bytesTransferred/taskSnapshot.totalByteCount
                    progressDialog.setMessage("Uploaded " + progress.toInt() + "%...")
                }
        }
    }


    fun getImages(){
        Log.d("TAG", "Enters Get Images")

        // Get the id of the Report
        val bundle = intent.extras
        val id = bundle?.get("reportID") as String

        val storage = FirebaseStorage.getInstance().reference.child("images/$id/")


        // WORKING CODE!
        // Create a reference to a file from a Google Cloud Storage URI

        val storageReference = FirebaseStorage.getInstance().reference
        //Log.d("SignActivity", "Buscando Imagenes $id, ${listRef == null} - ${listRef.toString()}")

        thread() {
            println("This is running on a different thread!!!!")
            storage.listAll().addOnSuccessListener { listResult ->
                println("listresult: ${listResult.prefixes}")
                listResult.prefixes.forEach { folder ->
                    println("Folder: ${folder.name}")
                    folder.listAll().addOnSuccessListener { result ->


                        result.items.forEach { image ->
                            val pathTemp = image.path.split("/")
                            image.downloadUrl.addOnSuccessListener {
                                if(it != null){
                                    print("Image Properties: ${image.name} ${image.path} -- ${pathTemp[3]}")
                                    println("\t / ${it.toString()} - ${it.path}")

                                    hashMap!!.put(pathTemp[3], it)
                                    println("Asi queda el hashmap: ${hashMap!!.keys.size} - ${hashMap!!}")
                                }
                            }

                        }
                    }
                }

            }.addOnFailureListener {
                Log.d("Bueno", "Error")
            }
            canContinue = true
        }

    }

    fun writeBytesAsJPEG(name: String, bytes: ByteArray) {
        val path = applicationContext.getExternalFilesDir(Environment.getExternalStorageState() + "/JulioGonzales")
        var success = true
        if(!path!!.exists()){
            success = path.mkdir()
        }
        if(success){
            var file = File.createTempFile(name, ".jpeg", path)
            var os = FileOutputStream(file);
            os.write(bytes);
            os.close();

        }
    }

    fun sendEmail(){
        // Initialize Database
        println("-------- ENTERS SEND EMAIL")
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

                    var imagesAndURL = ""
                    println()
                    hashMap!!.forEach { t, u ->
                        imagesAndURL += "<img src=\"${u}\" style=\"width: 50%; height: 50%;\" />\n"
                    }



                    var html = """
                        <!DOCTYPE html>
                        <html lang="en">
                        
                        <head>
                            <meta charset="UTF-8">
                            <meta http-equiv="X-UA-Compatible" content="IE=edge">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <link rel="preconnect" href="https://fonts.gstatic.com">
                            <link href="https://fonts.googleapis.com/css2?family=Roboto&display=swap" rel="stylesheet">
                            <title>Reporte</title>
                        </head>
                        
                        <body>
                            <h1>Report No. $reportId</h1>
                        
                            <ul>
                                <li>
                                    <h2>Load Property of: $propertyOf</h2>
                                    <h2>Vehicle's Economic Number: $vehicleNumber</h2>
                                    <h2>Licence Plate: $licence</h2>
                                    <h2>Printed name of person who conducted security inspection upon arrival: $personSecurity</h2>
                                    <h2>Email where the report will send $email</h2>
                                    <h2>Date: $date</h2>
                                    <h2>Printed name of person who conducted follow up security inspection: $personArrival</h2>
                                    <h2>Seal number(s) that was on container when it arrived at this facility: $sealNumber</h2>
                                    <h2>Printed name of person who affixed seal(s): $personAfixed</h2>
                                    <h2>Printed name of person who verified physical integrity of seal(s): $personVerified</h2>
                                    $imagesAndURL
                                </li>
                            </ul>
                        </body>
                        
                        </html>
                    """

                    println("#### Images and Url ###\n${imagesAndURL}")

                    val folder = Environment.getExternalStorageDirectory()
                    val f = File(folder, "JulioGonzales")
                    if(!f.exists()){ //Does not exists the folder
                        val m = f.mkdir()
                    }

                    //File
                    val file = File(
                        Environment.getExternalStorageDirectory().toString() + "/JulioGonzales",
                        "$reportId.pdf"
                    )
                    if(!file.exists()){
                        val create = file.createNewFile()

                        println("--------- se crea nuevo archivo ---------> $create")
                    }

                    HtmlToPdfConverter.fromHTMLString(this@SignActivity, html!!)
                        // Configure title for the created document.
                        .title("Converted document")
                        // Perform the conversion.
                        .convertToPdfAsync(file)
                        // Subscribe to the conversion result.
                        .subscribe({
                            // Open and process the converted document.
                            val document = PdfDocumentLoader.openDocument(
                                this@SignActivity, Uri.fromFile(
                                    file
                                )
                            )
                        }, {
                            println("...............................")
                            print(it.stackTrace)
                        })

                    val pdf = File(
                        Environment.getExternalStorageDirectory().toString() + "/JulioGonzales/",
                        "$reportId.pdf"
                    )
                    val pdfUri: Uri = FileProvider.getUriForFile(
                        applicationContext,
                        applicationContext.packageName + ".provider",
                        file
                    )

                    if(pdf.exists()){
                        println("------------------------ Existe -------------------------")
                        /*val shareIntent = Intent()
                        shareIntent.action = Intent.ACTION_SEND
                        shareIntent.type = "application/pdf"

                        val resInfoList: List<ResolveInfo> = getPackageManager().queryIntentActivities(
                                shareIntent,
                                PackageManager.MATCH_DEFAULT_ONLY
                            )
                        println("Print res info list: ${resInfoList.size}")
                        for (resolveInfo in resInfoList) {
                            val packageName = resolveInfo.activityInfo.packageName
                            grantUriPermission(
                                packageName,
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }

                        shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri)
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                        shareIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        startActivity(Intent.createChooser(shareIntent, "Share via"))*/

                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(pdfUri,"application/pdf")
                        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent)
                    } else {
                        println("------------------------ !Existe -------------------------")
                    }


                    /*val intent = Intent(Intent.ACTION_SEND)
                    intent.setType("application/image")
                    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(mail))
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Reporte No. $reportId")
                    intent.putExtra(Intent.EXTRA_STREAM, hashMap.get("Signature"))
                    intent.putExtra(
                        Intent.EXTRA_TEXT,
                        "Load Property of: \n" +
                                " $propertyOf\n" +
                                "\n" +
                                "Vehicle's Economic Number\n" +
                                " $vehicleNumber\n" +
                                "\n" +
                                "Licence plate\n" +
                                " $licence\n" +
                                "\n" +
                                "Printed name of person who conducted security inspection upon arrival:\n" +
                                " $personSecurity\n" +
                                "\n" +
                                "Email where the report will send\n" +
                                " $email\n" +
                                "\n" +
                                "Date\n" +
                                " $date\n" +
                                "\n" +
                                "Printed name of person who conducted follow up security inspection:\n" +
                                " $personArrival\n" +
                                "\n" +
                                "Seal number(s) that was on container when it arrived at this facility:\n" +
                                " $sealNumber\n" +
                                "\n" +
                                "Printed name of person who affixed seal(s):\n" +
                                " $personAfixed\n" +
                                "\n" +
                                "Printed name of person who verified physical integrity of seal(s):\n" +
                                " $personVerified\n" +
                                "\n" +
                                "Evidence URLs:\n"+
                                " $imagesAndURL\n"
                        /* +
                                "Images URL: \n" +
                                " ${listImages.toString()}\n" +
                                "\n"*/
                    )
                    startActivity(Intent.createChooser(intent, "Send mail..."));


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
                                    "\n" +
                                    "Evidence URLs:\n"+
                                    "$imagesAndURL\n"
                        )
//                    emailintent.putExtra(Intent.EXTRA_STREAM,sharePath)
                    emailintent.putExtra(Intent.EXTRA_STREAM, sharePath)
                    Log.d("BuenoImageUrl", imageUrl)
                    println("----------------- Sign Report Activity ------------------------")
                    println("ImageURL: $listImages")
                    if (emailintent.resolveActivity(this@SignActivity.packageManager) != null) {
                            startActivity(Intent.createChooser(emailintent,""))
                        }*/

                }


            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
            }
        }
        Question1Reference.addValueEventListener(postListener)
    }


}




