package com.example.cognicare.core.di

import com.example.cognicare.repository.AuthRepository
import com.example.cognicare.repository.CareRepository
import com.example.cognicare.repository.RemoteAuthRepository
import com.example.cognicare.repository.RemoteCareRepository
import com.example.cognicare.repository.RemoteReminderRepository
import com.example.cognicare.repository.ReminderRepository
import com.example.cognicare.repository.RemoteVoiceRepository
import com.example.cognicare.repository.VoiceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: RemoteAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindCareRepository(impl: RemoteCareRepository): CareRepository

    @Binds
    @Singleton
    abstract fun bindVoiceRepository(impl: RemoteVoiceRepository): VoiceRepository

    @Binds
    @Singleton
    abstract fun bindReminderRepository(impl: RemoteReminderRepository): ReminderRepository
}
