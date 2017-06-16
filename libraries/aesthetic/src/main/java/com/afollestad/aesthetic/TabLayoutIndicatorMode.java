package com.afollestad.aesthetic;

import static com.afollestad.aesthetic.TabLayoutIndicatorMode.ACCENT;
import static com.afollestad.aesthetic.TabLayoutIndicatorMode.PRIMARY;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings("WeakerAccess")
@Retention(SOURCE)
@IntDef(value = {PRIMARY, ACCENT})
public @interface TabLayoutIndicatorMode {
  int PRIMARY = 0;
  int ACCENT = 1;
}
