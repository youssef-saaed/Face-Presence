package com.yousefsaid04.facepresence

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.yousefsaid04.facepresence.db.AppDatabase
import com.yousefsaid04.facepresence.ui.theme.FacePresenceTheme
import com.yousefsaid04.facepresence.views.HomeScreen
import com.yousefsaid04.facepresence.views.StudentListScreen

enum class FacePresenceApp {
    Home,
    StudentsList
}

class MainActivity : ComponentActivity() {
    private val permissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (!isGranted) {
                    Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
                    this.finish()
                }
                Toast.makeText(this, getString(R.string.permission_granted), Toast.LENGTH_SHORT).show()
        }

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "FacePresenceDB"
        )
            .allowMainThreadQueries()
            .build()
        val students = db.studentDao().getAll()
        students.forEach { student ->
            student.attended = false
            db.studentDao().updateStudent(student)
        }


        val permissions = listOf(Manifest.permission.CAMERA)
        permissions.forEach{ permission ->
            when (PackageManager.PERMISSION_GRANTED)
            {
                ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) -> {

                }
                else -> {
                    permissionRequest.launch(permission)
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            FacePresenceTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = FacePresenceApp.Home.name
                ) {
                    composable (route = FacePresenceApp.Home.name) {
                        HomeScreen(
                            modifier = Modifier.fillMaxSize(),
                            navController = navController,
                            db = db
                        )
                    }
                    composable (route = FacePresenceApp.StudentsList.name){
                        StudentListScreen(
                            modifier = Modifier.fillMaxSize(),
                            db = db
                        )
                    }
                }
            }
        }
    }
}
