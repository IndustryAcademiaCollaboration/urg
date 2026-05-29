package com.urg.edge.tts

/**
 * テキストを音声に変換して再生する TTS エンジンの共通インターフェース。
 * STT 側の [com.urg.edge.stt.SttEngine] と対になる。
 * 音声合成・再生はプラットフォーム実装側に隠蔽し、commonMain を Android 非依存に保つ。
 */
interface TtsEngine {
    /** [text] を音声合成して再生する。再生完了までブロックするため、呼び出し側は IO ディスパッチャで実行する。 */
    fun speak(text: String)

    /** 再生中の音声を中断する。次の発話で前の再生を上書きする用途。 */
    fun stop()

    /** エンジンを解放する。 */
    fun close()
}
