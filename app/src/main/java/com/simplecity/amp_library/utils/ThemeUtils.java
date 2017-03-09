package com.simplecity.amp_library.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.annotation.IntDef;
import android.support.design.widget.TextInputLayout;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.StateSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.EdgeEffect;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.doomonafireball.betterpickers.hmspicker.HmsPicker;
import com.doomonafireball.betterpickers.hmspicker.HmsView;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ui.activities.MainActivity;
import com.simplecity.amp_library.ui.activities.PlayerActivity;
import com.simplecity.amp_library.ui.activities.WidgetConfigureExtraLarge;
import com.simplecity.amp_library.ui.activities.WidgetConfigureLarge;
import com.simplecity.amp_library.ui.activities.WidgetConfigureMedium;
import com.simplecity.amp_library.ui.activities.WidgetConfigureSmall;
import com.simplecity.amp_library.ui.views.FilterableStateListDrawable;
import com.simplecity.amp_library.ui.views.SizableSeekBar;
import com.simplecity.amp_library.ui.views.SlidingTabLayout;
import com.simplecity.amp_library.ui.views.Themable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ThemeUtils {

    private static final String TAG = "ThemeUtils";

    private static ThemeUtils sInstance;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({WHITE, BLACK})
    public @interface ThemeColor {
    }

    public static final int WHITE = 0;
    public static final int BLACK = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ThemeType.TYPE_LIGHT, ThemeType.TYPE_DARK, ThemeType.TYPE_BLACK, ThemeType.TYPE_SOLID_LIGHT,
            ThemeType.TYPE_SOLID_DARK, ThemeType.TYPE_SOLID_BLACK})
    public @interface ThemeType {
        int TYPE_LIGHT = 0;
        int TYPE_DARK = 1;
        int TYPE_BLACK = 2;
        int TYPE_SOLID_LIGHT = 3;
        int TYPE_SOLID_DARK = 4;
        int TYPE_SOLID_BLACK = 5;
    }

    public int themeType;

    public static ThemeUtils getInstance() {
        if (sInstance == null) {
            sInstance = new ThemeUtils();
        }
        return sInstance;
    }

    private ThemeUtils() {

    }

    /**
     * @param context the context
     * @return the current {@link com.simplecity.amp_library.utils.ThemeUtils.ThemeType}
     */
    public static int getThemeType(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String theme = sharedPreferences.getString("pref_theme_base", "0");

        if (Integer.valueOf(theme) == 0) {
            return ThemeType.TYPE_SOLID_LIGHT;
        } else if (Integer.valueOf(theme) == 1) {
            return ThemeType.TYPE_SOLID_DARK;
        } else if (Integer.valueOf(theme) == 2) {
            return ThemeType.TYPE_SOLID_BLACK;
        }

        return ThemeType.TYPE_SOLID_LIGHT;
    }

    public boolean isThemeDark() {
        return themeType == ThemeType.TYPE_DARK
                || themeType == ThemeType.TYPE_SOLID_DARK
                || themeType == ThemeType.TYPE_BLACK
                || themeType == ThemeType.TYPE_SOLID_BLACK;
    }


    public static void setTheme(Activity activity) {
        int themeType = getThemeType(activity);
        ThemeUtils.getInstance().themeType = themeType;

        if (activity instanceof PlayerActivity) {
            if (themeType == ThemeType.TYPE_LIGHT || themeType == ThemeType.TYPE_SOLID_LIGHT) {
                activity.setTheme(R.style.AppTheme_Solid_Light_Transparent_ActionBar);
            } else if (themeType == ThemeType.TYPE_DARK || themeType == ThemeType.TYPE_SOLID_DARK) {
                activity.setTheme(R.style.AppTheme_Solid_Dark_Transparent_ActionBar);
            } else if (themeType == ThemeType.TYPE_BLACK || themeType == ThemeType.TYPE_SOLID_BLACK) {
                activity.setTheme(R.style.AppTheme_Solid_Black_Transparent_ActionBar);
            }
            return;
        }

        if (activity instanceof WidgetConfigureMedium || activity instanceof WidgetConfigureExtraLarge
                || activity instanceof WidgetConfigureLarge || activity instanceof WidgetConfigureSmall) {
            activity.setTheme(R.style.AppTheme_Widgets);
            return;
        }

        if (themeType == ThemeType.TYPE_LIGHT) {
            activity.setTheme(R.style.AppTheme_Light);
        } else if (themeType == ThemeType.TYPE_DARK) {
            activity.setTheme(R.style.AppTheme_Dark);
        } else if (themeType == ThemeType.TYPE_SOLID_LIGHT) {
            activity.setTheme(R.style.AppTheme_Solid_Light);
        } else if (themeType == ThemeType.TYPE_SOLID_DARK) {
            activity.setTheme(R.style.AppTheme_Solid_Dark);
        } else if (themeType == ThemeType.TYPE_BLACK) {
            activity.setTheme(R.style.AppTheme_Black);
        } else if (themeType == ThemeType.TYPE_SOLID_BLACK) {
            activity.setTheme(R.style.AppTheme_Solid_Black);
        }
    }

    @SuppressLint("NewApi")
    public static Drawable themeActionBar(AppCompatActivity activity) {

        if (activity == null) {
            return null;
        }

        if (ShuttleUtils.hasLollipop()) {
            Bitmap bitmap = BitmapFactory.decodeResource(activity.getResources(), R.mipmap.ic_launcher);
            if (bitmap != null) {
                ActivityManager.TaskDescription td = new ActivityManager.TaskDescription(null, bitmap, ColorUtils.getPrimaryColor());
                activity.setTaskDescription(td);
                bitmap.recycle();
            }
        }

        if (ThemeUtils.getInstance().themeType == ThemeType.TYPE_LIGHT || ThemeUtils.getInstance().themeType == ThemeType.TYPE_DARK) {
            activity.getSupportActionBar().setBackgroundDrawable(
                    DrawableUtils.getColoredDrawable(activity, CompatUtils.getDrawableCompat(activity, R.drawable.ab_transparent)));
        }
        if (activity instanceof MainActivity || isActionBarSolid(activity)) {
            ActionBar actionBar = activity.getSupportActionBar();
            Drawable actionBarDrawable = DrawableUtils.getColoredDrawable(activity, CompatUtils.getDrawableCompat(activity, R.drawable.action_bar_bg));
            actionBar.setBackgroundDrawable(actionBarDrawable);
            return actionBarDrawable;
        }
        return null;
    }

    public static boolean isActionBarSolid(Context context) {
        return getThemeType(context) == ThemeType.TYPE_SOLID_LIGHT
                || getThemeType(context) == ThemeType.TYPE_SOLID_DARK
                || getThemeType(context) == ThemeType.TYPE_SOLID_BLACK;
    }

    @SuppressLint("NewApi")
    public static void themeStatusBar(Activity activity, SystemBarTintManager tintManager) {

        if (ShuttleUtils.hasKitKat()) {
            if (ShuttleUtils.hasLollipop()) {
                activity.getWindow().setStatusBarColor(ColorUtils.getPrimaryColorDark(activity));
            } else {
                if (tintManager != null) {
                    tintManager.setStatusBarTintEnabled(true);
                    tintManager.setStatusBarTintColor(ColorUtils.getPrimaryColor());
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void themeNavBar(Activity activity, boolean canTheme) {
        if (!ShuttleUtils.hasLollipop()) {
            return;
        }
        if (canTheme) {
            activity.getWindow().setNavigationBarColor(ColorUtils.getPrimaryColorDark(activity));
        } else {
            activity.getWindow().setNavigationBarColor(Color.BLACK);
        }

    }

    public static void themeHmsPicker(HmsPicker picker) {
        if ((ThemeUtils.getInstance().themeType == ThemeType.TYPE_DARK)
                || (ThemeUtils.getInstance().themeType == ThemeType.TYPE_SOLID_DARK)
                || (ThemeUtils.getInstance().themeType == ThemeType.TYPE_SOLID_BLACK)) {
            picker.setTheme(R.style.BetterPickersDialogFragment);
        } else {
            picker.setTheme(R.style.BetterPickersDialogFragment_Light);
        }
    }

    public static void themeHmsView(HmsView hmsView) {
        if ((ThemeUtils.getInstance().themeType == ThemeType.TYPE_DARK)
                || (ThemeUtils.getInstance().themeType == ThemeType.TYPE_SOLID_DARK)
                || (ThemeUtils.getInstance().themeType == ThemeType.TYPE_SOLID_BLACK)) {
            hmsView.setTheme(R.style.BetterPickersDialogFragment);
        } else {
            hmsView.setTheme(R.style.BetterPickersDialogFragment_Light);
        }
    }

    /**
     * Uses reflection to find the FastScroll drawable and themes it
     *
     * @param listView the {@link android.widget.AbsListView} to theme
     */

    public static void setFastscrollDrawable(Context context, AbsListView listView) {
        try {
            Object object;
            java.lang.reflect.Field field;

            if (ShuttleUtils.hasAndroidLPreview()) {
                field = AbsListView.class.getDeclaredField("mFastScroll");
            } else {
                field = AbsListView.class.getDeclaredField("mFastScroller");
            }

            field.setAccessible(true);
            object = field.get(listView);

            //Theme the fastscroll thumb
            if (ShuttleUtils.hasKitKat()) {
                Field thumbField = field.getType().getDeclaredField("mThumbImage");
                thumbField.setAccessible(true);
                ImageView imageView = (ImageView) thumbField.get(object);
                Drawable drawable = DrawableUtils.getColoredFastScrollDrawable(context, false);
                imageView.setImageDrawable(drawable);
                thumbField.set(object, imageView);
            } else {
                Field thumbField = field.getType().getDeclaredField("mThumbDrawable");
                thumbField.setAccessible(true);
                Drawable drawable = DrawableUtils.getColoredFastScrollDrawable(context, false);
                thumbField.set(object, drawable);
            }

            //Theme the SectionIndexer overlay ('Preview Image')
            if (ShuttleUtils.hasLollipop()) {
                Field previewImageField = field.getType().getDeclaredField("mPreviewImage");
                previewImageField.setAccessible(true);
                View view = (View) previewImageField.get(object);
                Drawable drawable = CompatUtils.getDrawableCompat(context, R.drawable.fastscroll_label_right_material);
                drawable.setColorFilter(new LightingColorFilter((ColorUtils.getAccentColor()), 0));
                view.setBackground(drawable);
                previewImageField.set(object, view);
            }

        } catch (Exception ignored) {
        }
    }

    public static void themeSeekBar(Context context, SizableSeekBar seekBar) {
        themeSeekBar(context, seekBar, false);
    }

    public static void themeSeekBar(Context context, SizableSeekBar seekBar, boolean noBackground) {

        int accentColor = ColorUtils.getAccentColor();
        if (noBackground) {
            if (accentColor == ColorUtils.getPrimaryColor()) {
                accentColor = android.graphics.Color.WHITE;
            }
        }

        seekBar.setThumb(DrawableUtils.getColoredDrawable(seekBar.getThumb(), accentColor));

        LayerDrawable progressDrawable = (LayerDrawable) seekBar.getProgressDrawable();
        progressDrawable.setDrawableByLayerId(
                android.R.id.progress, DrawableUtils.getColoredDrawable(progressDrawable.findDrawableByLayerId(android.R.id.progress), accentColor));

        if (!noBackground) {
            int color;
            int themeType = getThemeType(context);
            if (themeType == ThemeType.TYPE_DARK
                    || themeType == ThemeType.TYPE_SOLID_DARK
                    || themeType == ThemeType.TYPE_BLACK
                    || themeType == ThemeType.TYPE_SOLID_BLACK) {
                color = android.graphics.Color.parseColor("#5a5a5a");
            } else {
                color = android.graphics.Color.parseColor("#bfbfbf");
            }
            progressDrawable.setDrawableByLayerId(android.R.id.background, DrawableUtils.getColoredDrawable(progressDrawable.findDrawableByLayerId(android.R.id.background), color));
        } else {
            progressDrawable.setDrawableByLayerId(android.R.id.background, new ColorDrawable(context.getResources().getColor(android.R.color.transparent)));
        }
    }

    /**
     * Get the current base color (light or dark) depending on the theme
     *
     * @param context Context
     * @return The current base color (light or dark) depending on the theme
     */
    public static int getBaseColor(Context context) {
        if (ThemeUtils.getInstance().themeType == ThemeType.TYPE_LIGHT
                || ThemeUtils.getInstance().themeType == ThemeType.TYPE_SOLID_LIGHT) {
            return context.getResources().getColor(R.color.black);
        } else if (ThemeUtils.getInstance().themeType == ThemeType.TYPE_DARK
                || ThemeUtils.getInstance().themeType == ThemeType.TYPE_SOLID_DARK
                || ThemeUtils.getInstance().themeType == ThemeType.TYPE_BLACK
                || ThemeUtils.getInstance().themeType == ThemeType.TYPE_SOLID_BLACK) {
            return context.getResources().getColor(R.color.white);
        }
        return context.getResources().getColor(R.color.white);
    }

    public static void themeTabLayout(Activity activity, SlidingTabLayout slidingTabLayout) {

        if (activity == null) {
            return;
        }

        if (activity instanceof MainActivity) {
            if (ThemeUtils.getInstance().themeType == ThemeType.TYPE_SOLID_DARK
                    || ThemeUtils.getInstance().themeType == ThemeType.TYPE_SOLID_LIGHT
                    || ThemeUtils.getInstance().themeType == ThemeType.TYPE_SOLID_BLACK) {
                slidingTabLayout.setBackgroundColor(ColorUtils.getPrimaryColor());
                slidingTabLayout.setIndicatorColor(ColorUtils.getAccentColor() == ColorUtils.getPrimaryColor() ?
                        activity.getResources().getColor(R.color.white) : ColorUtils.getAccentColor());
                slidingTabLayout.setTextColor(activity.getResources().getColor(R.color.white));
            } else if (ThemeUtils.getInstance().themeType == ThemeType.TYPE_DARK || ThemeUtils.getInstance().themeType == ThemeType.TYPE_BLACK) {
                slidingTabLayout.setIndicatorColor(ColorUtils.getAccentColor() == ColorUtils.getPrimaryColor() ?
                        activity.getResources().getColor(R.color.white) : ColorUtils.getAccentColor());
                slidingTabLayout.setTextColor(activity.getResources().getColor(R.color.white));
            } else if (ThemeUtils.getInstance().themeType == ThemeType.TYPE_LIGHT) {
                slidingTabLayout.setIndicatorColor(ColorUtils.getAccentColor() == ColorUtils.getPrimaryColor() ?
                        activity.getResources().getColor(R.color.white) : ColorUtils.getAccentColor());
                slidingTabLayout.setTextColor(activity.getResources().getColor(R.color.black));
            }
        } else {
            slidingTabLayout.setBackgroundColor(ColorUtils.getPrimaryColor());
            slidingTabLayout.setIndicatorColor(ColorUtils.getAccentColor() == ColorUtils.getPrimaryColor() ?
                    activity.getResources().getColor(R.color.tab_underline_white) : ColorUtils.getAccentColor());
            slidingTabLayout.setTextColor(activity.getResources().getColor(R.color.white));
        }
    }

    public static void themeEditText(EditText editText) {

        int accentColor = ColorUtils.getAccentColor();

        //Not sure if this check is necessary..
        if (editText instanceof AppCompatEditText) {
            //Theme the background
            ((AppCompatEditText) editText).setSupportBackgroundTintList(ColorUtils.createEditTextColorStateList(editText.getContext()));
        }

        //Theme the cursor & handles
        setEditTextDrawablesColor(editText, accentColor);

        //If the parent is a TextInputLayout, theme that too
        if (editText.getParent() != null && editText.getParent() instanceof TextInputLayout) {
            setInputTextLayoutColor((TextInputLayout) editText.getParent(), accentColor);
        }
    }

    public static void setEditTextDrawablesColor(EditText editText, int color) {
        try {
            Field fCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
            fCursorDrawableRes.setAccessible(true);
            int mCursorDrawableRes = fCursorDrawableRes.getInt(editText);
            Field fEditor = TextView.class.getDeclaredField("mEditor");
            fEditor.setAccessible(true);
            Object editor = fEditor.get(editText);
            Class<?> clazz = editor.getClass();
            Field fCursorDrawable = clazz.getDeclaredField("mCursorDrawable");
            fCursorDrawable.setAccessible(true);
            Drawable[] drawables = new Drawable[2];
            drawables[0] = DrawableCompat.wrap(CompatUtils.getDrawableCompat(editText.getContext(), mCursorDrawableRes));
            drawables[1] = DrawableCompat.wrap(CompatUtils.getDrawableCompat(editText.getContext(), mCursorDrawableRes));
            DrawableCompat.setTint(drawables[0], color);
            DrawableCompat.setTint(drawables[1], color);
            fCursorDrawable.set(editor, drawables);

            Field fSelectHandleLeftDrawableRes = TextView.class.getDeclaredField("mTextSelectHandleLeftRes");
            fSelectHandleLeftDrawableRes.setAccessible(true);
            int mTextSelectHandleLeftRes = fSelectHandleLeftDrawableRes.getInt(editText);
            Field fSelectHandleRightDrawableRes = TextView.class.getDeclaredField("mTextSelectHandleRightRes");
            fSelectHandleRightDrawableRes.setAccessible(true);
            int mTextSelectHandleRightRes = fSelectHandleRightDrawableRes.getInt(editText);
            Field fSelectHandleCenterDrawableRes = TextView.class.getDeclaredField("mTextSelectHandleRes");
            fSelectHandleCenterDrawableRes.setAccessible(true);
            int mTextSelectHandleRes = fSelectHandleCenterDrawableRes.getInt(editText);

            Drawable mTextSelectHandleLeftDrawable = DrawableCompat.wrap(CompatUtils.getDrawableCompat(editText.getContext(), mTextSelectHandleLeftRes));
            Drawable mTextSelectHandleRightDrawable = DrawableCompat.wrap(CompatUtils.getDrawableCompat(editText.getContext(), mTextSelectHandleRightRes));
            Drawable mTextSelectHandleDrawable = DrawableCompat.wrap(CompatUtils.getDrawableCompat(editText.getContext(), mTextSelectHandleRes));

            if (mTextSelectHandleLeftDrawable != null) {
                DrawableCompat.setTint(mTextSelectHandleLeftDrawable, color);
            }
            if (mTextSelectHandleRightDrawable != null) {
                DrawableCompat.setTint(mTextSelectHandleRightDrawable, color);
            }
            if (mTextSelectHandleDrawable != null) {
                DrawableCompat.setTint(mTextSelectHandleDrawable, color);
            }

            final Field fSelectHandleLeft = editor.getClass().getDeclaredField("mSelectHandleLeft");
            final Field fSelectHandleRight = editor.getClass().getDeclaredField("mSelectHandleRight");
            final Field fSelectHandleCenter = editor.getClass().getDeclaredField("mSelectHandleCenter");

            fSelectHandleLeft.setAccessible(true);
            fSelectHandleRight.setAccessible(true);
            fSelectHandleCenter.setAccessible(true);

            fSelectHandleLeft.set(editor, mTextSelectHandleLeftDrawable);
            fSelectHandleRight.set(editor, mTextSelectHandleRightDrawable);
            fSelectHandleCenter.set(editor, mTextSelectHandleDrawable);

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static void setInputTextLayoutColor(TextInputLayout textInputLayout, @ColorInt int color) {
        try {
            Field fDefaultTextColor = TextInputLayout.class.getDeclaredField("mDefaultTextColor");
            fDefaultTextColor.setAccessible(true);
            fDefaultTextColor.set(textInputLayout, new ColorStateList(new int[][]{{0}}, new int[]{color}));

            Field fFocusedTextColor = TextInputLayout.class.getDeclaredField("mFocusedTextColor");
            fFocusedTextColor.setAccessible(true);
            fFocusedTextColor.set(textInputLayout, new ColorStateList(new int[][]{{0}}, new int[]{color}));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("InlinedApi")
    public static void themeSearchView(Context context, SearchView searchView) {
        FilterableStateListDrawable stateListDrawable = new FilterableStateListDrawable();
        NinePatchDrawable disabledDrawable = (NinePatchDrawable) CompatUtils.getDrawableCompat(context, R.drawable.abc_textfield_search_default_mtrl_alpha);
        NinePatchDrawable otherDrawable = (NinePatchDrawable) CompatUtils.getDrawableCompat(context, R.drawable.abc_textfield_search_activated_mtrl_alpha);

        int accentColor = ColorUtils.getAccentColor();
        ColorFilter colorFilter = new PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP);
        stateListDrawable.addState(new int[]{android.R.attr.state_enabled, android.R.attr.state_activated}, otherDrawable, colorFilter);
        stateListDrawable.addState(new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}, otherDrawable, colorFilter);
        stateListDrawable.addState(StateSet.WILD_CARD, disabledDrawable);

        View searchPlate = searchView.findViewById(android.support.v7.appcompat.R.id.search_plate);
        searchPlate.setBackground(stateListDrawable);

        EditText searchTextView = (EditText) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        try {
            Field mCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
            mCursorDrawableRes.setAccessible(true);
            mCursorDrawableRes.set(searchTextView, 0);
        } catch (final Exception | NoClassDefFoundError ignored) {
        }
    }

    public static void themeListView(AbsListView listView) {
        int accentColor = ColorUtils.getAccentColor();
        try {
            final Class<?> clazz = AbsListView.class;
            final Field fEdgeGlowTop = clazz.getDeclaredField("mEdgeGlowTop");
            final Field fEdgeGlowBottom = clazz.getDeclaredField("mEdgeGlowBottom");
            fEdgeGlowTop.setAccessible(true);
            fEdgeGlowBottom.setAccessible(true);
            setEdgeEffectColor((EdgeEffect) fEdgeGlowTop.get(listView), accentColor);
            setEdgeEffectColor((EdgeEffect) fEdgeGlowBottom.get(listView), accentColor);
        } catch (final Exception | NoClassDefFoundError ignored) {
        }
    }

    public static void themeRecyclerView(RecyclerView recyclerView) {
        int accentColor = ColorUtils.getAccentColor();
        try {
            final Class<?> clazz = RecyclerView.class;
            for (final String name : new String[]{"ensureTopGlow", "ensureBottomGlow", "ensureLeftGlow", "ensureRightGlow"}) {
                Method method = clazz.getDeclaredMethod(name);
                method.setAccessible(true);
                method.invoke(recyclerView);
            }
            for (final String name : new String[]{"mTopGlow", "mBottomGlow", "mRightGlow", "mLeftGlow"}) {
                final Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                final Object edge = field.get(recyclerView);
                final Field fEdgeEffect = edge.getClass().getDeclaredField("mEdgeEffect");
                fEdgeEffect.setAccessible(true);
                setEdgeEffectColor((EdgeEffect) fEdgeEffect.get(edge), accentColor);
            }
        } catch (final Exception | NoClassDefFoundError ignored) {
        }
    }

    public static void themeViewPager(ViewPager viewPager) {
        int accentColor = ColorUtils.getAccentColor();
        try {
            final Class<?> clazz = ViewPager.class;
            final Field fEdgeGlowLeft = clazz.getDeclaredField("mLeftEdge");
            final Field fEdgeGlowRight = clazz.getDeclaredField("mRightEdge");
            fEdgeGlowLeft.setAccessible(true);
            fEdgeGlowRight.setAccessible(true);
            final Object edgeLeft = fEdgeGlowLeft.get(viewPager);
            final Field fEdgeEffectLeft = edgeLeft.getClass().getDeclaredField("mEdgeEffect");
            fEdgeEffectLeft.setAccessible(true);
            final Object edgeRight = fEdgeGlowRight.get(viewPager);
            final Field fEdgeEffectRight = edgeRight.getClass().getDeclaredField("mEdgeEffect");
            fEdgeEffectRight.setAccessible(true);
            setEdgeEffectColor((EdgeEffect) fEdgeEffectLeft.get(edgeLeft), accentColor);
            setEdgeEffectColor((EdgeEffect) fEdgeEffectRight.get(edgeRight), accentColor);
        } catch (final Exception | NoClassDefFoundError ignored) {
        }
    }

    public static void themeScrollView(final ScrollView scrollView) {
        int accentColor = ColorUtils.getAccentColor();
        try {
            final Class<?> clazz = ScrollView.class;
            final Field fEdgeGlowTop = clazz.getDeclaredField("mEdgeGlowTop");
            final Field fEdgeGlowBottom = clazz.getDeclaredField("mEdgeGlowBottom");
            fEdgeGlowTop.setAccessible(true);
            fEdgeGlowBottom.setAccessible(true);
            setEdgeEffectColor((EdgeEffect) fEdgeGlowTop.get(scrollView), accentColor);
            setEdgeEffectColor((EdgeEffect) fEdgeGlowBottom.get(scrollView), accentColor);
        } catch (final Exception | NoClassDefFoundError ignored) {
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    static void setEdgeEffectColor(EdgeEffect edgeEffect, int color) {
        try {
            if (ShuttleUtils.hasLollipop()) {
                edgeEffect.setColor(color);
                return;
            }

            for (String name : new String[]{"mEdge", "mGlow"}) {
                final Field field = EdgeEffect.class.getDeclaredField(name);
                field.setAccessible(true);
                final Drawable drawable = (Drawable) field.get(edgeEffect);
                drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                drawable.setCallback(null);
            }
        } catch (final Exception | NoClassDefFoundError ignored) {
        }
    }

    public static void themeContextualActionBar(Activity activity) {
        if (activity != null) {
            View v = activity.findViewById(R.id.action_mode_bar);
            if (v != null) {
                Drawable bottom = CompatUtils.getDrawableCompat(activity, R.drawable.abc_cab_background_top_mtrl_alpha);
                if (bottom != null) {
                    bottom.setColorFilter(ColorUtils.getAccentColor(), PorterDuff.Mode.SRC_ATOP);
                }
                Drawable background = new ColorDrawable(ColorUtils.getPrimaryColorDark(activity));
                LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{background, bottom});
                v.setBackgroundDrawable(layerDrawable);
            }
        }
    }

    /**
     * Traverses the hierarchy of this view (if it's a ViewGroup). If the view itself or any of its children
     * implement {@link Themable}, calls {@link Themable#updateTheme()}
     */
    public static void updateThemableViews(View view) {
        if (view instanceof ViewGroup) {
            for (int i = 0, count = ((ViewGroup) view).getChildCount(); i < count; i++) {
                View child = ((ViewGroup) view).getChildAt(i);
                updateThemableViews(child);
            }
        } else {
            if (view instanceof Themable) {
                ((Themable) view).updateTheme();
            }
        }
    }
}
