package com.cxp.learningvideo

import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cxp.learningvideo.databinding.ActivityFfRepackBinding
import kotlin.concurrent.thread


/**
 * FFmpeg 音视频重打包
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @since LearningVideo
 * @version LearningVideo
 * @Datetime 2020-08-02 14:27
 *
 */
class FFEncodeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFfRepackBinding

    private var ffEncoder: Long = -1

    private val srcPath = Environment.getExternalStorageDirectory().absolutePath + "/mvtest2.mp4"
    private val destPath = Environment.getExternalStorageDirectory().absolutePath + "/mvtest_en_out.mp4"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFfRepackBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btn.text = "开始编码"
        ffEncoder = initEncoder(srcPath, destPath)
    }

    fun onStartClick(view: View) {
        if (ffEncoder != 0L) {
            startEncoder(ffEncoder)
            Toast.makeText(this, "开始编码", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        if (ffEncoder > 0) {
            releaseEncoder(ffEncoder)
        }
        super.onDestroy()
    }

    private external fun initEncoder(srcPath: String, destPath: String): Long

    private external fun startEncoder(encoder: Long)

    private external fun releaseEncoder(encoder: Long)

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }
}
