package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/**
 * Procedural PCM audio synthesizer for truck diesel engine hum and dual-tone horn.
 * Requires no external audio files or runtime permissions.
 */
class TruckAudioEngine {

  @Volatile private var soundEnabled: Boolean = true
  @Volatile private var volume: Float = 0.55f
  @Volatile private var speedKmh: Float = 0f
  @Volatile private var engineActive: Boolean = false
  @Volatile private var inForeground: Boolean = true
  @Volatile private var hornRemainingSamples: Int = 0

  @Volatile private var runningThread: Boolean = false
  private var audioThread: Thread? = null

  private val sampleRate = 22050
  private val hornDurationSamples = (sampleRate * 0.55f).toInt()

  fun startIfNeeded() {
    if (runningThread) return
    runningThread = true
    audioThread = thread(name = "AssamTruckAudio", isDaemon = true) {
      val minBuf = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
      ).coerceAtLeast(1024)

      val track = try {
        AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setSampleRate(sampleRate)
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(minBuf)
          .setTransferMode(AudioTrack.MODE_STREAM)
          .build()
      } catch (_: Throwable) {
        runningThread = false
        return@thread
      }

      val bufferSizeSamples = 512
      val pcm = ShortArray(bufferSizeSamples)
      var enginePhase = 0f
      var hornPhaseA = 0f
      var hornPhaseB = 0f
      var smoothedFreq = 55f
      var smoothedGain = 0f

      try {
        track.play()
        while (runningThread) {
          val canPlay = inForeground && soundEnabled
          val targetFreq = 48f + speedKmh * 1.35f
          val targetGain = if (canPlay && engineActive) {
            volume * 0.14f * (0.28f + (speedKmh / 95f).coerceIn(0f, 1f))
          } else {
            0f
          }

          for (i in 0 until bufferSizeSamples) {
            smoothedFreq += (targetFreq - smoothedFreq) * 0.002f
            smoothedGain += (targetGain - smoothedGain) * 0.003f

            enginePhase += smoothedFreq / sampleRate
            if (enginePhase >= 1f) enginePhase -= 1f
            // Softened sawtooth + sub-harmonic for warm diesel rumble
            val saw = (enginePhase * 2f - 1f) * 0.65f +
              sin(enginePhase * 2f * PI.toFloat()) * 0.35f
            var sample = saw * smoothedGain

            val hornRem = hornRemainingSamples
            if (hornRem > 0 && canPlay) {
              val progress = 1f - (hornRem.toFloat() / hornDurationSamples.toFloat())
              val env = max(0.01f, (1f - progress) * (1f - progress))
              hornPhaseA += 390f / sampleRate
              if (hornPhaseA >= 1f) hornPhaseA -= 1f
              hornPhaseB += 293f / sampleRate
              if (hornPhaseB >= 1f) hornPhaseB -= 1f
              val sqA = if (hornPhaseA < 0.5f) 1f else -1f
              val sqB = if (hornPhaseB < 0.5f) 1f else -1f
              sample += (sqA * 0.6f + sqB * 0.4f) * (volume * 0.16f * env)
              hornRemainingSamples = hornRem - 1
            }

            val clamped = sample.coerceIn(-0.95f, 0.95f)
            pcm[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
          }

          track.write(pcm, 0, bufferSizeSamples)
        }
      } catch (_: Throwable) {
        // Ignore audio write interruption on teardown
      } finally {
        try {
          track.stop()
        } catch (_: Throwable) {
        }
        track.release()
      }
    }
  }

  fun updateState(
    speedKmh: Float,
    engineActive: Boolean,
    soundEnabled: Boolean,
    volume: Float,
  ) {
    this.speedKmh = speedKmh
    this.engineActive = engineActive
    this.soundEnabled = soundEnabled
    this.volume = volume.coerceIn(0f, 1f)
  }

  fun setForeground(foreground: Boolean) {
    inForeground = foreground
  }

  fun playHorn() {
    if (!soundEnabled || !inForeground) return
    startIfNeeded()
    hornRemainingSamples = hornDurationSamples
  }

  fun release() {
    runningThread = false
    audioThread?.interrupt()
    audioThread = null
  }
}
