package com.afollestad.aesthetic;

import static com.afollestad.aesthetic.NavigationViewMode.SELECTED_ACCENT;
import static com.afollestad.aesthetic.NavigationViewMode.SELECTED_PRIMARY;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings("WeakerAccess")
@Retention(SOURCE)
@IntDef(value = {SELECTED_PRIMARY, SELECTED_ACCENT})
public @interface NavigationViewMode {
  int SELECTED_PRIMARY = 0;
  int SELECTED_ACCENT = 1;
}
