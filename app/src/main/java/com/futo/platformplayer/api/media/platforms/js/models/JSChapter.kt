package com.futo.platformplayer.api.media.platforms.js.models

import com.caoccao.javet.values.reference.V8ValueArray
import com.caoccao.javet.values.reference.V8ValueObject
import com.futo.platformplayer.api.media.models.chapters.ChapterType
import com.futo.platformplayer.api.media.models.chapters.IChapter
import com.futo.platformplayer.api.media.models.chapters.TimelineColor
import com.futo.platformplayer.api.media.platforms.js.SourcePluginConfig
import com.futo.platformplayer.engine.IV8PluginConfig
import com.futo.platformplayer.getOrDefault
import com.futo.platformplayer.getOrThrow

class JSChapter : IChapter {
    override val name: String;
    override val type: ChapterType;
    override val timeStart: Double;
    override val timeEnd: Double;
    override val timelineColor: Int?;

    constructor(name: String, timeStart: Double, timeEnd: Double, type: ChapterType = ChapterType.NORMAL, timelineColor: Int? = null) {
        this.name = name;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.type = type;
        this.timelineColor = timelineColor;
    }


    companion object {
        fun fromV8(config: IV8PluginConfig, obj: V8ValueObject): IChapter {
            val context = "Chapter";

            val name = obj.getOrThrow<String>(config,"name", context);
            val type = ChapterType.fromInt(obj.getOrDefault<Int>(config, "type", context, ChapterType.NORMAL.value) ?: ChapterType.NORMAL.value);
            val timeStart = obj.getOrThrow<Double>(config, "timeStart", context);
            val timeEnd = obj.getOrThrow<Double>(config, "timeEnd", context);
            val timelineColor = TimelineColor.parse(obj.getOrDefault<String>(config, "timelineColor", context, null));

            return JSChapter(name, timeStart, timeEnd, type, timelineColor);
        }

        fun fromV8(config: IV8PluginConfig, arr: V8ValueArray): List<IChapter> {
            return arr.keys.mapNotNull {
                val obj = arr.get<V8ValueObject>(it);
                return@mapNotNull fromV8(config, obj);
            };
        }
    }
}
