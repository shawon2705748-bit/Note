package com.example.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val handler = Handler(Looper.getMainLooper())
    private var transitionRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPoweredByText()
        startAnimations()

        transitionRunnable = Runnable {
            if (!isFinishing && !isDestroyed) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        }
        handler.postDelayed(transitionRunnable!!, 1300)
    }

    private fun setupPoweredByText() {
        val prefix = getString(R.string.powered_by_prefix)
        val brand = getString(R.string.brand_name)
        val fullText = "$prefix$brand"

        val spannable = SpannableString(fullText)
        val brandStart = prefix.length
        val brandEnd = fullText.length

        val purpleColor = ContextCompat.getColor(this, R.color.purple_accent_light)
        spannable.setSpan(
            ForegroundColorSpan(purpleColor),
            brandStart,
            brandEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            brandStart,
            brandEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.tvPoweredBy.text = spannable
    }

    private fun startAnimations() {
        binding.containerCenterLogo.alpha = 0f
        binding.containerCenterLogo.scaleX = 0.88f
        binding.containerCenterLogo.scaleY = 0.88f

        binding.tvPoweredBy.alpha = 0f

        binding.containerCenterLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(700)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        binding.tvPoweredBy.animate()
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(250)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    override fun onDestroy() {
        super.onDestroy()
        transitionRunnable?.let { handler.removeCallbacks(it) }
    }
}
