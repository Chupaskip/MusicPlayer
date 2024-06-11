package com.example.musicplayer.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.musicplayer.models.Song

@Dao
interface SongDao {
    // Метод для вставки или замены записи о песне в локальной БД.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: SongEntity)
    // Метод для удаления записи о песне из локальной БД.
    @Delete
    suspend fun delete(song: SongEntity)
    // Метод для получения списка всех песен из локальной БД.
    @Query("SELECT * FROM songs")
    suspend fun getAllSongs(): List<SongEntity>
    // Метод для получения информации о песне по ее идентификатору из локальной БД.
    @Query("select * from songs where idMemory=:id AND idUser=:idUser")
    suspend fun getSongById(id:String, idUser:Long): SongEntity?
    // Метод для регистрации нового пользователя в приложении.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun userSignUp(user: UserEntity)
    // Метод для входа пользователя в приложение по имени и паролю.
    @Query("SELECT * FROM users WHERE username = :username AND password = :password")
    suspend fun userLogin(username: String, password: String): UserEntity?
    // Метод для проверки существования пользователя с заданным именем в базе данных.
    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun userSameLogin(username: String): UserEntity?
    // Метод для получения информации о пользователе по его имени.
    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): UserEntity?
}
