package com.afollestad.aesthetic;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/** @author Aidan Follestad (afollestad) */
public class AestheticActivity extends AppCompatActivity implements AestheticKeyProvider {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    Aesthetic.attach(this);
    super.onCreate(savedInstanceState);
  }

  @Override
  protected void onResume() {
    super.onResume();
    Aesthetic.resume(this);
  }

  @Override
  protected void onPause() {
    Aesthetic.pause(this);
    super.onPause();
  }

  @Nullable
  @Override
  public String key() {
    return null;
  }
}
