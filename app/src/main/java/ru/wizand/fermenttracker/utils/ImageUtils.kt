package ru.wizand.fermenttracker.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache

object ImageUtils {

    // LruCache для кэширования decoded bitmaps
    // Размер кэша = 1/8 доступной памяти приложения
    private val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    ) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            // Размер в kilobytes вместо количества элементов
            return bitmap.byteCount / 1024
        }
    }

    /**
     * Декодирует bitmap с оптимальным размером для экономии памяти.
     * Использует LruCache для кэширования.
     *
     * @param path Путь к файлу изображения
     * @param reqWidth Требуемая ширина в пикселях
     * @param reqHeight Требуемая высота в пикселях
     * @return Bitmap или null если файл не найден
     */
    fun decodeSampledBitmapFromFile(
        path: String,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        // Проверяем кэш
        memoryCache.get(path)?.let {
            return it
        }

        // Декодируем с оптимизацией
        return BitmapFactory.Options().run {
            // Сначала получаем размеры без загрузки в память
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, this)

            // Вычисляем inSampleSize
            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

            // Декодируем с оптимальным размером
            inJustDecodeBounds = false
            BitmapFactory.decodeFile(path, this)
        }?.also { bitmap ->
            // Сохраняем в кэш
            memoryCache.put(path, bitmap)
        }
    }

    /**
     * Вычисляет inSampleSize для уменьшения изображения.
     * Использует степени двойки для оптимальной производительности декодера.
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Вычисляем наибольший inSampleSize (степень 2), который сохраняет
            // размеры больше или равные требуемым
            while (halfHeight / inSampleSize >= reqHeight &&
                halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * Очищает кэш изображений (вызывать при низкой памяти).
     */
    fun clearCache() {
        memoryCache.evictAll()
    }

    /**
     * Удаляет конкретное изображение из кэша.
     */
    fun removeCachedBitmap(path: String) {
        memoryCache.remove(path)
    }
}