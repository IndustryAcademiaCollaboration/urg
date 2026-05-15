package com.urg.edge

import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.viewmodel.compose.viewModel

fun MainViewController() = ComposeUIViewController {
    val viewModel = viewModel { ChatViewModel() }
    App(viewModel)
}
