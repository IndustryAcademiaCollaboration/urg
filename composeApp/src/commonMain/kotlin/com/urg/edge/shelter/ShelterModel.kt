package com.urg.edge.shelter

// 施設の種別
enum class ShelterType(val displayName: String, val csvFileName: String) {
    EMERGENCY_SHELTER("指定緊急避難場所", "aiti_Designated_emergency_shelter.csv"),
    EVACUATION_CENTER("指定避難所", "aiti_designated_evacuation_center.csv"),
    FIRST_AID_STATION("救護所", "First-aidstation.csv"),
    HOSPITAL("病院", "hospital.csv")
}

// 施設データ
data class Shelter(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val type: ShelterType,
    val distanceMeters: Double = 0.0
)