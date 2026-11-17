package vn.edu.vlu.milestone2.data.model

data class HealthProfile(
    val id: Long,
    val userId: String,
    val fullName: String,
    val age: Int,
    val dietGoal: String,
    val dailyCalorieTarget: Int,
    val conditions: List<String>,
    val allergies: List<String>,
    val updatedAt: Long
) {
    /** Mô tả ngắn gọn để đưa vào prompt Gemini. */
    fun toPromptText(): String = buildString {
        append("Tuổi: ").append(age).append('\n')
        append("Mục tiêu ăn uống: ").append(dietGoal.ifBlank { "không có" }).append('\n')
        append("Năng lượng mục tiêu: ").append(dailyCalorieTarget).append(" kcal/ngày\n")
        append("Bệnh lý: ").append(conditions.joinToString().ifBlank { "không có" }).append('\n')
        append("Dị ứng: ").append(allergies.joinToString().ifBlank { "không có" })
    }
}
