package org.antidepressants.mp5.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.antidepressants.mp5.auth.GlobalAuthManager
import org.antidepressants.mp5.domain.model.AuthState

@Composable
fun AccountScreen(
    modifier: Modifier = Modifier
) {
    val authManager = remember { GlobalAuthManager.instance }
    val authState by authManager.authState.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            "👤 Account",
            style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colors.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        when (authState) {
            is AuthState.Unauthenticated -> {
                UnauthenticatedView(
                    onSignIn = {
                        scope.launch {
                            authManager.signInWithGoogle()
                        }
                    }
                )
            }
            is AuthState.Loading -> {
                LoadingView()
            }
            is AuthState.Authenticated -> {
                val user = (authState as AuthState.Authenticated).user
                AuthenticatedView(
                    displayName = user.displayName,
                    email = user.email,
                    photoUrl = user.photoUrl,
                    onSignOut = {
                        authManager.signOut()
                    }
                )
            }
            is AuthState.Error -> {
                ErrorView(
                    message = (authState as AuthState.Error).message,
                    onRetry = {
                        scope.launch {
                            authManager.signInWithGoogle()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun UnauthenticatedView(
    onSignIn: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Not signed in",
            style = MaterialTheme.typography.h5,
            color = MaterialTheme.colors.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Sign in with Google to sync your playlists",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSignIn,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF4285F4), // Google Blue
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp)
        ) {
            Icon(
                Icons.Default.CloudUpload,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Sign in with Google",
                style = MaterialTheme.typography.button
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Continue without signing in to use local playlists only",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colors.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Signing in...",
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun AuthenticatedView(
    displayName: String,
    email: String,
    photoUrl: String?,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile picture placeholder
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colors.primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colors.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            displayName,
            style = MaterialTheme.typography.h5,
            color = MaterialTheme.colors.onSurface,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            email,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Account info cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colors.surface,
            shape = RoundedCornerShape(12.dp),
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow(
                    icon = Icons.Default.Cloud,
                    label = "Cloud Sync",
                    value = "Enabled"
                )
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                InfoRow(
                    icon = Icons.Default.LibraryMusic,
                    label = "Playlists",
                    value = "Synced to Google"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sign out button
        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colors.error
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out")
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                label,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colors.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Sign in failed",
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            message,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Try Again")
        }
    }
}
