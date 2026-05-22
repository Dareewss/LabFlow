package com.labflow.companion

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import java.time.LocalDate

private enum class HomeMode {
    BORROWED,
    CONTAINERS,
    BOTH
}

private enum class MainTab {
    HOME,
    SCAN,
    BORROWS,
    PROFILE
}

private enum class BorrowTab {
    ACTIVE,
    HISTORY
}

private enum class HomeScreenMode {
    DASHBOARD,
    LAB_STATUS
}

class MainActivity : AppCompatActivity() {
    private lateinit var settings: SettingsStore
    private lateinit var colors: CompanionColors
    private lateinit var networkMonitor: NetworkStateMonitor

    private lateinit var hostInput: EditText
    private lateinit var portInput: EditText
    private lateinit var apiKeyInput: EditText
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var resultText: TextView
    private lateinit var statusPill: TextView
    private lateinit var actionBanner: TextView
    private lateinit var borrowButton: Button
    private lateinit var returnButton: Button
    private lateinit var faultButton: Button
    private lateinit var refreshButton: Button
    private lateinit var loginButton: Button

    private var currentEquipment: EquipmentDto? = null
    private var currentHome: CompanionHomeDto? = null
    private var currentHomeMode = HomeMode.BOTH
    private var currentBorrowTab = BorrowTab.ACTIVE
    private var currentTab = MainTab.HOME
    private var currentHomeScreenMode = HomeScreenMode.DASHBOARD
    private var currentFocusField: View? = null
    private var rootScrollView: ScrollView? = null
    private var keyboardLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var keyboardRootView: View? = null
    private var isNetworkConnected = true
    private var isLoadingHome = false
    private var homeLoadError: String? = null

    private val scannerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val qrCode = result.data?.getStringExtra("qrCode")
        if (!qrCode.isNullOrBlank()) {
            loadEquipment(qrCode, presentSheet = true)
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            Toast.makeText(this, tr("notifications_permission_denied", "Notifications permission was not granted."), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            settings = SettingsStore(this)
            colors = CompanionTheme.resolve(settings.paletteEnum(), settings.modeEnum())
            networkMonitor = NetworkStateMonitor(this)
            networkMonitor.isConnected.observe(this) { connected ->
                isNetworkConnected = connected
                if (settings.isLoggedIn) {
                    if (!connected) {
                        isLoadingHome = false
                        if (currentHome == null) {
                            homeLoadError = tr("no_wifi_connection", "No WiFi connection. Connect to the same network as your PC.")
                        }
                        buildMainUi()
                    } else if (currentHome == null && !isLoadingHome) {
                        buildMainUi()
                    } else if (homeLoadError != null) {
                        buildMainUi()
                    }
                }
            }
            networkMonitor.start()
            NotificationChannelManager.createChannels(this)
            applyLaunchTarget(intent)
            if (settings.isLoggedIn) {
                ensureNotificationPermission()
                if (settings.notificationsEnabled) {
                    LabFlowNotificationService.schedule(this)
                } else {
                    LabFlowNotificationService.cancel(this)
                }
                buildMainUi()
            } else {
                buildLoginUi()
            }
        } catch (e: Exception) {
            showFatalError(e)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyLaunchTarget(intent)
        if (settings.isLoggedIn) {
            buildMainUi()
        }
    }

    override fun onDestroy() {
        keyboardLayoutListener?.let { listener ->
            keyboardRootView?.viewTreeObserver?.removeOnGlobalLayoutListener(listener)
        }
        keyboardLayoutListener = null
        keyboardRootView = null
        if (::networkMonitor.isInitialized) {
            networkMonitor.stop()
        }
        super.onDestroy()
    }

    private fun buildLoginUi() {
        currentEquipment = null
        currentHome = null

        val root = pageRoot().apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(dp(22), dp(18), dp(22), dp(28))
        }
        root.addView(loginTopBar())
        root.addView(loginHero())
        root.addView(space(1, 20))

        val loginCard = card().apply {
            background = rounded(colors.surface, dp(16), softBorderColor(), 1)
            elevation = dp(4).toFloat()
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        usernameInput = input("Username", settings.username).apply {
            imeOptions = EditorInfo.IME_ACTION_NEXT
        }
        passwordInput = input(
            "Password",
            "",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        ).apply {
            imeOptions = EditorInfo.IME_ACTION_DONE
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    saveSettings()
                    login()
                    true
                } else {
                    false
                }
            }
        }
        registerFocusTracking(usernameInput, passwordInput)

        loginCard.addView(fieldShell("Username", "U", usernameInput))
        loginCard.addView(space(1, 12))
        loginCard.addView(passwordFieldShell())

        loginButton = button(tr("sign_in", "Sign In"), colors.primary) {
            saveSettings()
            login()
        }.apply {
            minimumHeight = dp(52)
        }
        loginCard.addView(loginButton)

        resultText = messageText(tr("login_helper", "Enter your desktop credentials to continue.")).apply {
            setPadding(0, dp(12), 0, 0)
        }
        loginCard.addView(resultText)

        val toggleConnection = TextView(this).apply {
            text = tr("connect_server_first", "Connect to server first ->")
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(colors.muted)
            setPadding(0, dp(14), 0, 0)
        }

        val connectionCard = connectionInlineCard().apply {
            visibility = View.GONE
            alpha = 0f
        }
        toggleConnection.setOnClickListener {
            val expanding = connectionCard.visibility != View.VISIBLE
            if (expanding) {
                connectionCard.visibility = View.VISIBLE
                connectionCard.animate().alpha(1f).setDuration(180).start()
                toggleConnection.text = tr("hide_server_settings", "Hide server settings")
            } else {
                connectionCard.animate().alpha(0f).setDuration(160).withEndAction {
                    connectionCard.visibility = View.GONE
                }.start()
                toggleConnection.text = tr("connect_server_first", "Connect to server first ->")
            }
        }
        loginCard.addView(toggleConnection)
        loginCard.addView(connectionCard)

        root.addView(loginCard)
        setScrollableContent(root)
    }

    private fun loginTopBar(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(10))
            addView(View(this@MainActivity), LinearLayout.LayoutParams(0, 0, 1f))
            addView(TextView(this@MainActivity).apply {
                text = tr("settings", "Settings")
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(colors.foreground)
                background = rounded(colors.surfaceAlt, dp(14), softBorderColor(), 1)
                setPadding(dp(14), dp(10), dp(14), dp(10))
                setOnClickListener { showLoginSettingsDialog() }
                addPressAnimation()
            })
        }
    }

    private fun buildMainUi() {
        val home = currentHome
        val root = pageRoot().apply {
            setPadding(dp(22), dp(28), dp(22), dp(112))
        }
        root.addView(companionTopBar(home))
        offlineBannerText()?.let { root.addView(offlineBanner(it)) }
        if (home == null) {
            if (isLoadingHome) {
                root.addView(skeletonSection())
            } else {
                root.addView(companionErrorStateSection())
            }
        } else {
            when (currentTab) {
                MainTab.HOME -> {
                    if (currentHomeScreenMode == HomeScreenMode.LAB_STATUS) {
                        root.addView(labStatusDetailSection(home))
                    } else {
                        root.addView(homeGreetingSection(home))
                        root.addView(connectionStatusSection())
                        root.addView(homeQuickActionsSection(home))
                        root.addView(activeBorrowsHomeSection(home))
                        root.addView(homeHealthSection(home))
                        root.addView(blueContentSection(home))
                    }
                }
                MainTab.SCAN -> {
                    root.addView(scanHubSection())
                    root.addView(scannedEquipmentSection())
                }
                MainTab.BORROWS -> {
                    root.addView(borrowsSection(home))
                    root.addView(scannedEquipmentSection())
                }
                MainTab.PROFILE -> {
                    root.addView(profileSection(home))
                }
            }
        }
        setScrollableContent(root, buildBottomNavigation(), currentTab == MainTab.BORROWS)
        if (home == null) {
            if (!isLoadingHome) {
                loadCompanionHome()
            }
        } else {
            updateActionState()
            currentEquipment?.let { showEquipment(it) }
        }
    }

    private fun loginHero(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
            addView(brandWordmark(24f, 34))
            addView(TextView(this@MainActivity).apply {
                text = tr("login_subtitle", "Laboratory Management")
                textSize = 13f
                setTextColor(colors.muted)
                setPadding(dp(46), dp(6), 0, 0)
            })
        }
    }

    private fun connectionInlineCard(): LinearLayout {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(14), 0, 0)
        }
        hostInput = input(tr("pc_ip", "PC IP"), settings.host)
        portInput = input(tr("port", "Port"), settings.port.ifBlank { "8080" }, InputType.TYPE_CLASS_NUMBER)
        apiKeyInput = input(tr("api_key", "API key"), settings.apiKey)
        registerFocusTracking(hostInput, portInput, apiKeyInput)
        wrap.addView(fieldShell(tr("pc_ip", "PC IP"), "IP", hostInput))
        wrap.addView(space(1, 10))
        wrap.addView(fieldShell(tr("port", "Port"), "#", portInput))
        wrap.addView(space(1, 10))
        wrap.addView(fieldShell(tr("api_key", "API key"), "K", apiKeyInput))
        wrap.addView(button(tr("test_connection", "Test connection"), colors.accent) {
            saveSettings()
            testConnection()
        }.apply {
            minimumHeight = dp(48)
        })
        return wrap
    }

    private fun companionTopBar(home: CompanionHomeDto?): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(16))

            addView(topActionButton("N", colors.success, "${home?.stats?.notificationCount ?: 0}") {
                showNotificationsDialog(home?.notifications.orEmpty())
            })

            addView(View(this@MainActivity), LinearLayout.LayoutParams(0, 0, 1f))

            addView(topActionButton("M", colors.accent, tr("menu", "Menu")) {
                showSettingsMenu()
            })
        }
    }

    private fun homeGreetingSection(home: CompanionHomeDto?): LinearLayout {
        val now = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val greeting = when {
            now < 12 -> tr("good_morning", "Good morning")
            now < 18 -> tr("good_afternoon", "Good afternoon")
            else -> tr("good_evening", "Good evening")
        }
        val name = settings.fullName.ifBlank { settings.username.ifBlank { tr("labflow_user", "LabFlow user") } }.substringBefore(" ")
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@MainActivity).apply {
                text = "$greeting, $name"
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(colors.foreground)
            })
            addView(TextView(this@MainActivity).apply {
                text = home?.lab?.name.orEmpty().ifBlank { tr("active_laboratory", "Active laboratory") }
                textSize = 13f
                setTextColor(colors.muted)
                setPadding(0, dp(4), 0, dp(10))
            })
        }
    }

    private fun connectionStatusSection(): LinearLayout {
        return sectionCard(tr("connection", "Connection"), colors.success).apply {
            addView(TextView(this@MainActivity).apply {
                text = tr("connected", "Connected")
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(colors.success)
            })
            addView(TextView(this@MainActivity).apply {
                text = "${settings.host.ifBlank { tr("pc_ip_not_set", "PC IP not set") }} : ${settings.port.ifBlank { "8080" }}"
                textSize = 13f
                setTextColor(colors.muted)
                setPadding(0, dp(6), 0, dp(12))
            })
            addView(button(tr("connection_details", "Connection details"), colors.surfaceAlt) {
                showSettingsMenu()
            }.apply {
                setTextColor(colors.foreground)
            })
        }
    }

    private fun homeQuickActionsSection(home: CompanionHomeDto?): LinearLayout {
        val card = sectionCard(tr("quick_actions", "Quick actions"), colors.warning)
        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(6), 0, 0)
        }
        val firstRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        firstRow.addView(homeActionTile(tr("scan_qr", "Scan QR"), tr("scan_qr_subtitle", "Open camera and load a device"), colors.primary) {
            launchScanner()
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        firstRow.addView(space(12, 1))
        firstRow.addView(homeActionTile(tr("my_borrows", "My Borrows"), "${home?.stats?.myBorrowedCount ?: 0} ${tr("active_items", "active items")}", colors.accent) {
            currentTab = MainTab.BORROWS
            buildMainUi()
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        grid.addView(firstRow)

        val secondRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
        }
        secondRow.addView(homeActionTile(tr("lab_status", "Lab Status"), "${home?.stats?.totalEquipment ?: 0} ${tr("total_items", "total items")}", colors.success) {
            currentHomeScreenMode = HomeScreenMode.LAB_STATUS
            buildMainUi()
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        secondRow.addView(space(12, 1))
        secondRow.addView(homeActionTile(tr("report_fault", "Report Fault"), "${home?.stats?.faultCount ?: 0} ${tr("open_faults", "open faults")}", colors.danger) {
            currentTab = MainTab.SCAN
            buildMainUi()
            setActionMessage(tr("scan_then_report_fault", "Scan an item first, then report the fault from the action panel."), colors.danger)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        grid.addView(secondRow)
        card.addView(grid)
        return card
    }

    private fun activeBorrowsHomeSection(home: CompanionHomeDto?): LinearLayout {
        val card = sectionCard(tr("my_active_borrows", "My active borrows"), colors.primary)
        val items = home?.borrowedItems.orEmpty()
        if (items.isEmpty()) {
            card.addView(messageText(tr("no_active_borrows", "No active borrows. Scan a QR code to borrow equipment from your phone.")))
            return card
        }
        items.take(3).forEach { item ->
            card.addView(card().apply {
                background = rounded(colors.surfaceAlt, dp(14), mix(colors.primary, colors.border, 0.4f), 1)
                addView(TextView(this@MainActivity).apply {
                    text = item.name.orEmpty().ifBlank { tr("borrowed_item", "Borrowed item") }
                    textSize = 17f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(colors.foreground)
                })
                addView(TextView(this@MainActivity).apply {
                    text = item.location.orEmpty().ifBlank { tr("no_location", "No location") }
                    textSize = 12.5f
                    setTextColor(colors.muted)
                    setPadding(0, dp(4), 0, 0)
                })
                addView(TextView(this@MainActivity).apply {
                    text = tr("due_prefix", "Due") + " " + item.expectedReturnDate.orEmpty().ifBlank { tr("not_set", "not set") }
                    textSize = 12.5f
                    setTextColor(deadlineColor(item.expectedReturnDate))
                    setPadding(0, dp(10), 0, dp(12))
                })
                addView(button("Open item", colors.primary) {
                    loadEquipment("LABFLOW-EQ-${item.equipmentId}", "Loaded ${item.name.orEmpty()} into the action panel.")
                })
            })
        }
        return card
    }

    private fun homeHealthSection(home: CompanionHomeDto?): LinearLayout {
        val stats = home?.stats
        val health = stats?.healthScore ?: 100
        val healthColor = when {
            health >= 70 -> colors.success
            health >= 40 -> colors.warning
            else -> colors.danger
        }
        return sectionCard("Lab health", healthColor).apply {
            val row = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            row.addView(TextView(this@MainActivity).apply {
                text = "$health%"
                textSize = 30f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(healthColor)
            })
            row.addView(space(14, 1))
            row.addView(TextView(this@MainActivity).apply {
                text = when {
                    health >= 70 -> "Healthy lab state"
                    health >= 40 -> "Needs attention"
                    else -> "Critical issues detected"
                }
                textSize = 13f
                setTextColor(colors.muted)
            })
            addView(row)

            val meta = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(14), 0, 0)
            }
            meta.addView(statusInfoChip("Items", "${stats?.totalEquipment ?: 0}", colors.primary))
            meta.addView(space(10, 1))
            meta.addView(statusInfoChip("Faults", "${stats?.faultCount ?: 0}", colors.danger))
            meta.addView(space(10, 1))
            meta.addView(statusInfoChip("Maintenance", "${stats?.maintenanceDueCount ?: 0}", colors.warning))
            addView(meta)
        }
    }

    private fun homeActionTile(title: String, subtitle: String, accentColor: Int, action: () -> Unit): LinearLayout {
        return card().apply {
            background = rounded(colors.surfaceAlt, dp(16), mix(accentColor, colors.border, 0.5f), 1)
            minimumHeight = dp(144)
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(this@MainActivity).apply {
                text = title
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(colors.foreground)
            })
            addView(TextView(this@MainActivity).apply {
                text = subtitle
                textSize = 12.5f
                setTextColor(colors.muted)
                setPadding(0, dp(6), 0, dp(12))
            })
            addView(TextView(this@MainActivity).apply {
                text = tr("open", "Open")
                textSize = 12.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(accentColor)
            })
            setOnClickListener { action() }
            addPressAnimation()
        }
    }

    private fun labStatusDetailSection(home: CompanionHomeDto?): LinearLayout {
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        wrapper.addView(sectionCard(tr("lab_status", "Lab status"), colors.success).apply {
            addView(TextView(this@MainActivity).apply {
                text = home?.lab?.name.orEmpty().ifBlank { tr("current_laboratory", "Current laboratory") }
                textSize = 22f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(colors.foreground)
            })
            addView(TextView(this@MainActivity).apply {
                text = tr("lab_status_overview", "Overview of equipment, borrow state, maintenance pressure, and recent activity.")
                textSize = 13f
                setTextColor(colors.muted)
                setPadding(0, dp(6), 0, dp(12))
            })
            addView(button(tr("back_to_home", "Back to Home"), colors.surfaceAlt) {
                currentHomeScreenMode = HomeScreenMode.DASHBOARD
                buildMainUi()
            }.apply {
                setTextColor(colors.foreground)
            })
        })
        wrapper.addView(statusOverviewGrid(home))
        wrapper.addView(equipmentStatusSection(home))
        wrapper.addView(recentActivitySection(home))
        wrapper.addView(topRiskyEquipmentSection(home))
        return wrapper
    }

    private fun statusOverviewGrid(home: CompanionHomeDto?): LinearLayout {
        val stats = home?.stats
        val card = sectionCard(tr("overview_stats", "Overview stats"), colors.primary)
        val firstRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        firstRow.addView(statTile(tr("items", "Items"), "${stats?.totalEquipment ?: 0}", colors.primary), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        firstRow.addView(space(12, 1))
        firstRow.addView(statTile(tr("borrowed", "Borrowed"), "${stats?.borrowedEquipment ?: 0}", colors.accent), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        card.addView(firstRow)

        val secondRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
        }
        secondRow.addView(statTile(tr("faults", "Faults"), "${stats?.faultCount ?: 0}", colors.danger), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        secondRow.addView(space(12, 1))
        secondRow.addView(statTile(tr("maintenance", "Maintenance"), "${stats?.maintenanceDueCount ?: 0}", colors.warning), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        card.addView(secondRow)
        return card
    }

    private fun statTile(label: String, value: String, accentColor: Int): LinearLayout {
        return card().apply {
            background = rounded(colors.surfaceAlt, dp(16), mix(accentColor, colors.border, 0.45f), 1)
            minimumHeight = dp(124)
            addView(TextView(this@MainActivity).apply {
                text = value
                textSize = 28f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(accentColor)
            })
            addView(TextView(this@MainActivity).apply {
                text = label
                textSize = 13f
                setTextColor(colors.muted)
                setPadding(0, dp(6), 0, 0)
            })
        }
    }

    private fun equipmentStatusSection(home: CompanionHomeDto?): LinearLayout {
        val counts = home?.equipmentStatusCounts.orEmpty()
        val max = counts.values.maxOrNull()?.coerceAtLeast(1) ?: 1
        return sectionCard(tr("equipment_by_status", "Equipment by status"), colors.accent).apply {
            if (counts.isEmpty()) {
                addView(messageText(tr("no_equipment_status", "No equipment status data available yet.")))
            } else {
                counts.forEach { (status, total) ->
                    addView(TextView(this@MainActivity).apply {
                        text = "${status.replace('_', ' ')}  Ä‚ËĂ˘â€šÂ¬Ă‹Â  $total"
                        textSize = 12.5f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(colors.foreground)
                        setPadding(0, dp(4), 0, dp(6))
                    })
                    addView(statusBar(status, total, max))
                    addView(space(1, 10))
                }
            }
        }
    }

    private fun statusBar(status: String, total: Int, max: Int): View {
        val color = when (status.uppercase()) {
            "AVAILABLE" -> colors.success
            "BORROWED" -> colors.accent
            "DEFECT" -> colors.danger
            "MAINTENANCE" -> colors.warning
            else -> colors.primary
        }
        val ratio = (total.toFloat() / max.toFloat()).coerceIn(0.08f, 1f)
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = rounded(colors.surfaceAlt, dp(9), colors.border, 0)
            minimumHeight = dp(12)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(12)
            )
            addView(View(this@MainActivity).apply {
                background = rounded(color, dp(9), color, 0)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, ratio))
            addView(View(this@MainActivity), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f - ratio))
        }
    }

    private fun recentActivitySection(home: CompanionHomeDto?): LinearLayout {
        val items = home?.recentActivity.orEmpty()
        return sectionCard(tr("recent_activity", "Recent activity"), colors.primary).apply {
            if (items.isEmpty()) {
                addView(messageText(tr("no_recent_activity", "No recent activity found for this lab.")))
            } else {
                items.forEach { activity ->
                    addView(card().apply {
                        background = rounded(colors.surfaceAlt, dp(14), colors.border, 1)
                        addView(TextView(this@MainActivity).apply {
                            text = activity.description.orEmpty().ifBlank { activity.action.orEmpty().replace('_', ' ') }
                            textSize = 14f
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(colors.foreground)
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = "${activity.username.orEmpty().ifBlank { "LabFlow" }} Ä‚ËĂ˘â€šÂ¬Ă‹Â ${compactDateTime(activity.timestamp)}"
                            textSize = 12f
                            setTextColor(colors.muted)
                            setPadding(0, dp(6), 0, 0)
                        })
                    })
                }
            }
        }
    }

    private fun topRiskyEquipmentSection(home: CompanionHomeDto?): LinearLayout {
        val items = home?.topRiskyEquipment.orEmpty()
        return sectionCard(tr("top_risky_equipment", "Top risky equipment"), colors.danger).apply {
            if (items.isEmpty()) {
                addView(messageText(tr("no_risky_equipment", "No risky equipment surfaced for this lab.")))
            } else {
                items.forEach { risk ->
                    addView(card().apply {
                        background = rounded(colors.surfaceAlt, dp(14), mix(colors.danger, colors.border, 0.45f), 1)
                        addView(TextView(this@MainActivity).apply {
                            text = risk.equipmentName.orEmpty().ifBlank { "Equipment item" }
                            textSize = 16f
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(colors.foreground)
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = "Risk ${risk.score}% Ä‚ËĂ˘â€šÂ¬Ă‹Â ${risk.level.orEmpty().ifBlank { "UNKNOWN" }}"
                            textSize = 12.5f
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(colors.danger)
                            setPadding(0, dp(6), 0, 0)
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = risk.reasons?.firstOrNull().orEmpty().ifBlank { "No recent risk reason available." }
                            textSize = 12f
                            setTextColor(colors.muted)
                            setPadding(0, dp(8), 0, 0)
                        })
                    })
                }
            }
        }
    }

    private fun labStatusSection(home: CompanionHomeDto?): LinearLayout {
        val lab = home?.lab
        val card = sectionCard(tr("home_status", "Home status"), colors.danger)
        card.addView(TextView(this).apply {
            text = lab?.name ?: tr("loading_laboratory", "Loading laboratory...")
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(colors.foreground)
        })
        card.addView(TextView(this).apply {
            text = if (lab == null) {
                "Connecting to LabFlow Desktop..."
            } else {
                "${lab.role.orEmpty().ifBlank { settings.role.ifBlank { "Member" } }} | Invite ${lab.inviteCode.orEmpty()}"
            }
            textSize = 12.5f
            setTextColor(colors.muted)
            setPadding(0, dp(4), 0, dp(12))
        })

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(statusInfoChip(tr("theme", "Theme"), if (settings.modeEnum() == CompanionMode.DARK) tr("dark", "Dark") else tr("light", "Light"), colors.primary))
        row.addView(space(10, 1))
        row.addView(statusInfoChip(tr("palette", "Palette"), settings.paletteEnum().displayName, colors.accent))
        row.addView(space(10, 1))
        row.addView(button(tr("switch_lab", "Switch lab"), colors.danger, 0f) {
            showLabPicker(home?.labs.orEmpty())
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        })
        card.addView(row)
        return card
    }

    private fun favoriteActionsSection(home: CompanionHomeDto?): LinearLayout {
        val stats = home?.stats
        val card = sectionCard(tr("quick_actions", "Quick actions"), colors.warning)

        val metricsScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(Color.TRANSPARENT)
        }
        val chips = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        chips.addView(metricChip(tr("total", "Total"), "${stats?.totalEquipment ?: 0}", colors.primary))
        chips.addView(space(8, 1))
        chips.addView(metricChip(tr("borrowed", "Borrowed"), "${stats?.borrowedEquipment ?: 0}", colors.warning))
        chips.addView(space(8, 1))
        chips.addView(metricChip(tr("available", "Available"), "${stats?.availableEquipment ?: 0}", colors.success))
        chips.addView(space(8, 1))
        chips.addView(metricChip(tr("containers", "Containers"), "${stats?.containerCount ?: 0}", colors.accent))
        metricsScroll.addView(chips)
        card.addView(metricsScroll)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
        }
        row.addView(button(tr("scan_qr", "Scan QR"), colors.primary, 1f) {
            launchScanner()
        })
        row.addView(space(10, 1))
        row.addView(button(tr("refresh", "Refresh"), colors.accent, 1f) {
            loadCompanionHome()
            currentEquipment?.qrCode?.let { loadEquipment(it) }
        })
        card.addView(row)

        val modeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
        }
        modeRow.addView(filterChip(tr("borrowed", "Borrowed"), currentHomeMode == HomeMode.BORROWED) {
            currentHomeMode = HomeMode.BORROWED
            buildMainUi()
        })
        modeRow.addView(space(8, 1))
        modeRow.addView(filterChip(tr("containers", "Containers"), currentHomeMode == HomeMode.CONTAINERS) {
            currentHomeMode = HomeMode.CONTAINERS
            buildMainUi()
        })
        modeRow.addView(space(8, 1))
        modeRow.addView(filterChip(tr("both", "Both"), currentHomeMode == HomeMode.BOTH) {
            currentHomeMode = HomeMode.BOTH
            buildMainUi()
        })
        card.addView(modeRow)
        return card
    }

    private fun blueContentSection(home: CompanionHomeDto?): LinearLayout {
        val card = sectionCard(tr("borrowed_objects_containers", "Borrowed Objects & Containers"), colors.accent)
        val borrowedItems = home?.borrowedItems.orEmpty()
        val containers = home?.containers.orEmpty()
        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, 0)
        }

        val itemsToRender = when (currentHomeMode) {
            HomeMode.BORROWED -> borrowedItems.map { itemTile(it) }
            HomeMode.CONTAINERS -> containers.map { containerTile(it) }
            HomeMode.BOTH -> borrowedItems.map { itemTile(it) } + containers.map { containerTile(it) }
        }

        if (itemsToRender.isEmpty()) {
            grid.addView(messageText(tr("nothing_to_show_yet", "Nothing to show yet. Borrow equipment or organize items into containers on desktop.")))
        } else {
            itemsToRender.chunked(2).forEach { rowItems ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 0, 0, dp(10))
                }
                rowItems.forEachIndexed { index, view ->
                    row.addView(view, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                    if (index < rowItems.lastIndex) {
                        row.addView(space(10, 1))
                    }
                }
                if (rowItems.size == 1) {
                    row.addView(View(this@MainActivity), LinearLayout.LayoutParams(0, 0, 1f))
                }
                grid.addView(row)
            }
        }
        card.addView(grid)
        return card
    }

    private fun scannedEquipmentSection(): LinearLayout {
        val card = card()
        card.addView(sectionTitle(tr("scanned_item", "Scanned item")))
        statusPill = pill(tr("no_equipment_scanned", "No equipment scanned"))
        actionBanner = actionCallout(tr("scan_ready", "Ready. Scan a LabFlow QR code, then choose the highlighted action."))
        card.addView(statusPill)
        card.addView(actionBanner)
        refreshButton = button(tr("refresh_scanned_item", "Refresh scanned item"), colors.accent) {
            currentEquipment?.qrCode?.let { loadEquipment(it) } ?: message("Scan an equipment item first.")
        }
        borrowButton = button(tr("borrow_on_my_account", "Borrow on my account"), colors.primary) {
            currentEquipment?.let { borrowEquipment(it) } ?: message("Scan an equipment item first.")
        }
        returnButton = button(tr("return_borrowed_item", "Return borrowed item"), colors.accent) {
            currentEquipment?.let { returnEquipment(it) } ?: message("Scan an equipment item first.")
        }
        faultButton = button("Report fault", colors.danger) {
            currentEquipment?.let { showFaultDialog(it) } ?: message("Scan an equipment item first.")
        }
        resultText = messageText(tr("scan_qr_helper", "Scan a LabFlow QR code to see details and borrow directly from your phone."))
        card.addView(refreshButton)
        card.addView(borrowButton)
        card.addView(returnButton)
        card.addView(faultButton)
        card.addView(resultText)
        return card
    }

    private fun scanHubSection(): LinearLayout {
        val card = sectionCard(tr("scan_equipment", "Scan equipment"), colors.primary)
        card.addView(TextView(this).apply {
            text = tr("scan_equipment_desc", "Scan a LabFlow QR code to load an item instantly, then borrow, return, or report a fault.")
            textSize = 13f
            setTextColor(colors.muted)
        })
        card.addView(button(tr("open_qr_scanner", "Open QR scanner"), colors.primary) {
            launchScanner()
        }.apply {
            minimumHeight = dp(52)
        })
        card.addView(button(tr("refresh_current_lab", "Refresh current lab"), colors.accent) {
            loadCompanionHome()
        }.apply {
            minimumHeight = dp(48)
        })
        return card
    }

    private fun borrowsSection(home: CompanionHomeDto?): LinearLayout {
        val card = sectionCard(tr("my_borrows", "My borrows"), colors.accent)
        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, dp(12))
        }
        tabs.addView(filterChip(tr("active", "Active"), currentBorrowTab == BorrowTab.ACTIVE) {
            currentBorrowTab = BorrowTab.ACTIVE
            buildMainUi()
        })
        tabs.addView(space(8, 1))
        tabs.addView(filterChip(tr("history", "History"), currentBorrowTab == BorrowTab.HISTORY) {
            currentBorrowTab = BorrowTab.HISTORY
            buildMainUi()
        })
        card.addView(tabs)

        if (currentBorrowTab == BorrowTab.ACTIVE) {
            val borrowedItems = home?.borrowedItems.orEmpty()
            if (borrowedItems.isEmpty()) {
                card.addView(messageText(tr("no_active_borrows", "No active borrows right now. Scan a QR code to borrow equipment from your phone.")))
                return card
            }
            borrowedItems.forEach { item ->
                card.addView(activeBorrowCard(item))
            }
        } else {
            val historyItems = home?.borrowHistory.orEmpty()
            if (historyItems.isEmpty()) {
                card.addView(messageText(tr("no_borrow_history", "No borrow history yet. Your returned items will appear here.")))
                return card
            }
            historyItems.forEach { item ->
                card.addView(historyBorrowCard(item))
            }
        }
        return card
    }

    private fun profileSection(home: CompanionHomeDto?): LinearLayout {
        val card = sectionCard(tr("profile_settings", "Profile & settings"), colors.accentStrong)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(4), 0, dp(10))
        }
        header.addView(ImageView(this).apply {
            setImageBitmap(generateAvatar(settings.fullName.ifBlank { settings.username.ifBlank { "LF" } }, 80))
            layoutParams = LinearLayout.LayoutParams(dp(80), dp(80)).apply { bottomMargin = dp(14) }
        })
        header.addView(TextView(this).apply {
            text = settings.fullName.ifBlank { settings.username.ifBlank { tr("labflow_user", "LabFlow user") } }
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(colors.foreground)
            gravity = Gravity.CENTER
        })
        header.addView(TextView(this).apply {
            text = "${settings.role.ifBlank { tr("member", "Member") }} | ${home?.lab?.name.orEmpty().ifBlank { tr("no_active_lab", "No active lab") }}"
            textSize = 13f
            setTextColor(colors.muted)
            setPadding(0, dp(4), 0, 0)
            gravity = Gravity.CENTER
        })
        card.addView(header)

        card.addView(sectionTitle(tr("my_stats", "My stats")))
        card.addView(profileStatsGrid(home))

        card.addView(sectionTitle(tr("settings", "Settings")))
        card.addView(settingsRow(tr("appearance", "Appearance"), "${if (settings.modeEnum() == CompanionMode.DARK) tr("dark", "Dark") else tr("light", "Light")} | ${settings.paletteEnum().displayName}") {
            showAppearanceDialog()
        })
        card.addView(settingsRow(tr("language", "Language"), currentLanguageLabel()) {
            showLanguageDialog()
        })
        card.addView(settingsRow(tr("laboratory", "Laboratory"), home?.lab?.name.orEmpty().ifBlank { tr("choose_active_lab", "Choose active lab") }) {
            showLabPicker(home?.labs.orEmpty())
        })
        card.addView(settingsRow(tr("server_api", "Server & API"), "${settings.host.ifBlank { tr("pc_ip_not_set", "PC IP not set") }}:${settings.port.ifBlank { "8080" }}") {
            showConnectionDialog()
        })
        card.addView(settingsRow(tr("notifications", "Notifications"), if (settings.notificationsEnabled) tr("enabled", "Enabled") else tr("disabled", "Disabled")) {
            toggleNotifications()
        })
        card.addView(settingsRow(tr("about_labflow", "About LabFlow"), tr("about_subtitle", "Desktop companion over local Wi-Fi")) {
            showAboutDialog()
        })

        card.addView(button(tr("open_notifications", "Open notifications"), colors.success) {
            showNotificationsDialog(home?.notifications.orEmpty())
        })
        card.addView(button(tr("sign_out", "Sign out"), colors.danger) {
            LabFlowNotificationService.cancel(this@MainActivity)
            settings.clearUser()
            settings.currentLabId = 0
            currentHome = null
            currentEquipment = null
            currentTab = MainTab.HOME
            buildLoginUi()
        })
        return card
    }

    private fun activeBorrowCard(item: CompanionBorrowedItemDto): LinearLayout {
        return card().apply {
            background = rounded(colors.surfaceAlt, dp(14), mix(colors.accent, colors.border, 0.45f), 1)
            addView(TextView(this@MainActivity).apply {
                text = item.name.orEmpty().ifBlank { tr("borrowed_item", "Borrowed item") }
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(colors.foreground)
            })
            addView(TextView(this@MainActivity).apply {
                text = item.location.orEmpty().ifBlank { tr("no_location", "No location") }
                textSize = 12.5f
                setTextColor(colors.muted)
                setPadding(0, dp(4), 0, 0)
            })
            addView(TextView(this@MainActivity).apply {
                text = "${tr("borrowed_prefix", "Borrowed")} Ă˘â‚¬Ë ${tr("due_prefix", "Due")} ${item.expectedReturnDate.orEmpty().ifBlank { tr("not_set", "not set") }}"
                textSize = 12.5f
                setTextColor(deadlineColor(item.expectedReturnDate))
                setPadding(0, dp(10), 0, dp(8))
            })
            addView(deadlineProgress(item.expectedReturnDate))

            val row = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(12), 0, 0)
            }
            row.addView(button(tr("return_equipment", "Return equipment"), colors.primary, 1f) {
                returnBorrowedItem(item)
            })
            row.addView(space(10, 1))
            row.addView(button(tr("load_item", "Load item"), colors.accent, 1f) {
                loadEquipment("LABFLOW-EQ-${item.equipmentId}", tr("loaded_item_message", "Loaded ${item.name.orEmpty()} into the action panel."))
            })
            addView(row)
        }
    }

    private fun historyBorrowCard(item: CompanionBorrowHistoryItemDto): LinearLayout {
        val label = historyStatusLabel(item)
        val color = historyStatusColor(item)
        return card().apply {
            background = rounded(colors.surfaceAlt, dp(14), mix(color, colors.border, 0.4f), 1)
            addView(TextView(this@MainActivity).apply {
                text = item.name.orEmpty().ifBlank { tr("borrow_history_item", "Borrow history item") }
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(colors.foreground)
            })
            addView(TextView(this@MainActivity).apply {
                text = item.location.orEmpty().ifBlank { tr("no_location", "No location") }
                textSize = 12f
                setTextColor(colors.muted)
                setPadding(0, dp(4), 0, 0)
            })
            addView(TextView(this@MainActivity).apply {
                text = "${tr("borrowed_prefix", "Borrowed")} ${compactDate(item.borrowDate)} Ă˘â‚¬Ë ${tr("returned_prefix", "Returned")} ${compactDate(item.actualReturnDate)}"
                textSize = 12f
                setTextColor(colors.muted)
                setPadding(0, dp(8), 0, 0)
            })
            addView(TextView(this@MainActivity).apply {
                text = label
                textSize = 12.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(color)
                setPadding(0, dp(8), 0, 0)
            })
        }
    }

    private fun deadlineProgress(expectedReturnDate: String?): View {
        val color = deadlineColor(expectedReturnDate)
        val ratio = deadlineProgressRatio(expectedReturnDate)
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = rounded(colors.surface, dp(8), colors.border, 0)
            minimumHeight = dp(8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(8)
            )
            addView(View(this@MainActivity).apply {
                background = rounded(color, dp(8), color, 0)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, ratio))
            addView(View(this@MainActivity), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f - ratio))
        }
    }

    private fun loadCompanionHome(labId: Int = settings.currentLabId) {
        if (!settings.isLoggedIn) {
            return
        }
        if (!isNetworkConnected) {
            isLoadingHome = false
            homeLoadError = tr("no_wifi_connection", "No WiFi connection. Connect to the same network as your PC.")
            if (currentHome == null) {
                buildMainUi()
            }
            return
        }
        isLoadingHome = true
        homeLoadError = null
        if (currentHome == null) {
            buildMainUi()
        }
        lifecycleScope.launch {
            try {
                val response = api().home(settings.authorization(), settings.userId, if (labId > 0) labId else 0)
                val body = response.body()
                if (response.code() == 401) {
                    isLoadingHome = false
                    homeLoadError = tr("invalid_local_api_key", "Invalid local API key. Check the desktop key and the companion app settings.")
                    message(homeLoadError ?: tr("invalid_local_api_key", "Invalid local API key. Check the desktop key and the companion app settings."))
                    if (currentHome == null) buildMainUi()
                    return@launch
                }
                if (response.code() == 404) {
                    isLoadingHome = false
                    homeLoadError = tr("companion_endpoint_missing", "Companion endpoint is missing. Restart LabFlow Desktop so the mobile API updates are loaded.")
                    message(homeLoadError ?: tr("companion_endpoint_missing", "Companion endpoint is missing. Restart LabFlow Desktop so the mobile API updates are loaded."))
                    if (currentHome == null) buildMainUi()
                    return@launch
                }
                if (!response.isSuccessful || body?.success != true || body.data == null) {
                    isLoadingHome = false
                    homeLoadError = body?.error ?: tr("could_not_load_companion_home", "Could not load lab companion home.")
                    message(homeLoadError ?: tr("could_not_load_companion_home", "Could not load lab companion home."))
                    if (currentHome == null) buildMainUi()
                    return@launch
                }
                isLoadingHome = false
                homeLoadError = null
                currentHome = body.data
                val resolvedLabId = body.data.lab?.id ?: 0
                if (resolvedLabId > 0) {
                    settings.currentLabId = resolvedLabId
                }
                buildMainUi()
            } catch (e: Exception) {
                isLoadingHome = false
                homeLoadError = userFriendlyError(e)
                message(homeLoadError ?: userFriendlyError(e))
                if (currentHome == null) {
                    buildMainUi()
                } else {
                    buildMainUi()
                }
            }
        }
    }

    private fun login() {
        val username = usernameInput.text?.toString().orEmpty().trim()
        val password = passwordInput.text?.toString().orEmpty()
        clearFieldError(usernameInput)
        clearFieldError(passwordInput)

        if (username.isBlank()) {
            setFieldError(usernameInput, tr("username_required", "Username is required."))
            scrollToFocusedField(usernameInput)
            return
        }
        if (password.isBlank()) {
            setFieldError(passwordInput, tr("password_required", "Password is required."))
            scrollToFocusedField(passwordInput)
            return
        }

        setLoginLoading(true)
        lifecycleScope.launch {
            try {
                val response = api().login(LoginRequest(username, password))
                val body = response.body()
                if (!response.isSuccessful || body?.success != true || body.data == null) {
                    setFieldError(passwordInput, body?.error ?: tr("invalid_username_password", "Invalid username or password."))
                    return@launch
                }
                settings.saveUser(body.data)
                settings.currentLabId = 0
                currentHome = null
                ensureNotificationPermission()
                if (settings.notificationsEnabled) {
                    LabFlowNotificationService.schedule(this@MainActivity)
                } else {
                    LabFlowNotificationService.cancel(this@MainActivity)
                }
                buildMainUi()
                setActionMessage("${tr("signed_in_as", "Signed in as")} ${settings.username}. ${tr("loading_companion_home", "Loading your lab companion home...")}")
            } catch (e: Exception) {
                message(userFriendlyError(e))
            } finally {
                if (!settings.isLoggedIn) {
                    setLoginLoading(false)
                }
            }
        }
    }

    private fun saveSettings() {
        if (::hostInput.isInitialized) {
            settings.host = hostInput.text?.toString().orEmpty().trim()
            settings.port = portInput.text?.toString().orEmpty().trim().ifBlank { "8080" }
            settings.apiKey = apiKeyInput.text?.toString().orEmpty().trim().ifBlank { "LABFLOW_LOCAL_API_KEY" }
        }
    }

    private fun api(): LabFlowApi = ApiClient.create(settings.host, settings.port)

    private fun testConnection() {
        lifecycleScope.launch {
            try {
                val response = api().health()
                if (response.isSuccessful && response.body()?.success == true) {
                    message(tr("connection_ok", "Connection OK. LabFlow Desktop is reachable."))
                } else {
                    message(tr("desktop_not_available", "LabFlow Desktop is not available."))
                }
            } catch (e: Exception) {
                message(userFriendlyError(e))
            }
        }
    }

    private fun loadEquipment(qrCode: String, notice: String? = null, presentSheet: Boolean = false) {
        if (!settings.isLoggedIn) {
            message(tr("login_before_scan", "Log in before scanning equipment."))
            return
        }
        if (!qrCode.startsWith("LABFLOW-EQ-")) {
            message(tr("invalid_qr_code", "Invalid LabFlow QR code."))
            return
        }
        lifecycleScope.launch {
            try {
                val response = api().equipmentByQr(settings.authorization(), qrCode)
                if (response.code() == 401) {
                    message(tr("invalid_api_key", "Invalid API key."))
                    return@launch
                }
                val body = response.body()
                if (!response.isSuccessful || body?.success != true || body.data == null) {
                    message(body?.error ?: tr("equipment_not_found", "Equipment was not found."))
                    return@launch
                }
                currentEquipment = body.data
                showEquipment(body.data, notice)
                window.decorView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                if (presentSheet) {
                    showEquipmentBottomSheet(body.data)
                }
            } catch (e: Exception) {
                message(userFriendlyError(e))
            }
        }
    }

    private fun showEquipment(equipment: EquipmentDto, notice: String? = null) {
        statusPill.text = scannerStatusText(equipment)
        statusPill.setTextColor(statusColor(equipment.status))
        setActionMessage(notice ?: "Scanned ${equipment.name.orEmpty().ifBlank { "equipment item" }}. Available actions are highlighted below.")
        resultText.text = """
            ${equipment.name.orEmpty()}
            ${equipment.category.orEmpty().ifBlank { tr("not_available", "-") }} | ${equipment.location.orEmpty().ifBlank { tr("not_available", "-") }}

            ${tr("status", "Status")}: ${equipment.status.orEmpty().ifBlank { tr("not_available", "-") }}
            ${tr("manufacturer", "Manufacturer")}: ${equipment.manufacturer.orEmpty().ifBlank { tr("not_available", "-") }}
            ${tr("model", "Model")}: ${equipment.model.orEmpty().ifBlank { tr("not_available", "-") }}
            ${tr("serial", "Serial")}: ${equipment.serialNumber.orEmpty().ifBlank { tr("not_available", "-") }}
            QR: ${equipment.qrCode.orEmpty()}

            ${tr("description", "Description")}:
            ${equipment.description.orEmpty().ifBlank { tr("no_description", "No description.") }}

            ${tr("notes", "Notes")}:
            ${equipment.notes.orEmpty().ifBlank { tr("no_notes", "No notes.") }}
        """.trimIndent()
        updateActionState()
    }

    private fun showEquipmentBottomSheet(equipment: EquipmentDto) {
        EquipmentBottomSheet(
            colors = colors,
            equipment = equipment,
            currentUserId = settings.userId,
            onBorrow = { borrowEquipment(it) },
            onReturn = { returnEquipment(it) },
            onReportFault = { showFaultDialog(it) },
            onViewFullDetails = {
                currentTab = MainTab.SCAN
                buildMainUi()
                showEquipment(it, tr("loaded_item_message", "Loaded ${it.name.orEmpty().ifBlank { "equipment item" }} into the action panel."))
            }
        ).show(supportFragmentManager, "equipment_sheet")
    }

    private fun launchScanner() {
        saveSettings()
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this)
        scannerLauncher.launch(Intent(this, ScannerActivity::class.java), options)
    }

    private fun borrowEquipment(equipment: EquipmentDto) {
        if (!equipment.status.equals("AVAILABLE", ignoreCase = true)) {
            message(tr("only_available_can_borrow", "Only AVAILABLE equipment can be borrowed."))
            return
        }
        lifecycleScope.launch {
            try {
                val dueDate = LocalDate.now().plusDays(7).toString()
                val request = BorrowRequest(settings.userId, dueDate, "Borrowed from LabFlow Companion")
                val response = api().borrow(settings.authorization(), equipment.id, request)
                val body = response.body()
                if (response.code() == 401) {
                    message(tr("invalid_api_key", "Invalid API key."))
                    return@launch
                }
                if (response.isSuccessful && body?.success == true) {
                    val receipt = "Borrow complete: ${equipment.name.orEmpty()} is now borrowed by ${settings.username}. Due back: $dueDate."
                    Toast.makeText(this@MainActivity, tr("borrow_complete", "Borrow complete"), Toast.LENGTH_LONG).show()
                    loadCompanionHome(settings.currentLabId)
                    equipment.qrCode?.let { loadEquipment(it, receipt) } ?: setActionMessage(receipt, colors.success)
                } else {
                    message(body?.error ?: tr("could_not_borrow_equipment", "Could not borrow this equipment."))
                }
            } catch (e: Exception) {
                message(userFriendlyError(e))
            }
        }
    }

    private fun returnEquipment(equipment: EquipmentDto) {
        val borrowId = equipment.activeBorrowRecordId
        if (borrowId == null) {
            message(tr("no_active_borrow_record", "No active borrow record found for this item."))
            return
        }
        if (!currentUserOwnsActiveBorrow(equipment)) {
            message(tr("borrow_belongs_other_user", "This borrowed item belongs to another user and cannot be returned from your companion account."))
            return
        }
        lifecycleScope.launch {
            try {
                val request = ReturnRequest(borrowId, "GOOD", "Returned from LabFlow Companion", null)
                val response = api().returnEquipment(settings.authorization(), equipment.id, request)
                val body = response.body()
                if (response.code() == 401) {
                    message(tr("invalid_api_key", "Invalid API key."))
                    return@launch
                }
                if (response.isSuccessful && body?.success == true) {
                    val receipt = "Return complete: ${equipment.name.orEmpty()} was returned successfully and is ready for desktop refresh."
                    Toast.makeText(this@MainActivity, tr("return_complete", "Return complete"), Toast.LENGTH_LONG).show()
                    loadCompanionHome(settings.currentLabId)
                    equipment.qrCode?.let { loadEquipment(it, receipt) } ?: setActionMessage(receipt, colors.success)
                } else {
                    message(body?.error ?: tr("could_not_return_equipment", "Could not return this equipment."))
                }
            } catch (e: Exception) {
                message(userFriendlyError(e))
            }
        }
    }

    private fun showFaultDialog(equipment: EquipmentDto) {
        val description = input(tr("describe_fault", "Describe the fault"), "", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE)
        description.minLines = 3
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), 0)
            addView(description)
        }
        AlertDialog.Builder(this)
            .setTitle(tr("report_fault", "Report fault"))
            .setView(container)
            .setPositiveButton(tr("send", "Send")) { _, _ ->
                reportFault(equipment.id, description.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun reportFault(equipmentId: Int, description: String) {
        if (description.isBlank()) {
            message(tr("fault_description_required", "Fault description is required."))
            return
        }
        lifecycleScope.launch {
            try {
                val request = FaultReportRequest(settings.userId, description, "MAJOR")
                val response = api().faultReport(settings.authorization(), equipmentId, request)
                val body = response.body()
                if (response.code() == 401) {
                    message(tr("invalid_api_key", "Invalid API key."))
                    return@launch
                }
                if (response.isSuccessful && body?.success == true) {
                    val receipt = "Fault reported: the item was marked DEFECT and desktop views will refresh."
                    Toast.makeText(this@MainActivity, tr("fault_reported", "Fault reported"), Toast.LENGTH_LONG).show()
                    loadCompanionHome(settings.currentLabId)
                    currentEquipment?.qrCode?.let { loadEquipment(it, receipt) } ?: setActionMessage(receipt, colors.danger)
                } else {
                    message(body?.error ?: tr("could_not_report_fault", "Could not report the fault."))
                }
            } catch (e: Exception) {
                message(userFriendlyError(e))
            }
        }
    }

    private fun updateActionState() {
        if (!::borrowButton.isInitialized) {
            return
        }
        val equipment = currentEquipment
        val hasEquipment = equipment != null
        val canBorrow = equipment?.status.equals("AVAILABLE", ignoreCase = true)
        val canReturn = currentUserOwnsActiveBorrow(equipment)
        refreshButton.text = if (hasEquipment) tr("refresh_this_item", "Refresh this item") else tr("refresh_unavailable", "Refresh unavailable")
        borrowButton.text = when {
            canBorrow -> tr("borrow_this_item", "Borrow this item")
            equipment?.status.equals("BORROWED", ignoreCase = true) && canReturn -> tr("borrow_unavailable", "Borrow unavailable")
            hasEquipment -> tr("borrow_unavailable", "Borrow unavailable")
            else -> tr("scan_to_borrow", "Scan to borrow")
        }
        returnButton.text = when {
            canReturn -> tr("return_this_item", "Return this item")
            hasEquipment -> tr("no_active_borrow_to_return", "No active borrow to return")
            else -> tr("scan_to_return", "Scan to return")
        }
        faultButton.text = if (hasEquipment) tr("report_fault_this_item", "Report fault for this item") else tr("scan_to_report_fault", "Scan to report fault")
        styleActionButton(refreshButton, hasEquipment, colors.accent)
        styleActionButton(faultButton, hasEquipment, colors.danger)
        styleActionButton(borrowButton, canBorrow, colors.primary)
        styleActionButton(returnButton, canReturn, colors.accent)
    }

    private fun currentUserOwnsActiveBorrow(equipment: EquipmentDto?): Boolean {
        return equipment?.activeBorrowRecordId != null && equipment.activeBorrowUserId == settings.userId
    }

    private fun scannerStatusText(equipment: EquipmentDto): String {
        val parts = mutableListOf<String>()
            parts += equipment.status.orEmpty().ifBlank { tr("unknown", "UNKNOWN") }
        equipment.location?.takeIf { it.isNotBlank() }?.let(parts::add)
        if (equipment.status.equals("BORROWED", ignoreCase = true)) {
            parts += if (currentUserOwnsActiveBorrow(equipment)) tr("borrowed_by_you", "Borrowed by you") else "${tr("borrowed_by", "Borrowed by")} ${equipment.activeBorrowUsername.orEmpty().ifBlank { tr("another_user", "another user") }}"
        }
        return parts.joinToString(" | ")
    }

    private fun message(value: String) {
        if (::actionBanner.isInitialized && settings.isLoggedIn) {
            setActionMessage(value)
            return
        }
        if (::resultText.isInitialized) {
            resultText.setTextColor(colors.muted)
            resultText.text = value
        }
    }

    private fun setActionMessage(value: String, color: Int = colors.accent) {
        if (!::actionBanner.isInitialized) {
            return
        }
        actionBanner.text = value
        actionBanner.setTextColor(if (color == colors.danger) Color.rgb(255, 224, 230) else colors.foreground)
        actionBanner.background = rounded(
            Color.argb(72, Color.red(color), Color.green(color), Color.blue(color)),
            dp(10),
            color,
            1
        )
    }

    private fun userFriendlyError(error: Exception): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("IP", ignoreCase = true) || message.contains("Port", ignoreCase = true) -> message
            message.contains("CLEARTEXT", ignoreCase = true) ->
                tr("http_blocked", "HTTP traffic is blocked on this device. Keep cleartext enabled for the companion app.")
            message.contains("401") || message.contains("Unauthorized", ignoreCase = true) ->
                tr("invalid_local_api_key_sync", "Invalid local API key. Make sure the phone uses the same LabFlow API key as desktop.")
            message.contains("404") ->
                tr("desktop_restart_needed", "This LabFlow Desktop build does not expose the companion endpoint yet. Restart the desktop app.")
            error is UnknownHostException ->
                tr("host_not_resolved", "The PC host could not be resolved. Check the PC IP and make sure both devices are on the same Wi-Fi.")
            error is SocketTimeoutException ->
                tr("desktop_timeout", "LabFlow Desktop did not answer in time at ${settings.host}:${settings.port}. Check the PC IP and that the app is open.")
            error is ConnectException ->
                tr("desktop_connection_refused", "Connection was refused by LabFlow Desktop at ${settings.host}:${settings.port}. Check that the desktop app is running and the firewall allows it.")
            error is IOException || message.contains("Failed to connect", ignoreCase = true) || message.contains("Connection refused", ignoreCase = true) ->
                tr("desktop_unreachable", "Could not reach LabFlow Desktop at ${settings.host}:${settings.port}. Check the PC IP, firewall, and that both devices are on the same Wi-Fi.")
            else -> tr("desktop_not_available_full", "LabFlow Desktop is not available. Check the PC IP, API key, and that both devices are on the same Wi-Fi.")
        }
    }

    private fun setScrollableContent(root: LinearLayout, bottomNavigationView: View? = null, enablePullRefresh: Boolean = false) {
        val frame = FrameLayout(this)
        frame.setBackgroundColor(colors.background)
        frame.addView(
            StarFieldView(this, colors),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            isFillViewport = true
            addView(root)
        }
        rootScrollView = scroll
        attachKeyboardAwareScroll(scroll)
        val contentView: View = if (enablePullRefresh) {
            SwipeRefreshLayout(this).apply {
                setColorSchemeColors(colors.primary, colors.success)
                setProgressBackgroundColorSchemeColor(colors.surface)
                setOnRefreshListener {
                    loadCompanionHome(settings.currentLabId)
                    isRefreshing = false
                }
                addView(scroll)
            }
        } else {
            scroll
        }
        frame.addView(
            contentView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        if (bottomNavigationView != null) {
            frame.addView(
                bottomNavigationView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
                )
            )
        }
        setContentView(frame)
    }

    private fun skeletonSection(): LinearLayout {
        return sectionCard("Loading", colors.primary).apply {
            addView(SkeletonView(this@MainActivity, colors).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(82)
                ).apply { bottomMargin = dp(12) }
            })
            addView(SkeletonView(this@MainActivity, colors).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(128)
                ).apply { bottomMargin = dp(12) }
            })
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(SkeletonView(this@MainActivity, colors).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(144), 1f).apply { marginEnd = dp(6) }
                })
                addView(SkeletonView(this@MainActivity, colors).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(144), 1f).apply { marginStart = dp(6) }
                })
            })
            addView(SkeletonView(this@MainActivity, colors).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(184)
                ).apply { topMargin = dp(12) }
            })
        }
    }

    private fun offlineBannerText(): String? {
        if (!settings.isLoggedIn) {
            return null
        }
        if (!isNetworkConnected) {
            return tr("no_wifi_connection", "No WiFi connection. Connect to the same network as your PC.")
        }
        val error = homeLoadError ?: return null
        return when {
            error.contains("API key", ignoreCase = true) -> error
            error.contains("endpoint", ignoreCase = true) -> error
            error.contains("Desktop", ignoreCase = true) || error.contains("same Wi-Fi", ignoreCase = true) ->
                tr("desktop_not_reachable", "Desktop app not reachable. Make sure LabFlow is running on your PC.")
            else -> null
        }
    }

    private fun offlineBanner(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            gravity = Gravity.CENTER
            textSize = 12f
            setTextColor(colors.foreground)
            background = rounded(colors.warning, dp(12), mix(colors.warning, Color.WHITE, 0.18f), 0)
            setPadding(dp(12), dp(9), dp(12), dp(9))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        }
    }

    private fun companionErrorStateSection(): LinearLayout {
        val title: String
        val subtitle: String
        val actionLabel: String
        val action: () -> Unit

        when {
            !isNetworkConnected -> {
                title = tr("no_wifi_connection_title", "No WiFi connection")
                subtitle = tr("no_wifi_connection_subtitle", "Connect your phone to the same network as your PC, then try again.")
                actionLabel = tr("retry", "Retry")
                action = { loadCompanionHome(settings.currentLabId) }
            }
            homeLoadError?.contains("API key", ignoreCase = true) == true -> {
                title = tr("authentication_problem", "Authentication problem")
                subtitle = tr("authentication_problem_subtitle", "The companion app could not authenticate with LabFlow Desktop. Check the local API key.")
                actionLabel = tr("server_api", "Server & API")
                action = { showConnectionDialog() }
            }
            homeLoadError?.contains("endpoint", ignoreCase = true) == true -> {
                title = tr("desktop_update_needed", "Desktop update needed")
                subtitle = tr("desktop_update_needed_subtitle", "Restart LabFlow Desktop so the mobile companion endpoint is available.")
                actionLabel = tr("retry", "Retry")
                action = { loadCompanionHome(settings.currentLabId) }
            }
            homeLoadError?.isNotBlank() == true -> {
                title = tr("could_not_load_data", "Couldn't load data")
                subtitle = homeLoadError ?: tr("desktop_not_reachable", "Desktop app not reachable. Make sure LabFlow is running on your PC.")
                actionLabel = tr("retry", "Retry")
                action = { loadCompanionHome(settings.currentLabId) }
            }
            else -> {
                title = tr("loading_companion_home_title", "Loading companion home")
                subtitle = tr("loading_companion_home_subtitle", "We are preparing your mobile dashboard.")
                actionLabel = tr("retry", "Retry")
                action = { loadCompanionHome(settings.currentLabId) }
            }
        }

        return card().apply {
            setPadding(dp(20), dp(22), dp(20), dp(22))
            addView(TextView(this@MainActivity).apply {
                text = "!"
                gravity = Gravity.CENTER
                textSize = 28f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(colors.foreground)
                background = rounded(colors.surfaceAlt, dp(22), colors.border, 0)
                minimumWidth = dp(64)
                minimumHeight = dp(64)
                setPadding(0, dp(14), 0, dp(14))
            })
            addView(TextView(this@MainActivity).apply {
                text = title
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(colors.foreground)
                setPadding(0, dp(18), 0, dp(8))
            })
            addView(messageText(subtitle).apply {
                setTextColor(colors.muted)
            })

            val actions = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(18), 0, 0)
                addView(button(actionLabel, colors.primary) {
                    action()
                }.apply {
                    minimumHeight = dp(48)
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(space(10, 1))
                addView(button(tr("server_api", "Server & API"), colors.surfaceAlt) {
                    showConnectionDialog()
                }.apply {
                    minimumHeight = dp(48)
                    setTextColor(colors.foreground)
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }
            addView(actions)
        }
    }

    private fun buildBottomNavigation(): BottomNavigationView {
        val nav = BottomNavigationView(this).apply {
            setBackgroundColor(colors.surface)
            itemIconTintList = navColorStateList()
            itemTextColor = navColorStateList()
            itemActiveIndicatorColor = ColorStateList.valueOf(Color.argb(36, Color.red(colors.primary), Color.green(colors.primary), Color.blue(colors.primary)))
            elevation = dp(8).toFloat()
            setPadding(dp(12), dp(8), dp(12), dp(18))
            menu.add(0, 100, 0, tr("home", "Home")).setIcon(android.R.drawable.ic_menu_view)
            menu.add(0, 101, 1, tr("scan", "Scan")).setIcon(android.R.drawable.ic_menu_camera)
            menu.add(0, 102, 2, tr("borrows", "Borrows")).setIcon(android.R.drawable.ic_menu_agenda)
            menu.add(0, 103, 3, tr("profile", "Profile")).setIcon(android.R.drawable.ic_menu_manage)
            selectedItemId = when (currentTab) {
                MainTab.HOME -> 100
                MainTab.SCAN -> 101
                MainTab.BORROWS -> 102
                MainTab.PROFILE -> 103
            }
            setOnItemSelectedListener { item ->
                val nextTab = when (item.itemId) {
                    100 -> MainTab.HOME
                    101 -> MainTab.SCAN
                    102 -> MainTab.BORROWS
                    103 -> MainTab.PROFILE
                    else -> MainTab.HOME
                }
                if (currentTab != nextTab) {
                    currentTab = nextTab
                    buildMainUi()
                }
                true
            }
        }
        nav.post {
            val scanView = nav.findViewById<View>(101)
            scanView?.apply {
                scaleX = 1.14f
                scaleY = 1.14f
                alpha = 1f
            }
        }
        return nav
    }

    private fun navColorStateList(): ColorStateList {
        return ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(colors.primary, colors.muted)
        )
    }

    private fun attachKeyboardAwareScroll(scroll: ScrollView) {
        val rootView = scroll.rootView
        keyboardRootView = rootView
        keyboardLayoutListener?.let { rootView.viewTreeObserver.removeOnGlobalLayoutListener(it) }
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.height
            val keypadHeight = screenHeight - rect.bottom
            if (keypadHeight > screenHeight * 0.15f) {
                currentFocusField?.let { scrollToFocusedField(it) }
            }
        }
        keyboardLayoutListener = listener
        rootView.viewTreeObserver.addOnGlobalLayoutListener(listener)
    }

    private fun registerFocusTracking(vararg fields: EditText) {
        fields.forEach { field ->
            field.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    currentFocusField = view
                    clearFieldError(field)
                    rootScrollView?.postDelayed({ scrollToFocusedField(view) }, 120)
                }
            }
        }
    }

    private fun scrollToFocusedField(view: View) {
        rootScrollView?.post {
            val rect = Rect()
            view.getDrawingRect(rect)
            rootScrollView?.offsetDescendantRectToMyCoords(view, rect)
            val targetY = (rect.top - dp(120)).coerceAtLeast(0)
            rootScrollView?.smoothScrollTo(0, targetY)
        }
    }

    private fun setLoginLoading(loading: Boolean) {
        if (!::loginButton.isInitialized) {
            return
        }
        loginButton.isEnabled = !loading
        loginButton.text = if (loading) tr("signing_in", "Signing in...") else tr("sign_in", "Sign In")
        loginButton.alpha = if (loading) 0.82f else 1f
        usernameInput.isEnabled = !loading
        passwordInput.isEnabled = !loading
    }

    private fun setFieldError(field: EditText, message: String) {
        field.background = rounded(colors.surfaceAlt, dp(14), colors.danger, 2)
        if (::resultText.isInitialized) {
            resultText.setTextColor(colors.danger)
            resultText.text = message
        }
        field.animate().translationX(dp(6).toFloat()).setDuration(40).withEndAction {
            field.animate().translationX((-dp(6)).toFloat()).setDuration(40).withEndAction {
                field.animate().translationX(0f).setDuration(40).start()
            }.start()
        }.start()
    }

    private fun clearFieldError(field: EditText) {
        field.background = GradientDrawable().apply { setColor(Color.TRANSPARENT) }
        if (::resultText.isInitialized) {
            resultText.setTextColor(colors.muted)
        }
    }

    private fun topActionButton(icon: String, accentColor: Int, caption: String, action: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(TextView(this@MainActivity).apply {
                text = icon
                textSize = 18f
                gravity = Gravity.CENTER
                setTextColor(readableText(accentColor))
                background = rounded(accentColor, dp(13), mix(accentColor, Color.WHITE, 0.16f), 2)
                setPadding(dp(14), dp(12), dp(14), dp(12))
                setOnClickListener { action() }
                addPressAnimation()
            })
            addView(TextView(this@MainActivity).apply {
                text = caption
                textSize = 11f
                setTextColor(colors.muted)
                setPadding(0, dp(6), 0, 0)
                gravity = Gravity.CENTER
            })
        }
    }

    private fun sectionCard(title: String, accentColor: Int): LinearLayout {
        return card().apply {
            background = rounded(colors.surface, dp(16), colors.border, 1)
            addView(sectionTitle(title))
        }
    }

    private fun statusInfoChip(label: String, value: String, color: Int): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(colors.surfaceAlt, dp(10), colors.border, 1)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            addView(TextView(this@MainActivity).apply {
                text = label
                textSize = 11f
                setTextColor(colors.muted)
            })
            addView(TextView(this@MainActivity).apply {
                text = value
                textSize = 12.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(color)
            })
        }
    }

    private fun metricChip(label: String, value: String, color: Int): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(colors.surfaceAlt, dp(10), mix(colors.primary, colors.border, 0.35f), 1)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            addView(TextView(this@MainActivity).apply {
                text = label
                textSize = 11f
                setTextColor(colors.muted)
            })
            addView(TextView(this@MainActivity).apply {
                text = value
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(color)
            })
        }
    }

    private fun metricChip(label: String, value: String, color: Int, action: () -> Unit): LinearLayout {
        return metricChip(label, value, color).apply {
            setOnClickListener { action() }
            addPressAnimation()
        }
    }

    private fun filterChip(label: String, selected: Boolean, action: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(if (selected) readableText(colors.primary) else colors.foreground)
            background = rounded(if (selected) colors.primary else colors.surfaceAlt, dp(18), if (selected) colors.primary else colors.border, 1)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            setOnClickListener { action() }
        }
    }

    private fun itemTile(item: CompanionBorrowedItemDto): LinearLayout {
        return card().apply {
            background = rounded(colors.surfaceAlt, dp(14), softBorderColor(), 1)
            addView(TextView(this@MainActivity).apply {
                text = item.name.orEmpty().ifBlank { tr("borrowed_item", "Borrowed item") }
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(colors.foreground)
            })
            addView(TextView(this@MainActivity).apply {
                text = item.location.orEmpty().ifBlank { tr("no_location", "No location") }
                textSize = 12f
                setTextColor(colors.muted)
            })
            addView(TextView(this@MainActivity).apply {
                text = "${tr("due_prefix", "Due")} ${item.expectedReturnDate.orEmpty().ifBlank { tr("not_set", "not set") }}"
                textSize = 12f
                setTextColor(colors.warning)
                setPadding(0, dp(10), 0, 0)
            })
        }
    }

    private fun containerTile(item: CompanionContainerDto): LinearLayout {
        return card().apply {
            background = rounded(colors.surfaceAlt, dp(14), colors.border, 1)
            addView(TextView(this@MainActivity).apply {
                text = item.name.orEmpty().ifBlank { tr("container", "Container") }
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(colors.foreground)
            })
            addView(TextView(this@MainActivity).apply {
                text = "${item.itemCount} ${tr("items", "items")}"
                textSize = 12f
                setTextColor(colors.accent)
                setPadding(0, dp(10), 0, 0)
            })
        }
    }

    private fun showNotificationsDialog(notifications: List<CompanionNotificationDto>) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        if (notifications.isEmpty()) {
            container.addView(messageText(tr("no_notifications", "No notifications right now.")))
        } else {
            notifications.forEach { notification ->
                container.addView(card().apply {
                    background = rounded(colors.surfaceAlt, dp(12), colors.border, 1)
                    addView(TextView(this@MainActivity).apply {
                        text = notification.title.orEmpty().ifBlank { tr("notification", "Notification") }
                        textSize = 15f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(colors.foreground)
                    })
                    addView(TextView(this@MainActivity).apply {
                        text = notification.message.orEmpty()
                        textSize = 12.5f
                        setTextColor(colors.muted)
                    })
                })
            }
        }
        AlertDialog.Builder(this)
            .setTitle(tr("notifications", "Notifications"))
            .setView(ScrollView(this).apply { addView(container) })
            .setPositiveButton(tr("close", "Close"), null)
            .show()
            .also { lowerDialog(it) }
            .also { styleDialog(it) }
    }

    private fun showLabPicker(labs: List<LabSummaryDto>) {
        if (labs.isEmpty()) {
            message(tr("no_labs_available", "No laboratories available for this account."))
            return
        }
        val labels = labs.map { "${it.name.orEmpty()} | ${it.role.orEmpty().ifBlank { tr("member", "Member") }}" }.toTypedArray()
        val selectedIndex = labs.indexOfFirst { it.id == settings.currentLabId }.coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(tr("switch_laboratory", "Switch laboratory"))
            .setSingleChoiceItems(labels, selectedIndex) { dialog, which ->
                val selected = labs[which]
                settings.currentLabId = selected.id
                dialog.dismiss()
                loadCompanionHome(selected.id)
            }
            .setNegativeButton(tr("cancel", "Cancel"), null)
            .show()
    }

    private fun showSettingsMenu() {
        hostInput = input(tr("pc_ip", "PC IP"), settings.host)
        portInput = input(tr("port", "Port"), settings.port.ifBlank { "8080" }, InputType.TYPE_CLASS_NUMBER)
        apiKeyInput = input(tr("api_key", "API key"), settings.apiKey)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(sectionTitle(tr("appearance", "Appearance")))
            addView(appearanceCard())
            addView(sectionTitle(tr("connection", "Connection")))
            addView(fieldShell(tr("pc_ip", "PC IP"), "IP", hostInput))
            addView(space(1, 10))
            addView(fieldShell(tr("port", "Port"), "#", portInput))
            addView(space(1, 10))
            addView(fieldShell(tr("api_key", "API key"), "K", apiKeyInput))
        }

        AlertDialog.Builder(this)
            .setTitle(tr("companion_settings", "Companion settings"))
            .setView(ScrollView(this).apply { addView(content) })
            .setPositiveButton(tr("save", "Save")) { _, _ ->
                saveSettings()
                colors = CompanionTheme.resolve(settings.paletteEnum(), settings.modeEnum())
                currentHome = null
                buildMainUi()
            }
            .setNeutralButton(tr("logout", "Logout")) { _, _ ->
                LabFlowNotificationService.cancel(this)
                settings.clearUser()
                settings.currentLabId = 0
                currentHome = null
                currentEquipment = null
                buildLoginUi()
            }
            .setNegativeButton(tr("close", "Close"), null)
            .show()
            .also { lowerDialog(it) }
            .also { styleDialog(it) }
    }

    private fun profileStatsGrid(home: CompanionHomeDto?): LinearLayout {
        val stats = home?.stats
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val topRow = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
            val borrowsChip = metricChip(tr("borrows", "Borrows"), "${stats?.myTotalBorrows ?: 0}", colors.primary) {
                showInfoDialog(tr("borrows", "Borrows"), tr("borrows_info", "Total borrow records created on your account in this lab."))
            }
            topRow.addView(borrowsChip, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            topRow.addView(space(10, 1))
            val activeChip = metricChip(tr("active", "Active"), "${stats?.myBorrowedCount ?: 0}", colors.success) {
                showInfoDialog(tr("active", "Active"), tr("active_info", "Items currently borrowed on your account and not yet returned."))
            }
            topRow.addView(activeChip, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(topRow)

            val bottomRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(10), 0, 0)
            }
            val rankingChip = metricChip(tr("ranking", "Ranking"), rankLabel(stats?.myRank ?: 0), colors.warning) {
                showInfoDialog(tr("ranking", "Ranking"), tr("ranking_info", "Your current place in the lab leaderboard based on points earned."))
            }
            bottomRow.addView(rankingChip, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            bottomRow.addView(space(10, 1))
            val pointsChip = metricChip(tr("points", "Points"), "${stats?.myPoints ?: 0}", colors.accent) {
                showInfoDialog(tr("points", "Points"), tr("points_info", "Points earned through good lab behavior, timely returns, and useful reports."))
            }
            bottomRow.addView(pointsChip, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(bottomRow)
        }
    }

    private fun rankLabel(rank: Int): String {
        if (rank <= 0) return tr("unranked", "Unranked")
        return when (rank) {
            1 -> "#1"
            2 -> "#2"
            3 -> "#3"
            else -> "#$rank"
        }
    }

    private fun settingsRow(title: String, value: String, action: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(colors.surfaceAlt, dp(14), colors.border, 1)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(this@MainActivity).apply {
                    text = title
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(colors.foreground)
                })
                addView(TextView(this@MainActivity).apply {
                    text = value
                    textSize = 12.5f
                    setTextColor(colors.muted)
                    setPadding(0, dp(3), 0, 0)
                })
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(TextView(this@MainActivity).apply {
                text = "Ä‚ËĂ˘â€šÂ¬ÄąĹş"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(colors.muted)
            })
            setOnClickListener { action() }
            addPressAnimation()
        }
    }

    private fun showAppearanceDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(appearanceCard())
        }
        AlertDialog.Builder(this)
            .setTitle(tr("appearance", "Appearance"))
            .setView(container)
            .setPositiveButton(tr("close", "Close"), null)
            .show()
            .also { lowerDialog(it) }
            .also { styleDialog(it) }
    }

    private fun showLanguageDialog() {
        val languages = companionLanguages()
        val selected = languages.indexOfFirst { it.first == settings.appLanguage }.coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(tr("language", "Language"))
            .setSingleChoiceItems(languages.map { it.second }.toTypedArray(), selected) { dialog, which ->
                settings.appLanguage = languages[which].first
                dialog.dismiss()
                buildMainUi()
            }
            .setNegativeButton(tr("cancel", "Cancel"), null)
            .show()
            .also { lowerDialog(it) }
            .also { styleDialog(it) }
    }

    private fun showConnectionDialog() {
        hostInput = input(tr("pc_ip", "PC IP"), settings.host)
        portInput = input(tr("port", "Port"), settings.port.ifBlank { "8080" }, InputType.TYPE_CLASS_NUMBER)
        apiKeyInput = input(tr("api_key", "API key"), settings.apiKey)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(fieldShell(tr("pc_ip", "PC IP"), "IP", hostInput))
            addView(space(1, 10))
            addView(fieldShell(tr("port", "Port"), "#", portInput))
            addView(space(1, 10))
            addView(fieldShell(tr("api_key", "API key"), "K", apiKeyInput))
        }
        AlertDialog.Builder(this)
            .setTitle(tr("server_api", "Server & API"))
            .setView(ScrollView(this).apply { addView(content) })
            .setPositiveButton(tr("save", "Save")) { _, _ ->
                saveSettings()
                currentHome = null
                buildMainUi()
                loadCompanionHome(settings.currentLabId)
            }
            .setNegativeButton(tr("close", "Close"), null)
            .show()
            .also { lowerDialog(it) }
            .also { styleDialog(it) }
    }

    private fun showLoginSettingsDialog() {
        hostInput = input(tr("pc_ip", "PC IP"), settings.host)
        portInput = input(tr("port", "Port"), settings.port.ifBlank { "8080" }, InputType.TYPE_CLASS_NUMBER)
        apiKeyInput = input(tr("api_key", "API key"), settings.apiKey)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(sectionTitle(tr("appearance", "Appearance")))
            addView(appearanceCard())
            addView(sectionTitle(tr("connection", "Connection")))
            addView(fieldShell(tr("pc_ip", "PC IP"), "IP", hostInput))
            addView(space(1, 10))
            addView(fieldShell(tr("port", "Port"), "#", portInput))
            addView(space(1, 10))
            addView(fieldShell(tr("api_key", "API key"), "K", apiKeyInput))
        }
        AlertDialog.Builder(this)
            .setTitle(tr("companion_settings", "Companion settings"))
            .setView(ScrollView(this).apply { addView(content) })
            .setPositiveButton(tr("save", "Save")) { _, _ ->
                saveSettings()
                colors = CompanionTheme.resolve(settings.paletteEnum(), settings.modeEnum())
                buildLoginUi()
            }
            .setNegativeButton(tr("close", "Close"), null)
            .show()
            .also { lowerDialog(it) }
            .also { styleDialog(it) }
    }

    private fun toggleNotifications() {
        settings.notificationsEnabled = !settings.notificationsEnabled
        if (settings.notificationsEnabled) {
            ensureNotificationPermission()
            LabFlowNotificationService.schedule(this)
            Toast.makeText(this, tr("notifications_enabled", "Notifications enabled"), Toast.LENGTH_SHORT).show()
        } else {
            LabFlowNotificationService.cancel(this)
            Toast.makeText(this, tr("notifications_disabled", "Notifications disabled"), Toast.LENGTH_SHORT).show()
        }
        buildMainUi()
    }

    private fun showAboutDialog() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(brandWordmark(22f, 30).apply {
                setPadding(0, 0, 0, dp(4))
            })
            addView(TextView(this@MainActivity).apply {
                text = tr("about_subtitle", "Mobile companion for the LabFlow desktop application over local Wi-Fi.")
                textSize = 13f
                setTextColor(colors.muted)
                setPadding(0, dp(8), 0, dp(12))
            })
            addView(infoLineView(tr("theme", "Theme"), "${if (settings.modeEnum() == CompanionMode.DARK) tr("dark", "Dark") else tr("light", "Light")} | ${settings.paletteEnum().displayName}"))
            addView(infoLineView(tr("notifications", "Notifications"), if (settings.notificationsEnabled) tr("enabled", "Enabled") else tr("disabled", "Disabled")))
            addView(infoLineView(tr("server", "Server"), "${settings.host.ifBlank { tr("not_configured", "Not configured") }}:${settings.port.ifBlank { "8080" }}"))
        }
        AlertDialog.Builder(this)
            .setTitle(tr("about_labflow", "About LabFlow"))
            .setView(content)
            .setPositiveButton(tr("close", "Close"), null)
            .show()
            .also { lowerDialog(it) }
            .also { styleDialog(it) }
    }

    private fun showInfoDialog(title: String, body: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(body)
            .setPositiveButton(tr("close", "Close"), null)
            .show()
            .also { lowerDialog(it) }
            .also { styleDialog(it) }
    }

    private fun infoLineView(label: String, value: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
            addView(TextView(this@MainActivity).apply {
                text = "$label:"
                textSize = 12.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(colors.muted)
            })
            addView(space(10, 1))
            addView(TextView(this@MainActivity).apply {
                text = value
                textSize = 13f
                setTextColor(colors.foreground)
            })
        }
    }

    private fun brandWordmark(textSizeSp: Float, iconSizeDp: Int): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(ImageView(this@MainActivity).apply {
                setImageResource(R.drawable.labflow_logo)
                setColorFilter(colors.primary)
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                alpha = if (settings.modeEnum() == CompanionMode.DARK) 0.98f else 0.94f
                layoutParams = LinearLayout.LayoutParams(dp(iconSizeDp), dp(iconSizeDp)).apply {
                    marginEnd = dp(10)
                }
            })
            addView(TextView(this@MainActivity).apply {
                text = "Lab"
                textSize = textSizeSp
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(colors.foreground)
            })
            addView(TextView(this@MainActivity).apply {
                text = "Flow"
                textSize = textSizeSp
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(colors.primary)
            })
        }
    }

    private fun notificationColor(type: String?): Int {
        return when (type?.uppercase()) {
            "SUCCESS" -> colors.success
            "WARNING" -> colors.warning
            "DANGER" -> colors.danger
            else -> colors.accent
        }
    }

    private fun pageRoot(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(28))
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun styleDialog(dialog: AlertDialog) {
        dialog.window?.decorView?.setBackgroundColor(colors.background)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(colors.primary)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(colors.muted)
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(colors.danger)
    }

    private fun lowerDialog(dialog: AlertDialog) {
        dialog.window?.attributes = dialog.window?.attributes?.apply {
            y = dp(36)
        }
    }

    private fun softBorderColor(): Int = mix(colors.surfaceAlt, colors.foreground, 0.14f)

    private fun card(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(colors.surface, dp(12), softBorderColor(), 1)
            elevation = dp(3).toFloat()
            setPadding(dp(18), dp(16), dp(18), dp(18))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16) }
        }
    }

    private fun sectionTitle(value: String): TextView {
        return TextView(this).apply {
            text = value.uppercase()
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            setTextColor(colors.muted)
            setPadding(0, 0, 0, dp(10))
        }
    }

    private fun input(hint: String, value: String, inputTypeValue: Int = InputType.TYPE_CLASS_TEXT): EditText {
        return EditText(this).apply {
            this.hint = hint
            setText(value)
            inputType = inputTypeValue
            setSingleLine((inputTypeValue and InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0)
            setTextColor(colors.foreground)
            setHintTextColor(colors.muted)
            textSize = 14f
            background = GradientDrawable().apply { setColor(Color.TRANSPARENT) }
            setPadding(0, dp(10), 0, dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun fieldShell(label: String, icon: String, input: EditText): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@MainActivity).apply {
                text = label
                textSize = 12f
                setTextColor(colors.muted)
                setPadding(0, 0, 0, dp(6))
            })
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(colors.surfaceAlt, dp(14), softBorderColor(), 1)
                setPadding(dp(12), dp(4), dp(12), dp(4))
                addView(TextView(this@MainActivity).apply {
                    text = icon
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(colors.muted)
                })
                addView(space(10, 1))
                addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            })
        }
    }

    private fun passwordFieldShell(): LinearLayout {
        val toggle = TextView(this).apply {
            text = tr("show", "Show")
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(colors.primary)
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        toggle.setOnClickListener {
            val visibleType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            val hiddenType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            val visible = passwordInput.inputType == visibleType
            passwordInput.inputType = if (visible) hiddenType else visibleType
            passwordInput.setSelection(passwordInput.text?.length ?: 0)
            toggle.text = if (visible) tr("show", "Show") else tr("hide", "Hide")
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@MainActivity).apply {
                text = tr("password", "Password")
                textSize = 12f
                setTextColor(colors.muted)
                setPadding(0, 0, 0, dp(6))
            })
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(colors.surfaceAlt, dp(14), softBorderColor(), 1)
                setPadding(dp(12), dp(4), dp(8), dp(4))
                addView(TextView(this@MainActivity).apply {
                    text = "P"
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(colors.muted)
                })
                addView(space(10, 1))
                addView(passwordInput, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(toggle)
            })
        }
    }

    private fun button(value: String, color: Int, weight: Float? = null, action: () -> Unit): Button {
        return Button(this).apply {
            text = value
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            textSize = 14f
            setTextColor(readableText(color))
            background = rounded(color, dp(14), color, 0)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            elevation = dp(2).toFloat()
            setOnClickListener { action() }
            addPressAnimation()
            layoutParams = LinearLayout.LayoutParams(
                if (weight == null) LinearLayout.LayoutParams.MATCH_PARENT else 0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                weight ?: 0f
            ).apply { topMargin = dp(12) }
        }
    }

    private fun messageText(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(colors.muted)
            setLineSpacing(2f, 1.05f)
        }
    }

    private fun pill(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(colors.muted)
            gravity = Gravity.CENTER
            background = rounded(colors.surfaceAlt, dp(18), colors.border, 1)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(2) }
        }
    }

    private fun actionCallout(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 13.5f
            setTextColor(colors.foreground)
            setLineSpacing(2f, 1.08f)
            background = rounded(colors.cardOverlay, dp(10), colors.border, 1)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10); bottomMargin = dp(2) }
        }
    }

    private fun styleActionButton(button: Button, enabled: Boolean, color: Int) {
        button.isEnabled = enabled
        button.alpha = if (enabled) 1f else 0.62f
        button.setTextColor(if (enabled) readableText(color) else colors.muted)
        button.background = if (enabled) {
            rounded(color, dp(14), mix(color, colors.surface, 0.12f), 1)
        } else {
            rounded(colors.surfaceAlt, dp(14), colors.border, 1)
        }
    }

    private fun View.addPressAnimation() {
        setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(110).start()
            }
            false
        }
    }

    private fun appearanceCard(): LinearLayout {
        val card = card()
        card.addView(TextView(this).apply {
            text = tr("appearance_helper", "Keep the companion visually aligned with your desktop LabFlow workspace.")
            textSize = 12.5f
            setTextColor(colors.muted)
            setPadding(0, 0, 0, dp(6))
        })

        val modeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val darkButton = segmentedChoiceButton(tr("dark", "Dark"), settings.modeEnum() == CompanionMode.DARK)
        val lightButton = segmentedChoiceButton(tr("light", "Light"), settings.modeEnum() == CompanionMode.LIGHT)
        fun refreshModeButtons() {
            styleSegmentedChoiceButton(darkButton, settings.modeEnum() == CompanionMode.DARK)
            styleSegmentedChoiceButton(lightButton, settings.modeEnum() == CompanionMode.LIGHT)
        }
        darkButton.setOnClickListener {
            settings.themeMode = CompanionMode.DARK.name
            refreshModeButtons()
        }
        lightButton.setOnClickListener {
            settings.themeMode = CompanionMode.LIGHT.name
            refreshModeButtons()
        }
        modeRow.addView(darkButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            topMargin = dp(12)
        })
        modeRow.addView(space(10, 1))
        modeRow.addView(lightButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            topMargin = dp(12)
        })
        card.addView(modeRow)

        val chips = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }
        val chipViews = mutableListOf<Pair<CompanionPalette, TextView>>()
        CompanionPalette.values().forEachIndexed { index, palette ->
            val chip = themeChip(palette)
            chipViews += palette to chip
            chips.addView(chip)
            if (index < CompanionPalette.values().lastIndex) {
                chips.addView(space(8, 1))
            }
        }
        fun refreshPaletteChips() {
            chipViews.forEach { (palette, chip) -> styleThemeChip(chip, palette) }
        }
        chipViews.forEach { (palette, chip) ->
            chip.setOnClickListener {
                settings.themePalette = palette.name
                refreshPaletteChips()
            }
        }
        card.addView(chips)
        return card
    }

    private fun themeChip(palette: CompanionPalette): TextView {
        return TextView(this).apply {
            text = palette.displayName
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(8))
            styleThemeChip(this, palette)
            addPressAnimation()
        }
    }

    private fun segmentedChoiceButton(label: String, selected: Boolean): TextView {
        return TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            minHeight = dp(48)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            styleSegmentedChoiceButton(this, selected)
            addPressAnimation()
        }
    }

    private fun styleSegmentedChoiceButton(view: TextView, selected: Boolean) {
        view.setTextColor(if (selected) readableText(colors.primary) else colors.foreground)
        view.background = rounded(
            if (selected) colors.primary else colors.surfaceAlt,
            dp(14),
            if (selected) mix(colors.primary, Color.WHITE, 0.16f) else softBorderColor(),
            1
        )
    }

    private fun styleThemeChip(view: TextView, palette: CompanionPalette) {
        val preview = CompanionTheme.resolve(palette, settings.modeEnum())
        val selected = settings.paletteEnum() == palette
        view.setTextColor(if (selected) readableText(preview.primary) else colors.foreground)
        view.background = rounded(
            if (selected) preview.primary else colors.surfaceAlt,
            dp(18),
            if (selected) mix(preview.primary, Color.WHITE, 0.18f) else softBorderColor(),
            1
        )
    }

    private fun companionLanguages(): List<Pair<String, String>> {
        return listOf(
            "en" to "English",
            "ro" to "Romana",
            "es" to "Espanol",
            "zh" to "Chinese",
            "ja" to "Japanese",
            "ru" to "Russian",
            "fr" to "Francais",
            "de" to "Deutsch",
            "it" to "Italiano"
        )
    }
    private fun currentLanguageLabel(): String {
        return companionLanguages().firstOrNull { it.first == settings.appLanguage }?.second ?: "English"
    }

    private fun tr(key: String, fallback: String): String {
        val code = settings.appLanguage.lowercase()
        val ro = mapOf(
            "settings" to "Setari",
            "sign_in" to "Autentificare",
            "login_subtitle" to "Management laborator",
            "login_helper" to "Introdu credentialele desktop pentru a continua.",
            "connect_server_first" to "Conecteaza-te intai la server ->",
            "hide_server_settings" to "Ascunde setarile serverului",
            "my_stats" to "Statisticile mele",
            "appearance" to "Aspect",
            "language" to "Limba",
            "laboratory" to "Laborator",
            "choose_active_lab" to "Alege laboratorul activ",
            "server_api" to "Server si API",
            "notifications" to "Notificari",
            "notification" to "Notificare",
            "no_notifications" to "Nu exista notificari acum.",
            "about_labflow" to "Despre LabFlow",
            "about_subtitle" to "Companion desktop prin Wi-Fi local",
            "open_notifications" to "Deschide notificarile",
            "sign_out" to "Deconectare",
            "menu" to "Meniu",
            "home" to "Acasa",
            "scan" to "Scanare",
            "profile" to "Profil",
            "good_morning" to "Buna dimineata",
            "good_afternoon" to "Buna ziua",
            "good_evening" to "Buna seara",
            "labflow_user" to "Utilizator LabFlow",
            "active_laboratory" to "Laborator activ",
            "connected" to "Conectat",
            "connection_details" to "Detalii conexiune",
            "quick_actions" to "Actiuni rapide",
            "scan_qr" to "Scaneaza QR",
            "scan_qr_subtitle" to "Deschide camera si incarca un echipament",
            "my_borrows" to "Imprumuturile mele",
            "active_items" to "obiecte active",
            "lab_status" to "Stare laborator",
            "total_items" to "obiecte totale",
            "report_fault" to "Raporteaza defect",
            "open_faults" to "defecte deschise",
            "scan_then_report_fault" to "Scaneaza mai intai un obiect, apoi raporteaza defectul din panoul de actiuni.",
            "my_active_borrows" to "Imprumuturile mele active",
            "no_active_borrows" to "Nu exista imprumuturi active. Scaneaza un cod QR ca sa imprumuti echipamente de pe telefon.",
            "borrowed_item" to "Obiect imprumutat",
            "due_prefix" to "Scadent",
            "not_set" to "nesetat",
            "history" to "Istoric",
            "no_borrow_history" to "Inca nu exista istoric de imprumuturi. Obiectele returnate vor aparea aici.",
            "return_equipment" to "Returneaza echipamentul",
            "load_item" to "Incarca obiectul",
            "loaded_item_message" to "Obiectul a fost incarcat in panoul de actiuni.",
            "borrow_history_item" to "Element din istoric",
            "borrowed_prefix" to "Imprumutat",
            "returned_prefix" to "Returnat",
            "profile_settings" to "Profil si setari",
            "member" to "Membru",
            "no_active_lab" to "Niciun laborator activ",
            "borrows" to "Imprumuturi",
            "active" to "Active",
            "ranking" to "Clasament",
            "points" to "Puncte",
            "unranked" to "Neclasat",
            "borrows_info" to "Numarul total de imprumuturi facute pe contul tau in acest laborator.",
            "active_info" to "Obiectele imprumutate acum pe contul tau si inca nereturnate.",
            "ranking_info" to "Locul tau actual in clasamentul laboratorului pe baza punctelor.",
            "points_info" to "Puncte obtinute pentru comportament bun, returnari la timp si rapoarte utile.",
            "close" to "Inchide",
            "cancel" to "Anuleaza",
            "save" to "Salveaza",
            "logout" to "Iesire",
            "connection" to "Conexiune",
            "companion_settings" to "Setari companion",
            "switch_laboratory" to "Schimba laboratorul",
            "no_labs_available" to "Nu exista laboratoare disponibile pentru acest cont.",
            "enabled" to "Active",
            "disabled" to "Dezactivate",
            "notifications_enabled" to "Notificarile au fost activate",
            "notifications_disabled" to "Notificarile au fost dezactivate",
            "pc_ip" to "IP PC",
            "port" to "Port",
            "api_key" to "Cheie API",
            "test_connection" to "Testeaza conexiunea",
            "pc_ip_not_set" to "IP PC nesetat",
            "open" to "Deschide",
            "current_laboratory" to "Laborator curent",
            "lab_status_overview" to "Privire de ansamblu asupra echipamentelor, imprumuturilor, mentenantei si activitatii recente.",
            "back_to_home" to "Inapoi acasa",
            "overview_stats" to "Statistici generale",
            "items" to "Obiecte",
            "borrowed" to "Imprumutate",
            "faults" to "Defecte",
            "maintenance" to "Mentenanta",
            "equipment_by_status" to "Echipamente dupa status",
            "no_equipment_status" to "Nu exista inca date despre statusul echipamentelor.",
            "recent_activity" to "Activitate recenta",
            "no_recent_activity" to "Nu exista activitate recenta pentru acest laborator.",
            "top_risky_equipment" to "Echipamente cu risc ridicat",
            "no_risky_equipment" to "Nu exista echipamente riscante in acest laborator.",
            "equipment_item" to "Echipament",
            "risk_label" to "Risc",
            "no_risk_reason" to "Nu exista un motiv recent de risc.",
            "home_status" to "Stare generala",
            "loading_laboratory" to "Se incarca laboratorul...",
            "invite" to "Cod invitatie",
            "theme" to "Tema",
            "palette" to "Paleta",
            "switch_lab" to "Schimba laboratorul",
            "total" to "Total",
            "available" to "Disponibile",
            "containers" to "Containere",
            "refresh" to "Reimprospateaza",
            "both" to "Ambele",
            "borrowed_objects_containers" to "Obiecte imprumutate si containere",
            "nothing_to_show_yet" to "Nu este nimic de afisat inca. Imprumuta echipamente sau organizeaza obiectele in containere pe desktop.",
            "scanned_item" to "Obiect scanat",
            "no_equipment_scanned" to "Niciun echipament scanat",
            "scan_ready" to "Gata. Scaneaza un cod QR LabFlow, apoi alege actiunea evidentiata.",
            "refresh_scanned_item" to "Reimprospateaza obiectul scanat",
            "borrow_on_my_account" to "Imprumuta pe contul meu",
            "return_borrowed_item" to "Returneaza obiectul imprumutat",
            "scan_qr_helper" to "Scaneaza un cod QR LabFlow pentru a vedea detaliile si a imprumuta direct de pe telefon.",
            "scan_equipment" to "Scaneaza echipament",
            "scan_equipment_desc" to "Scaneaza un cod QR LabFlow pentru a incarca instant un obiect, apoi il poti imprumuta, returna sau raporta defect.",
            "open_qr_scanner" to "Deschide scannerul QR",
            "refresh_current_lab" to "Reimprospateaza laboratorul curent",
            "no_location" to "Fara locatie",
            "no_wifi_connection" to "Nu exista conexiune Wi-Fi. Conecteaza telefonul la aceeasi retea ca PC-ul.",
            "invalid_local_api_key" to "Cheie API locala invalida. Verifica cheia desktop si setarile companionului.",
            "companion_endpoint_missing" to "Endpointul companion lipseste. Reporneste LabFlow Desktop ca sa se incarce actualizarile mobile.",
            "could_not_load_companion_home" to "Nu am putut incarca pagina principala a companionului.",
            "username_required" to "Username-ul este obligatoriu.",
            "password_required" to "Parola este obligatorie.",
            "invalid_username_password" to "Username sau parola invalida.",
            "signed_in_as" to "Autentificat ca",
            "loading_companion_home" to "Se incarca laboratorul tau in companion...",
            "connection_ok" to "Conexiune reusita. LabFlow Desktop este disponibil.",
            "desktop_not_available" to "LabFlow Desktop nu este disponibil.",
            "login_before_scan" to "Autentifica-te inainte sa scanezi echipamente.",
            "invalid_qr_code" to "Cod QR LabFlow invalid.",
            "invalid_api_key" to "Cheie API invalida.",
            "equipment_not_found" to "Echipamentul nu a fost gasit.",
            "status" to "Status",
            "manufacturer" to "Producator",
            "model" to "Model",
            "serial" to "Serie",
            "description" to "Descriere",
            "notes" to "Note",
            "no_description" to "Fara descriere.",
            "no_notes" to "Fara note.",
            "only_available_can_borrow" to "Doar echipamentele disponibile pot fi imprumutate.",
            "borrow_complete" to "Imprumut finalizat",
            "could_not_borrow_equipment" to "Nu am putut imprumuta acest echipament.",
            "no_active_borrow_record" to "Nu exista un imprumut activ pentru acest obiect.",
            "borrow_belongs_other_user" to "Acest obiect este imprumutat de alt utilizator si nu poate fi returnat de pe contul tau.",
            "return_complete" to "Returnare finalizata",
            "could_not_return_equipment" to "Nu am putut returna acest echipament.",
            "describe_fault" to "Descrie defectul",
            "send" to "Trimite",
            "fault_description_required" to "Descrierea defectului este obligatorie.",
            "fault_reported" to "Defect raportat",
            "could_not_report_fault" to "Nu am putut raporta defectul.",
            "refresh_this_item" to "Reimprospateaza acest obiect",
            "refresh_unavailable" to "Reimprospatarea nu este disponibila",
            "borrow_this_item" to "Imprumuta acest obiect",
            "borrow_unavailable" to "Imprumut indisponibil",
            "scan_to_borrow" to "Scaneaza pentru imprumut",
            "return_this_item" to "Returneaza acest obiect",
            "no_active_borrow_to_return" to "Nu exista un imprumut activ de returnat",
            "scan_to_return" to "Scaneaza pentru returnare",
            "report_fault_this_item" to "Raporteaza defectul acestui obiect",
            "scan_to_report_fault" to "Scaneaza pentru a raporta defectul",
            "unknown" to "NECUNOSCUT",
            "borrowed_by_you" to "Imprumutat de tine",
            "borrowed_by" to "Imprumutat de",
            "another_user" to "alt utilizator",
            "http_blocked" to "Traficul HTTP este blocat pe acest dispozitiv. Pastreaza cleartext activ pentru companion.",
            "invalid_local_api_key_sync" to "Cheie API locala invalida. Asigura-te ca telefonul foloseste aceeasi cheie ca desktopul.",
            "desktop_restart_needed" to "Aceasta versiune LabFlow Desktop nu expune inca endpointul companion. Reporneste aplicatia desktop.",
            "host_not_resolved" to "Gazda PC-ului nu a putut fi rezolvata. Verifica IP-ul PC-ului si faptul ca ambele dispozitive sunt pe aceeasi retea Wi-Fi.",
            "desktop_timeout" to "LabFlow Desktop nu a raspuns la timp. Verifica IP-ul PC-ului si ca aplicatia este deschisa.",
            "desktop_connection_refused" to "Conexiunea a fost refuzata de LabFlow Desktop. Verifica firewall-ul si ca aplicatia ruleaza.",
            "desktop_unreachable" to "Nu am putut ajunge la LabFlow Desktop. Verifica IP-ul PC-ului, firewall-ul si reteaua Wi-Fi.",
            "desktop_not_available_full" to "LabFlow Desktop nu este disponibil. Verifica IP-ul PC-ului, cheia API si reteaua Wi-Fi.",
            "desktop_not_reachable" to "Aplicatia desktop nu poate fi contactata. Asigura-te ca LabFlow ruleaza pe PC.",
            "no_wifi_connection_title" to "Fara conexiune Wi-Fi",
            "no_wifi_connection_subtitle" to "Conecteaza telefonul la aceeasi retea ca PC-ul, apoi incearca din nou.",
            "retry" to "Reincearca",
            "authentication_problem" to "Problema de autentificare",
            "authentication_problem_subtitle" to "Aplicatia companion nu s-a putut autentifica la LabFlow Desktop. Verifica cheia API locala.",
            "desktop_update_needed" to "Actualizare desktop necesara",
            "desktop_update_needed_subtitle" to "Reporneste LabFlow Desktop pentru ca endpointul companion sa devina disponibil.",
            "could_not_load_data" to "Nu am putut incarca datele",
            "loading_companion_home_title" to "Se incarca pagina companion",
            "loading_companion_home_subtitle" to "Pregatim dashboardul mobil.",
            "signing_in" to "Se autentifica...",
            "container" to "Container",
            "server" to "Server",
            "not_configured" to "Neconfigurat",
            "appearance_helper" to "Pastreaza companion-ul aliniat vizual cu workspace-ul LabFlow de pe desktop.",
            "show" to "Arata",
            "hide" to "Ascunde"
        )
        val basic = when (code) {
            "ro" -> ro
            "es" -> mapOf("settings" to "Configuracion", "language" to "Idioma", "appearance" to "Apariencia", "notifications" to "Notificaciones", "save" to "Guardar", "close" to "Cerrar", "cancel" to "Cancelar", "sign_out" to "Cerrar sesion")
            "fr" -> mapOf("settings" to "Parametres", "language" to "Langue", "appearance" to "Apparence", "notifications" to "Notifications", "save" to "Enregistrer", "close" to "Fermer", "cancel" to "Annuler", "sign_out" to "Deconnexion")
            "de" -> mapOf("settings" to "Einstellungen", "language" to "Sprache", "appearance" to "Darstellung", "notifications" to "Benachrichtigungen", "save" to "Speichern", "close" to "Schliessen", "cancel" to "Abbrechen", "sign_out" to "Abmelden")
            "it" -> mapOf("settings" to "Impostazioni", "language" to "Lingua", "appearance" to "Aspetto", "notifications" to "Notifiche", "save" to "Salva", "close" to "Chiudi", "cancel" to "Annulla", "sign_out" to "Esci")
            else -> emptyMap()
        }
        return basic[key] ?: fallback
    }    private fun rebuildUi() {
        colors = CompanionTheme.resolve(settings.paletteEnum(), settings.modeEnum())
        if (settings.isLoggedIn) {
            buildMainUi()
        } else {
            buildLoginUi()
        }
    }

    private fun ensureNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun applyLaunchTarget(intent: Intent?) {
        when (intent?.getStringExtra("targetTab")) {
            "BORROWS" -> {
                currentTab = MainTab.BORROWS
                currentBorrowTab = BorrowTab.ACTIVE
            }
            "HOME" -> {
                currentTab = MainTab.HOME
                currentHomeScreenMode = HomeScreenMode.DASHBOARD
            }
            "SCAN" -> currentTab = MainTab.SCAN
            "PROFILE" -> currentTab = MainTab.PROFILE
        }
    }

    private fun space(width: Int, height: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(width), dp(height))
        }
    }

    private fun rounded(color: Int, radius: Int, strokeColor: Int, strokeWidth: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
            if (strokeWidth > 0) {
                setStroke(dp(strokeWidth), strokeColor)
            }
        }
    }

    private fun statusColor(status: String?): Int {
        return when (status?.uppercase()) {
            "AVAILABLE" -> colors.success
            "BORROWED" -> colors.warning
            "DEFECT" -> colors.danger
            "MAINTENANCE" -> colors.primary
            else -> colors.accent
        }
    }

    private fun deadlineColor(date: String?): Int {
        val parsed = runCatching { date?.let { LocalDate.parse(it) } }.getOrNull() ?: return colors.muted
        val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), parsed)
        return when {
            days < 0 -> colors.danger
            days <= 2 -> colors.warning
            else -> colors.success
        }
    }

    private fun deadlineProgressRatio(date: String?): Float {
        val parsed = runCatching { date?.let { LocalDate.parse(it) } }.getOrNull() ?: return 0.28f
        val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), parsed)
        return when {
            days < 0 -> 1f
            days <= 1 -> 0.86f
            days <= 3 -> 0.62f
            days <= 7 -> 0.4f
            else -> 0.24f
        }
    }

    private fun compactDate(value: String?): String {
        return value?.take(10).orEmpty().ifBlank { "n/a" }
    }

    private fun compactDateTime(value: String?): String {
        return value?.replace("T", " ")?.take(16).orEmpty().ifBlank { "recently" }
    }

    private fun historyStatusLabel(item: CompanionBorrowHistoryItemDto): String {
        return when {
            item.returnCondition.equals("DEFECT", ignoreCase = true) -> tr("returned_defect", "Returned Defect")
            item.actualReturnDate != null && item.expectedReturnDate != null &&
                runCatching { LocalDate.parse(item.actualReturnDate.take(10)).isAfter(LocalDate.parse(item.expectedReturnDate.take(10))) }.getOrDefault(false) ->
                tr("returned_late", "Returned Late")
            item.status.equals("ACTIVE", ignoreCase = true) -> tr("still_active", "Still Active")
            else -> tr("returned_on_time", "Returned On Time")
        }
    }

    private fun historyStatusColor(item: CompanionBorrowHistoryItemDto): Int {
        return when {
            item.returnCondition.equals("DEFECT", ignoreCase = true) -> colors.danger
            item.actualReturnDate != null && item.expectedReturnDate != null &&
                runCatching { LocalDate.parse(item.actualReturnDate.take(10)).isAfter(LocalDate.parse(item.expectedReturnDate.take(10))) }.getOrDefault(false) ->
                colors.warning
            item.status.equals("ACTIVE", ignoreCase = true) -> colors.primary
            else -> colors.success
        }
    }

    private fun returnBorrowedItem(item: CompanionBorrowedItemDto) {
        lifecycleScope.launch {
            try {
                val request = ReturnRequest(item.borrowRecordId, "GOOD", "Returned from LabFlow Companion", null)
                val response = api().returnEquipment(settings.authorization(), item.equipmentId, request)
                val body = response.body()
                if (response.code() == 401) {
                    message(tr("invalid_api_key", "Invalid API key."))
                    return@launch
                }
                if (response.isSuccessful && body?.success == true) {
                    Toast.makeText(this@MainActivity, "Return complete", Toast.LENGTH_LONG).show()
                    loadCompanionHome(settings.currentLabId)
                    setActionMessage("${item.name.orEmpty()} returned successfully.", colors.success)
                } else {
                    message(body?.error ?: "Could not return this equipment.")
                }
            } catch (e: Exception) {
                message(userFriendlyError(e))
            }
        }
    }

    private fun readableText(background: Int): Int {
        val darkness = 1 - (0.299 * Color.red(background) + 0.587 * Color.green(background) + 0.114 * Color.blue(background)) / 255
        return if (darkness >= 0.42) Color.WHITE else colors.foreground
    }

    private fun mix(start: Int, end: Int, ratio: Float): Int {
        val safe = ratio.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(start) + ((Color.red(end) - Color.red(start)) * safe)).toInt(),
            (Color.green(start) + ((Color.green(end) - Color.green(start)) * safe)).toInt(),
            (Color.blue(start) + ((Color.blue(end) - Color.blue(start)) * safe)).toInt()
        )
    }

    private fun generateAvatar(username: String, sizeDp: Int): Bitmap {
        val size = dp(sizeDp)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val base = when ((username.hashCode() and 0x7fffffff) % 4) {
            0 -> colors.primary
            1 -> colors.accent
            2 -> colors.success
            else -> colors.warning
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = base }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)
        val initials = username
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "LF" }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size * 0.34f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        val y = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(initials, size / 2f, y, textPaint)
        return bitmap
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun showFatalError(error: Exception) {
        val textView = TextView(this)
        textView.text = "LabFlow Companion could not start.\n\n${error.message.orEmpty()}"
        textView.textSize = 16f
        textView.setTextColor(Color.WHITE)
        textView.setPadding(dp(32), dp(48), dp(32), dp(32))
        textView.setBackgroundColor(Color.BLACK)
        setContentView(textView)
    }
}

