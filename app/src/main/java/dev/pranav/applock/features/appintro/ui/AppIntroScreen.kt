package dev.pranav.applock.features.appintro.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavController
import dev.pranav.appintro.AppIntro
import dev.pranav.appintro.IntroPage
import dev.pranav.applock.core.navigation.Screen
import dev.pranav.applock.core.utils.isAccessibilityServiceEnabled
import dev.pranav.applock.core.utils.launchProprietaryOemSettings
import dev.pranav.applock.features.appintro.domain.AppIntroManager
import dev.pranav.applock.ui.icons.Accessibility
import dev.pranav.applock.ui.icons.BatterySaver
import dev.pranav.applock.ui.icons.Display

@SuppressLint("BatteryLife")
@Composable
fun AppIntroScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    var overlayPermissionGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var notificationPermissionGranted by remember {
        mutableStateOf(
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        )
    }
    var accessibilityServiceEnabled by remember {
        mutableStateOf(context.isAccessibilityServiceEnabled())
    }

    val requestPermissionLauncher =
        if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    notificationPermissionGranted = true
                } else {
                    Toast.makeText(
                        context,
                        "Notification permission is required for AppLock to function properly.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        } else {
            null
        }

    LaunchedEffect(key1 = context) {
        overlayPermissionGranted = Settings.canDrawOverlays(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionGranted =
                NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        accessibilityServiceEnabled = context.isAccessibilityServiceEnabled()
    }

    val onFinishCallback = {
        AppIntroManager.markIntroAsCompleted(context)
        navController.navigate(Screen.SetPassword.route) {
            popUpTo(Screen.AppIntro.route) { inclusive = true }
        }
    }

    val introPages = listOf(
        IntroPage(
            title = "Welcome to AppLock",
            description = "Protect your apps and privacy with AppLock. We'll guide you through a quick setup.",
            icon = Icons.Filled.Lock,
            backgroundColor = Color(0xFF0F52BA),
            contentColor = Color.White,
            onNext = { true }),

        IntroPage(
            title = "Secure Your Apps",
            description = "Keep your private apps protected with advanced locking mechanisms",
            icon = Icons.Default.Lock,
            backgroundColor = Color(0xFF3C9401),
            contentColor = Color.White,
            onNext = { true }),

        IntroPage(
            title = "Display Over Apps",
            description = "AppLock needs permission to display over other apps to show the lock screen. Tap 'Next' and enable the permission.",
            icon = Display,
            backgroundColor = Color(0xFFDC143C),
            contentColor = Color.White,
            onNext = {
                overlayPermissionGranted = Settings.canDrawOverlays(context)
                if (!overlayPermissionGranted) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    false
                } else {
                    true
                }
            }),

        IntroPage(
            title = "Disable Battery Optimization",
            description = "To ensure AppLock runs reliably in the background, please disable battery optimizations for the app. Tap 'Next' to open settings.",
            icon = BatterySaver,
            backgroundColor = Color(0xFF08A471),
            contentColor = Color.White,
            onNext = {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val isIgnoringOptimizations =
                    powerManager.isIgnoringBatteryOptimizations(context.packageName)

                if (!isIgnoringOptimizations) {
                    launchProprietaryOemSettings(context)
                    return@IntroPage false
                }
                return@IntroPage true
            }),

        IntroPage(
            title = "Notification Permission",
            description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) "AppLock needs permission to show notifications to keep you informed and keep running in background properly. Tap 'Next' to grant permission."
            else "Notification permission is automatically granted on your Android version.",
            icon = Icons.Default.Notifications,
            backgroundColor = Color(0xFFE78A02),
            contentColor = Color.White,
            onNext = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val isGrantedCurrently =
                        NotificationManagerCompat.from(context).areNotificationsEnabled()
                    notificationPermissionGranted = isGrantedCurrently

                    if (!isGrantedCurrently) {
                        requestPermissionLauncher?.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        return@IntroPage false
                    } else {
                        return@IntroPage true
                    }
                } else {
                    true
                }
            }),

        IntroPage(
            title = "Accessibility Service",
            description = "Accessibility service is required for AppLock to function properly. Tap 'Next' to enable it.",
            icon = Accessibility,
            backgroundColor = Color(0xFFF1550E),
            contentColor = Color.White,
            onNext = {
                accessibilityServiceEnabled = context.isAccessibilityServiceEnabled()
                if (!accessibilityServiceEnabled) {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    false
                } else {
                    true
                }
            }),

        IntroPage(
            title = "Complete Privacy",
            description = "Your data never leaves your device. AppLock protects your privacy at all times.",
            icon = Icons.Default.Lock,
            backgroundColor = Color(0xFF0047AB),
            contentColor = Color.White,
            onNext = {
                // Re-check all permissions before finishing
                overlayPermissionGranted = Settings.canDrawOverlays(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionGranted =
                        NotificationManagerCompat.from(context).areNotificationsEnabled()
                }
                accessibilityServiceEnabled = context.isAccessibilityServiceEnabled()

                val allPermissionsGranted =
                    overlayPermissionGranted && notificationPermissionGranted && accessibilityServiceEnabled

                if (!allPermissionsGranted) {
                    Toast.makeText(
                        context, "All permissions are required to proceed.", Toast.LENGTH_SHORT
                    ).show()
                }
                allPermissionsGranted
            })
    )

    AppIntro(
        pages = introPages,
        onSkip = {
            AppIntroManager.markIntroAsCompleted(context)
            navController.navigate(Screen.SetPassword.route) {
                popUpTo(Screen.AppIntro.route) { inclusive = true }
            }
        },
        onFinish = onFinishCallback,
        showSkipButton = false,
        useAnimatedPager = true,
        nextButtonText = "Next",
        finishButtonText = "Get Started"
    )
}
