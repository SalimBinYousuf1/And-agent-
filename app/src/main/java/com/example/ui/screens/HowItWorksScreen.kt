package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppleBlue
import com.example.ui.theme.BorderGray
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SurfaceLayer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HowItWorksScreen(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceLayer, CircleShape)
                        .testTag("how_it_works_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "How salim Works",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Cards
            HowItWorksCard(
                icon = Icons.Outlined.Speed,
                title = "1. Natural Language to Structured JSON",
                description = "When you speak or type a command, salim sends your prompt to Groq's high-speed inference endpoint with current device context (e.g. current date/time). The AI returns structured JSON describing the intended parameters (recipient, message text, event time)."
            )

            Spacer(modifier = Modifier.height(14.dp))

            HowItWorksCard(
                icon = Icons.Outlined.Shield,
                title = "2. Closed Allowlist Enforcement",
                description = "salim only supports 6 permitted on-device actions: SEND_SMS, MAKE_CALL, CREATE_CALENDAR_EVENT, SET_ALARM, OPEN_APP, and OPEN_DEEP_LINK. Any unsupported, ambiguous, or dangerous command is rejected safely as UNKNOWN."
            )

            Spacer(modifier = Modifier.height(14.dp))

            HowItWorksCard(
                icon = Icons.Outlined.CheckCircle,
                title = "3. Mandatory Confirmation Sheet",
                description = "No action executes without your review. A native bottom sheet shows you the exact resolved parameters before anything happens. You can edit the contact, phone number, message body, or event title right on screen."
            )

            Spacer(modifier = Modifier.height(14.dp))

            HowItWorksCard(
                icon = Icons.Outlined.Lock,
                title = "4. Android Keystore AES-256 Encryption",
                description = "Your Groq API key is encrypted using a unique 256-bit AES key backed by the hardware Android KeyStore. The key is only decrypted in memory when making a call and is never exposed in logs, crash reports, or backups."
            )

            Spacer(modifier = Modifier.height(14.dp))

            HowItWorksCard(
                icon = Icons.Outlined.Security,
                title = "5. Transparent Honest Constraints",
                description = "No local application can guarantee 100% protection against an attacker with root privileges or physical extraction tools on a compromised device. Furthermore, emergency numbers (911, 112) are always routed through the system dialer for human confirmation."
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HowItWorksCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceLayer, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(PureWhite, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppleBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            lineHeight = 20.sp
        )
    }
}
