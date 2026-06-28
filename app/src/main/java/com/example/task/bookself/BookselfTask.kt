package com.example.task.bookself

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ai.AIService
import com.example.core.storage.*
import com.example.task.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class BookselfTask(private val context: Context) : AgentTask {

    override val metadata = TaskMetadata(
        id = "builtin_bookself",
        name = "Bookself 📚",
        description = "Store books, read with customizable reader, get AI summaries of paragraphs.",
        icon = Icons.Default.Book,
        category = TaskCategory.PRODUCTIVITY
    )

    private val sampleContent = """
        CHAPTER I. Down the Rabbit-Hole

        Alice was beginning to get very tired of sitting by her sister on the bank, and of having nothing to do: once or twice she had peeped into the book her sister was reading, but it had no pictures or conversations in it, 'and what is the use of a book,' thought Alice 'without pictures or conversations?'

        So she was considering in her own mind (as well as she could, for the hot day made her feel very sleepy and stupid), whether the pleasure of making a daisy-chain would be worth the trouble of getting up and picking the daisies, when suddenly a White Rabbit with pink eyes ran close by her.

        There was nothing so VERY remarkable in that; nor did Alice think it so VERY much out of the way to hear the Rabbit say to itself, 'Oh dear! Oh dear! I shall be late!' (when she thought it over afterwards, it occurred to her that she ought to have wondered at this, but at the time it all seemed quite natural).

        But when the Rabbit actually TOOK A WATCH OUT OF ITS WAISTCOAT-POCKET, and looked at it, and then hurried on, Alice started to her feet, for it flashed across her mind that she had never before seen a rabbit with either a waistcoat-pocket, or a watch to take out of it.

        And burning with curiosity, she ran across the field after it, and fortunately was just in time to see it pop down a large rabbit-hole under the hedge.

        In another moment down went Alice after it, never once considering how in the world she was to get out again.

        The rabbit-hole went straight on like a tunnel for some way, and then dipped suddenly down, so suddenly that Alice had not a moment to think about stopping herself before she found herself falling down a very deep well.

        Either the well was very deep, or she fell very slowly, for she had plenty of time as she went down to look about her and to wonder what was going to happen next. First, she tried to look down and make out what she was coming to, but it was too dark to see anything.

        Then she looked at the sides of the well, and noticed that they were filled with cupboards and book-shelves; here and there she saw maps and pictures hung upon pegs. She took down a jar from one of the shelves as she passed; it was labelled 'ORANGE MARMALADE', but to her great disappointment it was empty.

        'Well!' thought Alice to herself, 'after such a fall as this, I shall think nothing of tumbling down stairs! How brave they'll all think me at home! Why, I wouldn't say anything about it, even if I fell off the top of the house!'
    """.trimIndent()

    suspend fun ensureClassicPreinstalled(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val books = db.taskDao().getAllBooks()
        if (books.isEmpty()) {
            val file = File(context.filesDir, "alice.txt")
            if (!file.exists()) {
                file.writeText(sampleContent)
            }
            db.taskDao().insertBook(
                BookEntity(
                    title = "Alice in Wonderland",
                    author = "Lewis Carroll",
                    filePath = file.absolutePath,
                    coverUrl = null
                )
            )
        }
    }

    @Composable
    override fun ConfigurationScreen(
        settings: TaskSettings,
        onSettingsChanged: (TaskSettings) -> Unit
    ) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val db = remember { AppDatabase.getDatabase(context) }
        val books by db.taskDao().getAllBooksFlow().collectAsState(initial = emptyList())

        var showAddBookDialog by remember { mutableStateOf(false) }
        var inputTitle by remember { mutableStateOf("") }
        var inputAuthor by remember { mutableStateOf("") }
        var inputContent by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                ensureClassicPreinstalled(context)
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Book Collection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = { showAddBookDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Book")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (books.isEmpty()) {
                Text(
                    text = "No books added. Click add to add a book or let the agent pre-load classic stories.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                books.forEach { book ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = book.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "By ${book.author}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) {
                                            db.taskDao().deleteBookById(book.id)
                                        }
                                        Toast.makeText(context, "Book deleted", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Book",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddBookDialog) {
            AlertDialog(
                onDismissRequest = { showAddBookDialog = false },
                title = { Text("Add New Book", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = inputTitle,
                            onValueChange = { inputTitle = it },
                            label = { Text("Book Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = inputAuthor,
                            onValueChange = { inputAuthor = it },
                            label = { Text("Author") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = inputContent,
                            onValueChange = { inputContent = it },
                            label = { Text("Book Content / Text") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (inputTitle.isNotBlank() && inputContent.isNotBlank()) {
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        val file = File(context.filesDir, "book_${System.currentTimeMillis()}.txt")
                                        file.writeText(inputContent)
                                        db.taskDao().insertBook(
                                            BookEntity(
                                                title = inputTitle,
                                                author = inputAuthor.ifEmpty { "Unknown" },
                                                filePath = file.absolutePath
                                            )
                                        )
                                    }
                                    Toast.makeText(context, "Book added successfully!", Toast.LENGTH_SHORT).show()
                                    showAddBookDialog = false
                                    inputTitle = ""
                                    inputAuthor = ""
                                    inputContent = ""
                                }
                            } else {
                                Toast.makeText(context, "Title and Content are required", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddBookDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    override suspend fun execute(context: Context, settings: TaskSettings): TaskResult = withContext(Dispatchers.IO) {
        ensureClassicPreinstalled(context)
        val db = AppDatabase.getDatabase(context)
        val books = db.taskDao().getAllBooks()
        if (books.isEmpty()) {
            return@withContext TaskResult.Error("No books in collection to read.")
        }
        val targetBook = books.first()
        val bookText = try {
            File(targetBook.filePath).readText()
        } catch (e: Exception) {
            sampleContent
        }
        val outputData = AgentOutputData.BookContent(
            title = targetBook.title,
            author = targetBook.author,
            content = bookText,
            currentPage = targetBook.currentOffset
        )
        ResultStateHolder.saveResult(context, metadata.id, outputData)
        return@withContext TaskResult.Success("Opened book reader for '${targetBook.title}' successfully.")
    }

    override fun schedule(context: Context, settings: TaskSettings) {}

    override fun cancel(context: Context) {}

    @OptIn(ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    override fun ResultView(outputData: AgentOutputData, onAction: (AgentAction) -> Unit) {
        if (outputData is AgentOutputData.BookContent) {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val db = remember { AppDatabase.getDatabase(context) }
            val paragraphs = remember(outputData.content) {
                outputData.content.split("\n\n").filter { it.isNotBlank() }
            }

            // Book Reader Custom State
            var themeMode by remember { mutableStateOf("Sepia") } // Sepia, Dark, Light
            var fontSizeSp by remember { mutableStateOf(16f) }
            var selectedParagraphText by remember { mutableStateOf<String?>(null) }
            var aiSummaryText by remember { mutableStateOf<String?>(null) }
            var isSummarizing by remember { mutableStateOf(false) }

            // Theme colors mapping
            val (backgroundColor, textColor, activeThemeColor) = when (themeMode) {
                "Dark" -> Triple(Color(0xFF121212), Color(0xFFE0E0E0), MaterialTheme.colorScheme.primary)
                "Light" -> Triple(Color(0xFFFFFFFF), Color(0xFF1C1C1C), MaterialTheme.colorScheme.primary)
                else -> Triple(Color(0xFFF4ECD8), Color(0xFF5B4636), MaterialTheme.colorScheme.secondary) // Sepia
            }

            // Lazy list controller for bookmarks jump-to
            val listState = rememberLazyListState()

            // Fetch bookmarks dynamically
            var bookmarksList by remember { mutableStateOf<List<BookmarkEntity>>(emptyList()) }
            LaunchedEffect(outputData.title) {
                withContext(Dispatchers.IO) {
                    val books = db.taskDao().getAllBooks()
                    val targetBook = books.find { it.title == outputData.title }
                    if (targetBook != null) {
                        db.taskDao().getBookmarksForBookFlow(targetBook.id).collect {
                            bookmarksList = it
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(16.dp)
            ) {
                // Reader Header controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = outputData.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor,
                            maxLines = 1
                        )
                        Text(
                            text = "By ${outputData.author}",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }

                    // Theme Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Light", "Sepia", "Dark").forEach { name ->
                            Button(
                                onClick = { themeMode = name },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (themeMode == name) activeThemeColor else activeThemeColor.copy(alpha = 0.2f),
                                    contentColor = if (themeMode == name) Color.White else textColor
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(name, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = textColor.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                // Reading paragraphs layout
                Box(modifier = Modifier.weight(1f)) {
                    if (paragraphs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("The book has no readable content.", color = textColor)
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(paragraphs) { index, para ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {},
                                            onLongClick = {
                                                selectedParagraphText = para
                                            }
                                        ),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                ) {
                                    Text(
                                        text = para,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = fontSizeSp.sp,
                                            fontFamily = FontFamily.Serif,
                                            lineHeight = (fontSizeSp * 1.5).sp
                                        ),
                                        color = textColor,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bookmark List Sheet Area
                if (bookmarksList.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 100.dp)
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = activeThemeColor.copy(alpha = 0.1f)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                "Bookmarks",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                bookmarksList.forEach { bmark ->
                                    AssistChip(
                                        onClick = {
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(bmark.paragraphIndex)
                                            }
                                        },
                                        label = {
                                            Text(
                                                "Para ${bmark.paragraphIndex + 1}",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Bookmark,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = textColor.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Font Slider controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = "Font size reduction",
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Slider(
                        value = fontSizeSp,
                        onValueChange = { fontSizeSp = it },
                        valueRange = 12f..28f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = activeThemeColor,
                            activeTrackColor = activeThemeColor,
                            inactiveTrackColor = activeThemeColor.copy(alpha = 0.2f)
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = "Font size increase",
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Paragraph Action Alert Dialog (triggered on Long-Press)
            if (selectedParagraphText != null) {
                AlertDialog(
                    onDismissRequest = { selectedParagraphText = null },
                    title = { Text("Paragraph Actions", fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            text = selectedParagraphText ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 4,
                            textAlign = TextAlign.Justify
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val text = selectedParagraphText ?: ""
                                coroutineScope.launch {
                                    isSummarizing = true
                                    aiSummaryText = null
                                    val ai = AIService(context)
                                    val summaryPrompt = "Simplify and explain this book paragraph in 2 sentences:\n\n$text"
                                    try {
                                        val result = ai.executeCustomPrompt(summaryPrompt)
                                        aiSummaryText = result
                                    } catch (e: Exception) {
                                        aiSummaryText = "Error generating summary: ${e.message}"
                                    } finally {
                                        isSummarizing = false
                                    }
                                }
                            }
                        ) {
                            Text("AI Summarize")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                val text = selectedParagraphText ?: ""
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        val books = db.taskDao().getAllBooks()
                                        val targetBook = books.find { it.title == outputData.title }
                                        if (targetBook != null) {
                                            val index = paragraphs.indexOf(text)
                                            db.taskDao().insertBookmark(
                                                BookmarkEntity(
                                                    bookId = targetBook.id,
                                                    paragraphIndex = index,
                                                    text = text.take(50)
                                                )
                                            )
                                        }
                                    }
                                    Toast.makeText(context, "Bookmark Saved!", Toast.LENGTH_SHORT).show()
                                    selectedParagraphText = null
                                }
                            }
                        ) {
                            Text("Bookmark Paragraph")
                        }
                    }
                )
            }

            // AI Summary display Dialog
            if (isSummarizing || aiSummaryText != null) {
                AlertDialog(
                    onDismissRequest = {
                        aiSummaryText = null
                        selectedParagraphText = null
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI Smart Simplifier", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        if (isSummarizing) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Gemini is reading paragraph...")
                            }
                        } else {
                            Text(
                                text = aiSummaryText ?: "",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                aiSummaryText = null
                                selectedParagraphText = null
                            }
                        ) {
                            Text("Got it")
                        }
                    }
                )
            }
        } else {
            DefaultResultView(outputData, onAction)
        }
    }
}
