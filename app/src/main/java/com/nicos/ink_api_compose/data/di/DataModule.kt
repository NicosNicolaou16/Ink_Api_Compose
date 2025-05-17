package com.nicos.ink_api_compose.data.di

import android.content.Context
import com.nicos.ink_api_compose.data.database.MyRoomDatabase
import com.nicos.ink_api_compose.data.repositories_impl.StrokeRepositoryImpl
import com.nicos.ink_api_compose.domain.StrokeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(ViewModelComponent::class, SingletonComponent::class)
object DataModule {

    @Provides
    fun initRoomDataBase(@ApplicationContext context: Context): MyRoomDatabase {
        return MyRoomDatabase.Companion.invoke(context)
    }


    @Provides
    fun provideDrawingRepository(myRoomDatabase: MyRoomDatabase): StrokeRepository {
        return StrokeRepositoryImpl(myRoomDatabase)
    }
}