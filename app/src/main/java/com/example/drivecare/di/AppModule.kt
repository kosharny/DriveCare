package com.example.drivecare.di

import android.content.Context
import androidx.room.Room
import com.example.drivecare.data.local.DriveCareDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): DriveCareDatabase =
        Room.databaseBuilder(context, DriveCareDatabase::class.java, "drivecare.db")
            .addMigrations(DriveCareDatabase.MIGRATION_1_2)
            .build()
}
