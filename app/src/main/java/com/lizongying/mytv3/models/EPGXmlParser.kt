package com.lizongying.mytv0.models

import android.util.Xml
import com.lizongying.mytv0.Utils.getDateTimestamp
import com.lizongying.mytv0.data.EPG
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale


class EPGXmlParser {

    private val ns: String? = null
    private val channelMap = mutableMapOf<String, String>()
    private val epg = mutableMapOf<String, MutableList<EPG>>()
    private val dateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.getDefault())
    private val now = getDateTimestamp()

    private fun formatFTime(s: String): Int {
        return try {
            dateFormat.parse(s)?.time?.div(1000)?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun parse(inputStream: InputStream): Map<String, List<EPG>> {
        inputStream.use { input ->
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(input, null)
            
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        CHANNEL_TAG -> {
                            val id = parser.getAttributeValue(ns, ID_ATTRIBUTE)
                            if (id != null) {
                                var displayName = ""
                                while (parser.next() != XmlPullParser.END_TAG || parser.name != CHANNEL_TAG) {
                                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == DISPLAY_NAME_TAG) {
                                        displayName = parser.nextText()
                                    }
                                }
                                if (displayName.isNotEmpty()) {
                                    channelMap[id] = displayName
                                }
                            }
                        }
                        PROGRAMME_TAG -> {
                            val channelId = parser.getAttributeValue(ns, CHANNEL_ATTRIBUTE)
                            val start = parser.getAttributeValue(ns, START_ATTRIBUTE)
                            val stop = parser.getAttributeValue(ns, STOP_ATTRIBUTE)
                            
                            if (channelId != null && start != null && stop != null) {
                                var title = ""
                                while (parser.next() != XmlPullParser.END_TAG || parser.name != PROGRAMME_TAG) {
                                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == TITLE_TAG) {
                                        title = parser.nextText()
                                    }
                                }
                                
                                val stopTime = formatFTime(stop)
                                if (stopTime > now && title.isNotEmpty()) {
                                    val name = channelMap[channelId] ?: channelId
                                    if (!epg.containsKey(name)) {
                                        epg[name] = mutableListOf()
                                    }
                                    epg[name]?.add(EPG(title, formatFTime(start), stopTime))
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        }

        return epg
    }

    companion object {
        private const val CHANNEL_TAG = "channel"
        private const val PROGRAMME_TAG = "programme"
        private const val DISPLAY_NAME_TAG = "display-name"
        private const val TITLE_TAG = "title"
        private const val ID_ATTRIBUTE = "id"
        private const val CHANNEL_ATTRIBUTE = "channel"
        private const val START_ATTRIBUTE = "start"
        private const val STOP_ATTRIBUTE = "stop"
    }
}