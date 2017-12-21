package com.afollestad.aesthetic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.CheckResult;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.Pair;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.f2prateek.rx.preferences2.RxSharedPreferences;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

import static com.afollestad.aesthetic.Rx.onErrorLogAndRethrow;
import static com.afollestad.aesthetic.Util.isColorLight;
import static com.afollestad.aesthetic.Util.resolveColor;
import static com.afollestad.aesthetic.Util.setLightStatusBarCompat;
import static com.afollestad.aesthetic.Util.setNavBarColorCompat;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Aesthetic {

  private static final String PREFS_NAME = "[aesthetic-prefs]";
  private static final String KEY_FIRST_TIME = "first_time_%s";
  private static final String KEY_ACTIVITY_THEME = "activity_theme_%s";
  private static final String KEY_IS_DARK = "is_dark_%s";
  private static final String KEY_PRIMARY_COLOR = "primary_color_%s";
  private static final String KEY_PRIMARY_DARK_COLOR = "primary_dark_color";
  private static final String KEY_ACCENT_COLOR = "accent_color_%s";
  private static final String KEY_PRIMARY_TEXT_COLOR = "primary_text";
  private static final String KEY_SECONDARY_TEXT_COLOR = "secondary_text";
  private static final String KEY_PRIMARY_TEXT_INVERSE_COLOR = "primary_text_inverse";
  private static final String KEY_SECONDARY_TEXT_INVERSE_COLOR = "secondary_text_inverse";
  private static final String KEY_WINDOW_BG_COLOR = "window_bg_color_%s";
  private static final String KEY_STATUS_BAR_COLOR = "status_bar_color_%s";
  private static final String KEY_NAV_BAR_COLOR = "nav_bar_color_%s";
  private static final String KEY_LIGHT_STATUS_MODE = "light_status_mode";
  private static final String KEY_TAB_LAYOUT_BG_MODE = "tab_layout_bg_mode";
  private static final String KEY_TAB_LAYOUT_INDICATOR_MODE = "tab_layout_indicator_mode";
  private static final String KEY_NAV_VIEW_MODE = "nav_view_mode";
  private static final String KEY_BOTTOM_NAV_BG_MODE = "bottom_nav_bg_mode";
  private static final String KEY_BOTTOM_NAV_ICONTEXT_MODE = "bottom_nav_icontext_mode";
  private static final String KEY_CARD_VIEW_BG_COLOR = "card_view_bg_color";
  private static final String KEY_ICON_TITLE_ACTIVE_COLOR = "icon_title_active_color";
  private static final String KEY_ICON_TITLE_INACTIVE_COLOR = "icon_title_inactive_color";
  private static final String KEY_SNACKBAR_TEXT = "snackbar_text_color";
  private static final String KEY_SNACKBAR_ACTION_TEXT = "snackbar_action_text_color";

  @SuppressLint("StaticFieldLeak")
  private static Aesthetic instance;

  private final ArrayMap<String, Integer> lastActivityThemes;

  private CompositeDisposable subs;
  private Context context;
  private SharedPreferences prefs;
  private SharedPreferences.Editor editor;
  private RxSharedPreferences rxPrefs;

  @SuppressLint("CommitPrefEdits")
  private Aesthetic(Context context) {
    this.context = context;
    prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    editor = prefs.edit();
    rxPrefs = RxSharedPreferences.create(prefs);
    lastActivityThemes = new ArrayMap<>(2);
  }

  private static String key(@Nullable Context context) {
    String key;
    if (context instanceof AestheticKeyProvider) {
      key = ((AestheticKeyProvider) context).key();
    } else {
      key = "default";
    }
    if (key == null) {
      key = "default";
    }
    return key;
  }

  /** Should be called before super.onCreate() in each Activity. */
  @NonNull
  public void attach(@NonNull AppCompatActivity activity) {

    LayoutInflater li = activity.getLayoutInflater();
    Util.setInflaterFactory(li);

    String activityThemeKey = String.format(KEY_ACTIVITY_THEME, key(activity));
    int latestActivityTheme = instance.prefs.getInt(activityThemeKey, 0);
    instance.lastActivityThemes.put(activity.getClass().getName(), latestActivityTheme);
    if (latestActivityTheme != 0) {
      activity.setTheme(latestActivityTheme);
    }
  }

  private static int getLastActivityTheme(@Nullable Context forContext) {
    if (forContext == null || instance == null) {
      return 0;
    }
    Integer lastActivityTheme = instance.lastActivityThemes.get(forContext.getClass().getName());
    if (lastActivityTheme == null) {
      return 0;
    }
    return lastActivityTheme;
  }

  @NonNull
  @CheckResult
  public static Aesthetic get(Context context) {
    if (instance == null) {
      instance = new Aesthetic(context);
    }
    instance.context = context;

    return instance;
  }

  /** Should be called in onPause() of each Activity. */
  public void pause(@NonNull AppCompatActivity activity) {
    if (instance == null) {
        return;
    }
    if (instance.subs != null) {
        instance.subs.clear();
    }

    if (activity.isFinishing()) {
      if (instance.context != null
            && instance.context.getClass().getName().equals(activity.getClass().getName())) {
        instance.context = null;
      }
    }
  }

  /** Should be called in onResume() of each Activity. */
  public void resume(@NonNull final AppCompatActivity activity) {
    if (instance == null) {
      return;
    }
    instance.context = activity;
    
    if (instance.subs != null) {
      instance.subs.clear();
    }
    instance.subs = new CompositeDisposable();
    instance.subs.add(
        instance
            .colorPrimary()
            .compose(Rx.<Integer>distinctToMainThread())
            .subscribe(
                new Consumer<Integer>() {
                  @Override
                  public void accept(@io.reactivex.annotations.NonNull Integer color) {
                    Util.setTaskDescriptionColor(activity, color);
                  }
                },
                onErrorLogAndRethrow()));
    instance.subs.add(
        instance
            .activityTheme()
            .compose(Rx.<Integer>distinctToMainThread())
            .subscribe(
                new Consumer<Integer>() {
                  @Override
                  public void accept(@io.reactivex.annotations.NonNull Integer themeId) {
                    if (getLastActivityTheme(activity) == themeId) {
                      return;
                    }
                    instance.lastActivityThemes.put(activity.getClass().getName(), themeId);
                    activity.recreate();
                  }
                },
                onErrorLogAndRethrow()));
    instance.subs.add(
        Observable.combineLatest(
                instance.colorStatusBar(),
                instance.lightStatusBarMode(),
                new BiFunction<Integer, Integer, Pair<Integer, Integer>>() {
                  @Override
                  public Pair<Integer, Integer> apply(Integer integer, Integer integer2) {
                    return Pair.create(integer, integer2);
                  }
                })
            .compose(Rx.<Pair<Integer, Integer>>distinctToMainThread())
            .subscribe(
                new Consumer<Pair<Integer, Integer>>() {
                  @Override
                  public void accept(
                      @io.reactivex.annotations.NonNull Pair<Integer, Integer> result) {
                    instance.invalidateStatusBar(activity);
                  }
                },
                onErrorLogAndRethrow()));
    instance.subs.add(
        instance
            .colorNavigationBar()
            .compose(Rx.<Integer>distinctToMainThread())
            .subscribe(
                new Consumer<Integer>() {
                  @Override
                  public void accept(@io.reactivex.annotations.NonNull Integer color) {
                    setNavBarColorCompat(activity, color);
                  }
                },
                onErrorLogAndRethrow()));
    instance.subs.add(
        instance
            .colorWindowBackground()
            .compose(Rx.<Integer>distinctToMainThread())
            .subscribe(
                new Consumer<Integer>() {
                  @Override
                  public void accept(@io.reactivex.annotations.NonNull Integer color) {
                    activity.getWindow().setBackgroundDrawable(new ColorDrawable(color));
                  }
                },
                onErrorLogAndRethrow()));

    if (MaterialDialogsUtil.shouldSupport()) {
      instance.subs.add(MaterialDialogsUtil.observe(instance));
    }
  }

  /** Returns true if this method has never been called before. */
  public static boolean isFirstTime(AppCompatActivity appCompatActivity) {
    String key = String.format(KEY_FIRST_TIME, key(appCompatActivity));
    boolean firstTime = instance.prefs.getBoolean(key, true);
    instance.editor.putBoolean(key, false).commit();
    return firstTime;
  }

  private void invalidateStatusBar(AppCompatActivity activity) {
    String key = String.format(KEY_STATUS_BAR_COLOR, key(activity));
    final int color = prefs.getInt(key, resolveColor(activity, R.attr.colorPrimaryDark));

    ViewGroup rootView = Util.getRootView(activity);
    if (rootView instanceof DrawerLayout) {
      // Color is set to DrawerLayout, Activity gets transparent status bar
      setLightStatusBarCompat(activity, false);
      Util.setStatusBarColorCompat(
          activity, ContextCompat.getColor(activity, android.R.color.transparent));
      ((DrawerLayout) rootView).setStatusBarBackgroundColor(color);
    } else {
      Util.setStatusBarColorCompat(activity, color);
    }

    final int mode = prefs.getInt(KEY_LIGHT_STATUS_MODE, AutoSwitchMode.AUTO);
    switch (mode) {
      case AutoSwitchMode.OFF:
        setLightStatusBarCompat(activity, false);
        break;
      case AutoSwitchMode.ON:
        setLightStatusBarCompat(activity, true);
        break;
      default:
        setLightStatusBarCompat(activity, isColorLight(color));
        break;
    }
  }

  //
  /////// GETTERS AND SETTERS OF THEME PROPERTIES
  //

  @CheckResult
  public Aesthetic activityTheme(@StyleRes int theme) {
    String key = String.format(KEY_ACTIVITY_THEME, key(context));
    editor.putInt(key, theme);
    return this;
  }

  @CheckResult
  public Observable<Integer> activityTheme() {
    String key = String.format(KEY_ACTIVITY_THEME, key(context));
    return rxPrefs
        .getInteger(key, 0)
        .asObservable()
        .filter(
            new Predicate<Integer>() {
              @Override
              public boolean test(@io.reactivex.annotations.NonNull Integer next) throws Exception {
                return next != 0 && next != getLastActivityTheme(instance.context);
              }
            });
  }

  @CheckResult
  public Aesthetic isDark(boolean isDark) {
    String key = String.format(KEY_IS_DARK, key(context));
    editor.putBoolean(key, isDark).commit();
    return this;
  }

  @CheckResult
  public Observable<Boolean> isDark() {
    String key = String.format(KEY_IS_DARK, key(context));
    return rxPrefs.getBoolean(key, false).asObservable();
  }

  @CheckResult
  public Aesthetic colorPrimary(@ColorInt int color) {
    // needs to be committed immediately so that for statusBarColorAuto() and other auto methods
    String key = String.format(KEY_PRIMARY_COLOR, key(context));
    editor.putInt(key, color).commit();
    return this;
  }

  @CheckResult
  public Aesthetic colorPrimaryRes(@ColorRes int color) {
    return colorPrimary(ContextCompat.getColor(context, color));
  }

  @CheckResult
  public Observable<Integer> colorPrimary() {
    String key = String.format(KEY_PRIMARY_COLOR, key(context));
    return rxPrefs
        .getInteger(key, resolveColor(context, R.attr.colorPrimary))
        .asObservable();
  }

  @CheckResult
  public Aesthetic colorPrimaryDark(@ColorInt int color) {
    // needs to be committed immediately so that for statusBarColorAuto() and other auto methods
    editor.putInt(KEY_PRIMARY_DARK_COLOR, color).commit();
    return this;
  }

  @CheckResult
  public Aesthetic colorPrimaryDarkRes(@ColorRes int color) {
    return colorPrimaryDark(ContextCompat.getColor(context, color));
  }

  @CheckResult
  public Observable<Integer> colorPrimaryDark() {
    return rxPrefs
        .getInteger(KEY_PRIMARY_DARK_COLOR, resolveColor(context, R.attr.colorPrimaryDark))
        .asObservable();
  }

  @CheckResult
  public Aesthetic colorAccent(@ColorInt int color) {
    String key = String.format(KEY_ACCENT_COLOR, key(context));
    editor.putInt(key, color).commit();
    return this;
  }

  @CheckResult
  public Aesthetic colorAccentRes(@ColorRes int color) {
    return colorAccent(ContextCompat.getColor(context, color));
  }

  @CheckResult
  public Observable<Integer> colorAccent() {
    String key = String.format(KEY_ACCENT_COLOR, key(context));
    return rxPrefs
        .getInteger(key, resolveColor(context, R.attr.colorAccent))
        .asObservable();
  }

  @CheckResult
  public Aesthetic textColorPrimary(@ColorInt int color) {
    editor.putInt(KEY_PRIMARY_TEXT_COLOR, color);
    return this;
  }

  @CheckResult
  public Aesthetic textColorPrimaryRes(@ColorRes int color) {
    return textColorPrimary(ContextCompat.getColor(context, color));
  }

  @CheckResult
  public Observable<Integer> textColorPrimary() {
    return rxPrefs
        .getInteger(KEY_PRIMARY_TEXT_COLOR, resolveColor(context, android.R.attr.textColorPrimary))
        .asObservable();
  }

  @CheckResult
  public Aesthetic textColorSecondary(@ColorInt int color) {
    editor.putInt(KEY_SECONDARY_TEXT_COLOR, color);
    return this;
  }

  @CheckResult
  public Aesthetic textColorSecondaryRes(@ColorRes int color) {
    return textColorSecondary(ContextCompat.getColor(context, color));
  }

  @CheckResult
  public Observable<Integer> textColorSecondary() {
    return rxPrefs
        .getInteger(
            KEY_SECONDARY_TEXT_COLOR, resolveColor(context, android.R.attr.textColorSecondary))
        .asObservable();
  }

  @CheckResult
  public Aesthetic textColorPrimaryInverse(@ColorInt int color) {
    editor.putInt(KEY_PRIMARY_TEXT_INVERSE_COLOR, color);
    return this;
  }

  @CheckResult
  public Aesthetic textColorPrimaryInverseRes(@ColorRes int color) {
    return textColorPrimaryInverse(ContextCompat.getColor(context, color));
  }

  @CheckResult
  public Observable<Integer> textColorPrimaryInverse() {
    return rxPrefs
        .getInteger(
            KEY_PRIMARY_TEXT_INVERSE_COLOR,
            resolveColor(context, android.R.attr.textColorPrimaryInverse))
        .asObservable();
  }

  @CheckResult
  public Aesthetic textColorSecondaryInverse(@ColorInt int color) {
    editor.putInt(KEY_SECONDARY_TEXT_INVERSE_COLOR, color);
    return this;
  }

  @CheckResult
  public Aesthetic textColorSecondaryInverseRes(@ColorRes int color) {
    return textColorSecondaryInverse(ContextCompat.getColor(context, color));
  }

  @CheckResult
  public Observable<Integer> textColorSecondaryInverse() {
    return rxPrefs
        .getInteger(
            KEY_SECONDARY_TEXT_INVERSE_COLOR,
            resolveColor(context, android.R.attr.textColorSecondaryInverse))
        .asObservable();
  }

  @CheckResult
  public Aesthetic colorWindowBackground(@ColorInt int color) {
    String key = String.format(KEY_WINDOW_BG_COLOR, key(context));
    editor.putInt(key, color).commit();
    return this;
  }

  @CheckResult
  public Aesthetic colorWindowBackgroundRes(@ColorRes int color) {
    return colorWindowBackground(ContextCompat.getColor(context, color));
  }

  @CheckResult
  public Observable<Integer> colorWindowBackground() {
    String key = String.format(KEY_WINDOW_BG_COLOR, key(context));
    return rxPrefs
        .getInteger(key, resolveColor(context, android.R.attr.windowBackground))
        .asObservable();
  }

  @CheckResult
  public Aesthetic colorStatusBar(@ColorInt int color) {
    String key = String.format(KEY_STATUS_BAR_COLOR, key(context));
    editor.putInt(key, color);
    return this;
  }

  @CheckResult
  public Aesthetic colorStatusBarRes(@ColorRes int color) {
    return colorStatusBar(ContextCompat.getColor(context, color));
  }

  @CheckResult
  public Aesthetic colorStatusBarAuto() {
    String statusBarKey = String.format(KEY_STATUS_BAR_COLOR, key(context));
    String primaryColorKey = String.format(KEY_PRIMARY_COLOR, key(context));
    editor.putInt(
        statusBarKey,
        Util.darkenColor(
            prefs.getInt(primaryColorKey, resolveColor(context, R.attr.colorPrimary))));
    return this;
  }

  @CheckResult
  public Observable<Integer> colorStatusBar() {
    return colorPrimaryDark()
        .flatMap(
            new Function<Integer, ObservableSource<Integer>>() {
              @Override
              public ObservableSource<Integer> apply(
                  @io.reactivex.annotations.NonNull Integer primaryDarkColor) throws Exception {
                String key = String.format(KEY_STATUS_BAR_COLOR, key(context));
                return rxPrefs.getInteger(key, primaryDarkColor).asObservable();
              }
            });
  }

  @CheckResult
  public Aesthetic colorNavigationBar(@ColorInt int color) {
    String key = String.format(KEY_NAV_BAR_COLOR, key(context));
    editor.putInt(key, color);
    return this;
  }

  @CheckResult
  public Aesthetic colorNavigationBarRes(@ColorRes int color) {
    return colorNavigationBar(ContextCompat.getColor(context, color));
  }

  @CheckResult
  public Aesthetic colorNavigationBarAuto(boolean auto) {
    String navBarKey = String.format(KEY_NAV_BAR_COLOR, key(context));
    String primaryColorKey = String.format(KEY_PRIMARY_COLOR, key(context));
      if (auto) {
      int color = prefs.getInt(primaryColorKey, resolveColor(context, R.attr.colorPrimary));
      editor.putInt(navBarKey, isColorLight(color) ? Color.BLACK : color);
    } else {
      editor.remove(navBarKey);
    }
    return this;
  }

  @CheckResult
  public Observable<Integer> colorNavigationBar() {
    String key = String.format(KEY_NAV_BAR_COLOR, key(context));
    return rxPrefs.getInteger(key, Color.BLACK).asObservable();
  }

  @CheckResult
  public Aesthetic lightStatusBarMode(@AutoSwitchMode int mode) {
    editor.putInt(KEY_LIGHT_STATUS_MODE, mode);
    return this;
  }

  @CheckResult
  public Observable<Integer> lightStatusBarMode() {
    return rxPrefs.getInteger(KEY_LIGHT_STATUS_MODE, AutoSwitchMode.AUTO).asObservable();
  }

  @CheckResult
  public Aesthetic tabLayoutIndicatorMode(@TabLayoutIndicatorMode int mode) {
    editor.putInt(KEY_TAB_LAYOUT_INDICATOR_MODE, mode).commit();
    return this;
  }

  @CheckResult
  public Observable<Integer> tabLayoutIndicatorMode() {
    return rxPrefs
        .getInteger(KEY_TAB_LAYOUT_INDICATOR_MODE, TabLayoutIndicatorMode.ACCENT)
        .asObservable();
  }

  @CheckResult
  public Aesthetic tabLayoutBackgroundMode(@TabLayoutBgMode int mode) {
    editor.putInt(KEY_TAB_LAYOUT_BG_MODE, mode).commit();
    return this;
  }

  @CheckResult
  public Observable<Integer> tabLayoutBackgroundMode() {
    return rxPrefs.getInteger(KEY_TAB_LAYOUT_BG_MODE, TabLayoutBgMode.PRIMARY).asObservable();
  }

  @CheckResult
  public Aesthetic navigationViewMode(@NavigationViewMode int mode) {
    editor.putInt(KEY_NAV_VIEW_MODE, mode).commit();
    return this;
  }

  @CheckResult
  public Observable<Integer> navigationViewMode() {
    return rxPrefs
        .getInteger(KEY_NAV_VIEW_MODE, NavigationViewMode.SELECTED_PRIMARY)
        .asObservable();
  }

  @CheckResult
  public Aesthetic bottomNavigationBackgroundMode(@BottomNavBgMode int mode) {
    editor.putInt(KEY_BOTTOM_NAV_BG_MODE, mode).commit();
    return this;
  }

  @CheckResult
  public Observable<Integer> bottomNavigationBackgroundMode() {
    return rxPrefs
        .getInteger(KEY_BOTTOM_NAV_BG_MODE, BottomNavBgMode.BLACK_WHITE_AUTO)
        .asObservable();
  }

  @CheckResult
  public Aesthetic bottomNavigationIconTextMode(@BottomNavIconTextMode int mode) {
    editor.putInt(KEY_BOTTOM_NAV_ICONTEXT_MODE, mode).commit();
    return this;
  }

  @CheckResult
  public Observable<Integer> bottomNavigationIconTextMode() {
    return rxPrefs
        .getInteger(KEY_BOTTOM_NAV_ICONTEXT_MODE, BottomNavIconTextMode.SELECTED_ACCENT)
        .asObservable();
  }

  @CheckResult
  public Observable<Integer> colorCardViewBackground() {
    return isDark()
        .flatMap(
            new Function<Boolean, ObservableSource<Integer>>() {
              @Override
              public ObservableSource<Integer> apply(
                  @io.reactivex.annotations.NonNull Boolean isDark) throws Exception {
                return rxPrefs
                    .getInteger(
                        KEY_CARD_VIEW_BG_COLOR,
                        ContextCompat.getColor(
                            context,
                            isDark ? R.color.ate_cardview_bg_dark : R.color.ate_cardview_bg_light))
                    .asObservable();
              }
            });
  }

  @CheckResult
  public Aesthetic colorCardViewBackground(@ColorInt int color) {
    editor.putInt(KEY_CARD_VIEW_BG_COLOR, color);
    return this;
  }

  @CheckResult
  public Aesthetic colorCardViewBackgroundRes(@ColorRes int color) {
    return colorCardViewBackground(ContextCompat.getColor(context, color));
  }

  @CheckResult
  public Observable<ActiveInactiveColors> colorIconTitle(
      @Nullable Observable<Integer> backgroundObservable) {
    if (backgroundObservable == null) {
      backgroundObservable = Aesthetic.get(context).colorPrimary();
    }
    return backgroundObservable.flatMap(
        new Function<Integer, ObservableSource<ActiveInactiveColors>>() {
          @Override
          public ObservableSource<ActiveInactiveColors> apply(
              @io.reactivex.annotations.NonNull Integer primaryColor) throws Exception {
            final boolean isDark = !isColorLight(primaryColor);
            return Observable.zip(
                rxPrefs
                    .getInteger(
                        KEY_ICON_TITLE_ACTIVE_COLOR,
                        ContextCompat.getColor(
                            context, isDark ? R.color.ate_icon_dark : R.color.ate_icon_light))
                    .asObservable(),
                rxPrefs
                    .getInteger(
                        KEY_ICON_TITLE_INACTIVE_COLOR,
                        ContextCompat.getColor(
                            context,
                            isDark
                                ? R.color.ate_icon_dark_inactive
                                : R.color.ate_icon_light_inactive))
                    .asObservable(),
                new BiFunction<Integer, Integer, ActiveInactiveColors>() {
                  @Override
                  public ActiveInactiveColors apply(
                      @io.reactivex.annotations.NonNull Integer integer,
                      @io.reactivex.annotations.NonNull Integer integer2)
                      throws Exception {
                    return ActiveInactiveColors.create(integer, integer2);
                  }
                });
          }
        });
  }

  @CheckResult
  public Aesthetic colorIconTitleActive(@ColorInt int color) {
    editor.putInt(KEY_ICON_TITLE_ACTIVE_COLOR, color);
    return this;
  }

  @CheckResult
  public Aesthetic colorIconTitleActiveRes(@ColorRes int color) {
    return colorIconTitleActive(ContextCompat.getColor(context, color));
  }

  @CheckResult
  public Aesthetic colorIconTitleInactive(@ColorInt int color) {
    editor.putInt(KEY_ICON_TITLE_INACTIVE_COLOR, color);
    return this;
  }

  @CheckResult
  public Aesthetic colorIconTitleInactiveRes(@ColorRes int color) {
    return colorIconTitleActive(ContextCompat.getColor(context, color));
  }

  @CheckResult
  public Observable<Integer> snackbarTextColor() {
    return isDark()
        .flatMap(
            new Function<Boolean, ObservableSource<Integer>>() {
              @Override
              public ObservableSource<Integer> apply(
                  @io.reactivex.annotations.NonNull Boolean isDark) throws Exception {
                return (isDark ? textColorPrimary() : textColorPrimaryInverse())
                    .flatMap(
                        new Function<Integer, ObservableSource<Integer>>() {
                          @Override
                          public ObservableSource<Integer> apply(
                              @io.reactivex.annotations.NonNull Integer defaultTextColor)
                              throws Exception {
                            return rxPrefs
                                .getInteger(KEY_SNACKBAR_TEXT, defaultTextColor)
                                .asObservable();
                          }
                        });
              }
            });
  }

  @CheckResult
  public Aesthetic snackbarTextColor(@ColorInt int color) {
    editor.putInt(KEY_SNACKBAR_TEXT, color);
    return this;
  }

  @CheckResult
  public Aesthetic snackbarTextColorRes(@ColorRes int color) {
    return colorCardViewBackground(ContextCompat.getColor(context, color));
  }

  @CheckResult
  public Observable<Integer> snackbarActionTextColor() {
    return colorAccent()
        .flatMap(
            new Function<Integer, ObservableSource<Integer>>() {
              @Override
              public ObservableSource<Integer> apply(
                  @io.reactivex.annotations.NonNull Integer accentColor) throws Exception {
                return rxPrefs.getInteger(KEY_SNACKBAR_ACTION_TEXT, accentColor).asObservable();
              }
            });
  }

  @CheckResult
  public Aesthetic snackbarActionTextColor(@ColorInt int color) {
    editor.putInt(KEY_SNACKBAR_ACTION_TEXT, color);
    return this;
  }

  @CheckResult
  public Aesthetic snackbarActionTextColorRes(@ColorRes int color) {
    return colorCardViewBackground(ContextCompat.getColor(context, color));
  }

  /** Notifies all listening views that theme properties have been updated. */
  public void apply() {
    editor.commit();
  }
}
