package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        PostEntity::class,
        CommentEntity::class,
        FollowEntity::class,
        LikeEntity::class,
        ReportEntity::class
    ],
    version = 4, // Upgraded version for fresh setup with targetCommenterId and imageUrl/parentCommentId
    exportSchema = false
)
abstract class CyberDatabase : RoomDatabase() {
    abstract fun cyberDao(): CyberDao

    companion object {
        @Volatile
        private var INSTANCE: CyberDatabase? = null

        fun getDatabase(context: Context): CyberDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CyberDatabase::class.java,
                    "cyber_hideout_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
