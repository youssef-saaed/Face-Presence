package com.yousefsaid04.facepresence.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update

@Entity
data class Student (
    @PrimaryKey val id: Int,
    val name: String,
    val embeddings: String,
    var attended: Boolean
)

@Dao
interface StudentDao {
    @Query("SELECT * FROM student")
    fun getAll(): List<Student>

    @Update
    fun updateStudent(student: Student)

    @Delete
    fun deleteStudent(student: Student)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addStudent(student: Student)

}