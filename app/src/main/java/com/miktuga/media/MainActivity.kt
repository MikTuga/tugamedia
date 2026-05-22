package com.miktuga.media

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.miktuga.design.feedback.FeedbackLauncher
import java.io.File

class MainActivity : AppCompatActivity() {

    private enum class MediaType { MUSIC, VIDEO }
    private data class MediaItem(val file: File, val type: MediaType)

    private val all = mutableListOf<MediaItem>()
    private var filter: MediaType? = null
    private lateinit var adapter: MediaAdapter

    private val musicExt = setOf("mp3", "m4a", "flac", "ogg", "wav", "aac", "wma")
    private val videoExt = setOf("mp4", "mkv", "avi", "webm", "mov", "3gp", "m4v")

    private val usbRoots = listOf(
        "/storage/usbotg/usbotg-otg1",
        "/storage/usbotg",
        "/storage/usb0",
        "/mnt/usb_storage",
    )
    private val fallbackRoots = listOf(
        "/sdcard/Music",
        "/sdcard/Movies",
        "/sdcard/Download",
        "/storage/emulated/0/Music",
        "/storage/emulated/0/Movies",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        adapter = MediaAdapter()
        findViewById<ListView>(R.id.listMedia).adapter = adapter
        findViewById<ListView>(R.id.listMedia).setOnItemClickListener { _, _, position, _ ->
            playMedia(filteredItems()[position])
        }

        findViewById<TextView>(R.id.chipAll).setOnClickListener { setFilter(null) }
        findViewById<TextView>(R.id.chipMusic).setOnClickListener { setFilter(MediaType.MUSIC) }
        findViewById<TextView>(R.id.chipVideo).setOnClickListener { setFilter(MediaType.VIDEO) }

        findViewById<View>(R.id.buttonOverflow).setOnClickListener(::showOverflowMenu)

        scanMedia()
        setFilter(null)
    }

    private fun showOverflowMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add("Обратная связь")
        popup.setOnMenuItemClickListener {
            FeedbackLauncher.launch(this, packageName, appVersionName())
            true
        }
        popup.show()
    }

    private fun appVersionName(): String = runCatching {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "0.0.0"
    }.getOrDefault("0.0.0")

    private fun scanMedia() {
        all.clear()
        val usbFound = usbRoots.any { File(it).exists() }
        val roots = if (usbFound) usbRoots else fallbackRoots
        for (rootPath in roots) {
            val root = File(rootPath)
            if (!root.exists()) continue
            scanDir(root, 0)
        }

        // Status
        findViewById<TextView>(R.id.textUsbStatus).text =
            if (usbFound) "USB подключена" else "USB не найдена · показываются файлы устройства"

        val music = all.count { it.type == MediaType.MUSIC }
        val video = all.count { it.type == MediaType.VIDEO }
        val totalMb = all.sumOf { it.file.length() } / (1024 * 1024)
        findViewById<TextView>(R.id.textStats).text =
            "$music муз. · $video видео · $totalMb МБ"

        findViewById<TextView>(R.id.textSubtitle).text =
            if (all.isEmpty()) "Медиа не найдены" else "Найдено ${all.size} файлов"

        refreshList()
    }

    private fun scanDir(dir: File, depth: Int) {
        if (depth > 4 || all.size > 500) return
        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.isDirectory) {
                scanDir(child, depth + 1)
            } else {
                val ext = child.extension.lowercase()
                val type = when (ext) {
                    in musicExt -> MediaType.MUSIC
                    in videoExt -> MediaType.VIDEO
                    else -> null
                }
                if (type != null) all += MediaItem(child, type)
            }
        }
    }

    private fun setFilter(type: MediaType?) {
        filter = type
        val chipAll = findViewById<TextView>(R.id.chipAll)
        val chipMusic = findViewById<TextView>(R.id.chipMusic)
        val chipVideo = findViewById<TextView>(R.id.chipVideo)
        chipAll.isSelected = type == null
        chipMusic.isSelected = type == MediaType.MUSIC
        chipVideo.isSelected = type == MediaType.VIDEO
        refreshList()
    }

    private fun filteredItems(): List<MediaItem> =
        if (filter == null) all else all.filter { it.type == filter }

    private fun refreshList() {
        adapter.notifyDataSetChanged()
        val items = filteredItems()
        findViewById<ListView>(R.id.listMedia).visibility =
            if (items.isEmpty()) View.GONE else View.VISIBLE
        findViewById<View>(R.id.emptyState).visibility =
            if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun playMedia(item: MediaItem) {
        val mime = when (item.type) {
            MediaType.MUSIC -> "audio/*"
            MediaType.VIDEO -> "video/*"
        }
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.fromFile(item.file), mime)
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Нет приложения для воспроизведения: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    inner class MediaAdapter : BaseAdapter() {
        override fun getCount() = filteredItems().size
        override fun getItem(position: Int): Any = filteredItems()[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(this@MainActivity)
                .inflate(R.layout.item_media, parent, false)

            val item = filteredItems()[position]
            view.findViewById<TextView>(R.id.textName).text = item.file.name
            view.findViewById<TextView>(R.id.textPath).text = item.file.parent ?: ""

            val sizeMb = item.file.length() / (1024 * 1024)
            view.findViewById<TextView>(R.id.textSize).text =
                if (sizeMb < 1) "${item.file.length() / 1024} КБ"
                else "$sizeMb МБ"

            val icon = view.findViewById<android.widget.ImageView>(R.id.iconType)
            val holder = view.findViewById<View>(R.id.iconHolder)
            when (item.type) {
                MediaType.MUSIC -> {
                    icon.setImageResource(R.drawable.ic_music)
                    icon.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.accent_purple))
                    holder.setBackgroundResource(R.drawable.icon_circle_purple)
                }
                MediaType.VIDEO -> {
                    icon.setImageResource(R.drawable.ic_video)
                    icon.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.accent_blue))
                    holder.setBackgroundResource(R.drawable.icon_circle_blue)
                }
            }

            return view
        }
    }
}
