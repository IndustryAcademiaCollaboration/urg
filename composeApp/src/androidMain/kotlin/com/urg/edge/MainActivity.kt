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
import com.urg.edge.model.ModelInstaller
import com.urg.edge.model.VoiceModelAssets
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
import kotlinx.coroutines.tasks.await
import com.urg.edge.map.MapDownloadManager
import com.urg.edge.map.getPrefectureFileName
import kotlinx.coroutines.withContext
import android.app.AlertDialog
import android.content.Intent
import android.provider.Settings
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModels()
    private val audioRecorder = AudioRecorder()

    // マイク再要求用（handleMicStart から使用）
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            chatViewModel.addSystemMessage(Strings.ERROR_MIC_PERMISSION)
        }
    }

    // 起動時にマイク＋位置情報をまとめて要求
    private val requestInitialPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECORD_AUDIO] != true) {
            chatViewModel.addSystemMessage(Strings.ERROR_MIC_PERMISSION)
        }
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            initLocationTracking()
            initMapDownload()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        initRepository()
        initLlmEngine()
        initKnowledgeRetriever()
        initDisasterDetection()
        initLocationTracking()
        initMapDownload()
        initSttEngine()
        initTtsEngine()
        requestInitialPermissions()

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
        lifecycleScope.launch(Dispatchers.IO) {
            val database = DatabaseFactory(DatabaseDriverFactory(this@MainActivity)).createDatabase()
            val repository = TriageSessionRepositoryImpl(database)
            val session = repository.getLatestSession() ?: repository.startSession(latitude = null, longitude = null)
            withContext(Dispatchers.Main) {
                chatViewModel.setRepository(repository, session)
            }
        }
    }

    private fun initLlmEngine() {
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                chatViewModel.showInitBanner()
            }
            try {
                val modelFile = File(getExternalFilesDir(null), "models/${Strings.LLM_MODEL_FILE_NAME}")
                if (!modelFile.exists()) {
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
                } else {
                    // キャッシュ済み：即 100% に設定
                    withContext(Dispatchers.Main) {
                        chatViewModel.updateLlmProgress(1f)
                    }
                }
                val config = LlmConfig(modelPath = modelFile.absolutePath)
                val engine = createLlmEngine(config)
                withContext(Dispatchers.Main) {
                    chatViewModel.setLlmEngine(engine, config)
                    chatViewModel.updateLlmProgress(1f)
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
                            withContext(Dispatchers.Main) {
                                chatViewModel.updateLlmProgress(percent / 100f)
                            }
                        }
                    }
                }
            }
            tempFile.renameTo(destFile)
            withContext(Dispatchers.Main) {
                chatViewModel.updateLlmProgress(1f)
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
                val retriever = EmbeddingRetriever(this@MainActivity, chunks)
                chatViewModel.setChunks(chunks)
                chatViewModel.setRetriever(retriever)
                Log.d("EMBEDDING_INIT", "SUCCESS")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    chatViewModel.addSystemMessage("${Strings.INIT_ERROR_PREFIX}${e::class.simpleName}: ${e.message}")
                }
                Log.e("EMBEDDING_INIT", "FAILED", e)
            }
        }
    }

    private fun initLocationTracking() {
        val client = LocationServices.getFusedLocationProviderClient(this)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        val request = com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        ).setMinUpdateDistanceMeters(3f).build()

        client.requestLocationUpdates(
            request,
            object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    result.lastLocation?.let {
                        chatViewModel.setCurrentLocation(it.latitude, it.longitude)
                    }
                }
            },
            mainLooper
        )
    }

    private fun initMapDownload() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val downloadManager = MapDownloadManager(this@MainActivity)

                val saved = downloadManager.getDownloadedPrefecture()
                if (saved != null && downloadManager.isMbtilesDownloaded(saved)) {
                    withContext(Dispatchers.Main) {
                        chatViewModel.updateMapProgress(1f)
                    }
                    return@launch
                }

                if (ContextCompat.checkSelfPermission(
                        this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED) {
                    withContext(Dispatchers.Main) {
                        chatViewModel.updateMapProgress(1f)
                    }
                    return@launch
                }

                // ダウンロードが必要 → バナー表示、進捗を0にリセット
                withContext(Dispatchers.Main) {
                    chatViewModel.updateMapProgress(0f)
                    chatViewModel.showInitBanner()
                }

                val fusedClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)
                var loc = fusedClient.lastLocation.await()
                if (loc == null) {
                    val cts = CancellationTokenSource()
                    loc = fusedClient.getCurrentLocation(
                        com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cts.token
                    ).await()
                }
                val lat = loc?.latitude ?: return@launch
                val lng = loc?.longitude ?: return@launch

                val prefecture = downloadManager.getPrefectureFromLocation(lat, lng) ?: return@launch
                val fileName = getPrefectureFileName(prefecture) ?: return@launch

                val success = downloadManager.downloadMbtiles(fileName) { progress ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        chatViewModel.updateMapProgress(progress / 100f)
                    }
                }

                withContext(Dispatchers.Main) {
                    if (success) {
                        chatViewModel.updateMapProgress(1f)
                    } else {
                        chatViewModel.addSystemMessage("地図データのダウンロードに失敗しました")
                    }
                }
            } catch (e: Exception) {
                Log.w("MAP_INIT", "Map download failed: ${e.message}")
            }
        }
    }

    private val disasterModeManager by lazy { DisasterModeManager(this) }

    private fun initDisasterDetection() {
        lifecycleScope.launch {
            disasterModeManager.isDisasterMode.collect { enabled ->
                chatViewModel.setDisasterMode(enabled)
            }
        }
        DisasterCheckWorker.schedule(this)
        checkNlsPermission()
    }

    private fun checkNlsPermission() {
        val granted = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(packageName) == true
        if (!granted) {
            AlertDialog.Builder(this)
                .setTitle("緊急地震速報の自動検知")
                .setMessage("地震発生時に自動で災害モードへ切り替えるため、通知へのアクセスを許可してください。許可しなくても手動での切り替えは可能です。")
                .setPositiveButton("設定を開く") { _, _ ->
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                .setNegativeButton("後で設定する", null)
                .show()
        }
    }

    private fun initSttEngine() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val modelDir = File(getExternalFilesDir(null), VoiceModelAssets.STT_DIR)
                ModelInstaller.ensureFiles(
                    targetDir = modelDir,
                    files = VoiceModelAssets.sttFiles,
                    label = "STTモデル",
                    onStatus = { },
                    onProgress = { progress ->
                        withContext(Dispatchers.Main) {
                            chatViewModel.updateSttProgress(progress)
                        }
                    },
                )
                val engine = createSttEngine(this@MainActivity, SttConfig(), modelDir)
                chatViewModel.setSttEngine(engine)
                withContext(Dispatchers.Main) {
                    chatViewModel.updateSttProgress(1f)
                }
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
                val modelDir = File(getExternalFilesDir(null), VoiceModelAssets.TTS_DIR)
                ModelInstaller.ensureFiles(
                    targetDir = modelDir,
                    files = VoiceModelAssets.ttsFiles,
                    label = "TTSモデル",
                    onStatus = { },
                    onProgress = { progress ->
                        withContext(Dispatchers.Main) {
                            chatViewModel.updateTtsProgress(progress)
                        }
                    },
                )
                val engine = createTtsEngine(this@MainActivity, modelDir = modelDir)
                chatViewModel.setTtsEngine(engine)
                withContext(Dispatchers.Main) {
                    chatViewModel.updateTtsProgress(1f)
                }
                Log.d("TTS_INIT", "SUCCESS")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    chatViewModel.addSystemMessage("${Strings.INIT_TTS_ERROR_PREFIX}${e.message}")
                }
                Log.e("TTS_INIT", "FAILED", e)
            }
        }
    }

    private fun requestInitialPermissions() {
        val needed = buildList {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.RECORD_AUDIO)
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (needed.isNotEmpty()) {
            requestInitialPermissionsLauncher.launch(needed.toTypedArray())
        }
    }

    private fun handleMicStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED) {
            chatViewModel.addSystemMessage(Strings.ERROR_MIC_PERMISSION)
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        val started = audioRecorder.start { amplitude ->
            chatViewModel.updateMicAmplitude(amplitude)
        }
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