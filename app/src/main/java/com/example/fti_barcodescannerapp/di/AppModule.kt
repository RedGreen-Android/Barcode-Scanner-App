package com.example.fti_barcodescannerapp.di

import android.content.Context
import androidx.room.Room
import com.example.fti_barcodescannerapp.data.local.AppDatabase
import com.example.fti_barcodescannerapp.data.local.ScanDao
import com.example.fti_barcodescannerapp.data.repo.ScanRepositoryImpl
import com.example.fti_barcodescannerapp.domain.repo.ScanRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDb(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "scanner.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideScanDao(db: AppDatabase): ScanDao = db.scanDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepoModule {

    @Binds
    @Singleton
    abstract fun bindScanRepository(impl: ScanRepositoryImpl): ScanRepository
}
