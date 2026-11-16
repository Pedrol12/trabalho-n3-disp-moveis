package br.edu.unirv.lineup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import br.edu.unirv.lineup.ui.navigation.AppRoot
import br.edu.unirv.lineup.ui.theme.LineupTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LineupTheme {
                AppRoot(viewModel)
            }
        }
    }
}
