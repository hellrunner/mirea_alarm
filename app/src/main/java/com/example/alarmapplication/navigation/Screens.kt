package com.example.alarmapplication.navigation

/**
 * Перечисление доступных экранов в приложении.
 *
 * Это перечисление содержит идентификаторы всех основных экранов, которые используются для навигации
 * в рамках приложения. Каждый элемент перечисления представляет собой отдельный экран, доступный для пользователя.
 *
 * - `AlarmScreen`: экран будильника, где пользователь может установить и настроить будильники.
 * - `MusicScreen`: экран для управления музыкальным плеером.
 * - `ArticlesScreen`: экран для чтения рекомедаций.
 */
enum class Screens {
    AlarmScreen,
    MusicScreen,
    ArticlesScreen
}
