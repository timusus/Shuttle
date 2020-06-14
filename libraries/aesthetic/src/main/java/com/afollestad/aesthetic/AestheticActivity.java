package com.afollestad.aesthetic;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/** @author Aidan Follestad (afollestad) */
public class AestheticActivity extends AppCompatActivity implements AestheticKeyProvider {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    Aesthetic.get(this).attach(this);
    super.onCreate(savedInstanceState);
  }

  @Override
  protected void onResume() {
    super.onResume();
    Aesthetic.get(this).resume(this);
  }

  @Override
  protected void onPause() {
    Aesthetic.get(this).pause(this);
    super.onPause();
  }

  @Nullable
  @Override
  public String key() {
    return null;
  }
}
