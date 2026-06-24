package com.urg.edge

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.urg.edge.core.database.DatabaseDriverFactory
import com.urg.edge.core.database.DatabaseFactory
import com.urg.edge.llm.LlmConfig
import com.urg.edge.llm.createLlmEngine
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import com.urg.edge.stt.AudioRecorder
import com.urg.edge.stt.SttConfig
import com.urg.edge.stt.WavLoader
import com.urg.edge.stt.createSttEngine
import com.urg.edge.tts.createTtsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModels()
    private val audioRecorder = AudioRecorder()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            chatViewModel.addSystemMessage(Strings.ERROR_MIC_PERMISSION)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        initRepository()
        initLlmEngine()
        initKnowledgeRetriever()
        initSttEngine()
        initTtsEngine()
        requestMicPermission()

        setContent {
            App(
                viewModel = chatViewModel,
                onMicStart = ::handleMicStart,
                onMicStop = ::handleMicStop,
                onTestWavRecognize = ::handleTestWavRecognize,
            )
        }
    }

    private fun handleTestWavRecognize(path: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val samples = WavLoader.loadFromAssets(assets, path)
                withContext(Dispatchers.Main) {
                    chatViewModel.recognizeFromSamples(samples)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    chatViewModel.addSystemMessage("${Strings.ERROR_TEST_WAV_PREFIX}${e.message}")
                }
            }
        }
    }

    private fun initRepository() {
        val database = DatabaseFactory(DatabaseDriverFactory(this)).createDatabase()
        val repository = TriageSessionRepositoryImpl(database)
        val session = repository.startSession(latitude = null, longitude = null)
        chatViewModel.setRepository(repository, session)
    }

    private fun initLlmEngine() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val modelFile = File(getExternalFilesDir(null), "models/${Strings.LLM_MODEL_FILE_NAME}")
                if (!modelFile.exists()) {
                    // Try downloading; if it fails, show ADB install instructions instead of crashing
                    try {
                        downloadModel(modelFile)
                    } catch (downloadEx: Exception) {
                        Log.w("LLM_INIT", "Download failed: ${downloadEx.message}")
                        withContext(Dispatchers.Main) {
                            chatViewModel.addSystemMessage(
                                "⚠️ モデルの自動ダウンロードに失敗しました。\n" +
                                "PCから次のコマンドで手動インストールしてください:\n\n" +
                                "1. PCでモデルをダウンロード:\n" +
                                "   ${Strings.LLM_MODEL_DOWNLOAD_URL}\n\n" +
                                "2. ADBで端末に転送:\n" +
                                "   adb push model.litertlm \\\n" +
                                "   /sdcard/Android/data/com.urg.edge/files/models/model.litertlm\n\n" +
                                "3. アプリを再起動してください"
                            )
                        }
                        return@launch
                    }
                }
                val config = LlmConfig(modelPath = modelFile.absolutePath)
                val engine = createLlmEngine(config)
                withContext(Dispatchers.Main) {
                    chatViewModel.setLlmEngine(engine, config)
                    chatViewModel.addSystemMessage("✓ LLM 準備完了")
                    Log.d("LLM_INIT", "SUCCESS")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    chatViewModel.addSystemMessage("${Strings.INIT_LLM_ERROR_PREFIX}${e.message}")
                    Log.e("LLM_INIT", "FAILED", e)
                }
            }
        }
    }

    private suspend fun downloadModel(destFile: File) = withContext(Dispatchers.IO) {
        withContext(Dispatchers.Main) {
            chatViewModel.addSystemMessage("モデルをダウンロード中... 0%")
        }

        destFile.parentFile?.mkdirs()
        val tempFile = File(destFile.parent, "${destFile.name}.tmp")
        val startByte = if (tempFile.exists()) tempFile.length() else 0L

        val connection = URL(Strings.LLM_MODEL_DOWNLOAD_URL).openConnection() as HttpURLConnection
        connection.setRequestProperty("Range", "bytes=$startByte-")
        connection.connect()

        val totalBytes = connection.contentLengthLong + startByte
        var downloadedBytes = startByte
        var lastReportedPercent = -1

        try {
            connection.inputStream.use { input ->
                java.io.FileOutputStream(tempFile, true).use { output ->
                    val buffer = ByteArray(65536)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        val percent = if (totalBytes > 0) (downloadedBytes * 100 / totalBytes).toInt() else 0
                        if (percent != lastReportedPercent && percent % 5 == 0) {
                            lastReportedPercent = percent
                            val downloadedMb = downloadedBytes / 1024 / 1024
                            val totalMb = totalBytes / 1024 / 1024
                            withContext(Dispatchers.Main) {
                                chatViewModel.updateLastSystemMessage(
                                    "モデルをダウンロード中... $percent% ($downloadedMb MB / $totalMb MB)"
                                )
                            }
                        }
                    }
                }
            }
            tempFile.renameTo(destFile)
            withContext(Dispatchers.Main) {
                chatViewModel.updateLastSystemMessage("モデルのダウンロード完了")
            }
        } catch (e: Exception) {
            throw Exception("ダウンロード失敗（途中再開可能）: ${e.message}")
        } finally {
            connection.disconnect()
        }
    }

    private fun initKnowledgeRetriever() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val chunks = KnowledgeLoader.load(assets)
                withContext(Dispatchers.Main) {
                    chatViewModel.addSystemMessage(Strings.knowledgeBaseLoaded(chunks.size))
                }
                val retriever = EmbeddingRetriever(this@MainActivity, chunks)
                chatViewModel.setRetriever(retriever)
                withContext(Dispatchers.Main) {
                    chatViewModel.addSystemMessage(Strings.INIT_COMPLETE)
                }
                Log.d("EMBEDDING_INIT", "SUCCESS")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    chatViewModel.addSystemMessage("${Strings.INIT_ERROR_PREFIX}${e::class.simpleName}: ${e.message}")
                }
                Log.e("EMBEDDING_INIT", "FAILED", e)
            }
        }
    }

    private fun initSttEngine() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val engine = createSttEngine(this@MainActivity, SttConfig())
                chatViewModel.setSttEngine(engine)
                Log.d("STT_INIT", "SUCCESS")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    chatViewModel.addSystemMessage("${Strings.INIT_STT_ERROR_PREFIX}${e.message}")
                }
                Log.e("STT_INIT", "FAILED", e)
            }
        }
    }

    private fun initTtsEngine() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val engine = createTtsEngine(this@MainActivity)
                chatViewModel.setTtsEngine(engine)
                Log.d("TTS_INIT", "SUCCESS")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    chatViewModel.addSystemMessage("${Strings.INIT_TTS_ERROR_PREFIX}${e.message}")
                }
                Log.e("TTS_INIT", "FAILED", e)
            }
        }
    }

    private fun requestMicPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun handleMicStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED) {
            chatViewModel.addSystemMessage(Strings.ERROR_MIC_PERMISSION)
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        val started = audioRecorder.start()
        if (!started) {
            chatViewModel.addSystemMessage(Strings.ERROR_RECORDING_FAILED)
            return
        }
        chatViewModel.setListening(true)
    }

    private fun handleMicStop() {
        val samples = audioRecorder.stop()
        chatViewModel.setListening(false)
        chatViewModel.recognizeFromSamples(samples)
    }
}
