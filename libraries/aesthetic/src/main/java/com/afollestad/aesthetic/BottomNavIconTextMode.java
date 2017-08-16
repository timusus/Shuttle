package com.afollestad.aesthetic;

import static com.afollestad.aesthetic.BottomNavIconTextMode.BLACK_WHITE_AUTO;
import static com.afollestad.aesthetic.BottomNavIconTextMode.SELECTED_ACCENT;
import static com.afollestad.aesthetic.BottomNavIconTextMode.SELECTED_PRIMARY;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings("WeakerAccess")
@Retention(SOURCE)
@IntDef(value = {SELECTED_PRIMARY, SELECTED_ACCENT, BLACK_WHITE_AUTO})
public @interface BottomNavIconTextMode {
  int SELECTED_PRIMARY = 0;
  int SELECTED_ACCENT = 1;
  int BLACK_WHITE_AUTO = 2;
}
