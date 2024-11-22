import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    var state by remember { mutableStateOf(24) }

    Column {
        Button(onClick = { state++ }) {
            Text("Hello")
        }
        Text("👋 $state !", fontSize = 24.0.sp, modifier = Modifier.scale(scale))
    }
}

