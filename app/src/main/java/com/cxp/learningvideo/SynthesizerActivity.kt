package com.cxp.learningvideo

import android.os.Bundle
import android.os.Environment
import android.view.Surface
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.cxp.learningvideo.databinding.ActivitySynthesizerBinding
import com.cxp.learningvideo.media.BaseDecoder
import com.cxp.learningvideo.media.Frame
import com.cxp.learningvideo.media.IDecoder
import com.cxp.learningvideo.media.decoder.AudioDecoder
import com.cxp.learningvideo.media.decoder.DefDecodeStateListener
import com.cxp.learningvideo.media.decoder.VideoDecoder
import com.cxp.learningvideo.media.encoder.AudioEncoder
import com.cxp.learningvideo.media.encoder.BaseEncoder
import com.cxp.learningvideo.media.encoder.DefEncodeStateListener
import com.cxp.learningvideo.media.encoder.VideoEncoder
import com.cxp.learningvideo.media.muxer.MMuxer
import com.cxp.learningvideo.opengl.drawer.VideoDrawer
import com.cxp.learningvideo.opengl.egl.CustomerGLRenderer
import java.util.concurrent.Executors


/**
 * 合成器页面
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @since LearningVideo
 * @version LearningVideo
 *
 */
class SynthesizerActivity : AppCompatActivity(), MMuxer.IMuxerStateListener {

    private lateinit var binding: ActivitySynthesizerBinding

    private val path = Environment.getExternalStorageDirectory().absolutePath + "/mvtest.mp4"

    private val threadPool = Executors.newFixedThreadPool(10)

    private var renderer = CustomerGLRenderer()

    private var audioDecoder: IDecoder? = null
    private var videoDecoder: IDecoder? = null

    private lateinit var videoEncoder: VideoEncoder
    private lateinit var audioEncoder: AudioEncoder

    private var muxer = MMuxer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySynthesizerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        muxer.setStateListener(this)
    }

    fun onStartClick(view: View) {
        binding.btn.text = "正在编码"
        binding.btn.isEnabled = false
        initVideo()
        initAudio()
        initAudioEncoder()
        initVideoEncoder()
    }

    private fun initVideoEncoder() {
        videoEncoder = VideoEncoder(muxer, 1920, 1080)

        renderer.setRenderMode(CustomerGLRenderer.RenderMode.RENDER_WHEN_DIRTY)
        renderer.setSurface(videoEncoder.getEncodeSurface()!!, 1920, 1080)

        videoEncoder.setStateListener(object : DefEncodeStateListener {
            override fun encoderFinish(encoder: BaseEncoder) {
                renderer.stop()
            }
        })
        threadPool.execute(videoEncoder)
    }

    private fun initAudioEncoder() {
        audioEncoder = AudioEncoder(muxer)
        threadPool.execute(audioEncoder)
    }

    private fun initVideo() {
        val drawer = VideoDrawer()
        drawer.setVideoSize(1920, 1080)
        drawer.getSurfaceTexture {
            initVideoDecoder(path, Surface(it))
        }
        renderer.addDrawer(drawer)
    }

    private fun initVideoDecoder(path: String, sf: Surface) {
        videoDecoder?.stop()
        videoDecoder = VideoDecoder(path, null, sf).withoutSync()
        videoDecoder!!.setStateListener(object : DefDecodeStateListener {
            override fun decodeOneFrame(decodeJob: BaseDecoder?, frame: Frame) {
                renderer.notifySwap(frame.bufferInfo.presentationTimeUs)
                videoEncoder.encodeOneFrame(frame)
            }

            override fun decoderFinish(decodeJob: BaseDecoder?) {
                videoEncoder.endOfStream()
            }
        })
        videoDecoder!!.goOn()
        threadPool.execute(videoDecoder!!)
    }

    private fun initAudio() {
        audioDecoder?.stop()
        audioDecoder = AudioDecoder(path).withoutSync()
        audioDecoder!!.setStateListener(object : DefDecodeStateListener {

            override fun decodeOneFrame(decodeJob: BaseDecoder?, frame: Frame) {
                audioEncoder.encodeOneFrame(frame)
            }

            override fun decoderFinish(decodeJob: BaseDecoder?) {
                audioEncoder.endOfStream()
            }
        })
        audioDecoder!!.goOn()
        threadPool.execute(audioDecoder!!)
    }

    override fun onMuxerFinish() {
        runOnUiThread {
            binding.btn.isEnabled = true
            binding.btn.text = "编码完成"
        }

        audioDecoder?.stop()
        audioDecoder = null

        videoDecoder?.stop()
        videoDecoder = null
    }
}
