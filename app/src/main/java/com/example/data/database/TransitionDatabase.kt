package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.MilestoneEntry
import com.example.data.model.PhotoEntry
import com.example.data.model.VoiceEntry

@Database(
    entities = [PhotoEntry::class, VoiceEntry::class, MilestoneEntry::class],
    version = 1,
    exportSchema = false
)
abstract class TransitionDatabase : RoomDatabase() {

    abstract fun photoDao(): PhotoDao
    abstract fun voiceDao(): VoiceDao
    abstract fun milestoneDao(): MilestoneDao

    companion object {
        @Volatile
        private var INSTANCE: TransitionDatabase? = null

        fun getDatabase(context: Context): TransitionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TransitionDatabase::class.java,
                    "open_transition_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
