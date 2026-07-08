package com.urg.edge

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.urg.edge.ui.ManualScreen
import com.urg.edge.ui.MapScreen
import com.urg.edge.ui.PriorityScreen
import com.urg.edge.ui.RecordingOverlay
import com.urg.edge.ui.SettingsScreen
import com.urg.edge.ui.TriageTabScreen
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import urg.composeapp.generated.resources.Res
import urg.composeapp.generated.resources.ic_triage
import urg.composeapp.generated.resources.ic_triage_selected
import urg.composeapp.generated.resources.ic_map
import urg.composeapp.generated.resources.ic_map_selected
import urg.composeapp.generated.resources.ic_priority
import urg.composeapp.generated.resources.ic_priority_selected
import urg.composeapp.generated.resources.ic_more
import urg.composeapp.generated.resources.ic_more_selected

private val NavUnselectedColor = Color(0xFF788E98)
private val NavSelectedColor = Color(0xFF25B1BF)

private data class NavItem(
    val label: String,
    val icon: DrawableResource,
    val iconSelected: DrawableResource,
)

@Composable
fun App(
    viewModel: ChatViewModel,
    onMicStart: () -> Unit,
    onMicStop: () -> Unit,
    onTestWavRecognize: (String) -> Unit,
) {
    val uiState       by viewModel.uiState.collectAsStateWithLifecycle()
    val victims       by viewModel.victims.collectAsStateWithLifecycle()
    val victimNumbers by viewModel.victimNumbers.collectAsStateWithLifecycle()
    var selectedTab    by remember { mutableStateOf(0) }
    var showVoiceInput by remember { mutableStateOf(false) }
    var showManual     by remember { mutableStateOf(false) }
    var showTriageChat by remember { mutableStateOf(false) }

    // ─── ナビゲーションアイテム（4タブ） ─────────────────────────
    val navItems = listOf(
        NavItem("トリアージ", Res.drawable.ic_triage,    Res.drawable.ic_triage_selected),
        NavItem("優先度",    Res.drawable.ic_priority,   Res.drawable.ic_priority_selected),
        NavItem("地図",      Res.drawable.ic_map,        Res.drawable.ic_map_selected),
        NavItem("その他",    Res.drawable.ic_more,       Res.drawable.ic_more_selected),
    )

    fun resetOverlays() {
        showVoiceInput = false
        showManual     = false
        showTriageChat = false
    }

    MaterialTheme {
        Scaffold(
            bottomBar = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.background(Color.White)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            navItems.forEachIndexed { index, item ->
                                val isSelected = selectedTab == index
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable {
                                            selectedTab = index
                                            resetOverlays()
                                        }
                                        .padding(horizontal = 12.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (isSelected) item.iconSelected else item.icon
                                        ),
                                        contentDescription = item.label,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(30.dp)
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = item.label,
                                        fontSize = 10.sp,
                                        color = if (isSelected) NavSelectedColor else NavUnselectedColor,
                                    )
                                }
                            }
                        }
                    }

                    // 上部シャドウ
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-6).dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0x40000000),
                                    )
                                )
                            )
                    )
                }
            }
        ) { paddingValues ->
            when {
                // ── 録音オーバーレイ ───────────────────────────────────
                showVoiceInput -> RecordingOverlay(
                    onClose = {
                        showVoiceInput = false
                        onMicStop()
                    },
                    modifier = Modifier.padding(paddingValues)
                )

                // ── マニュアル画面 ─────────────────────────────────────
                showManual -> ManualScreen(
                    onBack = { showManual = false },
                    modifier = Modifier.padding(paddingValues)
                )

                // ── タブコンテンツ ─────────────────────────────────────
                // tab 0: トリアージ
                selectedTab == 0 -> TriageTabScreen(
                    chatViewModel = viewModel,
                    showChat = showTriageChat,
                    onToggle = { showTriageChat = it },
                    uiState = uiState,
                    onPromptChange = viewModel::updatePrompt,
                    onSendClick = viewModel::onSendClick,
                    onVoiceInputClick = {
                        onMicStart()
                    },
                    onMicStart = onMicStart,
                    onMicStop = onMicStop,
                    onTestWavRecognize = onTestWavRecognize,
                    onTriageYes = viewModel::answerTriageYes,
                    onTriageNo = viewModel::answerTriageNo,
                    onNavigateToMap = {
                        selectedTab = 2   // 地図は index 2 へ
                        showTriageChat = false
                    },
                    victims = victims,
                    onScopeChange = viewModel::setChatScope,
                    modifier = Modifier.padding(paddingValues)
                )

                // tab 1: 優先度
                selectedTab == 1 -> PriorityScreen(
                    victims = victims,
                    victimNumbers = victimNumbers,
                    onBack = { selectedTab = 0 },
                    onUpdateNote = { id, note -> viewModel.updateVictimNote(id, note) },
                    onDeleteVictim = { id -> viewModel.deleteVictim(id) },
                    modifier = Modifier.padding(paddingValues)
                )

                // tab 2: 地図
                selectedTab == 2 -> MapScreen(
                    modifier = Modifier.padding(paddingValues)
                )

                // tab 3: その他（設定）
                else -> SettingsScreen(
                    onManualClick = { showManual = true },
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}
