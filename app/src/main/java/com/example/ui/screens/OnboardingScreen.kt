package com.example.ui.screens

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AppleBlue
import com.example.ui.theme.BorderGray
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SurfaceLayer
import com.example.ui.theme.SystemGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.util.PermissionUtils
import com.example.ui.viewmodel.SalimViewModel

@Composable
fun OnboardingScreen(
    viewModel: SalimViewModel,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }

    val keyValidationInProgress by viewModel.keyValidationInProgress.collectAsStateWithLifecycle()
    val keyValidationResult by viewModel.keyValidationResult.collectAsStateWithLifecycle()

    var apiKeyInput by remember { mutableStateOf("") }

    // Permission Launchers
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        currentStep = 1
    }
    val contactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        currentStep = 2
    }
    val calendarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        currentStep = 3
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            ) {
                for (i in 0..3) {
                    Box(
                        modifier = Modifier
                            .size(width = if (i == currentStep) 24.dp else 8.dp, height = 8.dp)
                            .background(
                                if (i == currentStep) AppleBlue else BorderGray,
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
            }

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding_steps",
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    0 -> OnboardingStepCard(
                        icon = Icons.Outlined.Mic,
                        title = "Voice Commands",
                        subtitle = "salim converts spoken instructions directly to on-device actions. Speech is recognized using your device's native speech engine.",
                        permissionReason = "Requires Microphone access (RECORD_AUDIO). You can also type commands at any time.",
                        buttonText = "Allow Microphone",
                        onAction = {
                            if (PermissionUtils.checkPermission(context, Manifest.permission.RECORD_AUDIO)) {
                                currentStep = 1
                            } else {
                                micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onSkip = { currentStep = 1 }
                    )

                    1 -> OnboardingStepCard(
                        icon = Icons.Outlined.Contacts,
                        title = "Contact Resolution",
                        subtitle = "Recognize names in your contacts like \"Mom\", \"Sarah\", or \"Dr. Smith\" to look up their phone numbers safely.",
                        permissionReason = "Contacts are read locally on your phone and are never uploaded or synced to external servers.",
                        buttonText = "Allow Contacts Access",
                        onAction = {
                            if (PermissionUtils.checkPermission(context, Manifest.permission.READ_CONTACTS)) {
                                currentStep = 2
                            } else {
                                contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        },
                        onSkip = { currentStep = 2 }
                    )

                    2 -> OnboardingStepCard(
                        icon = Icons.Outlined.CalendarToday,
                        title = "Calendar Scheduling",
                        subtitle = "Schedule meetings and events seamlessly. salim checks your existing schedule to alert you about potential duplicates.",
                        permissionReason = "Requires Calendar read and write access to place verified events directly onto your device calendar.",
                        buttonText = "Allow Calendar Access",
                        onAction = {
                            if (PermissionUtils.checkPermission(context, Manifest.permission.WRITE_CALENDAR)) {
                                currentStep = 3
                            } else {
                                calendarLauncher.launch(Manifest.permission.WRITE_CALENDAR)
                            }
                        },
                        onSkip = { currentStep = 3 }
                    )

                    3 -> Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(SurfaceLayer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Key,
                                contentDescription = null,
                                tint = AppleBlue,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Connect Groq API Key",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "salim uses Groq's high-speed endpoint to interpret natural language commands into structured JSON actions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = {
                                apiKeyInput = it
                                viewModel.clearKeyValidationResult()
                            },
                            placeholder = { Text("Paste your Groq API key (gsk_...)", color = TextTertiary) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_api_key_input"),
                            shape = RoundedCornerShape(16.dp),
                            colors = outlinedFieldColors(),
                            singleLine = true
                        )

                        if (keyValidationResult != null) {
                            val isSuccess = keyValidationResult == "SUCCESS"
                            Text(
                                text = if (isSuccess) "Key verified and securely saved!" else keyValidationResult.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSuccess) SystemGreen else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (apiKeyInput.isNotBlank()) {
                                    viewModel.validateAndSaveApiKey(apiKeyInput)
                                }
                                viewModel.completeOnboarding()
                                onComplete()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("onboarding_get_started_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppleBlue,
                                contentColor = PureWhite
                            )
                        ) {
                            if (keyValidationInProgress) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = PureWhite,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (apiKeyInput.isNotBlank()) "Connect & Start" else "Start (Set Key Later)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    permissionReason: String,
    buttonText: String,
    onAction: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(SurfaceLayer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppleBlue,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceLayer, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Text(
                text = permissionReason,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAction,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("onboarding_action_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppleBlue,
                contentColor = PureWhite
            )
        ) {
            Text(
                text = buttonText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("onboarding_skip_button")
        ) {
            Text(
                text = "Not Now",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
        }
    }
}
