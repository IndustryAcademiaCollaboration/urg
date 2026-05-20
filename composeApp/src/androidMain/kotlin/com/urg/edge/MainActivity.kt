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
import com.urg.edge.llm.LlmConfig
import com.urg.edge.llm.createLlmEngine
import com.urg.edge.stt.AudioRecorder
import com.urg.edge.stt.SttConfig
import com.urg.edge.stt.WavLoader
import com.urg.edge.stt.createSttEngine
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

        initLlmEngine()
        initKnowledgeRetriever()
        initSttEngine()
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

    private fun initLlmEngine() {
        try {
            val config = LlmConfig()
            chatViewModel.setLlmEngine(createLlmEngine(this, config), config)
            Log.d("LLM_INIT", "SUCCESS")
        } catch (e: Exception) {
            chatViewModel.addSystemMessage("${Strings.INIT_LLM_ERROR_PREFIX}${e.message}")
            Log.e("LLM_INIT", "FAILED", e)
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
