package com.algorithmx.neoleaf.data.bookmark

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE uri = :uri")
    fun getBookmarksForUri(uri: String): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: Bookmark)

    @Delete
    suspend fun delete(bookmark: Bookmark)
    
    @Query("DELETE FROM bookmarks WHERE uri = :uri AND pageIndex = :pageIndex")
    suspend fun deleteByPage(uri: String, pageIndex: Int)
}
