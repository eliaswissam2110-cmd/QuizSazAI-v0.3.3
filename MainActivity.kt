package ir.quizzesaz.ai

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class Note(val id: Long, val title: String, val content: String)

data class Question(
    val text: String,
    val options: List<String>,
    val answer: Int,
    val explanation: String = ""
)

data class ResultRecord(
    val date: String,
    val subject: String,
    val total: Int,
    val correct: Int,
    val wrong: Int,
    val unanswered: Int,
    val percent: Int,
    val durationSec: Int
)

class LocalStore(private val context: Context) {
    private val preferences
        get() = context.getSharedPreferences("quizsaz_v03", Context.MODE_PRIVATE)

    fun notes(): MutableList<Note> {
        val array = JSONArray(preferences.getString("notes", "[]"))
        return MutableList(array.length()) { index ->
            val item = array.getJSONObject(index)
            Note(
                id = item.getLong("id"),
                title = item.getString("title"),
                content = item.optString("content")
            )
        }
    }

    fun addNote(note: Note) {
        val list = notes().apply { add(note) }
        val array = JSONArray()
        list.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("content", item.content)
                }
            )
        }
        preferences.edit().putString("notes", array.toString()).apply()
    }

    fun deleteNote(id: Long) {
        val array = JSONArray()
        notes().filterNot { it.id == id }.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("content", item.content)
                }
            )
        }
        preferences.edit().putString("notes", array.toString()).apply()
    }

    fun results(): List<ResultRecord> {
        val array = JSONArray(preferences.getString("results", "[]"))
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            ResultRecord(
                date = item.getString("date"),
                subject = item.getString("subject"),
                total = item.getInt("total"),
                correct = item.getInt("correct"),
                wrong = item.getInt("wrong"),
                unanswered = item.getInt("unanswered"),
                percent = item.getInt("percent"),
                durationSec = item.getInt("durationSec")
            )
        }.reversed()
    }

    fun addResult(result: ResultRecord) {
        val array = JSONArray(preferences.getString("results", "[]"))
        array.put(
            JSONObject().apply {
                put("date", result.date)
                put("subject", result.subject)
                put("total", result.total)
                put("correct", result.correct)
                put("wrong", result.wrong)
                put("unanswered", result.unanswered)
                put("percent", result.percent)
                put("durationSec", result.durationSec)
            }
        )
        preferences.edit().putString("results", array.toString()).apply()
    }

    fun apiKey(): String = preferences.getString("apiKey", "")

    fun saveApiKey(value: String) {
        preferences.edit().putString("apiKey", value).apply()
    }
}

fun demoQuestions(): List<Question> = listOf(
    Question(
        text = "کدام گزینه تعریف دقیق‌تری از یادگیری فعال است؟",
        options = listOf(
            "حفظ کردن بدون تمرین",
            "درگیر شدن ذهنی و عملی با موضوع",
            "فقط گوش دادن به مدرس",
            "خواندن سریع جزوه"
        ),
        answer = 1,
        explanation = "یادگیری فعال شامل مشارکت ذهنی و عملی یادگیرنده است."
    ),
    Question(
        text = "برای مرور مؤثر مطالب، کدام روش معمولاً مفیدتر است؟",
        options = listOf(
            "مرور فاصله‌دار",
            "یک‌بار خواندن",
            "حذف تمرین",
            "مطالعه بدون وقفه طولانی"
        ),
        answer = 0,
        explanation = "مرور فاصله‌دار باعث توزیع تمرین در زمان می‌شود."
    ),
    Question(
        text = "هدف اصلی آزمون تشخیصی چیست؟",
        options = listOf(
            "اعلام رتبه نهایی",
            "شناسایی سطح و نقاط ضعف",
            "افزایش زمان مطالعه",
            "حذف درس"
        ),
        answer = 1,
        explanation = "آزمون تشخیصی برای شناخت وضعیت اولیه و نقاط نیازمند تقویت به کار می‌رود."
    ),
    Question(
        text = "کدام گزینه به یک سؤال چهارگزینه‌ای استاندارد نزدیک‌تر است؟",
        options = listOf(
            "صورت سؤال روشن و یک پاسخ درست",
            "چند پاسخ مبهم",
            "گزینه‌های بسیار طولانی",
            "نبودن اطلاعات کافی"
        ),
        answer = 0,
        explanation = "سؤال استاندارد باید روشن و دارای پاسخ صحیح مشخص باشد."
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { QuizSazApp() }
    }
}

@Composable
fun QuizSazApp() {
    val context = LocalContext.current
    val store = remember { LocalStore(context) }

    var screen by remember { mutableStateOf("home") }
    var notes by remember { mutableStateOf(store.notes()) }
    var results by remember { mutableStateOf(store.results()) }
    var dark by remember { mutableStateOf(false) }
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var current by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var answers by remember { mutableStateOf(listOf<Int?>()) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var subject by remember { mutableStateOf("آزمون عمومی") }

    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            when (screen) {
                "home" -> HomeScreen(
                    notes = notes,
                    results = results,
                    dark = dark,
                    onDark = { dark = it },
                    onNotes = { screen = "notes" },
                    onExam = { screen = "setup" },
                    onHistory = { screen = "history" },
                    onSettings = { screen = "settings" }
                )

                "notes" -> NotesScreen(
                    notes = notes,
                    onBack = { screen = "home" },
                    onAdd = { title, content ->
                        store.addNote(Note(System.currentTimeMillis(), title, content))
                        notes = store.notes()
                    },
                    onDelete = { id ->
                        store.deleteNote(id)
                        notes = store.notes()
                    }
                )

                "setup" -> SetupScreen(
                    notes = notes,
                    onBack = { screen = "home" },
                    onStart = { count, title ->
                        val pool = demoQuestions()
                        subject = title.ifBlank { "آزمون عمومی" }
                        questions = pool.shuffled().take(count.coerceAtMost(pool.size))
                        answers = List(questions.size) { null }
                        current = 0
                        selected = null
                        startedAt = System.currentTimeMillis()
                        screen = if (questions.isEmpty()) "home" else "exam"
                    }
                )

                "exam" -> ExamScreen(
                    questions = questions,
                    index = current,
                    selected = selected,
                    onSelect = { selected = it },
                    onNext = {
                        val updated = answers.toMutableList()
                        updated[current] = selected
                        answers = updated

                        if (current < questions.lastIndex) {
                            current++
                            selected = answers[current]
                        } else {
                            screen = "result"
                        }
                    }
                )

                "result" -> {
                    val finalAnswers = answers
                    val correct = questions.indices.count { index ->
                        finalAnswers.getOrNull(index) == questions[index].answer
                    }
                    val wrong = questions.indices.count { index ->
                        finalAnswers.getOrNull(index) != null &&
                            finalAnswers[index] != questions[index].answer
                    }
                    val unanswered = questions.size - correct - wrong
                    val percent = if (questions.isEmpty()) {
                        0
                    } else {
                        (correct * 100f / questions.size).roundToInt()
                    }
                    val duration = ((System.currentTimeMillis() - startedAt) / 1000L).toInt()

                    LaunchedEffect(Unit) {
                        store.addResult(
                            ResultRecord(
                                date = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
                                    .format(Date()),
                                subject = subject,
                                total = questions.size,
                                correct = correct,
                                wrong = wrong,
                                unanswered = unanswered,
                                percent = percent,
                                durationSec = duration
                            )
                        )
                        results = store.results()
                    }

                    ResultScreen(
                        percent = percent,
                        correct = correct,
                        wrong = wrong,
                        unanswered = unanswered,
                        duration = duration,
                        onHome = { screen = "home" },
                        onAgain = {
                            val pool = demoQuestions()
                            questions = pool.shuffled().take(questions.size.coerceAtMost(pool.size))
                            answers = List(questions.size) { null }
                            current = 0
                            selected = null
                            startedAt = System.currentTimeMillis()
                            screen = "exam"
                        }
                    )
                }

                "history" -> HistoryScreen(
                    results = results,
                    onBack = { screen = "home" }
                )

                "settings" -> SettingsScreen(
                    saved = store.apiKey(),
                    onSave = {
                        store.saveApiKey(it)
                        screen = "home"
                    },
                    onBack = { screen = "home" }
                )
            }
        }
    }
}

@Composable
fun Shell(
    title: String,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun HomeScreen(
    notes: List<Note>,
    results: List<ResultRecord>,
    dark: Boolean,
    onDark: (Boolean) -> Unit,
    onNotes: () -> Unit,
    onExam: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit
) {
    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("آزمون‌ساز هوشمند", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    "نسخه ۰.۳ • مدیریت جزوه، آزمون و کارنامه",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text("داشبورد", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("جزوه‌ها: ${notes.size}    |    آزمون‌های ثبت‌شده: ${results.size}")
                        val average = if (results.isEmpty()) {
                            0
                        } else {
                            results.map { it.percent }.average().roundToInt()
                        }
                        Text("میانگین امتیاز: $average٪")
                    }
                }
            }
            item {
                Button(onClick = onExam, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("شروع آزمون جدید")
                }
            }
            item {
                OutlinedButton(onClick = onNotes, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.MenuBook, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("جزوه‌ها و منابع")
                }
            }
            item {
                OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("تاریخچه نتایج")
                }
            }
            item {
                OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("تنظیمات و اتصال هوش مصنوعی")
                }
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("حالت تاریک")
                    Spacer(Modifier.weight(1f))
                    Switch(checked = dark, onCheckedChange = onDark)
                }
            }
        }
    }
}

@Composable
fun NotesScreen(
    notes: List<Note>,
    onBack: () -> Unit,
    onAdd: (String, String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = uri.lastPathSegment ?: "منبع جدید"
            onAdd(name.substringAfterLast('/'), "فایل انتخاب‌شده: $uri")
        }
    }

    Shell("جزوه‌ها و منابع", onBack) {
        Text(
            "در نسخه ۰.۳ می‌توانید متن جزوه را مستقیماً ذخیره کنید یا فایل را به‌عنوان منبع اضافه کنید.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("عنوان جزوه") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("متن جزوه") },
            minLines = 5,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (title.isNotBlank() && content.isNotBlank()) {
                    onAdd(title.trim(), content.trim())
                    title = ""
                    content = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ذخیره جزوه")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                launcher.launch(
                    arrayOf(
                        "text/plain",
                        "application/pdf",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "image/*"
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AttachFile, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("افزودن فایل")
        }
        Spacer(Modifier.height(16.dp))
        notes.forEach { note ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(note.title, fontWeight = FontWeight.Bold)
                        Text(note.content.take(100))
                    }
                    IconButton(onClick = { onDelete(note.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف")
                    }
                }
            }
        }
    }
}

@Composable
fun SetupScreen(
    notes: List<Note>,
    onBack: () -> Unit,
    onStart: (Int, String) -> Unit
) {
    var count by remember { mutableIntStateOf(4) }
    var subject by remember {
        mutableStateOf(if (notes.isNotEmpty()) notes.first().title else "آزمون عمومی")
    }

    Shell("ساخت آزمون", onBack) {
        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("عنوان آزمون") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Text("تعداد سؤال: $count")
        Slider(
            value = count.toFloat(),
            onValueChange = { count = it.roundToInt().coerceIn(1, 20) },
            valueRange = 1f..20f,
            steps = 18
        )
        Text("سطح و ترتیب سؤال‌ها در تولید واقعی توسط موتور هوش مصنوعی تنظیم می‌شود.")
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onStart(count, subject) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("شروع آزمون")
        }
    }
}

@Composable
fun ExamScreen(
    questions: List<Question>,
    index: Int,
    selected: Int?,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit
) {
    val question = questions[index]

    Shell("آزمون • ${index + 1} از ${questions.size}") {
        LinearProgressIndicator(
            progress = (index + 1f) / questions.size,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        Text(question.text, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        question.options.forEachIndexed { optionIndex, option ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clickable { onSelect(optionIndex) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected == optionIndex) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selected == optionIndex,
                        onClick = { onSelect(optionIndex) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(option)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = selected != null
        ) {
            Text(if (index == questions.lastIndex) "ثبت پاسخ‌ها و مشاهده نتیجه" else "سؤال بعدی")
        }
    }
}

@Composable
fun ResultScreen(
    percent: Int,
    correct: Int,
    wrong: Int,
    unanswered: Int,
    duration: Int,
    onHome: () -> Unit,
    onAgain: () -> Unit
) {
    Shell("نتیجه آزمون") {
        Text(
            "$percent٪",
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "صحیح: $correct    غلط: $wrong    بی‌پاسخ: $unanswered",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Text(
            "زمان: ${duration / 60}:${"%02d".format(duration % 60)}",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAgain, modifier = Modifier.fillMaxWidth()) {
            Text("آزمون دوباره")
        }
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
            Text("بازگشت به داشبورد")
        }
    }
}

@Composable
fun HistoryScreen(results: List<ResultRecord>, onBack: () -> Unit) {
    Shell("تاریخچه نتایج", onBack) {
        if (results.isEmpty()) {
            Text("هنوز نتیجه‌ای ثبت نشده است.")
        }
        results.forEach { result ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(result.subject, fontWeight = FontWeight.Bold)
                    Text("${result.date}  •  ${result.percent}٪")
                    Text(
                        "صحیح ${result.correct} | غلط ${result.wrong} | " +
                            "بی‌پاسخ ${result.unanswered} | ${result.durationSec}s"
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    saved: String,
    onSave: (String) -> Unit,
    onBack: () -> Unit
) {
    var key by remember { mutableStateOf(saved) }

    Shell("تنظیمات", onBack) {
        Text("اتصال هوش مصنوعی", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "کلید API را فقط در دستگاه خود ذخیره کنید. برای نسخه تولیدی، استفاده از سرور واسط امن توصیه می‌شود."
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text("API Key") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = { onSave(key) }, modifier = Modifier.fillMaxWidth()) {
            Text("ذخیره تنظیمات")
        }
        Spacer(Modifier.height(20.dp))
        Text("نسخه ۰.۳", fontWeight = FontWeight.Bold)
        Text(
            "هسته آزمون، تصادفی‌سازی، کارنامه و ذخیره محلی در این نسخه پیاده‌سازی شده است. " +
                "اتصال واقعی به سرویس AI باید در مرحله بعد با endpoint امن تکمیل شود."
        )
    }
}
