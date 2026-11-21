package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.components.GlassCard
import com.example.ui.components.TxlBackdrop
import com.example.ui.components.TxlLogoMark
import com.example.ui.components.shareNexoLiveApp
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.DangerRose
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    errorMessage: String?,
    isBusy: Boolean,
    onGoogleIdToken: (String) -> Unit,
    onAuthError: (String?) -> Unit,
    onBusyChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var attemptedSilent by remember { mutableStateOf(false) }

    // Silent auto sign-in using GetGoogleIdOption only (separate request from interactive button)
    LaunchedEffect(Unit) {
        if (attemptedSilent) return@LaunchedEffect
        attemptedSilent = true
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            ""
        }
        if (clientId.isBlank()) return@LaunchedEffect
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val idToken = GoogleIdTokenCredential.createFrom(cred.data).idToken
                onGoogleIdToken(idToken)
            }
        } catch (_: Exception) {
            // Clean fallback to interactive Google Sign-In button below
        }
    }

    TxlBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TxlLogoMark()
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Salas de voz y directos globales sin límites de región",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(22.dp))

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Acceso verificado",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Cada usuario inicia sesión con su propio correo verificado de Google. Comparte la app con amigos en cualquier país y hablen al instante.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AuthFeaturePill(
                                icon = Icons.Filled.Public,
                                tint = NeonCyan,
                                text = "Disponible en todo el mundo sin restricciones"
                            )
                            AuthFeaturePill(
                                icon = Icons.Filled.GraphicEq,
                                tint = NeonPink,
                                text = "Micrófono multiusuario fluido en salas en vivo"
                            )
                            AuthFeaturePill(
                                icon = Icons.Filled.CardGiftcard,
                                tint = Color(0xFFFFD166),
                                text = "Regalos en sala, niveles de donador y tareas diarias"
                            )
                            AuthFeaturePill(
                                icon = Icons.Filled.VerifiedUser,
                                tint = NeonPurple,
                                text = "Cuenta propia y segura: nadie accede a la cuenta del dueño"
                            )
                        }

                        if (!errorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                color = DangerRose.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, DangerRose.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage,
                                    color = DangerRose,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                val clientId = try {
                                    context.getString(R.string.default_web_client_id)
                                } catch (_: Exception) {
                                    onAuthError("Configuración de Google Sign-In no encontrada en la compilación.")
                                    return@Button
                                }
                                onAuthError(null)
                                onBusyChange(true)
                                scope.launch {
                                    try {
                                        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
                                        val request = GetCredentialRequest.Builder()
                                            .addCredentialOption(signInOption)
                                            .build()
                                        val hostContext = context.findActivity() ?: context
                                        val cred = credentialManager.getCredential(hostContext, request).credential
                                        if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                            val idToken = GoogleIdTokenCredential.createFrom(cred.data).idToken
                                            onGoogleIdToken(idToken)
                                        } else {
                                            onBusyChange(false)
                                            onAuthError("Credencial de Google no reconocida.")
                                        }
                                    } catch (e: GetCredentialCancellationException) {
                                        Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
                                        onBusyChange(false)
                                    } catch (e: Exception) {
                                        Log.e("Auth", "Google Sign-In failed", e)
                                        onBusyChange(false)
                                        onAuthError(e.localizedMessage ?: "No se pudo iniciar sesión con Google.")
                                    }
                                }
                            },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("google_signin_button")
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color.Transparent,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .let { m ->
                                            m
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = if (isBusy) SurfaceElevated else Color.White,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isBusy) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(22.dp),
                                                    color = NeonPink,
                                                    strokeWidth = 2.5.dp
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = "Verificando correo...",
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            } else {
                                                Text(
                                                    text = "G",
                                                    fontSize = 19.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF4285F4)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = "Continuar con Google",
                                                    color = Color(0xFF1F1936),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 15.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { shareNexoLiveApp(context) },
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(NeonCyan, NeonPink))),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("auth_share_app_button")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compartir NexoLive con amigos", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthFeaturePill(
    icon: ImageVector,
    tint: Color,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceCard.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, BorderGlow.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = text, fontSize = 12.sp, color = TextPrimary)
        }
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
