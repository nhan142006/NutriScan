package vn.edu.vlu.milestone2.data.local

import androidx.room.TypeConverter
import vn.edu.vlu.milestone2.data.local.entity.ProfileItemType
import vn.edu.vlu.milestone2.data.local.entity.Rating
import vn.edu.vlu.milestone2.data.local.entity.RecipeSource
import vn.edu.vlu.milestone2.data.local.entity.ScanSource
import vn.edu.vlu.milestone2.data.local.entity.ScanStatus

/** Lưu enum dưới dạng TEXT để đọc được trực tiếp trong Database Inspector. */
class Converters {
    @TypeConverter fun ratingToString(value: Rating?): String? = value?.name
    @TypeConverter fun stringToRating(value: String?): Rating? = value?.let { enumValueOrNull<Rating>(it) }

    @TypeConverter fun statusToString(value: ScanStatus): String = value.name
    @TypeConverter fun stringToStatus(value: String): ScanStatus = enumValueOrNull<ScanStatus>(value) ?: ScanStatus.FAILED

    @TypeConverter fun sourceToString(value: ScanSource): String = value.name
    @TypeConverter fun stringToSource(value: String): ScanSource = enumValueOrNull<ScanSource>(value) ?: ScanSource.BARCODE

    @TypeConverter fun recipeSourceToString(value: RecipeSource): String = value.name
    @TypeConverter fun stringToRecipeSource(value: String): RecipeSource = enumValueOrNull<RecipeSource>(value) ?: RecipeSource.API

    @TypeConverter fun itemTypeToString(value: ProfileItemType): String = value.name
    @TypeConverter fun stringToItemType(value: String): ProfileItemType =
        enumValueOrNull<ProfileItemType>(value) ?: ProfileItemType.CONDITION
}

private inline fun <reified T : Enum<T>> enumValueOrNull(name: String): T? =
    enumValues<T>().firstOrNull { it.name == name }
