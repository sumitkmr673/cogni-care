package com.example.cognicare.ui.screens.patient.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cognicare.data.model.GameType
import com.example.cognicare.ui.components.BackTextButton
import com.example.cognicare.ui.components.IconCircle
import com.example.cognicare.ui.components.PatientScreen
import com.example.cognicare.ui.components.ScreenTitle
import com.example.cognicare.ui.components.titleRes

data class NamingObject(val icon: ImageVector, val name: String)

val objectsToName = listOf(
    NamingObject(Icons.Rounded.Pets, "Dog"),
    NamingObject(Icons.Rounded.DirectionsCar, "Car"),
    NamingObject(Icons.Rounded.Home, "House")
)

@Composable
fun ObjectNamingScreen(
    languageLabel: String,
    onBackToGames: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    val currentObject = objectsToName[currentIndex]

    PatientScreen(languageLabel = languageLabel) {
        BackTextButton(onClick = onBackToGames)
        Spacer(Modifier.height(12.dp))
        ScreenTitle(stringResource(GameType.OBJECT_NAMING.titleRes))
        Spacer(Modifier.height(24.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            Text(
                text = "What is the object on the screen?",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            
            // The object to name
            IconCircle(icon = currentObject.icon, size = 180.dp)

            Spacer(Modifier.height(40.dp))
            
            // Circular Speak Button at the bottom
            Button(
                onClick = { 
                    // Simulate correct answer and move to next
                    currentIndex = (currentIndex + 1) % objectsToName.size 
                },
                modifier = Modifier.size(160.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Hold to Speak", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
