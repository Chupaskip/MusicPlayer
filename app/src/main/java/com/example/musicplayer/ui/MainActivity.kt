package com.example.musicplayer.ui

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.musicplayer.R
import com.example.musicplayer.databinding.ActivityMainBinding
import com.example.musicplayer.models.Song
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

    lateinit var navController: NavController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("TEST", Song::class.java)?.also {
                viewModel.setCurrentSong(it)
            }
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Song>("TEST")?.also {
                viewModel.setCurrentSong(it)
            }
        }
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNavigationMenu.setupWithNavController(navController)
        permissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val hasReadPermission = sdk33AndUp {
                    permissions[READ_MEDIA_AUDIO]
                } ?: permissions[READ_EXTERNAL_STORAGE]
                if (hasReadPermission == true) {
                    viewModel.isReadPermissionGranted.postValue(true)
                } else {
                    Toast.makeText(this,
                        "To get songs you need to allow read audio files",
                        Toast.LENGTH_SHORT).show()
                }

            }
        requestPermission()
        if (readPermissionGranted) {
            viewModel.isReadPermissionGranted.postValue(true)
        }
        viewModel.isBottomMenuVisible.observe(this){
            binding.bottomNavigationMenu.alpha = if (it) 1f else 0f
        }
        viewModel.isLogin.observe(this) {
            if (!it)
                return@observe
            try {
                navController.navigate(
                    R.id.action_authorizationFragment_to_songsFragment,
                    null,
                    NavOptions.Builder().setPopUpTo(R.id.authorizationFragment, true).build()
                )
            } catch (_: Exception) {

            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        this.intent = intent
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