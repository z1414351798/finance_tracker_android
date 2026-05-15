package com.z.financetracker.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalUriHandler
import com.z.financetracker.R
import com.z.financetracker.client.NetworkClient
import com.z.financetracker.component.LanguageSwitcher
import com.z.financetracker.entity.User
import com.z.financetracker.util.TokenManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isUploadingAvatar by remember { mutableStateOf(false) }

    // Edit profile state
    var editEmail by remember { mutableStateOf("") }
    var isEditingProfile by remember { mutableStateOf(false) }
    var isSavingProfile by remember { mutableStateOf(false) }

    // Change password state
    var showPasswordSection by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPw by remember { mutableStateOf(false) }
    var showNewPw by remember { mutableStateOf(false) }
    var isSavingPassword by remember { mutableStateOf(false) }

    // URI handler for opening links
    val uriHandler = LocalUriHandler.current

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    val msgProfileUpdated     = stringResource(R.string.profile_updated)
    val msgUploadFailed       = stringResource(R.string.upload_failed)
    val msgUpdateFailed       = stringResource(R.string.update_failed)
    val msgPasswordChanged    = stringResource(R.string.password_changed)
    val msgWrongPassword      = stringResource(R.string.wrong_current_password)
    val msgFailedChangePw     = stringResource(R.string.failed_change_password)
    val msgPasswordsDontMatch = stringResource(R.string.passwords_dont_match)
    val msgPasswordTooShort   = stringResource(R.string.password_too_short)

    fun showMsg(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    fun loadProfile() {
        scope.launch {
            try {
                val resp = NetworkClient.getProfileApi(context).getProfile()
                if (resp.isSuccessful) {
                    user = resp.body()
                    editEmail = resp.body()?.email ?: ""
                }
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadProfile() }

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isUploadingAvatar = true
            try {
                val stream = context.contentResolver.openInputStream(uri) ?: return@launch
                val bytes = stream.readBytes()
                stream.close()

                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("image", "avatar.jpg", requestBody)

                val resp = NetworkClient.getProfileApi(context).uploadAvatar(part)
                if (resp.isSuccessful) {
                    val newUrl = resp.body()?.get("profileImageUrl")
                    // ← newUrl is already full MinIO URL, use directly
                    user = user?.copy(profileImageUrl = newUrl)
                    showMsg(msgProfileUpdated)
                } else {
                    showMsg(msgUploadFailed)
                }
            } catch (e: Exception) {
                showMsg("Error: ${e.message}")
            } finally {
                isUploadingAvatar = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF2563EB))
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // ── Hero Header ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1E40AF), Color(0xFF3B82F6))
                        )
                    )
                    .padding(top = 32.dp, bottom = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Avatar
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .border(3.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (user?.profileImageUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        // Authenticated proxy — bucket is private
                                        .data(user!!.presignedImageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Profile photo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }

                            if (isUploadingAvatar) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }

                        // Camera button
                        IconButton(
                            onClick = { if (!isUploadingAvatar) imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White, CircleShape)
                                .border(2.dp, Color(0xFF3B82F6), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        user?.username ?: "",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!user?.email.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            user?.email ?: "",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height((-20).dp)) // overlap card over hero

            // ── Profile Info Card ──────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.profile_info), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        TextButton(onClick = {
                            isEditingProfile = !isEditingProfile
                            if (!isEditingProfile) editEmail = user?.email ?: ""
                        }) {
                            Text(
                                if (isEditingProfile) stringResource(R.string.cancel) else stringResource(R.string.edit),
                                color = Color(0xFF2563EB),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Username (read-only)
                    ProfileField(
                        label = stringResource(R.string.username),
                        value = user?.username ?: "",
                        icon = Icons.Default.Person,
                        readOnly = true
                    )

                    Spacer(Modifier.height(12.dp))

                    // Email (editable)
                    AnimatedContent(targetState = isEditingProfile) { editing ->
                        if (editing) {
                            OutlinedTextField(
                                value = editEmail,
                                onValueChange = { editEmail = it },
                                label = { Text(stringResource(R.string.email)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, null, tint = Color(0xFF2563EB))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF2563EB),
                                    unfocusedBorderColor = Color(0xFFE5E7EB)
                                )
                            )
                        } else {
                            ProfileField(
                                label = stringResource(R.string.email),
                                value = user?.email?.ifBlank { stringResource(R.string.not_set) } ?: stringResource(R.string.not_set),
                                icon = Icons.Default.Email,
                                readOnly = true
                            )
                        }
                    }

                    AnimatedVisibility(visible = isEditingProfile) {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        isSavingProfile = true
                                        try {
                                            val resp = NetworkClient.getProfileApi(context)
                                                .updateProfile(mapOf("email" to editEmail))
                                            if (resp.isSuccessful) {
                                                user = resp.body()
                                                isEditingProfile = false
                                                showMsg(msgProfileUpdated)
                                            } else {
                                                showMsg(msgUpdateFailed)
                                            }
                                        } finally {
                                            isSavingProfile = false
                                        }
                                    }
                                },
                                enabled = !isSavingProfile,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isSavingProfile) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(stringResource(R.string.save_changes_profile), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Language Card ──────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFEFF6FF), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Language,
                                null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            stringResource(R.string.language),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                    LanguageSwitcher()
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Change Password Card ───────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { showPasswordSection = !showPasswordSection },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lock,
                                null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.change_password), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Icon(
                            if (showPasswordSection) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            null,
                            tint = Color(0xFF9CA3AF)
                        )
                    }

                    AnimatedVisibility(visible = showPasswordSection) {
                        Column {
                            Spacer(Modifier.height(16.dp))

                            PasswordField(
                                value = currentPassword,
                                onValueChange = { currentPassword = it },
                                label = stringResource(R.string.current_password),
                                showPassword = showCurrentPw,
                                onToggleShow = { showCurrentPw = !showCurrentPw }
                            )

                            Spacer(Modifier.height(10.dp))

                            PasswordField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = stringResource(R.string.new_password),
                                showPassword = showNewPw,
                                onToggleShow = { showNewPw = !showNewPw }
                            )

                            Spacer(Modifier.height(10.dp))

                            PasswordField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = stringResource(R.string.confirm_new_password),
                                showPassword = showNewPw,
                                onToggleShow = { showNewPw = !showNewPw },
                                isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword
                            )

                            if (confirmPassword.isNotEmpty() && confirmPassword != newPassword) {
                                Text(
                                    stringResource(R.string.passwords_dont_match),
                                    color = Color(0xFFDC2626),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                )
                            }

                            // Password strength indicator
                            if (newPassword.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                PasswordStrengthBar(newPassword)
                            }

                            Spacer(Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (newPassword != confirmPassword) {
                                        showMsg(msgPasswordsDontMatch)
                                        return@Button
                                    }
                                    if (newPassword.length < 6) {
                                        showMsg(msgPasswordTooShort)
                                        return@Button
                                    }
                                    scope.launch {
                                        isSavingPassword = true
                                        try {
                                            val resp = NetworkClient.getProfileApi(context)
                                                .changePassword(
                                                    mapOf(
                                                        "currentPassword" to currentPassword,
                                                        "newPassword" to newPassword
                                                    )
                                                )
                                            when (resp.code()) {
                                                200 -> {
                                                    currentPassword = ""
                                                    newPassword = ""
                                                    confirmPassword = ""
                                                    showPasswordSection = false
                                                    showMsg(msgPasswordChanged)
                                                }
                                                403 -> showMsg(msgWrongPassword)
                                                else -> showMsg(msgFailedChangePw)
                                            }
                                        } finally {
                                            isSavingPassword = false
                                        }
                                    }
                                },
                                enabled = !isSavingPassword && currentPassword.isNotEmpty()
                                        && newPassword.isNotEmpty() && confirmPassword.isNotEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isSavingPassword) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(stringResource(R.string.update_password), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Privacy & Legal Card ───────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.privacy_policy)) },
                        leadingContent = {
                            Icon(
                                Icons.Default.PrivacyTip,
                                contentDescription = null,
                                tint = Color(0xFF2563EB)
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF)
                            )
                        },
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://www.wisefintrakr.com/privacy")
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.terms_of_service)) },
                        leadingContent = {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(0xFF2563EB)
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF)
                            )
                        },
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://www.wisefintrakr.com/terms")
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Logout Button ──────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            TokenManager(context).clearToken()
                            onLogout()
                        }
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFEE2E2), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Logout,
                            null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        stringResource(R.string.log_out),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Color(0xFFDC2626)
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        Icons.Default.ChevronRight,
                        null,
                        tint = Color(0xFFDC2626).copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    readOnly: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = Color(0xFF9CA3AF))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF111827))
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    showPassword: Boolean,
    onToggleShow: () -> Unit,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF2563EB)) },
        trailingIcon = {
            IconButton(onClick = onToggleShow) {
                Icon(
                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    null,
                    tint = Color(0xFF9CA3AF)
                )
            }
        },
        visualTransformation = if (showPassword) VisualTransformation.None
        else PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = isError,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF2563EB),
            unfocusedBorderColor = Color(0xFFE5E7EB),
            errorBorderColor = Color(0xFFDC2626)
        )
    )
}

@Composable
private fun PasswordStrengthBar(password: String) {
    val strength = when {
        password.length >= 12 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isDigit() } &&
                password.any { !it.isLetterOrDigit() } -> 3
        password.length >= 8 &&
                (password.any { it.isUpperCase() } || password.any { it.isDigit() }) -> 2
        password.length >= 6 -> 1
        else -> 0
    }
    val (label, color) = when (strength) {
        3 -> stringResource(R.string.strength_strong) to Color(0xFF059669)
        2 -> stringResource(R.string.strength_medium) to Color(0xFFF59E0B)
        1 -> stringResource(R.string.strength_weak) to Color(0xFFEF4444)
        else -> stringResource(R.string.strength_too_short) to Color(0xFFD1D5DB)
    }

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            if (i < strength) color else Color(0xFFE5E7EB),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
    }
}