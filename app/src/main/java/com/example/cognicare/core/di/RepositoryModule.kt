package com.example.cognicare.core.di

import com.example.cognicare.repository.AuthRepository
import com.example.cognicare.repository.CareRepository
import com.example.cognicare.repository.DemoCareRepository
import com.example.cognicare.repository.MockAuthRepository
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
    abstract fun bindAuthRepository(impl: MockAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindCareRepository(impl: DemoCareRepository): CareRepository
}
