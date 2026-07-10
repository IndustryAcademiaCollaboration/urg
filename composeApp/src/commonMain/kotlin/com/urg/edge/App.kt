package com.urg.edge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.urg.edge.ui.FirstAidGuideScreen
import com.urg.edge.ui.InitProgressBanner
import com.urg.edge.ui.ManualScreen
import com.urg.edge.ui.MapScreen
import com.urg.edge.ui.PriorityScreen
import com.urg.edge.ui.RecordingOverlay
import com.urg.edge.ui.SettingsScreen
import com.urg.edge.ui.TriageTabScreen
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import urg.composeapp.generated.resources.Res
import urg.composeapp.generated.resources.ic_guide
import urg.composeapp.generated.resources.ic_guide_selected
import urg.composeapp.generated.resources.ic_map
import urg.composeapp.generated.resources.ic_map_selected
import urg.composeapp.generated.resources.ic_more
import urg.composeapp.generated.resources.ic_more_selected
import urg.composeapp.generated.resources.ic_priority
import urg.composeapp.generated.resources.ic_priority_selected
import urg.composeapp.generated.resources.ic_triage
import urg.composeapp.generated.resources.ic_triage_selected

private val NavUnselectedColor = Color(0xFF788E98)
private val NavSelectedColor   = Color(0xFF25B1BF)

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
    val allChunks     by viewModel.allChunks.collectAsStateWithLifecycle()
    var selectedTab    by remember { mutableStateOf(0) }
    var focusedVictimId by remember { mutableStateOf<String?>(null) }
    var showVoiceInput by remember { mutableStateOf(false) }
    var showManual     by remember { mutableStateOf(false) }
    var showTriageChat by remember { mutableStateOf(false) }
    val isDisasterMode by viewModel.isDisasterMode.collectAsStateWithLifecycle()

    val navItems = listOf(
        NavItem("トリアージ", Res.drawable.ic_triage,   Res.drawable.ic_triage_selected),
        NavItem("優先度",    Res.drawable.ic_priority,  Res.drawable.ic_priority_selected),
        NavItem("地図",      Res.drawable.ic_map,       Res.drawable.ic_map_selected),
        NavItem("辞書",      Res.drawable.ic_guide,     Res.drawable.ic_guide_selected),
        NavItem("その他",    Res.drawable.ic_more,      Res.drawable.ic_more_selected),
    )

    fun resetOverlays() {
        showVoiceInput = false
        showManual     = false
        showTriageChat = false
    }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomElementPadding = 100.dp + navBarBottom

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

            // ── コンテンツエリア ────────────────────────────────────────
            val contentModifier = Modifier.fillMaxSize()

            when {
                showVoiceInput -> RecordingOverlay(
                    onClose = { showVoiceInput = false; onMicStop() },
                    modifier = contentModifier
                )
                showManual -> ManualScreen(
                    onBack = { showManual = false },
                    bottomPadding = bottomElementPadding,
                    modifier = contentModifier
                )
                selectedTab == 0 -> TriageTabScreen(
                    chatViewModel = viewModel,
                    showChat = showTriageChat,
                    onToggle = { showTriageChat = it },
                    uiState = uiState,
                    onPromptChange = viewModel::updatePrompt,
                    onSendClick = viewModel::onSendClick,
                    onVoiceInputClick = { onMicStart() },
                    onMicStart = onMicStart,
                    onMicStop = onMicStop,
                    onTestWavRecognize = onTestWavRecognize,
                    onTriageYes = viewModel::answerTriageYes,
                    onTriageNo = viewModel::answerTriageNo,
                    onNavigateToMap = { selectedTab = 2; showTriageChat = false },
                    victims = victims,
                    onScopeChange = viewModel::setChatScope,
                    bottomPadding = bottomElementPadding,
                    modifier = contentModifier
                )
                selectedTab == 1 -> PriorityScreen(
                    victims = victims,
                    victimNumbers = victimNumbers,
                    onBack = { selectedTab = 0 },
                    onUpdateNote = { id, note -> viewModel.updateVictimNote(id, note) },
                    onDeleteVictim = { id -> viewModel.deleteVictim(id) },
                    bottomPadding = bottomElementPadding,
                    onShowOnMap = { victimId ->
                        focusedVictimId = victimId
                        selectedTab = 2
                    },
                    modifier = contentModifier
                )
                selectedTab == 2 -> MapScreen(
                    victims = victims,
                    focusedVictimId = focusedVictimId,
                    bottomPadding = bottomElementPadding,
                    modifier = contentModifier
                )
                selectedTab == 3 -> FirstAidGuideScreen(
                    chunks = allChunks,
                    bottomPadding = bottomElementPadding,
                    modifier = contentModifier
                )
                else -> SettingsScreen(
                    onManualClick = { showManual = true },
                    bottomPadding = bottomElementPadding,
                    isDisasterMode = isDisasterMode,
                    onDisasterModeChange = viewModel::setDisasterMode,
                    modifier = contentModifier
                )
            }

            // ── モデル初期化バナー ──────────────────────────────────────
            if (uiState.showInitBanner) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                ) {
                    InitProgressBanner(
                        llmProgress = uiState.llmProgress,
                        sttProgress = uiState.sttProgress,
                        ttsProgress = uiState.ttsProgress,
                        mapProgress  = uiState.mapProgress,
                        expanded    = uiState.initBannerExpanded,
                        onToggle    = viewModel::toggleInitBanner,
                        onDismiss   = viewModel::dismissInitBanner,
                    )
                }
            }

            // ── 浮遊ピル型ナビバー ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                // ── ピル ──────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(top = 12.dp)
                        .shadow(elevation = 16.dp, shape = RoundedCornerShape(999.dp))
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .border(1.dp, Color(0xFFDDE3E8).copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    navItems.forEachIndexed { index, item ->
                        val isSelected = selectedTab == index
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = index; resetOverlays() }
                                .padding(vertical = 2.dp),
                        ) {
                            if (isSelected) {
                                Spacer(modifier = Modifier.size(22.dp))
                            } else {
                                Icon(
                                    painter = painterResource(item.icon),
                                    contentDescription = item.label,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.label,
                                fontSize = 12.sp,
                                color = if (isSelected) NavSelectedColor else NavUnselectedColor,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }

                // ── 選択中アイコン円（ピルの上に浮く）───────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    navItems.forEachIndexed { index, item ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            if (selectedTab == index) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .shadow(3.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(NavSelectedColor)
                                        .clickable { selectedTab = index; resetOverlays() },
                                ) {
                                    Icon(
                                        painter = painterResource(item.iconSelected),
                                        contentDescription = item.label,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
