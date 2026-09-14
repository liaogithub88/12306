@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.railhelper.ticket

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsRailway
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private const val DAY_MILLIS = 86_400_000L

data class TicketQueryResult(
    val from: String,
    val to: String,
    val date: String,
    val trainType: String,
    val studentTicket: Boolean,
    val onlyHasTicket: Boolean,
    val trains: List<TrainInfo>
)

private val trainTypeOptions = listOf(
    "" to "全部",
    "G" to "高铁（G）",
    "D" to "动车（D）",
    "C" to "城际（C）",
    "Z" to "直达（Z）",
    "T" to "特快（T）",
    "K" to "快速（K）"
)

private val trainTypeCodes = trainTypeOptions.drop(1).map { it.first }

private fun trainTypeLabel(code: String): String =
    trainTypeOptions.firstOrNull { it.first == code }?.second ?: code

private fun splitCsvCodes(value: String): List<String> =
    value.split(",").map { it.trim().uppercase(Locale.ROOT) }.filter { it.isNotBlank() }.distinct()

private fun dateFormatter(): SimpleDateFormat =
    SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }

private fun todayUtcMillis(): Long {
    val local = Calendar.getInstance()
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }
    return utc.timeInMillis
}

private fun parseDateMillis(value: String): Long? =
    try {
        dateFormatter().parse(value)?.time
    } catch (_: Exception) {
        null
    }

private fun formatDateMillis(millis: Long): String = dateFormatter().format(millis)

private fun calendarFromDate(value: String): Calendar =
    Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.CHINA).apply {
        time = dateFormatter().parse(value) ?: dateFormatter().parse(formatDateMillis(todayUtcMillis()))!!
    }

private fun defaultTrainDate(): String = formatDateMillis(todayUtcMillis())

private fun formatMonthDay(value: String): String {
    val calendar = calendarFromDate(value)
    return "${calendar.get(Calendar.MONTH) + 1}月${calendar.get(Calendar.DAY_OF_MONTH)}日"
}

private fun formatShortDate(value: String): String {
    val calendar = calendarFromDate(value)
    return "%02d.%02d".format(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
}

private fun formatWeekday(value: String): String {
    val days = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
    return days[calendarFromDate(value).get(Calendar.DAY_OF_WEEK) - 1]
}

private fun todayText(value: String): String =
    if (value == defaultTrainDate()) "今天" else formatWeekday(value)

private fun queryDateOptions(selected: String): List<String> {
    val selectedMillis = parseDateMillis(selected) ?: todayUtcMillis()
    val today = todayUtcMillis()
    val first = maxOf(today, selectedMillis - 2 * DAY_MILLIS)
    return (0 until 6).map { formatDateMillis(first + it * DAY_MILLIS) }
}

private fun shiftDate(value: String, days: Int): String {
    val millis = (parseDateMillis(value) ?: todayUtcMillis()) + days * DAY_MILLIS
    return formatDateMillis(millis.coerceAtLeast(todayUtcMillis()))
}

private fun seatAvailable(value: String): Boolean =
    value.isNotBlank() && value !in listOf("--", "无", "*")

private fun availabilityText(train: TrainInfo): String {
    val values = listOf(
        train.businessSeat,
        train.firstSeat,
        train.secondSeat,
        train.softSleeper,
        train.hardSleeper,
        train.hardSeat,
        train.noSeat
    ).filter(::seatAvailable)
    if (!train.canBuy) return "候补"
    if (values.any { it == "有" }) return "有票"
    return values.firstOrNull { it.toIntOrNull()?.let { count -> count > 0 } == true }?.let { "${it}张" } ?: "有票"
}

private fun trainTypeSummary(trainType: String): String =
    if (trainType.isBlank()) "全部车次" else trainTypeLabel(trainType)

private fun isHighSpeedTrain(train: TrainInfo): Boolean =
    train.trainCode.startsWith("G", ignoreCase = true) ||
        train.trainCode.startsWith("D", ignoreCase = true) ||
        train.trainCode.startsWith("C", ignoreCase = true)

private fun durationMinutes(duration: String): Int {
    val parts = duration.split(":")
    if (parts.size == 2) {
        val hours = parts[0].toIntOrNull() ?: return Int.MAX_VALUE
        val minutes = parts[1].toIntOrNull() ?: return Int.MAX_VALUE
        return hours * 60 + minutes
    }
    return Int.MAX_VALUE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1206)
        }
        setContent {
            TicketHelperApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketHelperApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbars = remember { SnackbarHostState() }
    var initialized by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }
    var initError by remember { mutableStateOf<String?>(null) }
    var user by remember { mutableStateOf<User?>(null) }
    var screen by rememberSaveable { mutableStateOf("home") }
    var editDraft by remember { mutableStateOf<TaskDraft?>(null) }
    var detailTaskId by remember { mutableStateOf<Int?>(null) }
    var queryResult by remember { mutableStateOf<TicketQueryResult?>(null) }
    var pendingDraft by remember { mutableStateOf<TaskDraft?>(null) }

    suspend fun refreshUser(validate: Boolean = false) {
        val res = if (validate) {
            PythonBridge.call("validate_current_session")
        } else {
            PythonBridge.call("get_current_user")
        }
        user = parseUser(res.dataObject())
    }

    fun openTaskDraft(draft: TaskDraft) {
        if (user == null) {
            pendingDraft = draft
            screen = "login"
            scope.launch { snackbars.showSnackbar("请先登录 12306 后继续创建抢票任务") }
        } else {
            editDraft = draft
            screen = "edit"
        }
    }

    LaunchedEffect(Unit) {
        val startedAt = System.currentTimeMillis()
        try {
            PythonBridge.initialize(context)
            refreshUser(validate = true)
            initialized = true
        } catch (exc: Exception) {
            initError = exc.message ?: "初始化失败"
        }
        val elapsed = System.currentTimeMillis() - startedAt
        if (elapsed < 1600) delay(1600 - elapsed)
        showSplash = false
    }

    OriginTheme {
        if (showSplash) {
            JiebaoSplashScreen()
            return@OriginTheme
        }

        val title = when (screen) {
            "tasks" -> "任务"
            "edit" -> if (editDraft?.id == null) "创建抢票任务" else "编辑抢票任务"
            "detail" -> "任务详情"
            "login" -> if (user == null) "登录12306" else "账号"
            else -> "铁路12306"
        }
        val showTopBar = screen !in listOf("home", "results")
        val showBottomNav = screen != "results"
        Scaffold(
            topBar = {
                if (showTopBar) {
                    OriginTitleBar(
                        title = title,
                        subtitle = null,
                        actions = {
                            IconButton(onClick = {
                                scope.launch {
                                    PythonBridge.call("get_current_user")
                                    refreshUser()
                                }
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = Color.White)
                            }
                        }
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbars) },
            bottomBar = {
                if (showBottomNav) {
                    NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(
                        selected = screen == "home",
                        onClick = { screen = "home" },
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text("首页") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OriginColors.Blue,
                            selectedTextColor = OriginColors.Blue,
                            indicatorColor = OriginColors.Blue.copy(alpha = 0.10f),
                        )
                    )
                    NavigationBarItem(
                        selected = screen == "tasks",
                        onClick = { screen = "tasks" },
                        icon = { Icon(Icons.Default.List, null) },
                        label = { Text("任务") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OriginColors.Blue,
                            selectedTextColor = OriginColors.Blue,
                            indicatorColor = OriginColors.Blue.copy(alpha = 0.10f),
                        )
                    )
                    NavigationBarItem(
                        selected = screen == "login",
                        onClick = { screen = "login" },
                        icon = { Icon(Icons.Default.Login, null) },
                        label = { Text(if (user == null) "登录" else "账号") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OriginColors.Blue,
                            selectedTextColor = OriginColors.Blue,
                            indicatorColor = OriginColors.Blue.copy(alpha = 0.10f),
                        )
                    )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).originPage()) {
                when {
                    initError != null -> ErrorBox(initError!!)
                    !initialized -> LoadingBox("正在初始化本地核心...")
                    screen == "login" -> LoginScreen(
                        user = user,
                        onLoggedIn = {
                            scope.launch {
                                refreshUser()
                                pendingDraft?.let { draft ->
                                    editDraft = draft
                                    pendingDraft = null
                                    screen = "edit"
                                } ?: run {
                                    screen = "home"
                                }
                            }
                        },
                        onLogout = {
                            scope.launch {
                                PythonBridge.call("logout")
                                refreshUser()
                                screen = "home"
                                snackbars.showSnackbar("已退出登录")
                            }
                        },
                        onMessage = { msg -> scope.launch { snackbars.showSnackbar(msg) } }
                    )
                    screen == "tasks" -> TasksScreen(
                        onCreate = {
                            editDraft = TaskDraft()
                            screen = "edit"
                        },
                        onEdit = { task ->
                            editDraft = task.toDraft()
                            screen = "edit"
                        },
                        onDetail = { id ->
                            detailTaskId = id
                            screen = "detail"
                        },
                        onMessage = { msg -> scope.launch { snackbars.showSnackbar(msg) } }
                    )
                    screen == "edit" -> TaskEditScreen(
                        initial = editDraft ?: TaskDraft(),
                        onDone = {
                            editDraft = null
                            screen = "tasks"
                        },
                        onCancel = { screen = "tasks" },
                        onMessage = { msg -> scope.launch { snackbars.showSnackbar(msg) } }
                    )
                    screen == "detail" && detailTaskId != null -> TaskDetailScreen(
                        taskId = detailTaskId!!,
                        onBack = { screen = "tasks" },
                        onEdit = { task ->
                            editDraft = task.toDraft()
                            screen = "edit"
                        },
                        onMessage = { msg -> scope.launch { snackbars.showSnackbar(msg) } }
                    )
                    screen == "results" && queryResult != null -> TicketResultsScreen(
                        initial = queryResult!!,
                        user = user,
                        onBack = { screen = "home" },
                        onCreateTask = ::openTaskDraft,
                        onMessage = { msg -> scope.launch { snackbars.showSnackbar(msg) } }
                    )
                    else -> HomeScreen(
                        user = user,
                        onTasks = { screen = "tasks" },
                        onAccount = { screen = "login" },
                        onCreateTask = ::openTaskDraft,
                        onResults = { result ->
                            queryResult = result
                            screen = "results"
                        },
                        onMessage = { msg -> scope.launch { snackbars.showSnackbar(msg) } }
                    )
                }
            }
        }
    }
}

@Composable
fun JiebaoSplashScreen() {
    val transition = rememberInfiniteTransition(label = "splash")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "splashAngle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OriginColors.Blue)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 150.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 1.4.dp.toPx())
                    val diameter = size.minDimension * 0.92f
                    val inset = (size.minDimension - diameter) / 2f
                    val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)
                    val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                    drawArc(
                        color = Color.White.copy(alpha = 0.28f),
                        startAngle = angle,
                        sweepAngle = 240f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke
                    )
                    drawArc(
                        color = Color.White.copy(alpha = 0.56f),
                        startAngle = -angle * 0.72f,
                        sweepAngle = 150f,
                        useCenter = false,
                        topLeft = topLeft.copy(x = topLeft.x + 24f, y = topLeft.y + 24f),
                        size = androidx.compose.ui.geometry.Size(diameter - 48f, diameter - 48f),
                        style = stroke
                    )
                    repeat(9) { index ->
                        val dotAngle = Math.toRadians((index * 38 + angle / 3f).toDouble())
                        val radius = diameter * 0.38f
                        val center = androidx.compose.ui.geometry.Offset(
                            x = size.width / 2f + kotlin.math.cos(dotAngle).toFloat() * radius,
                            y = size.height / 2f + kotlin.math.sin(dotAngle).toFloat() * radius
                        )
                        drawCircle(Color.White.copy(alpha = 0.45f), radius = 1.8.dp.toPx(), center = center)
                    }
                }
                Surface(
                    modifier = Modifier.size(92.dp),
                    shape = CircleShape,
                    color = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.DirectionsRailway,
                            contentDescription = null,
                            tint = OriginColors.Blue,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
            Text(
                text = "尽享精彩出行服务",
                color = Color.White,
                fontSize = 31.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Canvas(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(280.dp)
                    .height(18.dp)
            ) {
                drawArc(
                    color = Color.White.copy(alpha = 0.75f),
                    startAngle = 190f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, -22f),
                    size = androidx.compose.ui.geometry.Size(size.width, 40f),
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Copyright © 2008-2025 | IPv6", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
            Spacer(Modifier.height(5.dp))
            Text("中国铁道科学研究院集团有限公司", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
        }
    }
}

@Composable
fun LoadingBox(text: String) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(text)
    }
}

@Composable
fun ErrorBox(text: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("发生错误", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Text(text)
    }
}

@Composable
fun HomeScreen(
    user: User?,
    onTasks: () -> Unit,
    onAccount: () -> Unit,
    onCreateTask: (TaskDraft) -> Unit,
    onResults: (TicketQueryResult) -> Unit,
    onMessage: (String) -> Unit
) {
    QueryScreen(
        user = user,
        onTasks = onTasks,
        onAccount = onAccount,
        onCreateTask = onCreateTask,
        onResults = onResults,
        onMessage = onMessage
    )
}

@Composable
fun LoginScreen(
    user: User?,
    onLoggedIn: () -> Unit,
    onLogout: () -> Unit,
    onMessage: (String) -> Unit
) {
    WebLoginScreen(
        user = user,
        onLoggedIn = onLoggedIn,
        onLogout = onLogout,
        onMessage = onMessage
    )
}
@Composable
fun QrImage(base64: String) {
    val bytes = remember(base64) { Base64.decode(base64, Base64.DEFAULT) }
    val bitmap = remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
    Image(bitmap = bitmap.asImageBitmap(), contentDescription = "登录二维码", modifier = Modifier.size(240.dp))
}

@Composable
fun QueryScreen(
    user: User? = null,
    onTasks: (() -> Unit)? = null,
    onAccount: (() -> Unit)? = null,
    onCreateTask: (TaskDraft) -> Unit,
    onResults: (TicketQueryResult) -> Unit,
    onMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var from by rememberSaveable { mutableStateOf("") }
    var to by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(defaultTrainDate()) }
    var trainType by rememberSaveable { mutableStateOf("") }
    var studentTicket by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    fun submitQuery() {
        scope.launch {
            if (from.isBlank() || to.isBlank()) {
                onMessage("请选择出发站和到达站")
                return@launch
            }
            loading = true
            val params = JSONObject()
                .put("from_station", from)
                .put("to_station", to)
                .put("train_date", date)
                .put("ticket_type", if (studentTicket) "0X00" else "ADULT")
                .put("only_has_ticket", false)
            if (trainType.isNotBlank()) params.put("train_types", trainType)
            val res = PythonBridge.call("query_tickets", params.toString())
            loading = false
            if (!res.success()) {
                onMessage(res.message())
                return@launch
            }
            onResults(
                TicketQueryResult(
                    from = from,
                    to = to,
                    date = date,
                    trainType = trainType,
                    studentTicket = studentTicket,
                    onlyHasTicket = false,
                    trains = parseTrains(res.dataObject()?.optJSONArray("trains"))
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OriginColors.Page)
            .padding(horizontal = 4.dp, vertical = 24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
            shadowElevation = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 40.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    JiebaoStationPicker(
                        label = "出发地",
                        value = from,
                        onValue = { from = it },
                        onMessage = onMessage,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start
                    )
                    IconButton(
                        onClick = {
                            val oldFrom = from
                            from = to
                            to = oldFrom
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = "交换出发到达站",
                            tint = OriginColors.Blue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    JiebaoStationPicker(
                        label = "到达地",
                        value = to,
                        onValue = { to = it },
                        onMessage = onMessage,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }
                OriginDivider()
                JiebaoDateSelector(
                    date = date,
                    onDate = { date = it },
                    studentTicket = studentTicket,
                    onStudentTicket = { studentTicket = it }
                )
                OriginPrimaryButton(
                    enabled = !loading,
                    onClick = ::submitQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    } else {
                        Text("查询车票", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val routeText = if (from.isNotBlank() && to.isNotBlank()) "$from--$to" else "出发站--到达站"
                    Text(routeText, color = OriginColors.TextMuted, fontSize = 15.sp)
                    Spacer(Modifier.width(24.dp))
                    Text(if (from.isNotBlank() && to.isNotBlank()) "$to--$from" else "到达站--出发站", color = OriginColors.TextMuted, fontSize = 15.sp)
                    Spacer(Modifier.width(24.dp))
                    Text("清除历史", color = OriginColors.TextMuted, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun JiebaoStationPicker(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    var keyword by remember(value) { mutableStateOf(value) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(keyword, expanded) {
        if (!expanded || keyword.isBlank()) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(220)
        val res = PythonBridge.call("search_stations", keyword)
        if (!res.success()) {
            suggestions = emptyList()
            onMessage(res.message())
            return@LaunchedEffect
        }
        val arr = res.dataObject()?.optJSONArray("stations") ?: JSONArray()
        suggestions = (0 until arr.length()).map { arr.getJSONObject(it).optString("name") }.filter { it.isNotBlank() }
    }

    Box(modifier = modifier) {
        Text(
            text = value.ifBlank { label },
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    keyword = value
                    expanded = true
                },
            color = if (value.isBlank()) OriginColors.TextMuted else OriginColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = { Text(label) },
                modifier = Modifier
                    .width(280.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                colors = OriginFieldColors(),
                singleLine = true
            )
            suggestions.take(10).forEach { station ->
                DropdownMenuItem(
                    text = { Text(station) },
                    onClick = {
                        onValue(station)
                        keyword = station
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun JiebaoDateSelector(
    date: String,
    onDate: (String) -> Unit,
    studentTicket: Boolean,
    onStudentTicket: (Boolean) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { showDialog = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(formatMonthDay(date), color = OriginColors.TextPrimary, fontSize = 24.sp)
            Spacer(Modifier.width(10.dp))
            Text(todayText(date), color = OriginColors.TextSecondary, fontSize = 16.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = studentTicket, onCheckedChange = onStudentTicket)
            Text("学生票", color = OriginColors.TextPrimary, fontSize = 16.sp)
        }
    }

    if (showDialog) {
        val minDate = remember { todayUtcMillis() }
        val maxDate = remember { minDate + 15 * DAY_MILLIS }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = parseDateMillis(date)?.coerceIn(minDate, maxDate) ?: minDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis in minDate..maxDate
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    enabled = pickerState.selectedDateMillis != null,
                    onClick = {
                        pickerState.selectedDateMillis?.let { onDate(formatDateMillis(it)) }
                        showDialog = false
                    }
                ) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("取消") } }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
fun TicketResultsScreen(
    initial: TicketQueryResult,
    user: User?,
    onBack: () -> Unit,
    onCreateTask: (TaskDraft) -> Unit,
    onMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var result by remember(initial) { mutableStateOf(initial) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var onlyHigh by rememberSaveable { mutableStateOf(false) }
    var onlyNormal by rememberSaveable { mutableStateOf(false) }
    var onlyAvailable by rememberSaveable { mutableStateOf(false) }
    var showSeatDetails by rememberSaveable { mutableStateOf(true) }
    var sortMode by rememberSaveable { mutableStateOf("duration") }
    var loading by remember { mutableStateOf(false) }

    fun queryForDate(nextDate: String) {
        scope.launch {
            loading = true
            val params = JSONObject()
                .put("from_station", result.from)
                .put("to_station", result.to)
                .put("train_date", nextDate)
                .put("ticket_type", if (result.studentTicket) "0X00" else "ADULT")
                .put("only_has_ticket", false)
            if (result.trainType.isNotBlank()) params.put("train_types", result.trainType)
            val res = PythonBridge.call("query_tickets", params.toString())
            loading = false
            if (!res.success()) {
                onMessage(res.message())
                return@launch
            }
            result = result.copy(
                date = nextDate,
                trains = parseTrains(res.dataObject()?.optJSONArray("trains"))
            )
            selected = emptySet()
        }
    }

    val visibleTrains = result.trains
        .filter { train -> !onlyHigh || isHighSpeedTrain(train) }
        .filter { train -> !onlyNormal || !isHighSpeedTrain(train) }
        .filter { train -> !onlyAvailable || train.canBuy }
        .let { trains ->
            when (sortMode) {
                "depart" -> trains.sortedBy { it.startTime }
                else -> trains.sortedBy { durationMinutes(it.duration) }
            }
        }

    fun createTaskFromSelection() {
        if (selected.isEmpty()) {
            onMessage("请选择至少一个车次")
            return
        }
        onCreateTask(
            TaskDraft(
                name = "${result.date} ${result.from}-${result.to} 抢票",
                fromStation = result.from,
                toStation = result.to,
                trainDate = result.date,
                trainCodes = selected.joinToString(","),
                trainTypes = result.trainType.takeIf { it.isNotBlank() }?.let { setOf(it) } ?: emptySet()
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OriginColors.Page)
    ) {
        JiebaoResultsHeader(
            result = result,
            loading = loading,
            onBack = onBack,
            onDate = { date ->
                if (date != result.date) queryForDate(date)
            }
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 0.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                JiebaoDirectHeader()
            }
            item {
                JiebaoResultFilters(
                    result = result,
                    onlyHigh = onlyHigh,
                    onlyNormal = onlyNormal,
                    onlyAvailable = onlyAvailable,
                    onOnlyHigh = {
                        onlyHigh = it
                        if (it) onlyNormal = false
                    },
                    onOnlyNormal = {
                        onlyNormal = it
                        if (it) onlyHigh = false
                    },
                    onOnlyAvailable = { onlyAvailable = it }
                )
            }
            if (loading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (visibleTrains.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("暂无符合条件的车次", color = OriginColors.TextSecondary)
                    }
                }
            } else {
                items(visibleTrains, key = { "${result.date}-${it.trainCode}-${it.startTime}-${it.toStation}" }) { train ->
                    JiebaoTrainResultCard(
                        train = train,
                        selected = selected.contains(train.trainCode),
                        showSeatDetails = showSeatDetails,
                        onClick = {
                            selected = if (selected.contains(train.trainCode)) {
                                selected - train.trainCode
                            } else {
                                selected + train.trainCode
                            }
                        }
                    )
                }
            }
        }
        JiebaoResultsBottomBar(
            selectedCount = selected.size,
            sortMode = sortMode,
            showSeatDetails = showSeatDetails,
            loggedIn = user != null,
            onFilter = { onlyAvailable = !onlyAvailable },
            onDurationSort = { sortMode = "duration" },
            onDepartSort = { sortMode = "depart" },
            onToggleSeatDetails = { showSeatDetails = !showSeatDetails },
            onSubmit = ::createTaskFromSelection
        )
    }
}

@Composable
fun JiebaoResultsHeader(
    result: TicketQueryResult,
    loading: Boolean,
    onBack: () -> Unit,
    onDate: (String) -> Unit
) {
    Surface(color = OriginColors.Blue, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Text(
                    "${result.from}  <>  ${result.to}",
                    color = Color.White,
                    fontSize = 27.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { }) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color.White)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(queryDateOptions(result.date)) { date ->
                        val selected = date == result.date
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (selected) Color.White else Color.Transparent,
                            modifier = Modifier.clickable(enabled = !loading) { onDate(date) }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    todayText(date),
                                    color = if (selected) OriginColors.Blue else Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp
                                )
                                Text(
                                    formatShortDate(date),
                                    color = if (selected) OriginColors.Blue else Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(44.dp)) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.White)
                    Text("日历", color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun JiebaoDirectHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.DirectionsRailway, contentDescription = null, tint = OriginColors.Blue)
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("直达", color = OriginColors.Blue, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(
                    Modifier
                        .width(46.dp)
                        .height(4.dp)
                        .background(OriginColors.Blue, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
fun JiebaoResultFilters(
    result: TicketQueryResult,
    onlyHigh: Boolean,
    onlyNormal: Boolean,
    onlyAvailable: Boolean,
    onOnlyHigh: (Boolean) -> Unit,
    onOnlyNormal: (Boolean) -> Unit,
    onOnlyAvailable: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF2F2F2))
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterCheck("只看高铁/动车", onlyHigh, onOnlyHigh)
            FilterCheck("只看普通车", onlyNormal, onOnlyNormal)
            FilterCheck("只看有票", onlyAvailable, onOnlyAvailable)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                Icon(Icons.Default.FilterList, contentDescription = null, tint = OriginColors.TextSecondary, modifier = Modifier.size(20.dp))
                Text("筛选", color = OriginColors.TextSecondary, fontSize = 15.sp)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StationTag(result.from)
            StationTag(result.to)
            StationTag(trainTypeSummary(result.trainType))
            StationTag(if (result.studentTicket) "学生票" else "成人票")
        }
    }
}

@Composable
fun FilterCheck(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
        Checkbox(checked = checked, onCheckedChange = onChecked, modifier = Modifier.size(34.dp))
        Text(label, color = OriginColors.TextSecondary, fontSize = 15.sp)
    }
}

@Composable
fun StationTag(text: String) {
    Surface(shape = RoundedCornerShape(5.dp), color = Color.White) {
        Text(
            text,
            color = OriginColors.TextSecondary,
            fontSize = 17.sp,
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 10.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun JiebaoTrainResultCard(
    train: TrainInfo,
    selected: Boolean,
    showSeatDetails: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(7.dp)
    var cardModifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
    if (selected) {
        cardModifier = cardModifier.border(1.5.dp, OriginColors.Blue, shape)
    }
    Surface(
        modifier = cardModifier,
        color = Color(0xFFFAFEFF),
        shape = shape,
        shadowElevation = 1.dp
    ) {
        Box {
            if (isHighSpeedTrain(train)) {
                Text(
                    "复兴号",
                    color = Color(0xFFC98B2A),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(Color(0xFFFFF4D9))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Column(
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.width(108.dp)) {
                        Text(train.startTime, color = OriginColors.TextPrimary, fontSize = 28.sp)
                        Text(train.fromStation, color = OriginColors.TextPrimary, fontSize = 20.sp, maxLines = 2)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(train.trainCode, color = OriginColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(
                            Modifier
                                .fillMaxWidth(0.82f)
                                .height(1.dp)
                                .background(OriginColors.Divider)
                        )
                        Text(train.duration, color = OriginColors.TextMuted, fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(116.dp)) {
                        Text(train.arriveTime, color = OriginColors.TextPrimary, fontSize = 28.sp)
                        Text(train.toStation, color = OriginColors.TextPrimary, fontSize = 20.sp, maxLines = 2, textAlign = TextAlign.End)
                    }
                    Text(
                        availabilityText(train),
                        color = if (train.canBuy) OriginColors.Warning else OriginColors.TextMuted,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 10.dp, top = 4.dp)
                    )
                }
                if (showSeatDetails) {
                    OriginDivider()
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SeatStatus("二等", train.secondSeat)
                        SeatStatus("一等", train.firstSeat)
                        SeatStatus("商务", train.businessSeat)
                        SeatStatus("软卧", train.softSleeper)
                        SeatStatus("硬卧", train.hardSleeper)
                        SeatStatus("无座", train.noSeat)
                    }
                }
            }
        }
    }
}

@Composable
fun SeatStatus(label: String, value: String) {
    val active = seatAvailable(value)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = OriginColors.TextSecondary, fontSize = 16.sp)
        Spacer(Modifier.width(3.dp))
        Text(
            value.ifBlank { "--" },
            color = if (active) OriginColors.Success else OriginColors.TextMuted,
            fontSize = 16.sp
        )
    }
}

@Composable
fun JiebaoResultsBottomBar(
    selectedCount: Int,
    sortMode: String,
    showSeatDetails: Boolean,
    loggedIn: Boolean,
    onFilter: () -> Unit,
    onDurationSort: () -> Unit,
    onDepartSort: () -> Unit,
    onToggleSeatDetails: () -> Unit,
    onSubmit: () -> Unit
) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(80.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BottomAction(Icons.Default.FilterList, "筛选", false, onFilter, Modifier.weight(1f))
            BottomAction(Icons.Default.Timer, "耗时最短", sortMode == "duration", onDurationSort, Modifier.weight(1f))
            BottomAction(Icons.Default.Refresh, "发时最早", sortMode == "depart", onDepartSort, Modifier.weight(1f))
            BottomAction(Icons.Default.Search, "显示余票", showSeatDetails, onToggleSeatDetails, Modifier.weight(1f))
            BottomAction(
                Icons.Default.ShoppingCart,
                if (selectedCount > 0) "候补下单($selectedCount)" else if (loggedIn) "候补下单" else "登录下单",
                selectedCount > 0,
                onSubmit,
                Modifier.weight(1.15f)
            )
        }
    }
}

@Composable
fun BottomAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (selected) OriginColors.Blue else OriginColors.TextPrimary
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
        Text(label, color = color, fontSize = 14.sp, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
fun StationInput(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    var searchRequest by remember { mutableStateOf(0) }

    LaunchedEffect(value, expanded, searchRequest) {
        if (!expanded) return@LaunchedEffect
        val keyword = value.trim()
        if (keyword.isBlank()) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(250)
        val res = PythonBridge.call("search_stations", keyword)
        if (!res.success()) {
            suggestions = emptyList()
            onMessage(res.message())
            return@LaunchedEffect
        }
        val arr = res.dataObject()?.optJSONArray("stations") ?: JSONArray()
        suggestions = (0 until arr.length()).map { arr.getJSONObject(it).optString("name") }.filter { it.isNotBlank() }
    }

    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValue(it)
                expanded = true
            },
            label = { Text(label) },
            modifier = modifier.fillMaxWidth(),
            colors = OriginFieldColors(),
            trailingIcon = {
                IconButton(onClick = {
                    expanded = true
                    searchRequest += 1
                }) {
                    Icon(Icons.Default.Search, null)
                }
            }
        )
        DropdownMenu(
            expanded = expanded && suggestions.isNotEmpty(),
            onDismissRequest = { expanded = false }
        ) {
            suggestions.take(8).forEach { station ->
                DropdownMenuItem(
                    text = { Text(station) },
                    onClick = {
                        onValue(station)
                        expanded = false
                        suggestions = emptyList()
                    }
                )
            }
        }
    }
}

@Composable
fun DatePickerField(label: String, value: String, onValue: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val minDate = remember { todayUtcMillis() }
    val maxDate = remember { minDate + 15 * DAY_MILLIS }
    val selectedDate = remember(value) { parseDateMillis(value)?.coerceIn(minDate, maxDate) ?: minDate }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().clickable { showDialog = true },
        colors = OriginFieldColors(),
        trailingIcon = {
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.DateRange, null)
            }
        }
    )

    if (showDialog) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis in minDate..maxDate
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    enabled = pickerState.selectedDateMillis != null,
                    onClick = {
                        pickerState.selectedDateMillis?.let { onValue(formatDateMillis(it)) }
                        showDialog = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
fun TrainTypeDropdown(label: String, value: String, onValue: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = trainTypeLabel(value),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            colors = OriginFieldColors(),
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, null)
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            trainTypeOptions.forEach { (code, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onValue(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun TrainCard(train: TrainInfo, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    OriginCard(contentPadding = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = checked, onCheckedChange = onCheckedChange)
                Column(Modifier.weight(1f)) {
                    Text(
                        train.trainCode,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OriginColors.TextPrimary
                    )
                    Text(
                        "${train.fromStation} ${train.startTime} → ${train.toStation} ${train.arriveTime} · ${train.duration}",
                        color = OriginColors.TextSecondary
                    )
                }
                OriginStatusChip(if (train.canBuy) "可购" else "候补", if (train.canBuy) OriginColors.Success else OriginColors.Warning)
            }
            OriginDivider()
            Text("商务 ${train.businessSeat} · 一等 ${train.firstSeat} · 二等 ${train.secondSeat}", color = OriginColors.TextPrimary)
            Text("软卧 ${train.softSleeper} · 硬卧 ${train.hardSleeper} · 硬座 ${train.hardSeat} · 无座 ${train.noSeat}", color = OriginColors.TextSecondary)
    }
}

@Composable
fun TasksScreen(
    onCreate: () -> Unit,
    onEdit: (TicketTask) -> Unit,
    onDetail: (Int) -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tasks by remember { mutableStateOf<List<TicketTask>>(emptyList()) }
    var filter by rememberSaveable { mutableStateOf("") }

    fun load() {
        scope.launch {
            val res = PythonBridge.call("get_tasks", filter)
            tasks = parseTasks(res.dataObject()?.optJSONArray("tasks"))
        }
    }

    LaunchedEffect(filter) { load() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().originPage(),
        contentPadding = PaddingValues(OriginPagePadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OriginCard(contentPadding = 16.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OriginSectionHeader("抢票任务", modifier = Modifier.weight(1f))
                    OriginSecondaryButton(onClick = { filter = ""; load() }) { Text("全部") }
                }
                OriginPrimaryButton(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.size(8.dp))
                    Text("创建任务")
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("pending", "running", "paused", "success", "failed", "cancelled").forEach { status ->
                        FilterChip(selected = filter == status, onClick = { filter = status }, label = { Text(statusText(status)) })
                    }
                }
            }
        }
        items(tasks, key = { it.id }) { task ->
            TaskCard(
                task = task,
                onDetail = { onDetail(task.id) },
                onEdit = { onEdit(task) },
                onStart = {
                    scope.launch {
                        requestBatteryOptimizationExemption(context)
                        val res = PythonBridge.call("start_task", task.id)
                        if (!res.success()) onMessage(res.message())
                        TaskRunnerService.start(context)
                        load()
                    }
                },
                onStop = {
                    scope.launch {
                        PythonBridge.call("stop_task", task.id)
                        load()
                    }
                },
                onCancel = {
                    scope.launch {
                        PythonBridge.call("cancel_task", task.id)
                        load()
                    }
                },
                onDelete = {
                    scope.launch {
                        PythonBridge.call("delete_task", task.id)
                        load()
                    }
                }
            )
        }
    }
}

@Composable
fun TaskCard(
    task: TicketTask,
    onDetail: () -> Unit,
    onEdit: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    OriginCard(contentPadding = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(task.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = OriginColors.TextPrimary)
                    Text("${task.fromStation} → ${task.toStation} · ${task.trainDate}", color = OriginColors.TextSecondary)
                }
                OriginStatusChip(statusText(task.status), originStatusColor(task.status))
            }
            OriginDivider()
            Text("席别 ${formatSeatTypes(task.seatTypes)} · 重试 ${task.retryCount}/${if (task.maxRetryCount < 0) "∞" else task.maxRetryCount}", color = OriginColors.TextPrimary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OriginSecondaryButton(onClick = onDetail) { Text("详情") }
                if (task.status != "running" && task.status != "success") OriginSecondaryButton(onClick = onEdit) { Text("编辑") }
                if (task.status in listOf("pending", "paused", "failed", "cancelled")) OriginPrimaryButton(onClick = onStart) {
                    Icon(Icons.Default.PlayArrow, null)
                    Text("启动")
                }
                if (task.status == "running") OriginSecondaryButton(onClick = onStop) {
                    Icon(Icons.Default.Pause, null)
                    Text("暂停")
                }
                if (task.status !in listOf("success", "cancelled")) OriginSecondaryButton(onClick = onCancel) {
                    Icon(Icons.Default.Stop, null)
                    Text("取消")
                }
                if (task.status != "running") OriginSecondaryButton(onClick = onDelete) { Text("删除") }
            }
    }
}

@Composable
fun TaskEditScreen(
    initial: TaskDraft,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    onMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf(initial) }
    var contacts by remember { mutableStateOf<List<Passenger>>(emptyList()) }
    var loadingPassengers by remember { mutableStateOf(false) }
    var loadingTrains by remember { mutableStateOf(false) }
    var availableTrainCodes by remember(initial.id) { mutableStateOf(splitCsvCodes(initial.trainCodes)) }

    fun updateSelectedTrainCodes(codes: List<String>) {
        draft = draft.copy(trainCodes = codes.distinct().joinToString(","))
    }

    fun queryTrainCodes() {
        scope.launch {
            if (draft.fromStation.isBlank() || draft.toStation.isBlank() || draft.trainDate.isBlank()) {
                onMessage("请先填写出发站、到达站和出发日期")
                return@launch
            }
            loadingTrains = true
            val params = JSONObject()
                .put("from_station", draft.fromStation)
                .put("to_station", draft.toStation)
                .put("train_date", draft.trainDate)
                .put("only_has_ticket", false)
            if (draft.trainTypes.isNotEmpty()) {
                params.put("train_types", draft.trainTypes.joinToString(","))
            }
            val range = draft.startTimeRange.split("-").map { it.trim() }
            if (range.size == 2 && range[0].isNotBlank() && range[1].isNotBlank()) {
                params.put("start_time_min", range[0])
                params.put("start_time_max", range[1])
            }
            val res = PythonBridge.call("query_tickets", params.toString())
            loadingTrains = false
            if (!res.success()) {
                onMessage(res.message())
                return@launch
            }
            val codes = parseTrains(res.dataObject()?.optJSONArray("trains")).map { it.trainCode }.distinct()
            availableTrainCodes = codes
            updateSelectedTrainCodes(splitCsvCodes(draft.trainCodes).filter { it in codes })
            onMessage("查询到 ${codes.size} 个符合条件的车次")
        }
    }

    fun addPassenger(passenger: Passenger) {
        if (draft.passengers.none { it.idNo == passenger.idNo }) {
            draft = draft.copy(passengers = draft.passengers + passenger.copy(passengerType = normalizeTicketType(passenger.passengerType)))
        }
    }

    fun removePassenger(idNo: String) {
        draft = draft.copy(passengers = draft.passengers.filterNot { it.idNo == idNo })
    }

    fun updatePassengerTicketType(idNo: String, ticketType: String) {
        draft = draft.copy(
            passengers = draft.passengers.map { passenger ->
                if (passenger.idNo == idNo) passenger.copy(passengerType = normalizeTicketType(ticketType)) else passenger
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().originPage(),
        contentPadding = PaddingValues(OriginPagePadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OriginCard(contentPadding = 16.dp) {
                    OriginSectionHeader(if (draft.id == null) "创建抢票任务" else "编辑抢票任务")
                    OutlinedTextField(
                        draft.name,
                        { draft = draft.copy(name = it) },
                        label = { Text("任务名称") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OriginFieldColors()
                    )
                    StationInput("出发站", draft.fromStation, { draft = draft.copy(fromStation = it) }, onMessage)
                    StationInput("到达站", draft.toStation, { draft = draft.copy(toStation = it) }, onMessage)
                    DatePickerField("出发日期", draft.trainDate) { draft = draft.copy(trainDate = it) }
                    OutlinedTextField(
                        draft.startTimeRange,
                        { draft = draft.copy(startTimeRange = it) },
                        label = { Text("出发时间段，例如 08:00-12:00") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OriginFieldColors()
                    )
                    TrainTypeChipSet(
                        title = "车次类型",
                        selected = draft.trainTypes,
                        onChange = { values ->
                            draft = draft.copy(trainTypes = values)
                        }
                    )
                    SeatPriorityEditor(draft.seatTypes) { draft = draft.copy(seatTypes = it) }
                    TrainCodeSelector(
                        availableCodes = availableTrainCodes,
                        selectedCodes = splitCsvCodes(draft.trainCodes),
                        loading = loadingTrains,
                        onQuery = ::queryTrainCodes,
                        onToggle = { code ->
                            val selectedCodes = splitCsvCodes(draft.trainCodes)
                            updateSelectedTrainCodes(
                                if (selectedCodes.contains(code)) selectedCodes - code else selectedCodes + code
                            )
                        }
                    )
            }
        }
        item {
            OriginCard(contentPadding = 16.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OriginSectionHeader("乘车人", modifier = Modifier.weight(1f))
                        OriginSecondaryButton(
                            enabled = !loadingPassengers,
                            onClick = {
                                scope.launch {
                                    loadingPassengers = true
                                    val res = PythonBridge.call("get_passengers")
                                    loadingPassengers = false
                                    if (!res.success()) {
                                        onMessage(res.message())
                                        return@launch
                                    }
                                    contacts = parsePassengers(res.optJSONArray("data"))
                                }
                            }
                        ) { Text("获取联系人") }
                    }
                    if (draft.passengers.isEmpty()) {
                        Text("请先获取联系人并选择至少一名乘车人")
                    } else {
                        Text("已选乘车人", style = MaterialTheme.typography.labelLarge)
                        draft.passengers.forEach { passenger ->
                            SelectedPassengerTicketTypeRow(
                                passenger = passenger,
                                onTicketTypeChange = { ticketType -> updatePassengerTicketType(passenger.idNo, ticketType) },
                                onRemove = { removePassenger(passenger.idNo) }
                            )
                        }
                    }
                    if (contacts.isNotEmpty()) {
                        Text("联系人", style = MaterialTheme.typography.labelLarge)
                        contacts.forEach { passenger ->
                            val checked = draft.passengers.any { it.idNo == passenger.idNo }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) addPassenger(passenger) else removePassenger(passenger.idNo)
                                    }
                                )
                                Column(Modifier.weight(1f)) {
                                    Text("${passenger.name} · ${maskId(passenger.idNo)}")
                                    Text(
                                        "联系人身份：${passengerIdentityLabel(passenger.passengerType)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
            }
        }
        item {
            OriginCard(contentPadding = 16.dp) {
                    OriginSectionHeader("任务配置")
                    OutlinedTextField(
                        draft.queryInterval,
                        { draft = draft.copy(queryInterval = it.filter(Char::isDigit)) },
                        label = { Text("刷票间隔 3-60 秒") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OriginFieldColors()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(draft.infiniteRetry, onCheckedChange = { draft = draft.copy(infiniteRetry = it) })
                        Text("无限重试")
                    }
                    if (!draft.infiniteRetry) {
                        OutlinedTextField(
                            draft.maxRetryCount,
                            { draft = draft.copy(maxRetryCount = it.filter(Char::isDigit)) },
                            label = { Text("最大重试次数") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OriginFieldColors()
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(draft.autoSubmit, onCheckedChange = { draft = draft.copy(autoSubmit = it) })
                        Text("发现余票后自动提交订单")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OriginPrimaryButton(
                            onClick = {
                                scope.launch {
                                    if (!draft.isValid()) {
                                        onMessage("请填写完整任务信息并选择乘车人/席别")
                                        return@launch
                                    }
                                    val taskId = draft.id
                                    val res = if (taskId == null) {
                                        PythonBridge.call("create_task", draft.toJson().toString())
                                    } else {
                                        PythonBridge.call("update_task", taskId, draft.toJson().toString())
                                    }
                                    if (res.success()) onDone() else onMessage(res.message())
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("保存") }
                        OriginSecondaryButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("取消") }
                    }
            }
        }
    }
}

@Composable
fun TrainTypeChipSet(title: String, selected: Set<String>, onChange: (Set<String>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = selected.isEmpty(),
                onClick = { onChange(emptySet()) },
                label = { Text("全部") }
            )
            trainTypeCodes.forEach { code ->
                FilterChip(
                    selected = selected.contains(code),
                    onClick = {
                        val next = if (selected.contains(code)) selected - code else selected + code
                        onChange(trainTypeCodes.filter { it in next }.toSet())
                    },
                    label = { Text(trainTypeLabel(code)) }
                )
            }
        }
    }
}

@Composable
fun TrainCodeSelector(
    availableCodes: List<String>,
    selectedCodes: List<String>,
    loading: Boolean,
    onQuery: () -> Unit,
    onToggle: (String) -> Unit
) {
    val optionCodes = (availableCodes + selectedCodes).distinct()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("指定车次", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            OriginSecondaryButton(enabled = !loading, onClick = onQuery) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Icon(Icons.Default.Search, null)
                }
                Spacer(Modifier.size(6.dp))
                Text(if (loading) "查询中..." else "查询车次")
            }
        }
        if (optionCodes.isEmpty()) {
            Text("留空表示不限车次", style = MaterialTheme.typography.bodySmall)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                optionCodes.forEach { code ->
                    FilterChip(
                        selected = selectedCodes.contains(code),
                        onClick = { onToggle(code) },
                        label = { Text(code) }
                    )
                }
            }
        }
    }
}

@Composable
fun SeatPriorityEditor(selected: List<String>, onChange: (List<String>) -> Unit) {
    val seats = listOf("9" to "商务", "M" to "一等", "O" to "二等", "4" to "软卧", "3" to "硬卧", "1" to "硬座")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("席别优先级", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            seats.forEach { (code, label) ->
                FilterChip(
                    selected = selected.contains(code),
                    onClick = {
                        onChange(if (selected.contains(code)) selected - code else selected + code)
                    },
                    label = { Text(label) }
                )
            }
        }
        Text("选择顺序即优先级：${formatSeatTypes(selected.joinToString(","))}")
    }
}

@Composable
fun SelectedPassengerTicketTypeRow(
    passenger: Passenger,
    onTicketTypeChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(passenger.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(maskId(passenger.idNo), style = MaterialTheme.typography.bodySmall)
            }
            OriginTextButton(onClick = onRemove) { Text("移除") }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ticketTypeOptions.forEach { (code, label) ->
                FilterChip(
                    selected = normalizeTicketType(passenger.passengerType) == code,
                    onClick = { onTicketTypeChange(code) },
                    label = { Text(label) }
                )
            }
        }
    }
}

@Composable
fun TaskDetailScreen(
    taskId: Int,
    onBack: () -> Unit,
    onEdit: (TicketTask) -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var task by remember { mutableStateOf<TicketTask?>(null) }
    var logs by remember { mutableStateOf<List<TaskLog>>(emptyList()) }

    suspend fun load() {
        val taskRes = PythonBridge.call("get_task", taskId)
        if (taskRes.success()) task = taskRes.dataObject()?.let { parseTask(it) }
        val logRes = PythonBridge.call("get_task_logs", taskId, 200)
        logs = parseLogs(logRes.dataObject()?.optJSONArray("logs"))
    }

    LaunchedEffect(taskId) {
        while (true) {
            load()
            delay(if (task?.status == "running") 3_000 else 8_000)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().originPage(),
        contentPadding = PaddingValues(OriginPagePadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OriginSecondaryButton(onClick = onBack) { Text("返回任务列表") }
        }
        val current = task
        if (current != null) {
            item {
                OriginCard(contentPadding = 16.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                current.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = OriginColors.TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            OriginStatusChip(statusText(current.status), originStatusColor(current.status))
                        }
                        OriginDivider()
                        Text("${current.fromStation} → ${current.toStation} · ${current.trainDate}", color = OriginColors.TextPrimary)
                        Text("车次 ${current.trainCodes ?: "不限"} · 席别 ${formatSeatTypes(current.seatTypes)}", color = OriginColors.TextSecondary)
                        Text("重试 ${current.retryCount}/${if (current.maxRetryCount < 0) "∞" else current.maxRetryCount} · 间隔 ${current.queryInterval}s", color = OriginColors.TextSecondary)
                        if (current.resultMessage != null) Text(current.resultMessage, color = OriginColors.TextPrimary)
                        if (current.orderId != null) Text("订单号：${current.orderId}", fontWeight = FontWeight.Bold, color = OriginColors.TextPrimary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (current.status != "running" && current.status != "success") OriginSecondaryButton(onClick = { onEdit(current) }) { Text("编辑") }
                            if (current.status in listOf("pending", "paused", "failed", "cancelled")) OriginPrimaryButton(onClick = {
                                scope.launch {
                                    requestBatteryOptimizationExemption(context)
                                    val res = PythonBridge.call("start_task", current.id)
                                    if (!res.success()) onMessage(res.message())
                                    TaskRunnerService.start(context)
                                    load()
                                }
                            }) { Text("启动") }
                            if (current.status == "running") OriginSecondaryButton(onClick = {
                                scope.launch { PythonBridge.call("stop_task", current.id); load() }
                            }) { Text("暂停") }
                            if (current.status == "success") OriginPrimaryButton(onClick = { openPaymentPage(context) }) { Text("去支付") }
                        }
                }
            }
            item {
                OriginCard(contentPadding = 16.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OriginSectionHeader("运行日志", modifier = Modifier.weight(1f))
                            IconButton(onClick = { scope.launch { load() } }) { Icon(Icons.Default.Refresh, null) }
                        }
                        logs.ifEmpty { listOf(TaskLog(0, "info", "暂无日志", "")) }.forEach { log ->
                            Text("${log.createdAt} [${log.level}] ${log.message}", color = OriginColors.TextSecondary)
                        }
                }
            }
        } else {
            item { LoadingBox("正在加载任务...") }
        }
    }
}

fun TicketTask.toDraft(): TaskDraft {
    val passengers = try {
        parsePassengers(JSONArray(this.passengers))
    } catch (_: Exception) {
        emptyList()
    }
    return TaskDraft(
        id = id,
        name = name,
        fromStation = fromStation,
        toStation = toStation,
        trainDate = trainDate,
        trainCodes = trainCodes.orEmpty(),
        trainTypes = trainTypes?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet(),
        seatTypes = seatTypes.split(",").filter { it.isNotBlank() },
        startTimeRange = startTimeRange.orEmpty(),
        passengers = passengers,
        queryInterval = queryInterval.toString(),
        maxRetryCount = maxRetryCount.takeIf { it >= 0 }?.toString() ?: "100",
        infiniteRetry = maxRetryCount < 0,
        autoSubmit = autoSubmit
    )
}

fun TaskDraft.isValid(): Boolean =
    name.isNotBlank() &&
        fromStation.isNotBlank() &&
        toStation.isNotBlank() &&
        trainDate.isNotBlank() &&
        seatTypes.isNotEmpty() &&
        passengers.isNotEmpty()

fun statusText(status: String): String = when (status) {
    "pending" -> "待运行"
    "running" -> "运行中"
    "paused" -> "已暂停"
    "success" -> "成功"
    "failed" -> "失败"
    "cancelled" -> "已取消"
    else -> status
}

fun formatSeatTypes(types: String): String {
    val map = mapOf("9" to "商务", "M" to "一等", "O" to "二等", "4" to "软卧", "3" to "硬卧", "1" to "硬座")
    return types.split(",").filter { it.isNotBlank() }.joinToString("、") { map[it] ?: it }
}

fun maskId(id: String): String =
    if (id.length < 8) id else id.take(4) + "****" + id.takeLast(4)

fun shareQr(context: Context, base64: String, onMessage: (String) -> Unit) {
    if (base64.isBlank()) return
    runCatching {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val file = File(context.cacheDir, "login-qrcode.png")
        file.writeBytes(bytes)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, "分享登录二维码"))
    }.onFailure { onMessage(it.message ?: "分享失败") }
}

fun openPaymentPage(context: Context) {
    val uri = Uri.parse("https://kyfw.12306.cn/otn/view/train_order.html")
    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
}

fun requestBatteryOptimizationExemption(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    val powerManager = context.getSystemService(PowerManager::class.java)
    if (powerManager.isIgnoringBatteryOptimizations(context.packageName)) return
    runCatching {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
