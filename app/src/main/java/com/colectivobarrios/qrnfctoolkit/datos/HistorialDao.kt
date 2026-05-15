package com.colectivobarrios.qrnfctoolkit.datos

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistorialDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HistorialEntity)

    @Query("SELECT * FROM historial ORDER BY timestamp DESC")
    fun getAll(): Flow<List<HistorialEntity>>

    @Query("SELECT * FROM historial WHERE tipo LIKE :filter OR tipo LIKE :filter2 ORDER BY timestamp DESC")
    fun getFiltered(filter: String, filter2: String): Flow<List<HistorialEntity>>

    @Delete
    suspend fun delete(item: HistorialEntity)

    @Query("DELETE FROM historial")
    suspend fun clearAll()
}
