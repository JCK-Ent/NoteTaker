package com.jckent.notetaker.ui.editor

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipInputStream

object VoskProvider : TranscriptionProvider {

    override val id = "vosk"
    override val displayName = "Offline (Vosk) — no internet needed"
    override val description = "Bundled in the app. No API key. Does not separate speakers."
    override val requiresApiKey = false
    override val hasSpeakerLabels = false

    private const val TARGET_HZ = 16_000
    private const val MODEL_ZIP_ASSET = "vosk-model-small-en-us-0.15.zip"
    private const val MODEL_DIR_NAME = "vosk_model"

    private var cachedModel: Model? = null

    override suspend fun transcribe(context: Context, file: File, apiKey: String): String =
        withContext(Dispatchers.IO) {
            val model = getOrLoadModel(context)
            val (pcm, srcHz, channels) = decodeM4A(file)
            val mono = if (channels == 2) downmixToMono(pcm) else pcm
            val pcm16k = if (srcHz == TARGET_HZ) mono else resample(mono, srcHz, TARGET_HZ)

            Recognizer(model, TARGET_HZ.toFloat()).use { rec ->
                val chunkSize = 8_000
                var i = 0
                while (i < pcm16k.size) {
                    val end = minOf(i + chunkSize, pcm16k.size)
                    val chunk = pcm16k.copyOfRange(i, end)
                    rec.acceptWaveForm(chunk, chunk.size)
                    i = end
                }
                JSONObject(rec.finalResult).optString("text", "").trim()
            }
        }

    private fun getOrLoadModel(context: Context): Model {
        cachedModel?.let { return it }
        val modelDir = File(context.filesDir, MODEL_DIR_NAME)
        if (!modelDir.exists() || modelDir.list().isNullOrEmpty()) {
            extractModelAsset(context, modelDir)
        }
        return Model(modelDir.absolutePath).also { cachedModel = it }
    }

    private fun extractModelAsset(context: Context, dest: File) {
        dest.mkdirs()
        context.assets.open(MODEL_ZIP_ASSET).use { assetIn ->
            ZipInputStream(assetIn).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    // Strip the top-level folder name from zip paths
                    val relative = entry.name.substringAfter("/")
                    if (relative.isNotEmpty()) {
                        val target = File(dest, relative)
                        if (entry.isDirectory) target.mkdirs()
                        else { target.parentFile?.mkdirs(); target.outputStream().use { zip.copyTo(it) } }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
    }

    private data class PCMResult(val data: ShortArray, val sampleRate: Int, val channels: Int)

    private fun decodeM4A(file: File): PCMResult {
        val extractor = MediaExtractor()
        extractor.setDataSource(file.absolutePath)

        var trackIdx = -1
        for (i in 0 until extractor.trackCount) {
            if (extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                trackIdx = i; break
            }
        }
        require(trackIdx >= 0) { "No audio track found in ${file.name}" }

        val fmt = extractor.getTrackFormat(trackIdx)
        val srcHz = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channels = if (fmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
            fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 1
        val mime = fmt.getString(MediaFormat.KEY_MIME)!!
        extractor.selectTrack(trackIdx)

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(fmt, null, null, 0)
        codec.start()

        val rawOut = ByteArrayOutputStream()
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false

        while (!outputDone) {
            if (!inputDone) {
                val inIdx = codec.dequeueInputBuffer(10_000)
                if (inIdx >= 0) {
                    val buf = codec.getInputBuffer(inIdx)!!
                    val n = extractor.readSampleData(buf, 0)
                    if (n < 0) {
                        codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inIdx, 0, n, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            when (val outIdx = codec.dequeueOutputBuffer(info, 10_000)) {
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED, MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                else -> if (outIdx >= 0) {
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    if (info.size > 0) {
                        val buf = codec.getOutputBuffer(outIdx)!!
                        val bytes = ByteArray(info.size)
                        buf.get(bytes)
                        rawOut.write(bytes)
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                }
            }
        }
        codec.stop(); codec.release(); extractor.release()

        val raw = rawOut.toByteArray()
        val shorts = ShortArray(raw.size / 2)
        ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
        return PCMResult(shorts, srcHz, channels)
    }

    private fun downmixToMono(stereo: ShortArray): ShortArray =
        ShortArray(stereo.size / 2) { i -> ((stereo[i * 2] + stereo[i * 2 + 1]) / 2).toShort() }

    private fun resample(input: ShortArray, srcHz: Int, dstHz: Int): ShortArray {
        val ratio = srcHz.toDouble() / dstHz
        val size = (input.size / ratio).toInt()
        return ShortArray(size) { i -> input[(i * ratio).toInt().coerceAtMost(input.size - 1)] }
    }
}
