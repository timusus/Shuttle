package com.afollestad.aesthetic;

import static com.afollestad.aesthetic.TabLayoutBgMode.ACCENT;
import static com.afollestad.aesthetic.TabLayoutBgMode.PRIMARY;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings("WeakerAccess")
@Retention(SOURCE)
@IntDef(value = {PRIMARY, ACCENT})
public @interface TabLayoutBgMode {
  int PRIMARY = 0;
  int ACCENT = 1;
}
