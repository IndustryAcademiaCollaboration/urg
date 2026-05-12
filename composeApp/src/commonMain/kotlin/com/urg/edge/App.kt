package com.urg.edge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp



@Composable
fun App(
    prompt: String,
    response: String,
    isLoading: Boolean,
    onPromptChange: (String) -> Unit,
    onSendClick: () -> Unit

) {
    MaterialTheme {
        Column(

            modifier = Modifier

                .fillMaxSize()

                .padding(16.dp),

            verticalArrangement = Arrangement.spacedBy(16.dp)

        ) {

            Text(

                text = "Local LLM Demo"

            )

            OutlinedTextField(

                value = prompt,

                onValueChange = onPromptChange,

                modifier = Modifier.fillMaxWidth(),

                label = {

                    Text("Prompt")

                }

            )

            Button(

                onClick = onSendClick

            ) {

                Text("Send")

            }

            if (isLoading) {

                Text("Loading...")

            }

            Text(

                text = response

            )
        }
    }
}