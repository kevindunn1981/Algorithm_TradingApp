package com.algotrader.app.data.repository

import com.algotrader.app.data.local.dao.StrategyDao
import com.algotrader.app.data.local.entity.StrategyEntity
import com.algotrader.app.data.model.Strategy
import com.algotrader.app.data.model.StrategyLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StrategyRepository @Inject constructor(
    private val strategyDao: StrategyDao
) {
    fun getAllStrategies(): Flow<List<Strategy>> =
        strategyDao.getAllStrategies().map { list -> list.map { it.toDomain() } }

    fun getActiveStrategies(): Flow<List<Strategy>> =
        strategyDao.getActiveStrategies().map { list -> list.map { it.toDomain() } }

    suspend fun getStrategyById(id: Long): Strategy? =
        strategyDao.getStrategyById(id)?.toDomain()

    suspend fun saveStrategy(strategy: Strategy): Long {
        val entity = strategy.toEntity()
        return strategyDao.insertStrategy(entity)
    }

    suspend fun updateStrategy(strategy: Strategy) {
        strategyDao.updateStrategy(strategy.toEntity())
    }

    suspend fun deleteStrategy(strategy: Strategy) {
        strategyDao.deleteStrategy(strategy.toEntity())
    }

    suspend fun setActive(id: Long, active: Boolean) {
        strategyDao.setStrategyActive(id, active)
    }

    suspend fun getStrategyCount(): Int = strategyDao.getStrategyCount()

    private fun StrategyEntity.toDomain() = Strategy(
        id = id,
        name = name,
        description = description,
        code = code,
        language = StrategyLanguage.valueOf(language),
        isActive = isActive,
        createdAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAt), ZoneId.systemDefault()),
        updatedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(updatedAt), ZoneId.systemDefault())
    )

    private fun Strategy.toEntity() = StrategyEntity(
        id = id,
        name = name,
        description = description,
        code = code,
        language = language.name,
        isActive = isActive,
        createdAt = createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        updatedAt = updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
}
