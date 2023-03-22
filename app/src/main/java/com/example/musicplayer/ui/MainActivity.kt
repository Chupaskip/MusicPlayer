package com.example.musicplayer.ui

import android.Manifest.permission.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.musicplayer.R
import com.example.musicplayer.databinding.ActivityMainBinding
import com.example.musicplayer.ui.fragments.PlayerFragment
import com.example.musicplayer.ui.util.sdk29AndUp
import com.example.musicplayer.ui.util.sdk33AndUp
import dagger.hilt.android.AndroidEntryPoint

private const val REQUEST_CODE = 1
private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private val viewModel: MusicViewModel by viewModels()
    private var readPermissionGranted = false
    private var writePermissionGranted = false
    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>

    private lateinit var navController: NavController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNavigationMenu.setupWithNavController(navController)
        permissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
               val hasReadPermission = sdk33AndUp {
                   permissions[READ_MEDIA_AUDIO]
               }?:permissions[READ_EXTERNAL_STORAGE]
                if(hasReadPermission==true){
                    viewModel.getSongs(this)
                }else{
                    Toast.makeText(this, "To get songs you need to allow read audio files", Toast.LENGTH_SHORT).show()
                }

            }
        requestPermission()
        if(readPermissionGranted){
            viewModel.getSongs(this)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return super.onSupportNavigateUp() || navController.navigateUp()
    }

    private fun requestPermission() {
        setInfoAboutPermissions()
        val permissionsToRequest = mutableListOf<String>()
        if (!readPermissionGranted) {
            sdk33AndUp { permissionsToRequest.add(READ_MEDIA_AUDIO) } ?: permissionsToRequest.add(
                READ_EXTERNAL_STORAGE)
        }
        if (!writePermissionGranted) {
            sdk29AndUp {
                permissionsToRequest.add(WRITE_EXTERNAL_STORAGE)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
        setInfoAboutPermissions()
    }

    private fun setInfoAboutPermissions() {
        val hasReadPermission = sdk33AndUp {
            ContextCompat.checkSelfPermission(applicationContext,
                READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } ?: (ContextCompat.checkSelfPermission(applicationContext,
            READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        val hasWritePermission = ContextCompat.checkSelfPermission(applicationContext,
            WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val isSdk29AndUp = sdk29AndUp { true } ?: false
        readPermissionGranted = hasReadPermission
        writePermissionGranted = hasWritePermission || isSdk29AndUp
    }

    override fun onBackPressed() {
        supportFragmentManager.fragments.find { f -> f is PlayerFragment }.also {
            if (it == null) {
                onBackPressedDispatcher.onBackPressed()
                return
            }
            it as PlayerFragment
            if (it.binding.mainPlayerContainer.currentState == R.id.start)
                it.binding.mainPlayerContainer.transitionToStart()
            else
                onBackPressedDispatcher.onBackPressed()
        }
    }

}