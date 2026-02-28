package org.androidbible.ui.screens.readingplan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.androidbible.domain.model.ReadingPlan
import org.androidbible.domain.repository.ReadingPlanRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ReadingPlanScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { ReadingPlanScreenModel() }
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Reading Plans") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        ) { padding ->
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.plans.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    Text("No reading plans available")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.plans) { plan ->
                        ReadingPlanCard(plan)
                    }
                }
            }
        }
    }
}

@Composable
fun ReadingPlanCard(plan: ReadingPlan) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = plan.title,
                style = MaterialTheme.typography.titleMedium,
            )
            if (!plan.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = plan.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${plan.totalDays} days",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

data class ReadingPlanState(
    val plans: List<ReadingPlan> = emptyList(),
    val isLoading: Boolean = false,
)

class ReadingPlanScreenModel : ScreenModel, KoinComponent {

    private val planRepo: ReadingPlanRepository by inject()

    private val _state = MutableStateFlow(ReadingPlanState())
    val state: StateFlow<ReadingPlanState> = _state.asStateFlow()

    init {
        _state.value = _state.value.copy(isLoading = true)
        screenModelScope.launch {
            planRepo.getReadingPlans().collect { plans ->
                _state.value = _state.value.copy(plans = plans, isLoading = false)
            }
        }
    }
}
