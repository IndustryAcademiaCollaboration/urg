package com.urg.edge.map

val prefectureToFile = mapOf(
    "北海道" to "hokkaido",
    "青森県" to "aomori",
    "岩手県" to "iwate",
    "宮城県" to "miyagi",
    "秋田県" to "akita",
    "山形県" to "yamagata",
    "福島県" to "fukushima",
    "茨城県" to "ibaraki",
    "栃木県" to "tochigi",
    "群馬県" to "gunma",
    "埼玉県" to "saitama",
    "千葉県" to "chiba",
    "東京都" to "tokyo",
    "神奈川県" to "kanagawa",
    "新潟県" to "niigata",
    "富山県" to "toyama",
    "石川県" to "ishikawa",
    "福井県" to "fukui",
    "山梨県" to "yamanashi",
    "長野県" to "nagano",
    "岐阜県" to "gifu",
    "静岡県" to "shizuoka",
    "愛知県" to "aichi",
    "三重県" to "mie",
    "滋賀県" to "shiga",
    "京都府" to "kyoto",
    "大阪府" to "osaka",
    "兵庫県" to "hyogo",
    "奈良県" to "nara",
    "和歌山県" to "wakayama",
    "鳥取県" to "tottori",
    "島根県" to "shimane",
    "岡山県" to "okayama",
    "広島県" to "hiroshima",
    "山口県" to "yamaguchi",
    "徳島県" to "tokushima",
    "香川県" to "kagawa",
    "愛媛県" to "ehime",
    "高知県" to "kochi",
    "福岡県" to "fukuoka",
    "佐賀県" to "saga",
    "長崎県" to "nagasaki",
    "熊本県" to "kumamoto",
    "大分県" to "oita",
    "宮崎県" to "miyazaki",
    "鹿児島県" to "kagoshima",
    "沖縄県" to "okinawa"
)

private const val GITHUB_RELEASES_BASE =
    "https://github.com/IndustryAcademiaCollaboration/urg/releases/download/map-tiles"

fun getPrefectureFileName(prefectureName: String): String? =
    prefectureToFile[prefectureName]

fun getMapDownloadUrl(fileName: String): String =
    "$GITHUB_RELEASES_BASE/$fileName.mbtiles"