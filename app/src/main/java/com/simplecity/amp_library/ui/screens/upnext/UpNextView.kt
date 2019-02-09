package com.simplecity.amp_library.ui.screens.upnext

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.util.Pair
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import com.afollestad.aesthetic.Aesthetic
import com.afollestad.aesthetic.ColorIsDarkState
import com.afollestad.aesthetic.LightDarkColorState
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.jakewharton.rxbinding2.widget.RxSeekBar
import com.jakewharton.rxbinding2.widget.SeekBarChangeEvent
import com.jakewharton.rxbinding2.widget.SeekBarProgressChangeEvent
import com.jakewharton.rxbinding2.widget.SeekBarStartChangeEvent
import com.jakewharton.rxbinding2.widget.SeekBarStopChangeEvent
import com.simplecity.amp_library.R
import com.simplecity.amp_library.ShuttleApplication
import com.simplecity.amp_library.glide.palette.ColorSet
import com.simplecity.amp_library.glide.palette.ColorSetTranscoder
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.rx.UnsafeAction
import com.simplecity.amp_library.rx.UnsafeConsumer
import com.simplecity.amp_library.ui.screens.nowplaying.PlayerPresenter
import com.simplecity.amp_library.ui.views.PlayerViewAdapter
import com.simplecity.amp_library.ui.views.RepeatButton
import com.simplecity.amp_library.ui.views.ShuffleButton
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.SettingsManager
import com.simplecity.amp_library.utils.ShuttleUtils
import com.simplecity.amp_library.utils.color.ArgbEvaluator
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import kotlinx.android.synthetic.main.up_next_view.view.arrowImageView
import kotlinx.android.synthetic.main.up_next_view.view.buttonContainer
import kotlinx.android.synthetic.main.up_next_view.view.nextButton
import kotlinx.android.synthetic.main.up_next_view.view.playPauseView
import kotlinx.android.synthetic.main.up_next_view.view.prevButton
import kotlinx.android.synthetic.main.up_next_view.view.queuePositionTextView
import kotlinx.android.synthetic.main.up_next_view.view.queueTextView
import kotlinx.android.synthetic.main.up_next_view.view.repeatButton
import kotlinx.android.synthetic.main.up_next_view.view.seekBar
import kotlinx.android.synthetic.main.up_next_view.view.shuffleButton
import kotlinx.android.synthetic.main.up_next_view.view.textContainer
import java.util.concurrent.TimeUnit

class UpNextView constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var playerPresenter: PlayerPresenter

    private lateinit var settingsManager: SettingsManager

    private var isSeeking: Boolean = false

    private val arrowDrawable: Drawable

    private val disposables = CompositeDisposable()

    private val playerViewAdapter: PlayerViewAdapter

    private var isLandscape: Boolean = false

    private var colorAnimator: ValueAnimator? = null

    private var colorSet = ColorSet.empty()

    init {
        orientation = LinearLayout.HORIZONTAL

        View.inflate(context, R.layout.up_next_view, this)

        arrowDrawable = DrawableCompat.wrap(arrowImageView.drawable)
        arrowImageView.setImageDrawable(arrowDrawable)

        playPauseView?.setOnClickListener { v ->
            playPauseView?.toggle {
                playerPresenter.togglePlayback()
            }
        }

        repeatButton?.setOnClickListener { playerPresenter.toggleRepeat() }
        repeatButton?.tag = ":aesthetic_ignore"

        shuffleButton?.setOnClickListener { playerPresenter.toggleShuffle() }
        shuffleButton?.tag = ":aesthetic_ignore"

        nextButton?.setOnClickListener { playerPresenter.skip() }
        nextButton?.setRepeatListener { _, duration, repeatCount -> playerPresenter.scanForward(repeatCount, duration) }

        prevButton?.setOnClickListener { playerPresenter.prev(false) }
        prevButton?.setRepeatListener { _, duration, repeatCount -> playerPresenter.scanBackward(repeatCount, duration) }

        seekBar?.max = 1000

        playerViewAdapter = object : PlayerViewAdapter() {

            override fun queueChanged(queuePosition: Int, queueLength: Int) {
                super.queueChanged(queuePosition, queueLength)

                queuePositionTextView.text = String.format("%d / %d", queuePosition, queueLength)
            }

            override fun playbackChanged(isPlaying: Boolean) {
                playPauseView?.let { playPauseView ->
                    if (isPlaying) {
                        if (playPauseView.isPlay) {
                            playPauseView.toggle(null)
                            playPauseView.contentDescription = getContext().getString(R.string.btn_pause)
                        }
                    } else {
                        if (!playPauseView.isPlay) {
                            playPauseView.toggle(null)
                            playPauseView.contentDescription = getContext().getString(R.string.btn_play)
                        }
                    }
                }
            }

            override fun shuffleChanged(shuffleMode: Int) {
                (shuffleButton as? ShuffleButton)?.setShuffleMode(shuffleMode)
            }

            override fun repeatChanged(repeatMode: Int) {
                (repeatButton as? RepeatButton)?.setRepeatMode(repeatMode)
            }

            override fun trackInfoChanged(song: Song?) {
                super.trackInfoChanged(song)

                if (isLandscape && settingsManager.usePalette) {

                    Glide.clear(paletteTarget)

                    Glide.with(getContext())
                        .load(song)
                        .asBitmap()
                        .transcode(ColorSetTranscoder(getContext()), ColorSet::class.java)
                        .override(250, 250)
                        .priority(Priority.HIGH)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(paletteTarget)
                }
            }

            override fun setSeekProgress(progress: Int) {
                if (!isSeeking) {
                    seekBar?.progress = progress
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        isLandscape = ShuttleUtils.isLandscape(context.applicationContext as ShuttleApplication)

        playerPresenter.bindView(playerViewAdapter)

        if (!isLandscape) {

            disposables.add(
                Observable.combineLatest(
                    Aesthetic.get(context).textColorPrimary(),
                    Aesthetic.get(context).textColorPrimaryInverse(),
                    Observable.just(false),
                    LightDarkColorState.creator()
                )
                    .subscribe { colorState ->
                        DrawableCompat.setTint(arrowDrawable, colorState.color())
                    })

        }

        disposables.add(Aesthetic.get(context).isDark
            .map { isDark -> isLandscape && !isDark }
            .flatMap { isDark -> if (isDark) Aesthetic.get(context).textColorPrimaryInverse() else Aesthetic.get(context).textColorPrimary() }
            .subscribe { color -> queueTextView.setTextColor(color) })

        disposables.add(Aesthetic.get(context).isDark
            .map { isDark -> isLandscape && !isDark }
            .flatMap { isDark -> if (isDark) Aesthetic.get(context).textColorSecondaryInverse() else Aesthetic.get(context).textColorSecondary() }
            .subscribe { color -> queuePositionTextView.setTextColor(color) })

        if (isLandscape) {

            var observable = getAestheticColorSetDisposable()
            // If we're managing the color scheme ourselves based on artwork changes, we only need the first ColorSet
            // emission, as no artwork has been loaded yet.
            if (settingsManager.usePalette || settingsManager.usePaletteNowPlayingOnly) {
                observable = observable.take(1)
            }

            disposables.add(
                observable.subscribe(
                    { colorSet ->
                        if (this@UpNextView.colorSet == colorSet) {
                            invalidateColors(colorSet)
                        } else {
                            animateColors(this@UpNextView.colorSet, colorSet, 800, UnsafeConsumer { this.invalidateColors(it) }, null)
                        }
                    },
                    { _ ->
                        // Nothing to do
                    })
            )
        }

        seekBar?.let { seekBar ->
            val sharedSeekBarEvents = RxSeekBar.changeEvents(seekBar)
                .toFlowable(BackpressureStrategy.LATEST)
                .ofType(SeekBarChangeEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .share()

            disposables.add(sharedSeekBarEvents.subscribe({ seekBarChangeEvent ->
                if (seekBarChangeEvent is SeekBarStartChangeEvent) {
                    isSeeking = true
                } else if (seekBarChangeEvent is SeekBarStopChangeEvent) {
                    isSeeking = false
                }
            }, { error -> LogUtils.logException(TAG, "Error in seek change event", error) }))

            disposables.add(
                sharedSeekBarEvents
                    .ofType(SeekBarProgressChangeEvent::class.java)
                    .filter { it.fromUser() }
                    .debounce(15, TimeUnit.MILLISECONDS)
                    .subscribe({ seekBarChangeEvent -> playerPresenter.seekTo(seekBarChangeEvent.progress()) },
                        { error -> LogUtils.logException(TAG, "Error receiving seekbar progress", error) })
            )
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        playerPresenter.unbindView(playerViewAdapter)

        disposables.dispose()
    }

    fun invalidateColors(colorSet: ColorSet) {

        if (isLandscape) {

            var ignorePalette = false
            if (!settingsManager.usePalette && !settingsManager.usePaletteNowPlayingOnly) {
                // If we're not using Palette at all, use non-tinted colors for text.
                colorSet.primaryTextColorTinted = colorSet.primaryTextColor
                colorSet.secondaryTextColorTinted = colorSet.secondaryTextColor
                ignorePalette = true
            }

            buttonContainer?.setBackgroundColor(colorSet.primaryColor)
            textContainer?.setBackgroundColor(colorSet.primaryColor)

            (shuffleButton as? ShuffleButton)?.invalidateColors(colorSet.primaryTextColor, colorSet.primaryTextColorTinted)

            (repeatButton as? RepeatButton)?.invalidateColors(colorSet.primaryTextColor, colorSet.primaryTextColorTinted)

            prevButton?.invalidateColors(colorSet.primaryTextColor)

            nextButton?.invalidateColors(colorSet.primaryTextColor)

            playPauseView?.setDrawableColor(colorSet.primaryTextColor)

            seekBar?.invalidateColors(ColorIsDarkState(if (ignorePalette) colorSet.accentColor else colorSet.primaryTextColorTinted, false))

            queueTextView?.setTextColor(colorSet.primaryTextColor)

            queuePositionTextView?.setTextColor(colorSet.secondaryTextColor)

            DrawableCompat.setTint(arrowDrawable, colorSet.primaryTextColor)

            arrowDrawable
        }

        this.colorSet = colorSet
    }

    private fun animateColors(from: ColorSet, to: ColorSet, duration: Int, consumer: UnsafeConsumer<ColorSet>, onComplete: UnsafeAction?) {
        colorAnimator = ValueAnimator.ofFloat(1f, 0f)
        colorAnimator!!.duration = duration.toLong()
        colorAnimator!!.interpolator = AccelerateDecelerateInterpolator()
        val argbEvaluator = ArgbEvaluator.getInstance()
        colorAnimator!!.addUpdateListener { animator ->
            val colorSet = ColorSet(
                argbEvaluator.evaluate(animator.animatedFraction, from.primaryColor, to.primaryColor) as Int,
                argbEvaluator.evaluate(animator.animatedFraction, from.accentColor, to.accentColor) as Int,
                argbEvaluator.evaluate(animator.animatedFraction, from.primaryTextColorTinted, to.primaryTextColorTinted) as Int,
                argbEvaluator.evaluate(animator.animatedFraction, from.secondaryTextColorTinted, to.secondaryTextColorTinted) as Int,
                argbEvaluator.evaluate(animator.animatedFraction, from.primaryTextColor, to.primaryTextColor) as Int,
                argbEvaluator.evaluate(animator.animatedFraction, from.secondaryTextColor, to.secondaryTextColor) as Int
            )
            consumer.accept(colorSet)
        }
        colorAnimator!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                animation.removeAllListeners()
                onComplete?.run()
            }
        })
        colorAnimator!!.start()
    }

    private fun getAestheticColorSetDisposable(): Observable<ColorSet> {
        return Observable.combineLatest(
            Aesthetic.get(context).colorPrimary(),
            Aesthetic.get(context).colorAccent(),
            BiFunction { first: Int, second: Int -> Pair(first, second) }
        ).map { pair -> ColorSet.fromPrimaryAccentColors(context!!, pair.first!!, pair.second!!) }
    }

    private val paletteTarget = object : SimpleTarget<ColorSet>() {
        override fun onResourceReady(newColorSet: ColorSet, glideAnimation: GlideAnimation<in ColorSet>) {

            if (colorSet === newColorSet) {
                return
            }

            val oldColorSet = colorSet

            animateColors(
                oldColorSet,
                newColorSet,
                800,
                UnsafeConsumer { intermediateColorSet ->
                    // Update all the colours related to the now playing screen first
                    invalidateColors(intermediateColorSet)

                    // We need to update the nav bar colour at the same time, since it's visible as well.
                    if (settingsManager.tintNavBar) {
                        Aesthetic.get(getContext()).colorNavigationBar(intermediateColorSet.primaryColor).apply()
                    }
                },
                UnsafeAction {
                    // Wait until the first set of color change animations is complete, before updating Aesthetic.
                    // This allows our invalidateColors() animation to run smoothly, as the Aesthetic color change
                    // introduces some jank.
                    if (!settingsManager.usePaletteNowPlayingOnly) {

                        animateColors(oldColorSet, newColorSet, 450, UnsafeConsumer { intermediateColorSet ->
                            val aesthetic = Aesthetic.get(getContext())
                                .colorPrimary(intermediateColorSet.primaryColor)
                                .colorAccent(intermediateColorSet.accentColor)
                                .colorStatusBarAuto()

                            aesthetic.apply()
                        }, null)
                    }
                }
            )
        }

        @SuppressLint("CheckResult")
        override fun onLoadFailed(e: Exception?, errorDrawable: Drawable?) {
            super.onLoadFailed(e, errorDrawable)

            getAestheticColorSetDisposable()
                .take(1)
                .subscribe(
                    { colorSet -> animateColors(this@UpNextView.colorSet, colorSet, 800, UnsafeConsumer { intermediateColorSet -> invalidateColors(intermediateColorSet) }, null) },
                    { _ ->
                        // Nothing ot do
                    }
                )
        }
    }

    companion object {

        private val TAG = "UpNextView"

        fun newInstance(context: Context, playerPresenter: PlayerPresenter, settingsManager: SettingsManager): UpNextView {
            val upNextView = UpNextView(context)
            upNextView.playerPresenter = playerPresenter
            upNextView.settingsManager = settingsManager
            return upNextView
        }
    }
}
