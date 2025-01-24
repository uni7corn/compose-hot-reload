import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.reload.AfterHotReloadEffect
import org.jetbrains.compose.reload.staticHotReloadScope

@Composable
fun SomeWidget() {
    LaunchedEffect(Unit) {

    }

    AfterHotReloadEffect {

    }
    staticHotReloadScope.invokeAfterHotReload {

    }

    val transition = rememberInfiniteTransition()
    val scale by transition.animateFloat(
        0.95f, 1.04f, infiniteRepeatable(tween(500), RepeatMode.Reverse)
    )

    Text("👋 Hello from 'widgets' :)", fontSize = 24.0.sp, modifier = Modifier.scale(scale))
}
