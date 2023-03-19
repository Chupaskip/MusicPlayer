package com.example.musicplayer.ui

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore.Audio.Media
import android.util.TypedValue
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.musicplayer.R
import com.example.musicplayer.databinding.ActivityMainBinding
import com.example.musicplayer.ui.fragments.PlayerFragment
import com.example.musicplayer.ui.models.Song
import com.example.musicplayer.ui.models.setImages
import kotlinx.coroutines.launch

private const val REQUEST_CODE = 1
private const val TAG = "MainActivity"


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    companion object {
        var songs: ArrayList<Song> = arrayListOf()
    }

    private lateinit var navController: NavController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNavigationMenu.setupWithNavController(navController)
        requestPermission()
        binding.mainActivityContainer.getConstraintSet(R.id.start)?.let { start->
        }
    }

    fun showOrHidePlayer(isVisible: Boolean = false) {
        supportFragmentManager.fragments.find { f -> f is PlayerFragment }.also {
            if (it == null)
                binding.mainActivityContainer.also { mainContainer ->
                    binding.fragmentContainerView.updateLayoutParams<MarginLayoutParams> {
                        val marginBottom = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                            0f,
                            resources.displayMetrics)
                        bottomMargin = marginBottom.toInt()
                    }
                } else {
                binding.fragmentContainerView.updateLayoutParams<MarginLayoutParams> {
                    val marginBottom = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        150f,
                        resources.displayMetrics)
                    bottomMargin = marginBottom.toInt()
                }
            }
        }
        recreate()
    }

    fun showPlayer(){
        val params = (binding.fragmentContainerView.layoutParams as ConstraintLayout.LayoutParams)
        params.setMargins(100,100,100,100)
        binding.fragmentContainerView.layoutParams = params
        setContentView(binding.root )
//        binding.fragmentContainerView.updateLayoutParams<MarginLayoutParams> {
//            val marginBottom = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
//                100f,
//                resources.displayMetrics)
//            bottomMargin = marginBottom.toInt()
//            recreate()
//        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return super.onSupportNavigateUp() || navController.navigateUp()
    }


    private fun requestPermission() {
        if (ContextCompat.checkSelfPermission(applicationContext,
                WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE)
        } else {
            songs = getSongs(this)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission is granted!", Toast.LENGTH_SHORT).show()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE)
            }
        }
    }

    @SuppressLint("Recycle")
    fun getSongs(context: Context): ArrayList<Song> {
        var tempSongs: ArrayList<Song> = arrayListOf()
        val uri: Uri = Media.EXTERNAL_CONTENT_URI
        val selection = StringBuilder("is_music != 0 AND title != ''")
        val projection: Array<String> = arrayOf(
            Media._ID,
            Media.DATA,
            Media.TITLE,
            Media.ARTIST,
            Media.ALBUM,
            Media.DURATION
        )
        val cursor: Cursor? = context.contentResolver.query(uri, projection,
            selection.toString(), null, null)
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val song = Song(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5)
                )

                tempSongs.add(song)
            }
            cursor.close()
            tempSongs.setImages()
        }
        return tempSongs
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