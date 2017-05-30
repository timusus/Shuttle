package com.afollestad.aesthetic;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;

import static com.afollestad.aesthetic.AutoSwitchMode.AUTO;
import static com.afollestad.aesthetic.AutoSwitchMode.OFF;
import static com.afollestad.aesthetic.AutoSwitchMode.ON;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings("WeakerAccess")
@Retention(SOURCE)
@IntDef(value = {OFF, ON, AUTO})
public @interface AutoSwitchMode {
  int OFF = 0;
  int ON = 1;
  int AUTO = 2;
}
