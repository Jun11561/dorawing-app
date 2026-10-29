package com.team3.dorawing.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DiagnosisResult::class, GameSetResult::class, Attendance::class, LevelState::class, ChangeAlert::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun diagnosisDao(): DiagnosisDao
    abstract fun gameSetDao(): GameSetDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun levelStateDao(): LevelStateDao
    abstract fun changeAlertDao(): ChangeAlertDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "dorawing.db")
                    // 개발 중에는 구조가 바뀌면 지우고 새로 만든다
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { instance = it }
            }
    }
}
