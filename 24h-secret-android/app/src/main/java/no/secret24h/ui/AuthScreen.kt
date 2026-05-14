package no.secret24h.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import no.secret24h.data.AuthApi
import no.secret24h.data.Config
import no.secret24h.data.UserSession

@Composable
fun AuthScreen(
    onSuccess: () -> Unit,
    onSkip: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(Config.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
    }
    val googleClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                isLoading = true
                error = null
                scope.launch {
                    try {
                        val userInfo = AuthApi.signInWithGoogle(idToken)
                        UserSession.accessToken = userInfo.accessToken
                        UserSession.userId = userInfo.userId
                        UserSession.email = userInfo.email
                        isLoading = false
                        onSuccess()
                    } catch (e: Exception) {
                        isLoading = false
                        error = "Server error: ${e.message}"
                    }
                }
            } else {
                error = "No ID token — check Google Cloud OAuth setup"
            }
        } catch (e: ApiException) {
            error = when (e.statusCode) {
                12501 -> null // user cancelled — silent
                10    -> "OAuth misconfiguration (code 10) — check client ID"
                else  -> "Google sign-in failed (code ${e.statusCode})"
            }
        }
    }

    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0xFF3A0F15),
                        0.35f to Color(0xFF120608),
                        1.0f to Color(0xFF07030A),
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(32.dp),
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontFamily = InstrumentSerif, fontStyle = FontStyle.Italic, fontSize = 40.sp, color = SmText)) {
                            append("24h ")
                        }
                        withStyle(SpanStyle(fontFamily = InstrumentSerif, fontSize = 40.sp, color = SmAccent)) {
                            append("Secret")
                        }
                    }
                )

                Text(
                    "Sign in to send whispers and manage your secrets.",
                    fontSize = 14.sp,
                    color = SmTextDim,
                    fontFamily = GeistFamily,
                )

                error?.let {
                    Text(it, color = Color(0xFFFF5F5F), fontSize = 13.sp, fontFamily = GeistFamily)
                }

                Button(
                    onClick = {
                        if (!isLoading) {
                            launcher.launch(googleClient.signInIntent)
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1A1A1A),
                    ),
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = SmAccent,
                        )
                    } else {
                        Text(
                            "Continue with Google",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = GeistFamily,
                        )
                    }
                }

                Surface(
                    onClick = onSkip,
                    color = Color.Transparent,
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .border(1.dp, SmBorder, RoundedCornerShape(100.dp)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "Stay anonymous",
                            fontSize = 15.sp,
                            color = SmTextDim,
                            fontFamily = GeistFamily,
                        )
                    }
                }
            }
        }
    }
}
