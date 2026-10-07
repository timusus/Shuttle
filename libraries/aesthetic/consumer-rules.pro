# Aesthetic tints widgets by reflecting on private members of these AndroidX / Material classes
-keepclassmembers class androidx.appcompat.widget.Toolbar { private <fields>; }
-keepclassmembers class androidx.appcompat.widget.SearchView { private <fields>; }
-keepclassmembers class com.google.android.material.textfield.TextInputLayout { private <fields>; private <methods>; }
-keepclassmembers class androidx.mediarouter.app.MediaRouteButton { private <fields>; }
-keepclassmembers class androidx.core.widget.EdgeEffectCompat { private <fields>; }
-keepclassmembers class androidx.recyclerview.widget.RecyclerView { private <fields>; }
-keepclassmembers class androidx.core.widget.NestedScrollView { private <fields>; }
-keepclassmembers class androidx.viewpager.widget.ViewPager { private <fields>; }

# MaterialDialogsUtil loads ThemeSingleton by name
-keep class com.afollestad.materialdialogs.internal.ThemeSingleton { *; }
