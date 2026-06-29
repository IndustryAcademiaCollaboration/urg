package com.urg.edge.map

// .rd5セグメントファイルのGoogle Drive設定
// TODO: 県ごとのMBTilesに対応する際は、ユーザーの県に必要な.rd5ファイルのみ
//       ダウンロードするよう変更する。現在は中部地方の全セグメントを取得している。
val rd5FileConfigs = listOf(
    Rd5FileConfig("E130_N30.rd5", "160L1QrkvsucxxMu6k0ZOtGwfN-7a5CtV"),
    Rd5FileConfig("E135_N30.rd5", "1n3nMXI-7bc_aV0fV75GpvAiVVewJ62Ai"),
    Rd5FileConfig("E135_N35.rd5", "1zmAtq5LLQHGK87Qrt_Isn2ix_v3FCd0v"),
    Rd5FileConfig("E135_N40.rd5", "1qjPI8Movd8E2R8_KzK9if6r0yXmbKc53"),
    Rd5FileConfig("E140_N30.rd5", "1LNz3GM5bJF7LmUuu4y2PJfdZn_W6BAH-"),
    Rd5FileConfig("E140_N35.rd5", "1gr1cX7i5ibFNb3rG4olVaWcFTcJZEvFr"),
    Rd5FileConfig("E140_N40.rd5", "1HHp2he4pVF1_QSm1ee8NhqHsHtFwA-QI")
)

data class Rd5FileConfig(
    val fileName: String,
    val googleDriveId: String
) {
    val downloadUrl: String
        get() = "https://drive.usercontent.google.com/download?id=$googleDriveId&export=download&confirm=t"
}