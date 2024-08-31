package com.krxkli.fasturl

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private val TAG = "mainActivity"
    private val CHANNEL_ID = "fastUrlChannel"
    private var lastUsedUri : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: ")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 请求通知权限（仅在 Android 13 及以上版本需要）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            } else {
                createNotificationChannel()
                showNotification()
            }
        } else {
            createNotificationChannel()
            showNotification()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        // 获取焦点时可以尝试读取剪贴板
        Log.d(TAG, "onWindowFocusChanged: $hasFocus")
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            Log.d(TAG, "got focus")
            readClipboard(this)
        }
    }

    private fun checkContentUpdate(uri: String) : Boolean {
        if (uri == lastUsedUri) {
            return false
        } else {
            return true
        }
    }

    // 拉起 B 站 App
    private fun pullApp(uri: String) {
        if (!checkContentUpdate(uri)) {
            return
        }

        val biliPackageName = "tv.danmaku.bili"

        Log.d(TAG, "pullApp: ")
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setData(Uri.parse(uri))
        intent.setPackage(biliPackageName)
        startActivity(intent)
    }

    // 转发 Uri 以拉起 B 站 App
    private fun forwardUri(clipboardText: String, clipboard : ClipboardManager) {
        val regex = Regex("https://[\\S]+")
        val matchResult = regex.find(clipboardText)

        if (matchResult != null) {
            val extractedSubstring = matchResult.value
            pullApp(extractedSubstring)

            // 清空剪贴板，避免重复拉起
            clipboard.setPrimaryClip(ClipData.newPlainText(null, ""))

            Log.d(TAG, "合法的子串是: $extractedSubstring")
        } else {
            println("未找到以 https 开头的合法子串")
        }
    }

    // 读取剪贴板内容
    private fun readClipboard(context: Context) {
        // 获取 ClipboardManager
        val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

        // 检查剪贴板是否有内容
        if (clipboard.hasPrimaryClip()) {
            // 获取剪贴板内容
            val clipData = clipboard.primaryClip
            val item = clipData!!.getItemAt(0)

            // 获取文本内容
            val clipboardText = item.text.toString()

            // 显示剪贴板内容
//            Toast.makeText(context, "剪贴板内容: $clipboardText", Toast.LENGTH_SHORT).show()
            forwardUri(clipboardText, clipboard)
        } else {
            Toast.makeText(context, "剪贴板为空", Toast.LENGTH_SHORT).show()
            return
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "fastUrl",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        Log.d(TAG, "showNotification: ")
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getText(R.string.notification_title))
            .setContentText(getText(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 替换为你的图标
            .setLargeIcon(Icon.createWithResource(this, R.drawable.ic_launcher_foreground))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // 点击后自动消失，不过会在 onCreate 中重新设置
            .setOngoing(true) // 不希望被清除
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(1, notification) // 1 是通知的 ID
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createNotificationChannel()
                showNotification()
            } else {
                Log.d("MainActivity", "通知权限被拒绝")
            }
        }
    }
}