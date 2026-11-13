package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.HisabDatabase
import com.example.data.local.OpeningBalanceDataStore
import com.example.data.repository.HisabRepository
import com.example.ui.BulletHisabApp
import com.example.ui.theme.BulletHisabTheme
import com.example.ui.viewmodel.HisabViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = HisabDatabase.getInstance(applicationContext)
        val openingBalanceStore = OpeningBalanceDataStore(applicationContext)
        val repository = HisabRepository(
            transactionDao = database.transactionDao(),
            debtDao = database.debtDao(),
            recurringDao = database.recurringDao(),
            openingBalanceStore = openingBalanceStore
        )

        setContent {
            BulletHisabTheme {
                val vm: HisabViewModel = viewModel(
                    factory = HisabViewModel.Factory(repository)
                )
                BulletHisabApp(viewModel = vm)
            }
        }
    }
}
