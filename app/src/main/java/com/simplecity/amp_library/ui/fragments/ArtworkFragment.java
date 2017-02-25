package com.simplecity.amp_library.ui.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.crashlytics.android.Crashlytics;
import com.jp.wasabeef.glide.transformations.BlurTransformation;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.glide.utils.GlideUtils;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.ui.activities.PlayerActivity;
import com.simplecity.amp_library.utils.SettingsManager;

import java.lang.ref.WeakReference;

public class ArtworkFragment extends BaseFragment {

    private static final String TAG = "ArtworkFragment";

    private static final String ARG_SONG = "song";

    private ImageView imageView;

    private Target blurTarget;

    private Song song;

    public static ArtworkFragment newInstance(Song song) {
        ArtworkFragment fragment = new ArtworkFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_SONG, song);
        fragment.setArguments(args);
        return fragment;
    }

    public ArtworkFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        song = (Song) getArguments().getSerializable(ARG_SONG);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_artwork, container, false);

        GestureDetector gestureDetector = new GestureDetector(this.getActivity(), new GestureListener(this));

        imageView = (ImageView) rootView.findViewById(R.id.image);
        imageView.setOnTouchListener((view, motionEvent) -> gestureDetector.onTouchEvent(motionEvent));

        updateArtwork();

        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (imageView != null) {
            Glide.clear(imageView);
        }

        if (blurTarget != null) {
            Glide.clear(blurTarget);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        updateArtwork();
    }

    private void updateArtwork() {

        RequestManager requestManager = null;
        if (getParentFragment() != null && getParentFragment() instanceof RequestManagerProvider) {
            requestManager = ((RequestManagerProvider) (getParentFragment())).getRequestManager();
        }
        // There's currently no situation where this will happen, but this keeps the ArtworkFragment
        // decoupled from its parent.
        if (requestManager == null) {
            requestManager = Glide.with(this);
        }

        if (imageView != null && song != null) {
            DrawableRequestBuilder builder = requestManager
                    .load(song)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .placeholder(GlideUtils.getLargePlaceHolderResId());
            if (SettingsManager.getInstance().cropArtwork()) {
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
            try {
                builder.into(imageView);
            } catch (IllegalArgumentException e) {
                Crashlytics.log("ArtworkFragment load normal image failed: " + e.getMessage());
            }

            if (!SettingsManager.getInstance().cropArtwork()) {
                try {
                    blurTarget = requestManager
                            .load(song)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .bitmapTransform(new BlurTransformation(getContext()))
                            .override(100, 100)
                            .into(new Target(imageView));
                } catch (IllegalArgumentException e) {
                    Crashlytics.log("ArtworkFragment load blur image failed: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        if (imageView != null) {
            imageView.setOnTouchListener(null);
        }
        super.onDestroy();
    }

    private static class Target extends SimpleTarget<GlideDrawable> {

        private WeakReference<ImageView> imageViewWeakReference;

        public Target(ImageView imageView) {
            imageViewWeakReference = new WeakReference<>(imageView);
        }

        @Override
        public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
            ImageView imageView = imageViewWeakReference.get();
            if (imageView != null) {
                imageView.setBackground(resource);
            }
        }
    }

    private static class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private WeakReference<Fragment> fragmentWeakReference;

        public GestureListener(Fragment fragment) {
            fragmentWeakReference = new WeakReference<>(fragment);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {

            Fragment fragment = fragmentWeakReference.get();
            if (fragment == null) {
                return false;
            }

            Activity activity = fragment.getActivity();
            if (activity instanceof PlayerActivity) {
                ((PlayerActivity) activity).toggleLyrics();
                return true;
            } else {
                Fragment parentFragment = fragment.getParentFragment();
                Fragment playingFragment = null;
                if (parentFragment != null) {
                    playingFragment = parentFragment.getParentFragment();
                }
                if (playingFragment != null && playingFragment instanceof PlayerFragment) {
                    ((PlayerFragment) playingFragment).toggleLyrics();
                }
            }
            return false;
        }
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
