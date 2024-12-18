import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun App() {
    var count by remember { mutableStateOf(0) }
    Card(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Column {
            Text("Counter: $count", fontSize = 48.sp)
            Button(onClick = { count++ }) {
                Text("JetBrains", fontSize = 48.sp)
            }
        }
    }
}
