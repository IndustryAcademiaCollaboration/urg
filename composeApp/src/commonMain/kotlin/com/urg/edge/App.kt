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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.urg.edge.ui.CalmMode
import com.urg.edge.ui.ChatHistory
import com.urg.edge.ui.ChatScreen
import com.urg.edge.ui.HomeScreen
import com.urg.edge.ui.HurryMode
import com.urg.edge.ui.ModeSelect
import com.urg.edge.ui.PriorityScreen
import com.urg.edge.ui.Result
import com.urg.edge.ui.SafetyCheck
import com.urg.edge.ui.TriageScreen
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import urg.composeapp.generated.resources.Res
import urg.composeapp.generated.resources.ic_home
import urg.composeapp.generated.resources.ic_home_selected
import urg.composeapp.generated.resources.ic_map
import urg.composeapp.generated.resources.ic_map_selected
import urg.composeapp.generated.resources.ic_priority
import urg.composeapp.generated.resources.ic_priority_selected
import urg.composeapp.generated.resources.ic_settings
import urg.composeapp.generated.resources.ic_settings_selected
import urg.composeapp.generated.resources.ic_triage
import urg.composeapp.generated.resources.ic_triage_selected

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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) }
    var showSafetyCheck by remember { mutableStateOf(false) }
    var showModeSelect by remember { mutableStateOf(false) }
    var showCalmMode by remember { mutableStateOf(false) }
    var showHurryMode by remember { mutableStateOf(false) }
    var showChatHistory by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    var calmModeAnswers by remember { mutableStateOf<List<Boolean>>(emptyList()) }


    val navItems = listOf(
        NavItem("ホーム",     Res.drawable.ic_home,     Res.drawable.ic_home_selected),
        NavItem("トリアージ", Res.drawable.ic_triage,   Res.drawable.ic_triage_selected),
        NavItem("地図",      Res.drawable.ic_map,      Res.drawable.ic_map_selected),
        NavItem("優先度",    Res.drawable.ic_priority,  Res.drawable.ic_priority_selected),
        NavItem("設定",      Res.drawable.ic_settings,  Res.drawable.ic_settings_selected),
    )

    MaterialTheme {
        Scaffold(
            bottomBar = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // ナビゲーションバー
                    Column(modifier = Modifier.background(Color.White)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            navItems.forEachIndexed { index, item ->
                                val isSelected = selectedTab == index
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable {
                                            selectedTab = index
                                            showSafetyCheck = false
                                            showModeSelect = false
                                            showCalmMode = false
                                            showHurryMode = false
                                            showChatHistory = false
                                            showResult = false
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
                showChatHistory -> ChatHistory(
                    onBack = {
                        showChatHistory = false
                        showHurryMode = true
                    },
                    modifier = Modifier.padding(paddingValues)
                )
                showHurryMode -> HurryMode(
                    onBack = {
                        showHurryMode = false
                        showSafetyCheck = false
                        showModeSelect = false
                        selectedTab = 0
                    },
                    onHistoryClick = {
                        showChatHistory = true
                    },
                    onMicClick = onMicStart,
                    modifier = Modifier.padding(paddingValues)
                )
                showResult -> Result(
                    answers = calmModeAnswers,
                    onBack = {
                        showResult = false
                        showCalmMode = true
                    },
                    onHome = {
                        showResult = false
                        showCalmMode = false
                        showModeSelect = false
                        showSafetyCheck = false
                        selectedTab = 0
                    },
                    modifier = Modifier.padding(paddingValues)
                )
                showCalmMode -> CalmMode(
                    onBack = {
                        showCalmMode = false
                        showModeSelect = true
                    },
                    onComplete = { answers ->
                        calmModeAnswers = answers
                        showCalmMode = false
                        showResult = true
                    },
                    modifier = Modifier.padding(paddingValues)
                )
                showModeSelect -> ModeSelect(
                    onBack = {
                        showModeSelect = false
                        showSafetyCheck = true
                    },
                    onCalmClick = {
                        showModeSelect = false
                        showCalmMode = true
                    },
                    onHurryClick = {
                        showModeSelect = false
                        showHurryMode = true
                    },
                    modifier = Modifier.padding(paddingValues)
                )
                showSafetyCheck -> SafetyCheck(
                    onBack = {
                        showSafetyCheck = false
                        selectedTab = 0
                    },
                    onSafeClick = {
                        showSafetyCheck = false
                        showModeSelect = true
                        selectedTab = 1
                    },
                    onDangerClick = {
                        showSafetyCheck = false
                        showHurryMode = true
                    },
                    modifier = Modifier.padding(paddingValues)
                )
                selectedTab == 0 -> HomeScreen(
                    uiState = uiState,
                    onDisasterClick = { showSafetyCheck = true },
                    modifier = Modifier.padding(paddingValues)
                )
                selectedTab == 1 -> ChatScreen(
                    uiState = uiState,
                    onPromptChange = viewModel::updatePrompt,
                    onSendClick = viewModel::onSendClick,
                    onTriageClick = viewModel::startTriage,
                    onMicStart = onMicStart,
                    onMicStop = onMicStop,
                    onTestWavRecognize = onTestWavRecognize,
                    onTriageYes = viewModel::answerTriageYes,
                    onTriageNo = viewModel::answerTriageNo,
                    modifier = Modifier.padding(paddingValues)
                )
                selectedTab == 3 -> PriorityScreen(
                    onBack = { selectedTab = 0 },
                    modifier = Modifier.padding(paddingValues)
                )
                else -> {
                    Box(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("準備中...")
                    }
                }
            }
        }
    }
}
