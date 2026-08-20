package network.bahn.androidcryptowallet.ui.util

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
fun SecureWindow() {
    val activity = LocalActivity.current
    DisposableEffect(activity) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
