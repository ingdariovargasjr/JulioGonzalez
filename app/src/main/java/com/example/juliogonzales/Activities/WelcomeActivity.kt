package com.example.juliogonzales.Activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.net.toFile
import com.example.juliogonzales.R
import com.example.juliogonzales.Utils.GlideApp
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_welcome.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        btnLogOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        createReport.setOnClickListener {
            val ir = Intent(this@WelcomeActivity, NewReportActivity::class.java)
            startActivity(ir)
            finish()
        }
        btnLastReports.setOnClickListener {
            val intent = Intent(this, LatestReportActivity::class.java)
            startActivity(intent)
        }

        println("---------------------DEBUG--------------------------------------\n\n")
        pruebasFirebase()
    }

    fun pruebasFirebase(){
        println("----------------------------Pruebas Firebase---------------------------------")

        val id = "-MX4qOShEZXk1Dll_IEe"
        val hashMap = HashMap<String, Uri>()
        val storage = FirebaseStorage.getInstance().reference.child("images/$id/")


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

                                hashMap.put(pathTemp[3],it)
                                println("Asi queda el hashmap: ${hashMap.keys.size} - ${hashMap}")
                            }
                        }

                    }
                }
            }

        }


    }
}
