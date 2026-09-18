package com.example.cognicare.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cognicare.R
import com.example.cognicare.data.demo.DemoData
import com.example.cognicare.ui.components.BrandLockup
import com.example.cognicare.ui.components.CaregiverPageHeader
import com.example.cognicare.ui.components.CaregiverScrollPage
import com.example.cognicare.ui.components.DashboardCard
import com.example.cognicare.ui.components.ONBOARDING_STEP_COUNT
import com.example.cognicare.ui.components.SafetyBanner
import com.example.cognicare.ui.theme.CaregiverTheme
import com.example.cognicare.viewmodel.CaregiverLoginError
import com.example.cognicare.viewmodel.CaregiverLoginViewModel

@Composable
fun CaregiverLoginScreen(
    onBack: () -> Unit,
    viewModel: CaregiverLoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = CaregiverTheme.colors
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
        ) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.action_back)
                            )
                        }
                        BrandLockup(
                            tagline = stringResource(R.string.brand_tagline_caregiver),
                            markSize = 36.dp
                        )
                    }
                    HorizontalDivider(color = colors.border)
                }
            }

            CaregiverScrollPage {
                CaregiverPageHeader(
                    eyebrow = stringResource(
                        R.string.step_eyebrow,
                        ONBOARDING_STEP_COUNT,
                        ONBOARDING_STEP_COUNT,
                        stringResource(R.string.step_sign_in)
                    ),
                    title = stringResource(R.string.caregiver_login_title),
                    subtitle = stringResource(R.string.caregiver_login_subtitle)
                )

                DashboardCard(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = viewModel::onEmailChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.label_email)) },
                        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                        singleLine = true,
                        isError = state.error == CaregiverLoginError.INVALID_EMAIL,
                        supportingText = if (state.error == CaregiverLoginError.INVALID_EMAIL) {
                            { Text(stringResource(R.string.error_invalid_email)) }
                        } else {
                            null
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.label_password)) },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = stringResource(
                                        if (passwordVisible) R.string.cd_hide_password else R.string.cd_show_password
                                    )
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = state.error == CaregiverLoginError.INVALID_CREDENTIALS || state.error == CaregiverLoginError.NETWORK_ERROR,
                        supportingText = when (state.error) {
                            CaregiverLoginError.INVALID_CREDENTIALS -> { { Text(stringResource(R.string.error_invalid_credentials)) } }
                            CaregiverLoginError.NETWORK_ERROR -> { { Text(stringResource(R.string.error_network)) } }
                            else -> null
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { viewModel.signIn() }),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = viewModel::signIn,
                        enabled = state.canSubmit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (state.isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = LocalContentColor.current,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.action_sign_in), style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.width(10.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                DemoCredentialsCard(onUse = viewModel::useDemoCredentials)

                SafetyBanner(
                    title = stringResource(R.string.caregiver_login_safety_title),
                    body = stringResource(R.string.caregiver_login_safety_body),
                    tag = null
                )
            }
        }
    }
}

@Composable
private fun DemoCredentialsCard(onUse: () -> Unit) {
    val colors = CaregiverTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.demo.container)
            .border(1.dp, colors.demoBorder, shape)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.caregiver_login_demo_title),
                style = MaterialTheme.typography.labelLarge,
                color = colors.demo.content
            )
            Text(
                text = stringResource(
                    R.string.caregiver_login_demo_body,
                    DemoData.CAREGIVER_EMAIL,
                    DemoData.CAREGIVER_PASSWORD
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.demo.content
            )
        }
        TextButton(onClick = onUse) {
            Text(stringResource(R.string.action_use_demo))
        }
    }
}
