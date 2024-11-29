import androidx.compose.animation.core.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.reload.DevelopmentEntryPoint

@DevelopmentEntryPoint
@Composable
fun SomeWidget() {
    val transition = rememberInfiniteTransition()
    val scale by transition.animateFloat(
        0.95f, 1.04f, infiniteRepeatable(tween(500), RepeatMode.Reverse)
    )

    Text("👋 Hello from 'widgets'!", fontSize = 24.0.sp, modifier = Modifier.scale(scale))
}

