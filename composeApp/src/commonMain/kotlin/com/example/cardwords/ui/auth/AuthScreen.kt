package com.example.cardwords.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val Black            = Color(0xFF000000)
private val FieldBg          = Color(0xFFF2F2F2)
private val PlaceholderColor = Color(0xFFAAAAAA)
private val SubtitleGray     = Color(0xFF8A8A8A)
private val ErrorRed         = Color(0xFFD32F2F)
private val TabInactive      = Color(0xFFAAAAAA)

@Composable
fun AuthScreen(
    initialTab: AuthTab = AuthTab.LOGIN,
    onAuthSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val viewModel = remember { AuthViewModel() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.switchTab(initialTab)
    }

    LaunchedEffect(state.phase) {
        if (state.phase == AuthPhase.SUCCESS) onAuthSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(64.dp))

        Text(
            text = "CardWords",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Black,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Учи слова эффективно",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = SubtitleGray,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(40.dp))

        // Tabs
        Row(modifier = Modifier.fillMaxWidth()) {
            TabItem(
                text = "Вход",
                isSelected = state.tab == AuthTab.LOGIN,
                onClick = { viewModel.switchTab(AuthTab.LOGIN) },
                modifier = Modifier.weight(1f),
            )
            TabItem(
                text = "Регистрация",
                isSelected = state.tab == AuthTab.REGISTER,
                onClick = { viewModel.switchTab(AuthTab.REGISTER) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(28.dp))

        // Fields — fade + partial slide: fade masks overlap, slide gives direction
        AnimatedContent(
            targetState = state.tab,
            transitionSpec = {
                val dir = if (targetState == AuthTab.REGISTER) 1 else -1
                (fadeIn(tween(250)) + slideInHorizontally(tween(300)) { it / 4 * dir })
                    .togetherWith(
                        fadeOut(tween(200)) + slideOutHorizontally(tween(300)) { -it / 4 * dir }
                    )
            },
            label = "auth_tab",
            modifier = Modifier.fillMaxWidth(),
        ) { tab ->
            Column(modifier = Modifier.fillMaxWidth()) {
                if (tab == AuthTab.REGISTER) {
                    AuthField(
                        value = state.name,
                        onValueChange = viewModel::onNameChanged,
                        placeholder = "Имя",
                        keyboardType = KeyboardType.Text,
                    )
                    Spacer(Modifier.height(12.dp))
                }

                AuthField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChanged,
                    placeholder = "Email",
                    keyboardType = KeyboardType.Email,
                )
                Spacer(Modifier.height(12.dp))

                AuthField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChanged,
                    placeholder = "Пароль",
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    imeAction = ImeAction.Done,
                    onDone = {
                        if (tab == AuthTab.LOGIN) viewModel.login()
                        else viewModel.register()
                    },
                )
            }
        }

        // Error
        if (state.error != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = state.error!!,
                fontSize = 13.sp,
                color = ErrorRed,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(28.dp))

        // Submit button
        val isLoading = state.phase == AuthPhase.LOADING
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(50))
                .background(Black)
                .then(
                    if (!isLoading) Modifier.clickable {
                        if (state.tab == AuthTab.LOGIN) viewModel.login()
                        else viewModel.register()
                    } else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = if (state.tab == AuthTab.LOGIN) "Войти" else "Создать аккаунт",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(24.dp))

        Text(
            text = "Назад",
            fontSize = 14.sp,
            color = SubtitleGray,
            modifier = Modifier
                .clickable(onClick = onNavigateBack)
                .padding(12.dp),
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TabItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Black else TabInactive,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (isSelected) Black else Color.Transparent),
        )
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    isPassword: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        placeholder = {
            Text(
                text = placeholder,
                color = PlaceholderColor,
                fontSize = 15.sp,
            )
        },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        keyboardActions = if (onDone != null) KeyboardActions(onDone = { onDone() }) else KeyboardActions.Default,
        colors = TextFieldDefaults.colors(
            focusedTextColor = Black,
            unfocusedTextColor = Black,
            focusedContainerColor = FieldBg,
            unfocusedContainerColor = FieldBg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = Black,
        ),
    )
}
