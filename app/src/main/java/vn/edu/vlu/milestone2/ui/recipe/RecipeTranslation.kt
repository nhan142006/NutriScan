package vn.edu.vlu.milestone2.ui.recipe

import android.util.LruCache
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.common.MlKitException
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.tasks.await
import vn.edu.vlu.milestone2.data.local.entity.RecipeSource
import vn.edu.vlu.milestone2.data.model.Recipe

/** One model client per visible screen. Translations are display values only. */
internal class RecipeTranslation(fragment: Fragment) : DefaultLifecycleObserver {
    private val target = if (fragment.resources.configuration.locales[0].language == "vi") "vi" else "en"
    private val other = if (target == "vi") "en" else "vi"
    private val translator = Translation.getClient(
        TranslatorOptions.Builder().setSourceLanguage(other).setTargetLanguage(target).build()
    )
    private val identifier = LanguageIdentification.getClient()
    private val cache = LruCache<String, String>(400)
    private var modelReady = false

    init { fragment.viewLifecycleOwner.lifecycle.addObserver(this) }

    override fun onDestroy(owner: LifecycleOwner) {
        translator.close()
        identifier.close()
    }

    private suspend fun sourceLanguage(source: RecipeSource, sample: String): String = when (source) {
        RecipeSource.API -> "en"
        RecipeSource.AI -> "vi"
        RecipeSource.USER -> identifier.identifyLanguage(sample).await()
    }

    private suspend fun translate(text: String): String {
        if (text.isBlank()) return text
        cache.get(text)?.let { return it }
        if (!modelReady) {
            translator.downloadModelIfNeeded().await()
            modelReady = true
        }
        return translator.translate(text).await().also { cache.put(text, it) }
    }

    suspend fun title(item: RecipeCardItem): String = try {
        if (sourceLanguage(item.source, item.title) == other) translate(item.title) else item.title
    } catch (_: MlKitException) { item.title }

    suspend fun recipe(original: Recipe): Recipe = try {
        val sample = (listOf(original.title) + original.steps.take(2)).joinToString(" ")
        if (sourceLanguage(original.source, sample) == other) original.copy(
            title = translate(original.title),
            ingredients = original.ingredients.map { translate(it) },
            steps = original.steps.map { translate(it) }
        ) else original
    } catch (_: MlKitException) { original }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal fun <T> Flow<T>.withRecipeTranslation(localize: suspend (T) -> T): Flow<T> =
    transformLatest { original ->
        emit(original)
        val translated = localize(original)
        if (translated != original) emit(translated)
    }
