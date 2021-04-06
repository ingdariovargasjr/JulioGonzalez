package com.example.juliogonzales.Activities

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import com.example.juliogonzales.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_welcome.*
import java.io.File

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val path = File(applicationContext.filesDir,"images")
        path.mkdirs()
        println("PATH FILE: $path")

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

    }


}
