package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.ReminderScheduler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ensure daily reminder alarm is active if enabled
        if (ReminderScheduler.isReminderEnabled(this)) {
            ReminderScheduler.scheduleDailyReminder(this)
        }

        val openQuickAdd = intent?.getBooleanExtra("OPEN_QUICK_ADD", false) ?: false

        setContent {
            MyApplicationTheme {
                val viewModel: FinanceViewModel = viewModel(
                    factory = FinanceViewModel.provideFactory(application)
                )
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppScreen(
                        viewModel = viewModel,
                        initialOpenQuickAdd = openQuickAdd
                    )
                }
            }
        }
    }
}
