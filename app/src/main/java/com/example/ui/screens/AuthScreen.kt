package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.VideoRepository
import com.example.model.UserProfile
import com.example.ui.theme.*
import androidx.compose.foundation.border
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN,
    SIGNUP
}

enum class AuthStep {
    METHOD_SELECT,
    CREDENTIALS_FORM,
    OTP_VERIFICATION
}

enum class CredentialTab {
    PHONE,
    EMAIL_USERNAME
}

data class CountryCode(val code: String, val country: String, val flag: String)

val supportedCountries = listOf(
    CountryCode("+92", "Pakistan", "🇵🇰"),
    CountryCode("+91", "India", "🇮🇳"),
    CountryCode("+1", "United States", "🇺🇸"),
    CountryCode("+44", "United Kingdom", "🇬🇧"),
    CountryCode("+971", "UAE", "🇦🇪"),
    CountryCode("+966", "Saudi Arabia", "🇸🇦"),
    CountryCode("+1", "Canada", "🇨🇦"),
    CountryCode("+61", "Australia", "🇦🇺")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthModalBottomSheet(
    repository: VideoRepository,
    reason: String = "Log in to continue",
    onDismiss: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val safeDismiss = {
        focusManager.clearFocus()
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = safeDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = BgBase,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(BorderSubtle)
            )
        },
        modifier = Modifier
            .fillMaxHeight(0.92f)
            .testTag("tiktok_auth_bottom_sheet")
    ) {
        AuthScreenContent(
            repository = repository,
            reason = reason,
            onClose = safeDismiss,
            isBottomSheet = true
        )
    }
}

@Composable
fun AuthScreen(
    repository: VideoRepository,
    reason: String = "Log in to enjoy full Tashan experience",
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgBase)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("auth_screen")
    ) {
        AuthScreenContent(
            repository = repository,
            reason = reason,
            onClose = onClose,
            isBottomSheet = false
        )
    }
}

@Composable
fun AuthScreenContent(
    repository: VideoRepository,
    reason: String,
    onClose: () -> Unit,
    isBottomSheet: Boolean
) {
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var currentStep by remember { mutableStateOf(AuthStep.METHOD_SELECT) }
    var credentialTab by remember { mutableStateOf(CredentialTab.PHONE) }

    var selectedCountry by remember { mutableStateOf(supportedCountries[0]) }
    var phoneNumber by remember { mutableStateOf("") }
    var emailOrUsername by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayNameInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var otpCode by remember { mutableStateOf("") }
    var otpTimer by remember { mutableIntStateOf(60) }
    var isSendingOtp by remember { mutableStateOf(false) }
    var verificationId by remember { mutableStateOf("") }

    var showCountryPicker by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var activeBrowserOAuthProvider by remember { mutableStateOf<String?>(null) }
    var oauthSetupGuideProvider by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showEmailVerificationNotice by remember { mutableStateOf(false) }
    var pendingVerificationEmail by remember { mutableStateOf("") }
    var isVerifyingEmailStatus by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    fun resetOtpState() {
        otpCode = ""
        verificationId = ""
        errorMessage = null
        successMessage = null
    }

    /**
     * Helper to find the Activity from the current context.
     */
    fun android.content.Context.findActivity(): android.app.Activity? {
        var context = this
        while (context is android.content.ContextWrapper) {
            if (context is android.app.Activity) return context
            context = context.baseContext
        }
        return null
    }

    // Check if an OAuth browser redirect just returned a pending result
    LaunchedEffect(Unit) {
        val pendingResult = repository.authManager.checkPendingOAuth()
        pendingResult.getOrNull()?.let { firebaseUser ->
            val providerId = firebaseUser.providerData.firstOrNull { it.providerId != "firebase" }?.providerId ?: "oauth"
            repository.finalizeSignIn(firebaseUser, providerId)
            onClose()
        }
    }

    fun triggerSocialSignIn(provider: String) {
        val activity = context.findActivity()
        if (activity == null) {
            errorMessage = "Unable to launch browser login: Activity context unavailable."
            return
        }
        errorMessage = null
        successMessage = null
        activeBrowserOAuthProvider = provider
        coroutineScope.launch {
            val result = repository.authManager.signInWithOAuthBrowser(activity, provider)
            activeBrowserOAuthProvider = null
            result.onSuccess { firebaseUser ->
                repository.finalizeSignIn(firebaseUser, provider.lowercase())
                onClose()
            }.onFailure { error ->
                val rawMessage = error.localizedMessage ?: "Browser authentication failed or was cancelled."
                errorMessage = "$provider Login failed: $rawMessage"
                if (
                    rawMessage.contains("not enabled", ignoreCase = true) ||
                    rawMessage.contains("operation is not allowed", ignoreCase = true) ||
                    rawMessage.contains("configuration", ignoreCase = true) ||
                    rawMessage.contains("provider", ignoreCase = true) ||
                    rawMessage.contains("internal", ignoreCase = true) ||
                    rawMessage.contains("INVALID_APP_ID", ignoreCase = true) ||
                    rawMessage.contains("GetAuthDomainTask", ignoreCase = true) ||
                    rawMessage.contains("project config", ignoreCase = true) ||
                    rawMessage.contains("400", ignoreCase = true)
                ) {
                    oauthSetupGuideProvider = provider to rawMessage
                }
            }
        }
    }

    fun triggerGoogleSignIn() {
        coroutineScope.launch {
            errorMessage = null
            try {
                val credentialManager = androidx.credentials.CredentialManager.create(context)
                val webClientId = "414569788030-tlvdqpeejcp98vdq9vjkapupni689hk4.apps.googleusercontent.com"
                val googleIdOption = com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption.Builder(webClientId)
                    .build()
                val request = androidx.credentials.GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result = credentialManager.getCredential(context = context, request = request)
                val credential = result.credential
                if (credential is androidx.credentials.CustomCredential &&
                    credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val res = repository.signInWithGoogleToken(idToken)
                    if (res.isSuccess) {
                        onClose()
                    } else {
                        errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Google token verification failed."
                    }
                } else {
                    errorMessage = "Invalid Google credential received."
                }
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                errorMessage = "Google Sign-In was cancelled."
            } catch (e: Exception) {
                // Launch real browser OAuth for Google if device CredentialManager has no Google account configured
                triggerSocialSignIn("Google")
            }
        }
    }

    LaunchedEffect(currentStep, otpTimer) {
        if (currentStep == AuthStep.OTP_VERIFICATION && otpTimer > 0) {
            delay(1000)
            otpTimer -= 1
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBase)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep != AuthStep.METHOD_SELECT) {
                IconButton(
                    onClick = {
                        errorMessage = null
                        if (currentStep == AuthStep.OTP_VERIFICATION) {
                            currentStep = AuthStep.CREDENTIALS_FORM
                        } else {
                            currentStep = AuthStep.METHOD_SELECT
                        }
                    },
                    modifier = Modifier.testTag("auth_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        focusManager.clearFocus()
                        onClose()
                    },
                    modifier = Modifier.testTag("auth_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = TextPrimary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(TashanTeal, TashanCyan))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "Tashan",
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = 0.5.sp
                )
            }

            IconButton(
                onClick = { showHelpDialog = true },
                modifier = Modifier.testTag("auth_help_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.HelpOutline,
                    contentDescription = "Help",
                    tint = TextSecondary
                )
            }
        }

        HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

        AnimatedVisibility(visible = errorMessage != null || successMessage != null) {
            Surface(
                color = if (errorMessage != null) TashanPink.copy(alpha = 0.2f) else SuccessGreen.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (errorMessage != null) TashanPink.copy(alpha = 0.6f) else SuccessGreen.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (errorMessage != null) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = if (errorMessage != null) TashanPink else SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = errorMessage ?: successMessage ?: "",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            errorMessage = null
                            successMessage = null
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (currentStep) {
                AuthStep.METHOD_SELECT -> {
                    AuthMethodSelectView(
                        authMode = authMode,
                        reason = reason,
                        onSelectPhoneEmail = {
                            errorMessage = null
                            currentStep = AuthStep.CREDENTIALS_FORM
                        },
                onSelectSocial = { provider ->
                    if (provider.equals("Google", ignoreCase = true)) {
                        triggerGoogleSignIn()
                    } else {
                        triggerSocialSignIn(provider)
                    }
                }
                    )
                }
                AuthStep.CREDENTIALS_FORM -> {
                    AuthCredentialsFormView(
                        authMode = authMode,
                        credentialTab = credentialTab,
                        onTabSelected = { credentialTab = it },
                        selectedCountry = selectedCountry,
                        onCountryClick = { showCountryPicker = true },
                        phoneNumber = phoneNumber,
                        onPhoneNumberChange = { phoneNumber = it },
                        emailOrUsername = emailOrUsername,
                        onEmailOrUsernameChange = { emailOrUsername = it },
                        password = password,
                        onPasswordChange = { password = it },
                        confirmPassword = confirmPassword,
                        onConfirmPasswordChange = { confirmPassword = it },
                        displayNameInput = displayNameInput,
                        onDisplayNameChange = { displayNameInput = it },
                        usernameInput = usernameInput,
                        onUsernameChange = { usernameInput = it },
                        passwordVisible = passwordVisible,
                        onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                        onForgotPasswordClick = { showForgotPasswordDialog = true },
                        onSubmitPhone = {
                            if (phoneNumber.trim().length < 6) {
                                errorMessage = "Please enter a valid phone number"
                                return@AuthCredentialsFormView
                            }
                            resetOtpState() // FORCE CLEAN STATE
                            isSendingOtp = true
                            val fullPhone = "${selectedCountry.code}$phoneNumber".replace(" ", "")
                            val activity = context.findActivity()
                            
                            if (activity == null) {
                                errorMessage = "Failed to initialize verification. Please restart the app."
                                isSendingOtp = false
                                return@AuthCredentialsFormView
                            }

                            repository.authManager.sendPhoneOtp(
                                phoneNumber = fullPhone,
                                activity = activity,
                                onCodeSent = { vid ->
                                    isSendingOtp = false
                                    verificationId = vid
                                    otpTimer = 60
                                    successMessage = "Verification code sent to $fullPhone"
                                    currentStep = AuthStep.OTP_VERIFICATION
                                },
                                onVerificationCompleted = { credential ->
                                    coroutineScope.launch {
                                        repository.authManager.signInWithSocialCredential(credential)
                                            .onSuccess { user ->
                                                repository.finalizeSignIn(user, "phone")
                                                onClose()
                                            }
                                            .onFailure {
                                                errorMessage = it.message ?: "Auto-verification failed"
                                            }
                                    }
                                },
                                onVerificationFailed = { e ->
                                    isSendingOtp = false
                                    errorMessage = e.message ?: "Failed to send SMS. Please check the number and try again."
                                }
                            )
                        },
                        onSubmitEmailPassword = {
                            if (emailOrUsername.isBlank()) {
                                errorMessage = "Please enter your email or username"
                                return@AuthCredentialsFormView
                            }
                            val cleanEmail = if (emailOrUsername.contains("@")) emailOrUsername.lowercase().trim() else "$emailOrUsername@tashan.app"
                            if (authMode == AuthMode.SIGNUP) {
                                val emailCheck = repository.authManager.validateEmail(cleanEmail)
                                if (!emailCheck.isValid) {
                                    errorMessage = emailCheck.errorMessage ?: "Please enter a valid personal or business email"
                                    return@AuthCredentialsFormView
                                }
                                val passCheck = repository.authManager.validatePassword(password)
                                if (!passCheck.isValid) {
                                    errorMessage = passCheck.feedbackMessage
                                    return@AuthCredentialsFormView
                                }
                                if (password != confirmPassword) {
                                    errorMessage = "Passwords do not match"
                                    return@AuthCredentialsFormView
                                }
                                isSendingOtp = true
                                repository.signUp(
                                    displayName = displayNameInput.ifBlank { usernameInput.ifBlank { cleanEmail.substringBefore("@") } },
                                    username = usernameInput.ifBlank { cleanEmail.substringBefore("@") },
                                    emailOrPhone = cleanEmail,
                                    password = password,
                                    isPhone = false
                                ) { success, msg ->
                                    isSendingOtp = false
                                    if (success) {
                                        pendingVerificationEmail = cleanEmail
                                        showEmailVerificationNotice = true
                                        successMessage = msg
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            } else {
                                if (password.length < 6) {
                                    errorMessage = "Password must be at least 6 characters"
                                    return@AuthCredentialsFormView
                                }
                                isSendingOtp = true
                                repository.loginWithEmail(cleanEmail, password) { success, msg, needsVerification ->
                                    isSendingOtp = false
                                    if (success) {
                                        if (needsVerification) {
                                            pendingVerificationEmail = cleanEmail
                                            showEmailVerificationNotice = true
                                            errorMessage = "Your email is not verified yet. A verification link has been sent to $cleanEmail."
                                        } else {
                                            onClose()
                                        }
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            }
                        },
                        onSendEmailOtp = {
                            if (emailOrUsername.isBlank()) {
                                errorMessage = "Please enter your email address"
                                return@AuthCredentialsFormView
                            }
                            resetOtpState() // FORCE CLEAN STATE
                            val cleanEmail = if (emailOrUsername.contains("@")) emailOrUsername.lowercase().trim() else "$emailOrUsername@tashan.app"
                            val emailCheck = repository.authManager.validateEmail(cleanEmail)
                            if (!emailCheck.isValid) {
                                errorMessage = emailCheck.errorMessage ?: "Please enter a valid email address"
                                return@AuthCredentialsFormView
                            }
                            isSendingOtp = true
                            coroutineScope.launch {
                                delay(400)
                                isSendingOtp = false
                                otpTimer = 60
                                // For Email OTP, we still use the secure generator for now 
                                // until a full transactional email service is integrated.
                                val code = repository.sendEmailOtp(cleanEmail)
                                // DO NOT auto-fill otpCode anymore to prevent "hackable" perception
                                successMessage = "A 6-digit verification code has been sent to your email. (Simulation: Please check your inbox)"
                                currentStep = AuthStep.OTP_VERIFICATION
                            }
                        }
                    )
                }
                AuthStep.OTP_VERIFICATION -> {
                    var currentOtpSubStep by remember { mutableStateOf(0) } // 0: Code Input, 1: Validating, 2: Finalizing
                    val otpSubSteps = listOf("Verifying Code", "Securing Account", "Finalizing Setup")

                    if (currentOtpSubStep == 0) {
                        AuthOtpVerificationView(
                            contactMethod = if (credentialTab == CredentialTab.EMAIL_USERNAME) emailOrUsername else "${selectedCountry.code} $phoneNumber",
                            otpCode = otpCode,
                            onOtpChange = { otpCode = it },
                            otpTimer = otpTimer,
                            onResendOtp = {
                                otpTimer = 60
                                if (credentialTab == CredentialTab.EMAIL_USERNAME) {
                                    val cleanEmail = if (emailOrUsername.contains("@")) emailOrUsername.lowercase().trim() else "$emailOrUsername@tashan.app"
                                    repository.sendEmailOtp(cleanEmail)
                                    successMessage = "New verification code sent to your email."
                                } else {
                                    val fullPhone = "${selectedCountry.code}$phoneNumber"
                                    val activity = context.findActivity()
                                    if (activity == null) {
                                        errorMessage = "Failed to resend code."
                                        return@AuthOtpVerificationView
                                    }
                                    repository.authManager.sendPhoneOtp(
                                        phoneNumber = fullPhone,
                                        activity = activity,
                                        onCodeSent = { vid ->
                                            verificationId = vid
                                            successMessage = "New verification SMS sent."
                                        },
                                        onVerificationCompleted = {},
                                        onVerificationFailed = { e ->
                                            errorMessage = e.message ?: "Failed to resend SMS."
                                        }
                                    )
                                }
                            },
                            onVerifyOtp = {
                                if (otpCode.length < 4) {
                                    errorMessage = "Enter 4 to 6 digit verification code"
                                    return@AuthOtpVerificationView
                                }
                                coroutineScope.launch {
                                if (credentialTab == CredentialTab.PHONE) {
                                    if (verificationId.isEmpty()) {
                                        errorMessage = "Session expired. Please request a new code."
                                        currentOtpSubStep = 0
                                        return@launch
                                    }
                                    repository.verifyPhoneOtp(verificationId, otpCode) { success, msg ->
                                        coroutineScope.launch {
                                            if (success) {
                                                currentOtpSubStep = 2
                                                delay(600)
                                                onClose()
                                            } else {
                                                errorMessage = msg
                                                currentOtpSubStep = 0
                                            }
                                        }
                                    }
                                } else {
                                    val cleanEmail = if (emailOrUsername.contains("@")) emailOrUsername.lowercase().trim() else "$emailOrUsername@tashan.app"
                                    val isValid = repository.verifyEmailOtp(cleanEmail, otpCode)
                                    if (isValid) {
                                        currentOtpSubStep = 1
                                        delay(800)
                                        currentOtpSubStep = 2
                                        delay(600)
                                        repository.loginWithEmailOtp(cleanEmail, otpCode) { success, msg ->
                                            if (success) {
                                                successMessage = msg
                                                onClose()
                                            } else {
                                                errorMessage = msg
                                                currentOtpSubStep = 0
                                            }
                                        }
                                    } else {
                                        errorMessage = "Invalid verification code."
                                        currentOtpSubStep = 0
                                    }
                                }
                                }
                            }
                        )
                    } else {
                        // "Long Process" Loading View
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = TashanTeal, strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = otpSubSteps.getOrElse(currentOtpSubStep - 1) { "Processing..." },
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This may take a few moments. Please do not close the app.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            LinearProgressIndicator(
                                progress = { if (currentOtpSubStep == 1) 0.5f else 0.9f },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = TashanCyan,
                                trackColor = BorderSubtle
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgElevated)
        ) {
            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
            Text(
                text = "By continuing, you agree to Tashan's Terms of Service and confirm that you have read Tashan's Privacy Policy.",
                color = TextTertiary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(BgElevated2)
                    .clickable {
                        errorMessage = null
                        successMessage = null
                        authMode = if (authMode == AuthMode.LOGIN) AuthMode.SIGNUP else AuthMode.LOGIN
                        currentStep = AuthStep.METHOD_SELECT
                    }
                    .testTag("auth_mode_toggle_footer"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (authMode == AuthMode.LOGIN) "Don't have an account? " else "Already have an account? ",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    text = if (authMode == AuthMode.LOGIN) "Sign up" else "Log in",
                    color = TashanTeal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }

    if (showCountryPicker) {
        Dialog(onDismissRequest = { showCountryPicker = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BgElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Select Country / Region",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(supportedCountries) { country ->
                            Surface(
                                color = if (selectedCountry.code == country.code && selectedCountry.country == country.country)
                                    TashanTeal.copy(alpha = 0.15f) else BgElevated2,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCountry = country
                                        showCountryPicker = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(text = country.flag, fontSize = 20.sp)
                                        Text(text = country.country, color = TextPrimary, fontSize = 14.sp)
                                    }
                                    Text(
                                        text = country.code,
                                        color = TashanCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    activeBrowserOAuthProvider?.let { provider ->
        Dialog(onDismissRequest = { activeBrowserOAuthProvider = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = BgElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, TashanCyan.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = TashanCyan, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Opening $provider in Browser...",
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Complete your $provider login in the secure browser window. You will only be signed in after $provider verifies your account.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { activeBrowserOAuthProvider = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            }
        }
    }

    oauthSetupGuideProvider?.let { (provider, firebaseError) ->
        OAuthConsoleSetupGuideDialog(
            provider = provider,
            firebaseError = firebaseError,
            onDismiss = { oauthSetupGuideProvider = null },
            onRetryBrowserLogin = {
                oauthSetupGuideProvider = null
                triggerSocialSignIn(provider)
            }
        )
    }

    if (showForgotPasswordDialog) {
        var recoveryMode by remember { mutableStateOf(0) } // 0: Email/Username, 1: Phone
        var resetInput by remember { mutableStateOf(if (recoveryMode == 0) emailOrUsername else phoneNumber) }
        var isProcessing by remember { mutableStateOf(false) }
        var recoveryMessage by remember { mutableStateOf<String?>(null) }
        var isSuccess by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = {
                Text(
                    text = "Recover Account",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!isSuccess) {
                        TabRow(
                            selectedTabIndex = recoveryMode,
                            containerColor = Color.Transparent,
                            contentColor = TashanTeal,
                            divider = {},
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[recoveryMode]),
                                    color = TashanTeal
                                )
                            }
                        ) {
                            Tab(selected = recoveryMode == 0, onClick = { recoveryMode = 0; resetInput = emailOrUsername }) {
                                Text("Email/User", modifier = Modifier.padding(8.dp), fontSize = 12.sp)
                            }
                            Tab(selected = recoveryMode == 1, onClick = { recoveryMode = 1; resetInput = phoneNumber }) {
                                Text("Phone", modifier = Modifier.padding(8.dp), fontSize = 12.sp)
                            }
                        }

                        Text(
                            text = if (recoveryMode == 0) 
                                "Enter your username or email address. We'll send a password reset link to your verified email." 
                                else "Enter your registered phone number. We'll send a verification code to reset your password.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        OutlinedTextField(
                            value = resetInput,
                            onValueChange = { resetInput = it },
                            placeholder = { Text(if (recoveryMode == 0) "Username or email" else "Phone number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (recoveryMode == 1) KeyboardType.Phone else KeyboardType.Email
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TashanTeal,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )

                        if (recoveryMessage != null) {
                            Text(
                                text = recoveryMessage!!,
                                color = if (recoveryMessage!!.contains("success", ignoreCase = true) || recoveryMessage!!.contains("sent", ignoreCase = true)) SuccessGreen else TashanPink,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = recoveryMessage ?: "Recovery instructions sent!",
                                color = TextPrimary,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Please follow the instructions sent to your ${if (recoveryMode == 0) "email" else "phone"} to regain access to your Tashan account.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isSuccess) {
                            showForgotPasswordDialog = false
                            return@Button
                        }
                        if (resetInput.isBlank()) {
                            recoveryMessage = "Please enter your ${if (recoveryMode == 0) "email/username" else "phone"}"
                            return@Button
                        }

                        isProcessing = true
                        coroutineScope.launch {
                            if (recoveryMode == 0) {
                                if (resetInput.contains("@")) {
                                    repository.sendPasswordReset(resetInput) { success, msg ->
                                        isProcessing = false
                                        recoveryMessage = msg
                                        isSuccess = success
                                    }
                                } else {
                                    repository.recoverAccountByUsername(resetInput) { success, msg ->
                                        isProcessing = false
                                        recoveryMessage = msg
                                        isSuccess = success
                                    }
                                }
                            } else {
                                repository.sendRecoverySms(resetInput) { success, msg ->
                                    isProcessing = false
                                    recoveryMessage = msg
                                    isSuccess = success
                                    // In a real app, this would then prompt for the SMS OTP to allow password reset
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TashanTeal),
                    enabled = !isProcessing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (isSuccess) "Got It" else "Recover Account", color = Color.White)
                    }
                }
            },
            dismissButton = {
                if (!isSuccess) {
                    TextButton(onClick = { showForgotPasswordDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            },
            containerColor = BgElevated
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Help, contentDescription = null, tint = TashanCyan)
                    Text("Tashan Login Help", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Login Options in Tashan:", color = TashanCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("• Phone: Enter phone number and verify with 6-digit SMS code.", color = TextSecondary, fontSize = 12.sp)
                    Text("• Email: Log in using your email address or username.", color = TextSecondary, fontSize = 12.sp)
                    Text("• Social: Connect via Google or launch secure Browser OAuth for Apple, Facebook, and X.", color = TextSecondary, fontSize = 12.sp)
                    Text("• Security: All OAuth sessions and 6-digit OTP codes require real verification.", color = TextSecondary, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHelpDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = TashanTeal)
                ) {
                    Text("Got It", color = Color.White)
                }
            },
            containerColor = BgElevated
        )
    }

    if (showEmailVerificationNotice) {
        AlertDialog(
            onDismissRequest = { showEmailVerificationNotice = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.MarkEmailRead,
                    contentDescription = null,
                    tint = TashanTeal,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Verify Your Email Address",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "A real verification link has been sent to:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = pendingVerificationEmail,
                        color = TashanCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Please open your inbox and click the verification link to unlock access. Or verify instantly using a 6-digit Email OTP code.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedButton(
                        onClick = {
                            showEmailVerificationNotice = false
                            resetOtpState()
                            repository.sendEmailOtp(pendingVerificationEmail)
                            otpTimer = 60
                            successMessage = "A 6-digit verification code has been sent to $pendingVerificationEmail. Please check your inbox."
                            currentStep = AuthStep.OTP_VERIFICATION
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TashanCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TashanCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Use 6-Digit Email OTP Instead", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isVerifyingEmailStatus = true
                            val res = repository.authManager.checkEmailVerified()
                            isVerifyingEmailStatus = false
                            if (res.getOrDefault(false)) {
                                successMessage = "Email verified successfully! Welcome to Tashan."
                                showEmailVerificationNotice = false
                                onClose()
                            } else {
                                errorMessage = "Email not verified yet. Please check your inbox or spam folder, or tap Resend Link."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TashanTeal)
                ) {
                    Text(if (isVerifyingEmailStatus) "Checking..." else "I Have Verified", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            val res = repository.authManager.sendEmailVerification()
                            if (res.isSuccess) {
                                successMessage = "A new verification link has been sent to $pendingVerificationEmail"
                            } else {
                                errorMessage = "Failed to resend link: ${res.exceptionOrNull()?.message}"
                            }
                        }
                    }
                ) {
                    Text("Resend Link", color = TashanCyan)
                }
            },
            containerColor = BgElevated
        )
    }
}

@Composable
fun AuthMethodSelectView(
    authMode: AuthMode,
    reason: String,
    onSelectPhoneEmail: () -> Unit,
    onSelectSocial: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (authMode == AuthMode.LOGIN) "Log in to Tashan" else "Sign up for Tashan",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (reason.isNotBlank()) reason else "Manage your profile, upload videos, comment, and connect with creators",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))

        AuthOptionCard(
            icon = Icons.Filled.Smartphone,
            text = if (authMode == AuthMode.LOGIN) "Use phone / email / username" else "Use phone or email",
            isPrimary = true,
            onClick = onSelectPhoneEmail,
            testTag = "auth_opt_phone_email"
        )
        Spacer(modifier = Modifier.height(10.dp))
        AuthOptionCard(
            icon = Icons.Filled.Language,
            text = "Continue with Google",
            onClick = { onSelectSocial("Google") },
            testTag = "auth_opt_google"
        )
        Spacer(modifier = Modifier.height(10.dp))
        AuthOptionCard(
            icon = Icons.Filled.AccountCircle,
            text = "Continue with Apple",
            onClick = { onSelectSocial("Apple") },
            testTag = "auth_opt_apple"
        )
        Spacer(modifier = Modifier.height(10.dp))
        AuthOptionCard(
            icon = Icons.Filled.Groups,
            text = "Continue with Facebook",
            onClick = { onSelectSocial("Facebook") },
            testTag = "auth_opt_facebook"
        )
        Spacer(modifier = Modifier.height(10.dp))
        AuthOptionCard(
            icon = Icons.Filled.Bolt,
            text = "Continue with X (Twitter)",
            onClick = { onSelectSocial("Twitter") },
            testTag = "auth_opt_twitter"
        )
    }
}

@Composable
fun AuthOptionCard(
    icon: ImageVector,
    text: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = if (isPrimary) BgElevated2 else BgElevated,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPrimary) TashanTeal.copy(alpha = 0.5f) else BorderSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPrimary) TashanTeal else TextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun AuthCredentialsFormView(
    authMode: AuthMode,
    credentialTab: CredentialTab,
    onTabSelected: (CredentialTab) -> Unit,
    selectedCountry: CountryCode,
    onCountryClick: () -> Unit,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    emailOrUsername: String,
    onEmailOrUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    displayNameInput: String,
    onDisplayNameChange: (String) -> Unit,
    usernameInput: String,
    onUsernameChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisible: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSubmitPhone: () -> Unit,
    onSubmitEmailPassword: () -> Unit,
    onSendEmailOtp: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = if (authMode == AuthMode.LOGIN) "Log in" else "Sign up",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BgElevated)
                .padding(3.dp)
        ) {
            Surface(
                color = if (credentialTab == CredentialTab.PHONE) BgElevated2 else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onTabSelected(CredentialTab.PHONE) }
                    .testTag("tab_auth_phone")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Phone",
                        color = if (credentialTab == CredentialTab.PHONE) TashanTeal else TextSecondary,
                        fontWeight = if (credentialTab == CredentialTab.PHONE) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            }
            Surface(
                color = if (credentialTab == CredentialTab.EMAIL_USERNAME) BgElevated2 else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onTabSelected(CredentialTab.EMAIL_USERNAME) }
                    .testTag("tab_auth_email")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (authMode == AuthMode.LOGIN) "Email / Username" else "Email",
                        color = if (credentialTab == CredentialTab.EMAIL_USERNAME) TashanTeal else TextSecondary,
                        fontWeight = if (credentialTab == CredentialTab.EMAIL_USERNAME) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (authMode == AuthMode.SIGNUP) {
            Text("Full Name / Display Name", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = displayNameInput,
                onValueChange = onDisplayNameChange,
                placeholder = { Text("e.g. Alex Rivera", color = TextTertiary) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_signup_name_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TashanTeal,
                    unfocusedBorderColor = BorderSubtle,
                    focusedContainerColor = BgInput,
                    unfocusedContainerColor = BgInput
                )
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text("Tashan Username", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = usernameInput,
                onValueChange = onUsernameChange,
                placeholder = { Text("e.g. alex_vibe", color = TextTertiary) },
                leadingIcon = { Text("@", color = TashanCyan, fontWeight = FontWeight.Bold) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_signup_username_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TashanTeal,
                    unfocusedBorderColor = BorderSubtle,
                    focusedContainerColor = BgInput,
                    unfocusedContainerColor = BgInput
                )
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (credentialTab == CredentialTab.PHONE) {
            Text("Phone Number", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = BgInput,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .height(56.dp)
                        .clickable { onCountryClick() }
                        .testTag("auth_country_picker_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = selectedCountry.flag, fontSize = 18.sp)
                        Text(text = selectedCountry.code, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                    }
                }
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = onPhoneNumberChange,
                    placeholder = { Text("Phone number", color = TextTertiary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("auth_phone_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TashanTeal,
                        unfocusedBorderColor = BorderSubtle,
                        focusedContainerColor = BgInput,
                        unfocusedContainerColor = BgInput
                    )
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onSubmitPhone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("auth_send_code_button"),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TashanTeal)
            ) {
                Text(
                    text = "Send Code",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        } else {
            Text(
                text = if (authMode == AuthMode.LOGIN) "Email or Username" else "Email Address",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = emailOrUsername,
                onValueChange = onEmailOrUsernameChange,
                placeholder = { Text(if (authMode == AuthMode.LOGIN) "Email or @username" else "alex@example.com", color = TextTertiary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_email_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TashanTeal,
                    unfocusedBorderColor = BorderSubtle,
                    focusedContainerColor = BgInput,
                    unfocusedContainerColor = BgInput
                )
            )

            if (emailOrUsername.isBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "Quick fill:", color = TextTertiary, fontSize = 11.sp)
                    Surface(
                        color = TashanTeal.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TashanTeal.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable { onEmailOrUsernameChange("ah3334r@gmail.com") }
                    ) {
                        Text(
                            text = "ah3334r@gmail.com",
                            color = TashanCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("Password", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                placeholder = { Text("Enter password", color = TextTertiary) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisible) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = "Toggle password visibility",
                            tint = TextSecondary
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_password_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TashanTeal,
                    unfocusedBorderColor = BorderSubtle,
                    focusedContainerColor = BgInput,
                    unfocusedContainerColor = BgInput
                )
            )

            if (authMode == AuthMode.SIGNUP) {
                val hasMinLength = password.length >= 8
                val hasUppercase = password.any { it.isUpperCase() }
                val hasLowercase = password.any { it.isLowerCase() }
                val hasDigit = password.any { it.isDigit() }
                val hasSpecialChar = password.any { "!@#$%^&*()_+-=[]{};':\"|,.<>/?".contains(it) }
                var strengthScore = 0
                if (hasMinLength) strengthScore += 20
                if (hasUppercase) strengthScore += 20
                if (hasLowercase) strengthScore += 20
                if (hasDigit) strengthScore += 20
                if (hasSpecialChar) strengthScore += 20

                val meterColor = when {
                    strengthScore <= 40 -> Color(0xFFEF4444)
                    strengthScore <= 80 -> Color(0xFFF59E0B)
                    else -> Color(0xFF10B981)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Password Strength: ${if (strengthScore == 100) "Strong" else if (strengthScore >= 60) "Moderate" else "Weak"}",
                        color = meterColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$strengthScore%",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { strengthScore / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = meterColor,
                    trackColor = BorderSubtle
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text("Confirm Password", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    placeholder = { Text("Re-enter password", color = TextTertiary) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_confirm_password_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TashanTeal,
                        unfocusedBorderColor = BorderSubtle,
                        focusedContainerColor = BgInput,
                        unfocusedContainerColor = BgInput
                    )
                )
            }

            if (authMode == AuthMode.LOGIN) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Forgot password?",
                    color = TashanCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { onForgotPasswordClick() }
                        .padding(4.dp)
                        .testTag("auth_forgot_password_button")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onSubmitEmailPassword,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("auth_submit_email_button"),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TashanTeal)
            ) {
                Text(
                    text = if (authMode == AuthMode.LOGIN) "Log In" else "Sign Up",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = BorderSubtle)
                Text(
                    text = "OR",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = BorderSubtle)
            }

            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = onSendEmailOtp,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TashanCyan),
                border = androidx.compose.foundation.BorderStroke(1.dp, TashanCyan.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("auth_send_email_otp_button")
            ) {
                Icon(imageVector = Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Verify / Sign In via 6-Digit Email OTP",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun AuthOtpVerificationView(
    contactMethod: String,
    otpCode: String,
    onOtpChange: (String) -> Unit,
    otpTimer: Int,
    onResendOtp: () -> Unit,
    onVerifyOtp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(TashanTeal.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Sms,
                contentDescription = null,
                tint = TashanTeal,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Enter 6-digit code",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Your code was sent to $contactMethod",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = otpCode,
            onValueChange = { if (it.length <= 6) onOtpChange(it) },
            placeholder = { Text("● ● ● ● ● ●", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                textAlign = TextAlign.Center,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp,
                color = TextPrimary
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onVerifyOtp() }),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .testTag("auth_otp_input"),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TashanTeal,
                unfocusedBorderColor = BorderSubtle,
                focusedContainerColor = BgInput,
                unfocusedContainerColor = BgInput
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
        if (otpTimer > 0) {
            Text(
                text = "Resend code in ${otpTimer}s",
                color = TextTertiary,
                fontSize = 13.sp
            )
        } else {
            Text(
                text = "Resend code",
                color = TashanCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable { onResendOtp() }
                    .padding(8.dp)
                    .testTag("auth_resend_otp_button")
            )
        }
        val context = androidx.compose.ui.platform.LocalContext.current
        if (contactMethod.contains("@") || contactMethod.contains("gmail", ignoreCase = true)) {
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                            addCategory(android.content.Intent.CATEGORY_APP_EMAIL)
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val webIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://mail.google.com"))
                        webIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(webIntent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("auth_open_gmail_app_button"),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TashanCyan.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TashanCyan)
            ) {
                Icon(
                    imageVector = Icons.Filled.Mail,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open Gmail App 📬",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onVerifyOtp,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("auth_verify_otp_submit_button"),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TashanTeal)
        ) {
            Text(
                text = "Log In / Continue",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun OAuthConsoleSetupGuideDialog(
    provider: String,
    firebaseError: String,
    onDismiss: () -> Unit,
    onRetryBrowserLogin: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val providerDomain = when (provider.lowercase()) {
        "apple" -> "apple.com"
        "facebook" -> "facebook.com"
        "twitter", "x" -> "twitter.com"
        else -> "${provider.lowercase()}.com"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = BgElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, TashanPink.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(TashanPink.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = TashanPink,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "$provider Browser OAuth Notice",
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Access was blocked because $provider ($providerDomain) did not complete verification in the browser.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = BgInput,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Firebase Response:",
                            color = TashanPink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = firebaseError,
                            color = TextPrimary,
                            fontSize = 11.sp
                        )
                        HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "Required Firebase Console Setup ($provider):",
                            color = TashanCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "1. Open Firebase Console -> Authentication -> Sign-in method.\n" +
                                "2. Select '$provider' ($providerDomain) and switch Enable to ON.\n" +
                                "3. Enter your official $provider App ID / Services ID and Client Secret.\n" +
                                "4. Paste the Firebase OAuth Redirect URI into your $provider Developer Portal.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onRetryBrowserLogin,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TashanTeal),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(Icons.Filled.OpenInBrowser, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry $provider Login in Browser", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://console.firebase.google.com/")
                        ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("Open Firebase Console 🌐", color = TextPrimary, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TashanCyan.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TashanCyan),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Filled.Smartphone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Use Phone SMS or Email OTP Instead 📱", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

