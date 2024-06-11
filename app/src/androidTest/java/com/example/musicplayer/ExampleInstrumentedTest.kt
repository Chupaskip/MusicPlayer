package com.example.musicplayer

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.musicplayer.db.SongDao
import com.example.musicplayer.db.SongEntity
import com.example.musicplayer.models.Song
import com.example.musicplayer.db.SongsDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDatabaseTest {

    private lateinit var database: SongsDatabase
    private lateinit var dao: SongDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().context
        database = Room.inMemoryDatabaseBuilder(context, SongsDatabase::class.java).build()
        dao = database.songDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveData() {
        // Создаем тестовую сущность
        val song = Song(
            "1",
            "/emulated/music",
            "The Less I Know The Better",
            "Tame Impala",
            "4:55",
            "235",
            "The Currents",
            "5"
        )
        val entity = SongEntity(
           idMemory =  song.id,
        )

        CoroutineScope(Dispatchers.IO).launch {
            // Вставляем данные в базу данных
            dao.insert(entity)
            // Получаем данные из базы данных
            val retrievedEntity = dao.getAllSongs()
            // Проверяем, что полученные данные соответствуют ожиданиям
            assertThat(retrievedEntity[0], `is`(equalTo(entity)))
        }
    }
}
