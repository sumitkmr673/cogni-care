package com.example.cognicare.core.locale

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides access to localized strings and the active [Locale] for ViewModels,
 * repositories, and non-Composable classes that cannot observe Compose composition.
 *
 * Keeps state synchronized with the user's selected [AppLanguage] as emitted by SessionViewModel.
 */
@Singleton
class AppLocaleProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    var language: AppLanguage = AppLanguage.ENGLISH
        private set

    val locale: Locale
        get() = Locale.forLanguageTag(language.tag)

    @Volatile
    private var localizedContext: Context = createContextForLanguage(language)

    fun update(newLanguage: AppLanguage) {
        if (language == newLanguage) return
        language = newLanguage
        localizedContext = createContextForLanguage(newLanguage)
    }

    fun getString(@StringRes resId: Int): String =
        localizedContext.getString(resId)

    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String =
        localizedContext.getString(resId, *formatArgs)

    private fun createContextForLanguage(appLanguage: AppLanguage): Context {
        val targetLocale = Locale.forLanguageTag(appLanguage.tag)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(targetLocale)
        }
        return context.createConfigurationContext(configuration)
    }
}

/**
 * Overrides [LocalConfiguration] and [LocalContext] in the Compose tree so that
 * all UI composables within [content] resolve strings and resources according to [language].
 */
@Composable
fun LocalizedContent(
    language: AppLanguage,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val locale = remember(language) { Locale.forLanguageTag(language.tag) }
    val configuration = remember(language, context) {
        Configuration(context.resources.configuration).apply {
            setLocale(locale)
        }
    }
    val localizedContext = remember(context, configuration) {
        LocalizedContextWrapper(
            base = context,
            localizedContext = context.createConfigurationContext(configuration)
        )
    }

    CompositionLocalProvider(
        LocalConfiguration provides configuration,
        LocalContext provides localizedContext,
        content = content
    )
}

private class LocalizedContextWrapper(
    base: Context,
    private val localizedContext: Context
) : ContextWrapper(base) {
    override fun getResources(): Resources = localizedContext.resources
    override fun getAssets(): AssetManager = localizedContext.assets
}
