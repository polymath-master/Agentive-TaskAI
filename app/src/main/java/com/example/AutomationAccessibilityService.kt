package com.example

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AutomationAccessibilityService : AccessibilityService() {

    companion object {
        private var instance: AutomationAccessibilityService? = null
        private val _logFlow = MutableSharedFlow<String>(extraBufferCapacity = 50)
        val logFlow = _logFlow.asSharedFlow()

        fun isRunning(): Boolean = instance != null

        fun log(message: String) {
            _logFlow.tryEmit(message)
        }

        fun performClickAt(x: Float, y: Float): Boolean {
            val active = instance ?: run {
                log("Click failed: Accessibility Service not active")
                return false
            }
            log("Executing Accessibility simulation: dispatching click at ($x, $y)")
            val path = Path().apply { moveTo(x, y) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
                .build()
            return active.dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    log("Simulation click completed successfully at ($x, $y)")
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    log("Simulation click cancelled by system")
                }
            }, null)
        }

        fun performSwipeAt(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 300L): Boolean {
            val active = instance ?: run {
                log("Swipe failed: Accessibility Service not active")
                return false
            }
            log("Executing Accessibility simulation: dispatching swipe from ($x1, $y1) to ($x2, $y2) duration ${durationMs}ms")
            val path = Path().apply {
                moveTo(x1, y1)
                lineTo(x2, y2)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
                .build()
            return active.dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    log("Simulation swipe completed successfully.")
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    log("Simulation swipe cancelled by system")
                }
            }, null)
        }

        fun performBack(): Boolean {
            val active = instance ?: run {
                log("Back action failed: Accessibility Service not active")
                return false
            }
            log("Executing Accessibility simulation: global BACK action")
            return active.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        }

        fun performHome(): Boolean {
            val active = instance ?: run {
                log("Home action failed: Accessibility Service not active")
                return false
            }
            log("Executing Accessibility simulation: global HOME action")
            return active.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        }

        fun performRecents(): Boolean {
            val active = instance ?: run {
                log("Recents action failed: Accessibility Service not active")
                return false
            }
            log("Executing Accessibility simulation: global RECENTS action")
            return active.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
        }

        fun scrapeScreen(): List<String> {
            val active = instance ?: run {
                log("Execution scrape failed: Accessibility Service not active")
                return emptyList()
            }
            val root = active.rootInActiveWindow ?: run {
                log("Scraper returned empty: No active window content available")
                return emptyList()
            }
            val items = mutableListOf<String>()
            traverseNodes(root, items)
            log("Swept active UI: Captured ${items.size} text nodes from view-tree")
            return items
        }

        private fun traverseNodes(node: AccessibilityNodeInfo?, list: MutableList<String>) {
            if (node == null) return
            val txt = node.text?.toString()
            val desc = node.contentDescription?.toString()
            if (!txt.isNullOrBlank()) {
                list.add("[Text] $txt (Id: ${node.viewIdResourceName ?: "None"})")
            } else if (!desc.isNullOrBlank()) {
                list.add("[Desc] $desc (Id: ${node.viewIdResourceName ?: "None"})")
            }
            for (i in 0 until node.childCount) {
                traverseNodes(node.getChild(i), list)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        log("SaaS-Agent Accessibility Engine: Service Connected & Hooked successfully.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Accessibility focus trace events silenced to avoid spamming the console trace.
        // If developer level debugging is required, you can uncomment the track logs here.
        /*
        val eventType = AccessibilityEvent.eventTypeToString(event.eventType)
        val packageName = event.packageName?.toString() ?: "Unknown"
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            log("Focus Sweep: Active Window changed. Target package: $packageName")
        }
        */
    }

    override fun onInterrupt() {
        log("SaaS-Agent Accessibility Engine: Execution interrupted/paused.")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        log("SaaS-Agent Accessibility Engine: Service disconnected / shut down.")
    }
}
