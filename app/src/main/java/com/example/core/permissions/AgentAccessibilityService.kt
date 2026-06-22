package com.example.core.permissions

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AgentAccessibilityService : AccessibilityService() {

    companion object {
        var lastWhatsAppTranscript = ""
            private set

        fun clearTranscript() {
            lastWhatsAppTranscript = ""
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Filter events coming specifically from WhatsApp
        val packageName = event.packageName?.toString() ?: ""
        if (packageName == "com.whatsapp") {
            val rootNode = rootInActiveWindow ?: return
            val transcriptBuilder = StringBuilder()
            
            // Traverse view hierarchy to extract chat bubble text nodes
            extractChatBubbles(rootNode, transcriptBuilder)
            
            val currentTranscript = transcriptBuilder.toString().trim()
            if (currentTranscript.isNotEmpty()) {
                lastWhatsAppTranscript = currentTranscript
                Log.d("AgentAccessibility", "Captured WhatsApp Chat Transcript updated: ${currentTranscript.take(100)}...")
            }
        }
    }

    private fun extractChatBubbles(node: AccessibilityNodeInfo?, builder: StringBuilder) {
        if (node == null) return

        // WhatsApp chat items are typically TextViews containing the message bodies
        if (node.className == "android.widget.TextView") {
            val text = node.text?.toString() ?: ""
            // Filter timestamps and typical non-message noise
            if (text.isNotBlank() && text.length > 2 && !text.matches(Regex("\\d{1,2}:\\d{2}\\s*(AM|PM)?", RegexOption.IGNORE_CASE))) {
                // Approximate standard dialogue tagging by checking message node layout metadata (alignment etc.)
                builder.append(text).append("\n")
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            extractChatBubbles(child, builder)
        }
    }

    override fun onInterrupt() {
        Log.w("AgentAccessibility", "Accessibility service interrupted.")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("AgentAccessibility", "Accessibility service connected successfully.")
    }
}
