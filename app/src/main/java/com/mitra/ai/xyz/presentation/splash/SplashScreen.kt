package com.mitra.ai.xyz.presentation.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit,
    viewModel: SplashViewModel
) {
    val isReady by viewModel.isReady.collectAsState()
    var startAnimation by remember { mutableStateOf(false) }
    
    // Logo animations
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    val logoRotation by animateFloatAsState(
        targetValue = if (startAnimation) 720f else 0f,
        animationSpec = tween(
            durationMillis = 1500,
            easing = FastOutSlowInEasing
        )
    )

    // Background circle animations
    val circleScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        )
    )

    // Text animations with sequential delay
    val titleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 500,
            delayMillis = 700,
            easing = LinearEasing
        )
    )

    val titleSlide by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 50f,
        animationSpec = tween(
            durationMillis = 500,
            delayMillis = 700,
            easing = FastOutSlowInEasing
        )
    )

    val subtitleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 500,
            delayMillis = 900,
            easing = LinearEasing
        )
    )

    val subtitleSlide by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 30f,
        animationSpec = tween(
            durationMillis = 500,
            delayMillis = 900,
            easing = FastOutSlowInEasing
        )
    )

    // Loading dots animation
    val infiniteTransition = rememberInfiniteTransition()
    val dotsAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Trigger animations and handle completion
    LaunchedEffect(key1 = true) {
        startAnimation = true
    }

    LaunchedEffect(key1 = isReady) {
        if (isReady) {
            delay(2000) // Give time for animations to play
            onSplashComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        // Animated background circle
        Box(
            modifier = Modifier
                .size(280.dp)
                .scale(circleScale)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated logo with background
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(logoScale)
                    .rotate(logoRotation),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.SmartToy,
                    contentDescription = "App Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(80.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Animated title with slide
            Text(
                text = "Mitra AI",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    letterSpacing = (-1).sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .alpha(titleAlpha)
                    .graphicsLayer { 
                        translationY = titleSlide
                    }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Animated subtitle with slide
            Text(
                text = "Your Intelligent Assistant",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .alpha(subtitleAlpha)
                    .graphicsLayer { 
                        translationY = subtitleSlide
                    }
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Loading dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.alpha(if (startAnimation) 1f else 0f)
            ) {
                repeat(3) { index ->
                    val dotDelay = index * 100
                    val dotAlpha by animateFloatAsState(
                        targetValue = if (startAnimation) 1f else 0f,
                        animationSpec = tween(
                            durationMillis = 300,
                            delayMillis = 1200 + dotDelay
                        )
                    )
                    LoadingDot(
                        color = MaterialTheme.colorScheme.primary,
                        size = 10.dp,
                        alpha = dotsAlpha * dotAlpha
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingDot(
    color: Color,
    size: Dp,
    alpha: Float = 1f
) {
    Box(
        modifier = Modifier
            .size(size)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color)
    )
} 