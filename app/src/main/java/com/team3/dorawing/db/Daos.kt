package com.team3.dorawing.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface DiagnosisDao {
    @Insert
    suspend fun insert(result: DiagnosisResult): Long

    @Query("SELECT * FROM DiagnosisResult ORDER BY takenAt DESC, id DESC LIMIT 1")
    suspend fun last(): DiagnosisResult?

    @Query("DELETE FROM DiagnosisResult")
    suspend fun deleteAll()
}

@Dao
interface GameSetDao {
    @Insert
    suspend fun insert(result: GameSetResult): Long

    // 최신 것부터
    @Query("SELECT * FROM GameSetResult WHERE gameType = :type ORDER BY playedAt DESC, id DESC LIMIT :n")
    suspend fun recent(type: String, n: Int): List<GameSetResult>

    @Query("SELECT * FROM GameSetResult WHERE gameType = :type AND playedAt >= :from AND playedAt < :to ORDER BY playedAt")
    suspend fun between(type: String, from: Long, to: Long): List<GameSetResult>

    @Query("SELECT * FROM GameSetResult WHERE gameType = :type ORDER BY playedAt")
    suspend fun allOf(type: String): List<GameSetResult>

    @Query("SELECT * FROM GameSetResult ORDER BY playedAt DESC LIMIT 1")
    suspend fun last(): GameSetResult?

    @Query("DELETE FROM GameSetResult")
    suspend fun deleteAll()
}

@Dao
interface AttendanceDao {
    @Upsert
    suspend fun upsert(attendance: Attendance)

    @Query("SELECT * FROM Attendance WHERE date = :date")
    suspend fun get(date: String): Attendance?

    // 이번 주 출석은 월요일과 일요일 날짜를 넘긴다
    @Query("SELECT * FROM Attendance WHERE date >= :from AND date <= :to ORDER BY date")
    suspend fun between(from: String, to: String): List<Attendance>

    @Query("DELETE FROM Attendance")
    suspend fun deleteAll()
}

@Dao
interface LevelStateDao {
    @Upsert
    suspend fun upsert(state: LevelState)

    // 자가진단 뒤 시작 레벨을 넣을 때. 이미 있으면 덮어쓰지 않는다
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfEmpty(state: LevelState)

    @Query("SELECT * FROM LevelState WHERE gameType = :type")
    suspend fun get(type: String): LevelState?

    @Query("SELECT * FROM LevelState")
    suspend fun all(): List<LevelState>

    @Query("DELETE FROM LevelState")
    suspend fun deleteAll()
}

@Dao
interface ChangeAlertDao {
    @Insert
    suspend fun insert(alert: ChangeAlert): Long

    @Query("SELECT * FROM ChangeAlert WHERE dismissed = 0 ORDER BY createdAt DESC")
    suspend fun active(): List<ChangeAlert>

    @Query("SELECT * FROM ChangeAlert WHERE gameType = :type ORDER BY createdAt DESC")
    suspend fun allOf(type: String): List<ChangeAlert>

    @Query("UPDATE ChangeAlert SET dismissed = 1 WHERE id = :id")
    suspend fun dismiss(id: Long)

    @Query("DELETE FROM ChangeAlert")
    suspend fun deleteAll()
}
