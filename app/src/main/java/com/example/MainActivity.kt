package com.example

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ChatMessage
import com.example.data.CoachPersona
import com.example.data.VocabularyWord
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.TutorViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(dynamicColor = false) {
                MainScreen()
            }
        }
    }
}

@Composable
fun rememberTextToSpeech(): (String) -> Unit {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isReady by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val obj = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isReady = true
                }
            }
        }
        tts = obj
        onDispose {
            obj.stop()
            obj.shutdown()
        }
    }

    return { text ->
        if (isReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: TutorViewModel = viewModel()) {
    val selectedCoach by viewModel.selectedCoach.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val vocabularyList by viewModel.vocabularyList.collectAsStateWithLifecycle()

    val ttsSpeak = rememberTextToSpeech()
    var activeTab by remember { mutableStateOf(0) } // 0: Chat, 1: Vocabulary Book
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Dynamic accent color based on active coach
    val themeAccentColor = remember(selectedCoach) {
        Color(android.graphics.Color.parseColor(selectedCoach.accentColorHex))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(themeAccentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "L",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Column {
                            Text(
                                text = "LingoCoach AI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                val pulseAlpha by infiniteTransition.animateFloat(
                                    initialValue = 0.4f,
                                    targetValue = 1.0f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "pulseAlpha"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50).copy(alpha = pulseAlpha))
                                )
                                Text(
                                    text = "Online & Responsive",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (activeTab == 0 && chatMessages.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.testTag("clear_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Clear Chat history",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Practice Chat") },
                    label = { Text("Practice Chat") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = themeAccentColor,
                        selectedTextColor = themeAccentColor,
                        indicatorColor = themeAccentColor.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("tab_chat")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (vocabularyList.isNotEmpty()) {
                                    Badge { Text("${vocabularyList.size}") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Book, contentDescription = "Vocabulary Book")
                        }
                    },
                    label = { Text("Vocabulary") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = themeAccentColor,
                        selectedTextColor = themeAccentColor,
                        indicatorColor = themeAccentColor.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("tab_vocabulary")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (activeTab == 0) {
                // CHAT PRACTICE VIEW
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. Coach Selector Header
                    CoachSelectorRow(
                        selectedCoach = selectedCoach,
                        onCoachSelected = { viewModel.selectCoach(it) }
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 2. Chat Feed & Input
                    ChatSection(
                        messages = chatMessages,
                        isGenerating = isGenerating,
                        coach = selectedCoach,
                        themeAccentColor = themeAccentColor,
                        onSpeak = ttsSpeak,
                        onSendMessage = { viewModel.sendMessage(it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // VOCABULARY BOOK VIEW
                VocabularySection(
                    words = vocabularyList,
                    onDeleteWord = { viewModel.deleteVocabularyWord(it) }
                )
            }

            // Dialog for clearing history
            if (showClearConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showClearConfirmDialog = false },
                    title = { Text("Reset Chat?") },
                    text = { Text("This will delete your conversation history with ${selectedCoach.coachName}. Your Vocabulary Book will remain safe.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.clearChatHistory()
                                showClearConfirmDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = themeAccentColor)
                        ) {
                            Text("Reset")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearConfirmDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CoachSelectorRow(
    selectedCoach: CoachPersona,
    onCoachSelected: (CoachPersona) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Text(
            text = "Choose Your AI Coach",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CoachPersona.entries.forEach { coach ->
                val isSelected = coach == selectedCoach
                val coachColor = Color(android.graphics.Color.parseColor(coach.accentColorHex))

                val borderAnimWidth by animateDpAsState(
                    targetValue = if (isSelected) 2.dp else 1.dp, label = "borderWidth"
                )
                val bgAlphaAnim by animateFloatAsState(
                    targetValue = if (isSelected) 0.08f else 0.0f, label = "bgAlpha"
                )

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onCoachSelected(coach) }
                        .border(
                            width = borderAnimWidth,
                            color = if (isSelected) coachColor else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .testTag("coach_selector_${coach.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) coachColor.copy(alpha = bgAlphaAnim) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = coach.avatarEmoji,
                            fontSize = 28.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = coach.coachName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isSelected) coachColor else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = coach.title.split(" ").firstOrNull() ?: "",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = selectedCoach.description,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.SansSerif,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun ChatSection(
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    coach: CoachPersona,
    themeAccentColor: Color,
    onSpeak: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var textState by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Automatically scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = modifier) {
        // Chat Bubbles List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    coach = coach,
                    onSpeak = onSpeak
                )
            }

            if (isGenerating) {
                item {
                    TypingIndicator(coach = coach)
                }
            }
        }

        // Send Controls Box
        Surface(
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    placeholder = { Text("Write in English...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (textState.isNotBlank()) {
                                onSendMessage(textState)
                                textState = ""
                                keyboardController?.hide()
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeAccentColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = {
                        if (textState.isNotBlank()) {
                            onSendMessage(textState)
                            textState = ""
                            keyboardController?.hide()
                        }
                    },
                    containerColor = themeAccentColor,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Message",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    coach: CoachPersona,
    onSpeak: (String) -> Unit
) {
    val isUser = message.sender == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(if (isUser) "user_message" else "coach_message"),
        horizontalAlignment = alignment
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            if (!isUser) {
                // Coach Avatar Icon
                Text(
                    text = coach.avatarEmoji,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 8.dp, top = 4.dp)
                )
            }

            Column(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(bubbleColor)
                    .padding(12.dp)
            ) {
                if (!isUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = coach.coachName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(android.graphics.Color.parseColor(coach.accentColorHex))
                        )

                        IconButton(
                            onClick = { onSpeak(message.text) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Listen to pronunciation",
                                tint = Color(android.graphics.Color.parseColor(coach.accentColorHex)),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = message.text,
                    fontSize = 15.sp,
                    color = textColor,
                    lineHeight = 20.sp
                )
            }
        }

        // Show grammar corrections if available (Underneath user or coach bubbles)
        if (!isUser && message.grammarCorrection != null) {
            Spacer(modifier = Modifier.height(8.dp))
            CorrectionCard(
                correctionText = message.grammarCorrection,
                explanation = message.grammarExplanation ?: "Grammar enrichment tips."
            )
        }
    }
}

@Composable
fun CorrectionCard(
    correctionText: String,
    explanation: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .padding(start = 32.dp)
            .testTag("correction_card"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF9C4) // Warm yellow
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Language coach feedback",
                    tint = Color(0xFFF57F17), // Orange warning tint
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Grammar & Phrasing Coach Feedback",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF5D4037)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Recommended Phrasing:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5D4037).copy(alpha = 0.8f)
            )
            Text(
                text = correctionText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32), // Direct green
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Coach's Explanation / الشرح والتعليم:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5D4037).copy(alpha = 0.8f)
            )
            Text(
                text = explanation,
                fontSize = 13.sp,
                color = Color(0xFF5D4037),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun TypingIndicator(coach: CoachPersona) {
    val coachColor = Color(android.graphics.Color.parseColor(coach.accentColorHex))
    
    // Simple pulsing animation for the dots
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha1"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha2"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha3"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("typing_indicator"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = coach.avatarEmoji,
            fontSize = 24.sp,
            modifier = Modifier.padding(end = 8.dp)
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${coach.coachName} is typing",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(coachColor.copy(alpha = alpha1)))
            Spacer(modifier = Modifier.width(4.dp))
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(coachColor.copy(alpha = alpha2)))
            Spacer(modifier = Modifier.width(4.dp))
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(coachColor.copy(alpha = alpha3)))
        }
    }
}

@Composable
fun VocabularySection(
    words: List<VocabularyWord>,
    onDeleteWord: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Your Vocabulary Book",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Words, idioms, and phrases introduced dynamically during chats.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (words.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = "Empty Vocabulary Book",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Vocabulary Book is Empty",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Start chatting with your AI coach. New terms will automatically show up here!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(words) { vocab ->
                    VocabularyCard(
                        vocab = vocab,
                        onDelete = { onDeleteWord(vocab.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun VocabularyCard(
    vocab: VocabularyWord,
    onDelete: () -> Unit
) {
    val coach = remember(vocab.coachId) { CoachPersona.fromId(vocab.coachId) }
    val coachColor = Color(android.graphics.Color.parseColor(coach.accentColorHex))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vocabulary_item_${vocab.word}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = coach.avatarEmoji,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = vocab.word,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = coachColor
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete from vocabulary",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Definition / المعنى:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = vocab.definition,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Example Sentence / مثال:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = vocab.exampleSentence,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}
