package com.algorithmx.neoleaf.data.bookmark

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks", primaryKeys = ["uri", "pageIndex"])
data class Bookmark(
    val uri: String,
    val pageIndex: Int
)
