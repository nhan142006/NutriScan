package vn.edu.vlu.milestone2.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import vn.edu.vlu.milestone2.data.local.entity.HealthProfileItemEntity
import vn.edu.vlu.milestone2.data.local.entity.ProductEntity
import vn.edu.vlu.milestone2.data.local.entity.ProfileItemType
import vn.edu.vlu.milestone2.data.local.entity.RecipeEntity
import vn.edu.vlu.milestone2.data.local.entity.ScanResultEntity
import vn.edu.vlu.milestone2.data.local.model.ProfileWithItems
import vn.edu.vlu.milestone2.data.model.HealthProfile
import vn.edu.vlu.milestone2.data.model.LabelAnalysis
import vn.edu.vlu.milestone2.data.model.Nutrition
import vn.edu.vlu.milestone2.data.model.Product
import vn.edu.vlu.milestone2.data.model.ProductSummary
import vn.edu.vlu.milestone2.data.model.Recipe
import vn.edu.vlu.milestone2.data.model.ScanResult

private val stringListType = object : TypeToken<List<String>>() {}.type

internal fun Gson.toJsonList(items: List<String>): String = toJson(items)

internal fun Gson.fromJsonList(json: String?): List<String> =
    if (json.isNullOrBlank()) emptyList()
    else runCatching<List<String>?> { fromJson(json, stringListType) }.getOrNull().orEmpty()

internal fun ProfileWithItems.toDomain(): HealthProfile = HealthProfile(
    id = profile.id,
    userId = profile.userId,
    fullName = profile.fullName,
    age = profile.age,
    dietGoal = profile.dietGoal,
    dailyCalorieTarget = profile.dailyCalorieTarget,
    conditions = items.filter { it.type == ProfileItemType.CONDITION }.map { it.name }.sorted(),
    allergies = items.filter { it.type == ProfileItemType.ALLERGY }.map { it.name }.sorted(),
    updatedAt = profile.updatedAt
)

internal fun List<String>.toItems(type: ProfileItemType): List<HealthProfileItemEntity> =
    map(String::trim).filter(String::isNotEmpty).distinctBy { it.lowercase() }
        .map { HealthProfileItemEntity(profileId = 0, type = type, name = it) }

internal fun Product.toEntity(gson: Gson, now: Long): ProductEntity = ProductEntity(
    barcode = barcode,
    name = name,
    brand = brand,
    quantity = quantity,
    imageUrl = imageUrl,
    nutriScore = nutriScore,
    novaGroup = novaGroup,
    ingredients = ingredients,
    allergens = allergens.joinToString(","),
    nutritionJson = gson.toJson(nutrition),
    cachedAt = now
)

internal fun ProductEntity.toDomain(gson: Gson): Product = Product(
    barcode = barcode,
    name = name,
    brand = brand,
    quantity = quantity,
    imageUrl = imageUrl,
    nutriScore = nutriScore,
    novaGroup = novaGroup,
    ingredients = ingredients,
    allergens = allergens.split(',').map { it.trim() }.filter { it.isNotEmpty() },
    nutrition = runCatching { gson.fromJson(nutritionJson, Nutrition::class.java) }.getOrNull() ?: Nutrition()
)

internal fun ProductEntity.toSummary(): ProductSummary =
    ProductSummary(barcode = barcode, name = name, brand = brand, imageUrl = imageUrl, nutriScore = nutriScore)

internal fun ScanResultEntity.toDomain(gson: Gson, parseAnalysis: (String) -> LabelAnalysis?): ScanResult = ScanResult(
    scanId = scanId,
    productBarcode = productBarcode,
    imagePath = imagePath,
    productName = productName,
    ingredients = gson.fromJsonList(ingredientsJson),
    rating = rating,
    analysis = analysisJson?.let { json -> runCatching { parseAnalysis(json) }.getOrNull() },
    note = note,
    status = status,
    source = source,
    errorMessage = errorMessage,
    createdAt = createdAt
)

internal fun RecipeEntity.toDomain(gson: Gson): Recipe = Recipe(
    id = recipeId,
    title = title,
    imageUrl = imageUrl,
    category = category,
    ingredients = gson.fromJsonList(ingredientsJson),
    steps = gson.fromJsonList(stepsJson),
    source = source,
    fitReason = fitReason,
    basedOnScanIds = basedOnScanIds.orEmpty().split(',').mapNotNull { it.trim().toLongOrNull() },
    youtubeUrl = youtubeUrl,
    isSaved = isSaved,
    updatedAt = updatedAt
)

internal fun Recipe.toEntity(gson: Gson): RecipeEntity = RecipeEntity(
    recipeId = id,
    title = title,
    imageUrl = imageUrl,
    category = category,
    ingredientsJson = gson.toJsonList(ingredients),
    stepsJson = gson.toJsonList(steps),
    source = source,
    fitReason = fitReason,
    basedOnScanIds = basedOnScanIds.takeIf { it.isNotEmpty() }?.joinToString(","),
    youtubeUrl = youtubeUrl,
    isSaved = isSaved,
    updatedAt = updatedAt
)
