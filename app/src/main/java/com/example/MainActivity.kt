package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModelProvider
import com.example.data.LunaDatabase
import com.example.data.LunaRepository
import com.example.data.WorkspaceTask
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Room Database, DAO and single repository instance
        val db = LunaDatabase.getDatabase(this)
        val repository = LunaRepository(db.taskDao, db.chatDao)
        val viewModelFactory = LunaViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[LunaViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                    bottomBar = {
                        LunaBottomNavigationBar(
                            selectedTab = viewModel.currentTab,
                            onTabSelected = { viewModel.currentTab = it }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF0C0E17), // DeepMidnight
                                        Color(0xFF13172E)  // Soft dark violet background glow
                                    )
                                )
                            )
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = viewModel.currentTab,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220)) togetherWith
                                fadeOut(animationSpec = tween(220))
                            },
                            label = "tab_switch"
                        ) { tab ->
                            when (tab) {
                                0 -> DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToTab = { viewModel.currentTab = it }
                                )
                                1 -> TasksScreen(viewModel = viewModel)
                                2 -> AIChatScreen(viewModel = viewModel)
                                3 -> PromptBlueprintsScreen(viewModel = viewModel)
                            }
                        }
                        
                        // Action Dialog for Creating New Task
                        if (viewModel.showAddTaskDialog) {
                            AddTaskDialog(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// TAB 0: DASHBOARD SCREEN
// -----------------------------------------------------------------
@Composable
fun DashboardScreen(
    viewModel: LunaViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val tasks by viewModel.tasksState.collectAsState()
    val rawHistory by viewModel.chatHistory.collectAsState()
    
    // Quick calculations for the status indicators
    val completedCount = tasks.count { it.isCompleted }
    val totalCount = tasks.size
    val activeCount = totalCount - completedCount
    val aiDecomposedCount = tasks.count { it.aiDecomposed }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        // Welcome and Header Area
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Luna Workspace",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        ),
                        color = Color(0xFFFAFAFC), // PremiumWhite
                        modifier = Modifier.testTag("app_title")
                    )
                    Text(
                        text = "Aesthetic generative creative engine",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB5BDD3) // SoftGrayText
                    )
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6C5DD3).copy(alpha = 0.2f))
                        .border(1.dp, Color(0xFF6C5DD3), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Luna AI Logo Spark",
                        tint = Color(0xFF00E5FF) // CyberTeal
                    )
                }
            }
        }

        // Live Performance Stats Glassmorphic Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF6C5DD3).copy(alpha = 0.3f), Color(0xFF00E5FF).copy(alpha = 0.3f))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151829))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "WORKSPACE METRICS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricItem(label = "Active Goals", count = activeCount.toString(), icon = Icons.Default.List)
                        MetricItem(label = "Done Goals", count = completedCount.toString(), icon = Icons.Default.CheckCircle)
                        MetricItem(label = "AI Subtasks", count = aiDecomposedCount.toString(), icon = Icons.Default.Build)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val progressPercent = if (totalCount > 0) (completedCount.toFloat() / totalCount) else 0.0f
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Project Completion Progress",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB5BDD3)
                        )
                        Text(
                            text = "${(progressPercent * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF6C5DD3),
                        trackColor = Color(0xFF292D46)
                    )
                }
            }
        }

        // Quick Entry Actions Grid
        item {
            Text(
                text = "DOCK ACTIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9D4EDD), // SoftPurple
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier
                    .fillHorizontal()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "AI Chat Desk",
                    desc = "Ask Gemini models",
                    icon = Icons.Default.Send,
                    accentColor = Color(0xFF6C5DD3),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToTab(2) }
                )
                QuickActionCard(
                    title = "Add Goals",
                    desc = "Store new projects",
                    icon = Icons.Default.AddCircle,
                    accentColor = Color(0xFF00E5FF),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.showAddTaskDialog = true }
                )
            }
        }

        // Smart Prompt Shortcuts Area
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FEATURED SCRIPT PROMPTS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9D4EDD),
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "View All Templates",
                    fontSize = 12.sp,
                    color = Color(0xFF00E5FF),
                    modifier = Modifier
                        .clickable { onNavigateToTab(3) }
                        .padding(4.dp)
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PromptShortcutItem(
                    label = "⚡ Code Refactor",
                    summary = "Refactor Kotlin functions cleanly.",
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            viewModel.selectedPersona = "Luna-Coder"
                            viewModel.chatInputText = "Could you please help me refactor this Kotlin function to be more idiomatic and performant?\n\n```kotlin\n\n```"
                            onNavigateToTab(2)
                        }
                )
                PromptShortcutItem(
                    label = "✍️ Smart Writer",
                    summary = "Rewrite text in high-end brand tone.",
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            viewModel.selectedPersona = "Luna-Writer"
                            viewModel.chatInputText = "Rewrite the following text into executive, professional corporate copywriting of premium grade: "
                            onNavigateToTab(2)
                        }
                )
            }
        }

        // Recents Dashboard Preview
        item {
            Text(
                text = "ACTIVE PLANNER CHECKS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF),
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (tasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151829).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Empty goals indicator",
                            tint = Color(0xFF292D46),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No custom goals active",
                            color = Color(0xFFB5BDD3),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            items(tasks.take(3)) { task ->
                DashboardTaskItem(task = task, onCheckedChange = { viewModel.toggleTaskComplete(task) })
            }
        }
    }
}

@Composable
fun MetricItem(label: String, count: String, icon: ImageVector) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFB5BDD3),
                modifier = Modifier
                    .size(14.dp)
                    .padding(end = 4.dp)
            )
            Text(text = label, fontSize = 11.sp, color = Color(0xFFB5BDD3))
        }
        Text(
            text = count,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFAFAFC),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun QuickActionCard(
    title: String,
    desc: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(98.dp)
            .border(1.dp, Color(0xFF292D46), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151829)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                }
                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF292D46))
            }
            Column {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFAFAFC))
                Text(text = desc, fontSize = 10.sp, color = Color(0xFFB5BDD3))
            }
        }
    }
}

@Composable
fun PromptShortcutItem(
    label: String,
    summary: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, Color(0xFF292D46), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151829).copy(alpha = 0.7f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFAFAFC),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = summary,
                fontSize = 11.sp,
                color = Color(0xFFB5BDD3),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun DashboardTaskItem(
    task: WorkspaceTask,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .border(1.dp, Color(0xFF292D46).copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151829).copy(alpha = 0.6f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF6C5DD3),
                    uncheckedColor = Color(0xFF292D46)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = task.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (task.isCompleted) Color(0xFFB5BDD3) else Color(0xFFFAFAFC),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Priority: ${task.priority} • Category: ${task.category}",
                    fontSize = 11.sp,
                    color = Color(0xFFB5BDD3).copy(alpha = 0.8f)
                )
            }
        }
    }
}

fun Modifier.fillHorizontal(): Modifier = this.fillMaxWidth()

// -----------------------------------------------------------------
// TAB 1: WORKSPACE TASKS & DECOMPOSITION SCREEN
// -----------------------------------------------------------------
@Composable
fun TasksScreen(viewModel: LunaViewModel) {
    val tasks by viewModel.tasksState.collectAsState()
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    val categories = listOf("ALL", "GENERAL", "CREATIVE", "WORK", "EXPERIMENT")

    val filteredTasks = if (selectedCategoryFilter == "ALL") {
        tasks
    } else {
        tasks.filter { it.category == selectedCategoryFilter }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Screen Header & Clear
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Creative Workspace",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFAFAFC)
                    )
                    Text(
                        text = "AI Task Planner and Decomposition Core",
                        fontSize = 12.sp,
                        color = Color(0xFFB5BDD3)
                    )
                }
                if (tasks.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearAllTasks() },
                        modifier = Modifier.testTag("clear_tasks_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear all workspace tasks",
                            tint = Color(0xFFFF4D4D)
                        )
                    }
                }
            }

            // Filtering Chips Row
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { cat ->
                            val isSelected = selectedCategoryFilter == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) Color(0xFF6C5DD3) else Color(0xFF151829)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color(0xFF6C5DD3) else Color(0xFF292D46),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { selectedCategoryFilter = cat }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFFFAFAFC) else Color(0xFFB5BDD3)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable Task List
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            tint = Color(0xFF292D46),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No custom tasks here",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFB5BDD3)
                        )
                        Text(
                            text = "Tap the plus button below to create one!",
                            fontSize = 12.sp,
                            color = Color(0xFFB5BDD3).copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        WorkspaceTaskItemCard(task = task, viewModel = viewModel)
                    }
                }
            }
        }

        // Floating Action Button to Add New Goal
        FloatingActionButton(
            onClick = { viewModel.showAddTaskDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_task_fab")
                .minimumInteractiveComponentSize(),
            containerColor = Color(0xFF6C5DD3),
            contentColor = Color(0xFFFAFAFC)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Trigger Add Task Dialog"
            )
        }
    }
}

@Composable
fun WorkspaceTaskItemCard(task: WorkspaceTask, viewModel: LunaViewModel) {
    val subtasks = remember(task.subtasksJson) { deserializeSubTasks(task.subtasksJson) }
    val isDecomposing = viewModel.decomposingTaskId == task.id
    
    // Rotating key animation for Gemini processing loading loops
    val infiniteTransition = rememberInfiniteTransition(label = "sparks")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF292D46), RoundedCornerShape(14.dp))
            .testTag("task_card_${task.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151829)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Task priority and category badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (task.priority) {
                                    "HIGH" -> Color(0xFFFF4D4D).copy(alpha = 0.15f)
                                    "MEDIUM" -> Color(0xFFFFB703).copy(alpha = 0.15f)
                                    else -> Color(0xFF00E5FF).copy(alpha = 0.15f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.priority,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (task.priority) {
                                "HIGH" -> Color(0xFFFF4D4D)
                                "MEDIUM" -> Color(0xFFFFB703)
                                else -> Color(0xFF00E5FF)
                            }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF292D46))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.category,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB5BDD3)
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.deleteTask(task) },
                    modifier = Modifier.size(24.dp).testTag("delete_task_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete task item",
                        tint = Color(0xFFFF4D4D).copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title and Description
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (task.isCompleted) Color(0xFFB5BDD3).copy(alpha = 0.6f) else Color(0xFFFAFAFC)
                    )
                    if (task.description.isNotEmpty()) {
                        Text(
                            text = task.description,
                            fontSize = 12.sp,
                            color = Color(0xFFB5BDD3),
                            modifier = Modifier.padding(top = 4.dp),
                            lineHeight = 15.sp
                        )
                    }
                }
                
                // Done check trigger box
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { viewModel.toggleTaskComplete(task) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF6C5DD3),
                        uncheckedColor = Color(0xFF292D46)
                    ),
                    modifier = Modifier.testTag("complete_checkbox_${task.id}")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI Deco triggers
            Divider(color = Color(0xFF292D46), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            if (!task.aiDecomposed) {
                // Not decomposed yet: Display AI button
                Button(
                    onClick = { viewModel.runAIDecompose(task) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDecomposing) Color(0xFF292D46) else Color(0xFF6C5DD3)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("decompose_btn_${task.id}"),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isDecomposing
                ) {
                    if (isDecomposing) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "AI Loading Animation",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier
                                .rotate(angle)
                                .size(18.dp)
                                .padding(end = 4.dp)
                        )
                        Text(
                            text = "Consulting Luna Gemini...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "AI Decomposer Sparks icon",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                        )
                        Text(
                            text = "Decompose Goal into Action Steps (AI)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Decomposed: Display Subtask list as checklist items!
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Decomposed tag spark",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Luna AI Checklist",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF),
                                letterSpacing = 1.sp
                            )
                        }
                        
                        Text(
                            text = "RE-DECOMPOSE (AI)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9D4EDD),
                            modifier = Modifier
                                .clickable { viewModel.runAIDecompose(task) }
                                .padding(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    subtasks.forEachIndexed { index, sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleSubTaskComplete(task, index) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (sub.isCompleted) Icons.Default.CheckCircle else Icons.Default.Check,
                                contentDescription = "Subtask complete bubble toggle",
                                tint = if (sub.isCompleted) Color(0xFF6C5DD3) else Color(0xFF292D46),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = sub.title,
                                fontSize = 13.sp,
                                color = if (sub.isCompleted) Color(0xFFB5BDD3).copy(alpha = 0.5f) else Color(0xFFFAFAFC)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// TAB 2: AI COMPANION CHAT & PERSONA LAB
// -----------------------------------------------------------------
@Composable
fun AIChatScreen(viewModel: LunaViewModel) {
    val rawHistory by viewModel.chatHistory.collectAsState()
    val isGenerating = viewModel.isGeneratingResponse
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Personas definition
    val personas = listOf(
        ChatMessagePersona("Luna-General", "Standard", Icons.Default.Face, "Helpful General AI"),
        ChatMessagePersona("Luna-Writer", "Writer", Icons.Default.Create, "Elite Brand Copywriter"),
        ChatMessagePersona("Luna-Coder", "Developer", Icons.Default.Build, "Senior Android Expert"),
        ChatMessagePersona("Luna-Brainstormer", "Creative", Icons.Default.Info, "Startup Planner")
    )

    // Scroll to latest message automatic trigger
    LaunchedEffect(rawHistory.size) {
        if (rawHistory.isNotEmpty()) {
            scope.launch {
                lazyListState.animateScrollToItem(rawHistory.size - 1)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Screen Header / Persona Selection Slider
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Luna Chat Companion",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFAFAFC)
                    )
                    Text(
                        text = "Gemini multi-agent sandbox engine",
                        fontSize = 12.sp,
                        color = Color(0xFFB5BDD3)
                    )
                }

                if (rawHistory.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearHistory() },
                        modifier = Modifier.testTag("chat_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset chat history logs",
                            tint = Color(0xFFFF4D4D)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Horizontally scrolling list of agent personas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                personas.forEach { per ->
                    val isSelected = viewModel.selectedPersona == per.id
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) Color(0xFF6C5DD3) else Color(0xFF151829)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF6C5DD3) else Color(0xFF292D46),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.selectedPersona = per.id }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = per.icon,
                            contentDescription = per.name,
                            tint = if (isSelected) Color(0xFFFAFAFC) else Color(0xFFB5BDD3),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = per.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFFFAFAFC) else Color(0xFFB5BDD3)
                        )
                    }
                }
            }
        }

        // Chat Conversation Log Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            if (rawHistory.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        tint = Color(0xFF292D46),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Initialize the Agent Sandbox",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFAFAFC)
                    )
                    Text(
                        text = "Hi, I am ${viewModel.selectedPersona.substringAfter("-")}. Send me a message below to start your creative process with Gemini! I persist chat history locally.",
                        fontSize = 12.sp,
                        color = Color(0xFFB5BDD3),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rawHistory) { msg ->
                        ChatBubbleRow(msg = msg)
                    }
                    if (isGenerating) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151829)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color(0xFF00E5FF),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Agent processing query...",
                                            fontSize = 12.sp,
                                            color = Color(0xFFB5BDD3)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Chat Input Section with Notch/Bottom-bar padding bounds
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF292D46), RoundedCornerShape(16.dp)),
            color = Color(0xFF151829)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = viewModel.chatInputText,
                    onValueChange = { viewModel.chatInputText = it },
                    placeholder = {
                        Text(text = "Prompt the model...", color = Color(0xFFB5BDD3).copy(alpha = 0.5f), fontSize = 13.sp)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_text"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color(0xFFFAFAFC),
                        unfocusedTextColor = Color(0xFFFAFAFC)
                    ),
                    maxLines = 4
                )

                IconButton(
                    onClick = { viewModel.sendChatMessage() },
                    modifier = Modifier
                        .testTag("chat_send_button")
                        .minimumInteractiveComponentSize(),
                    enabled = viewModel.chatInputText.isNotBlank() && !isGenerating
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Submit query to agent",
                        tint = if (viewModel.chatInputText.isNotBlank() && !isGenerating) Color(0xFF6C5DD3) else Color(0xFF292D46)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubbleRow(msg: com.example.data.ChatMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6C5DD3).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFF6C5DD3), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .fillHorizontalMax(0.82f)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) Color(0xFF6C5DD3) else Color(0xFF151829)
                )
                .border(
                    width = 1.dp,
                    color = if (isUser) Color(0xFF6C5DD3) else Color(0xFF292D46),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = msg.content,
                    fontSize = 13.sp,
                    color = Color(0xFFFAFAFC),
                    lineHeight = 17.sp
                )
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF292D46)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFFFAFAFC),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

fun Modifier.fillHorizontalMax(fraction: Float): Modifier = this.fillMaxWidth(fraction)

data class ChatMessagePersona(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val description: String
)

// -----------------------------------------------------------------
// TAB 3: PROMPT BLUEPRINTS
// -----------------------------------------------------------------
@Composable
fun PromptBlueprintsScreen(viewModel: LunaViewModel) {
    val blueprints = listOf(
        BlueprintCategory(
            "DEVELOPMENT CORE",
            listOf(
                PromptBlueprintItem(
                    "Senior Android Reviewer",
                    "Ask Gemini to audit codebase, pointing performance bottlenecks or architectural recommendations.",
                    "Act as a Principal Android Developer. Review the following code block for structural improvements, memory leaks, and performance optimizations. Provide clear refactors:\n\n"
                ),
                PromptBlueprintItem(
                    "API Model generator",
                    "Input a server model response example and generate complete Kotlin Serialized entity models.",
                    "Generate complete Kotlin Data Classes with annotated serialization annotations for the following server REST JSON response outline:\n\n"
                )
            )
        ),
        BlueprintCategory(
            "CREATIVE & COPYWRITING",
            listOf(
                PromptBlueprintItem(
                    "Elite Concept Naming",
                    "Instruct the writer to formulate modern trademarkable product names.",
                    "Produce 5 distinctive, asymmetrical, high-end product names for a startup described below. Keep descriptions short, snappy, and premium-tier:\n\n"
                ),
                PromptBlueprintItem(
                    "Landing Page Slogan Solver",
                    "Formulate three hyper-focused responsive page header taglines.",
                    "Formulate 3 high-impact marketing taglines for a Landing Page. Focus on bold copy, visual power, and minimalist structures:\n\n"
                )
            )
        ),
        BlueprintCategory(
            "LOGICAL SUMMARY",
            listOf(
                PromptBlueprintItem(
                    "Decompose Complex Idea",
                    "Take a dense article or goal and compile exactly 5 sequential high-value takeouts.",
                    "Analyze the following paragraph/topic, extracting exactly 5 distinct executive bullet points representing the high-value logic of the discussion. Be concise:\n\n"
                )
            )
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        item {
            Text(
                text = "Prompt Blueprints",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFAFAFC)
            )
            Text(
                text = "Pre-arranged generative formulas. Click on any to load into prompt sandbox.",
                fontSize = 12.sp,
                color = Color(0xFFB5BDD3),
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }

        blueprints.forEach { cat ->
            item {
                Text(
                    text = cat.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9D4EDD),
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )
            }

            items(cat.items) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.dp, Color(0xFF292D46), RoundedCornerShape(12.dp))
                        .clickable {
                            // Select target persona based on Blueprint Category
                            viewModel.selectedPersona = when {
                                cat.title.contains("DEV") -> "Luna-Coder"
                                cat.title.contains("CREATIVE") -> "Luna-Writer"
                                else -> "Luna-General"
                            }
                            viewModel.chatInputText = item.textTemplate
                            viewModel.currentTab = 2 // Redirect to Chat tab
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151829)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFAFAFC))
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(text = item.description, fontSize = 11.sp, color = Color(0xFFB5BDD3), lineHeight = 14.sp)
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Load Blueprint Template",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

data class BlueprintCategory(val title: String, val items: List<PromptBlueprintItem>)
data class PromptBlueprintItem(val title: String, val description: String, val textTemplate: String)

// -----------------------------------------------------------------
// ACTIONS DIALOG COMPONENT
// -----------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(viewModel: LunaViewModel) {
    val categories = listOf("GENERAL", "CREATIVE", "WORK", "EXPERIMENT")
    val priorities = listOf("LOW", "MEDIUM", "HIGH")

    Dialog(onDismissRequest = { viewModel.showAddTaskDialog = false }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color(0xFF6C5DD3), RoundedCornerShape(16.dp))
                .testTag("add_task_dialog"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151829)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Add Workspace Goal",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFAFAFC),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Goal Input Title
                Text(text = "TITLE", fontSize = 10.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = viewModel.inputTaskTitle,
                    onValueChange = { viewModel.inputTaskTitle = it },
                    placeholder = { Text("e.g. Design applet layout structure", color = Color(0xFF292D46)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 14.dp)
                        .testTag("dialog_task_title"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFFAFAFC),
                        unfocusedTextColor = Color(0xFFFAFAFC),
                        focusedBorderColor = Color(0xFF6C5DD3),
                        unfocusedBorderColor = Color(0xFF292D46)
                    ),
                    singleLine = true
                )

                // Goal Description Input
                Text(text = "DESCRIPTION", fontSize = 10.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = viewModel.inputTaskDesc,
                    onValueChange = { viewModel.inputTaskDesc = it },
                    placeholder = { Text("Outline details or expectations...", color = Color(0xFF292D46)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 14.dp)
                        .testTag("dialog_task_desc"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFFAFAFC),
                        unfocusedTextColor = Color(0xFFFAFAFC),
                        focusedBorderColor = Color(0xFF6C5DD3),
                        unfocusedBorderColor = Color(0xFF292D46)
                    ),
                    maxLines = 3
                )

                // Priority badging selector
                Text(text = "PRIORITY", fontSize = 10.sp, color = Color(0xFF9D4EDD), fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    priorities.forEach { prio ->
                        val isSelected = viewModel.inputTaskPriority == prio
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) Color(0xFF6C5DD3) else Color(0xFF0C0E17)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF6C5DD3) else Color(0xFF292D46),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.inputTaskPriority = prio }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = prio,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFFFAFAFC) else Color(0xFFB5BDD3)
                            )
                        }
                    }
                }

                // Category badging selection
                Text(text = "CATEGORY WORKSPACE", fontSize = 10.sp, color = Color(0xFF9D4EDD), fontWeight = FontWeight.Bold)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.chunked(2).forEach { rowList ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowList.forEach { cat ->
                                val isSelected = viewModel.inputTaskCategory == cat
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) Color(0xFF6C5DD3) else Color(0xFF0C0E17)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFF6C5DD3) else Color(0xFF292D46),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.inputTaskCategory = cat }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFFFAFAFC) else Color(0xFFB5BDD3)
                                    )
                                }
                            }
                        }
                    }
                }

                // Action controls: Cancel / Insert
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.showAddTaskDialog = false },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFAFAFC)),
                        border = BorderStroke(1.dp, Color(0xFF292D46)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "CANCEL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.addTask() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5DD3)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("dialog_add_button"),
                        shape = RoundedCornerShape(8.dp),
                        enabled = viewModel.inputTaskTitle.isNotBlank()
                    ) {
                        Text(text = "ADD GOAL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// IMMERSIVE COHESIVE M3 BOTTOM NAVIGATION
// -----------------------------------------------------------------
@Composable
fun LunaBottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars) // Essential for edge-to-edge layout buffer, stops clip!
            .border(width = (0.5).dp, color = Color(0xFF292D46)),
        color = Color(0xFF0C0E17)
    ) {
        NavigationBar(
            containerColor = Color(0xFF0C0E17),
            tonalElevation = 8.dp,
            modifier = Modifier.height(68.dp)
        ) {
            val items = listOf(
                NavigationTabItem(0, "Dashboard", Icons.Default.Home, "tab_button_0"),
                NavigationTabItem(1, "Goals", Icons.Default.List, "tab_button_1"),
                NavigationTabItem(2, "AI Chat", Icons.Default.Face, "tab_button_2"),
                NavigationTabItem(3, "Scripts", Icons.Default.Star, "tab_button_3")
            )

            items.forEach { item ->
                val isSelected = selectedTab == item.index
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onTabSelected(item.index) },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) Color(0xFF00E5FF) else Color(0xFFB5BDD3),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF00E5FF) else Color(0xFFB5BDD3)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color(0xFF6C5DD3).copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.testTag(item.testTag)
                )
            }
        }
    }
}

data class NavigationTabItem(
    val index: Int,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)
