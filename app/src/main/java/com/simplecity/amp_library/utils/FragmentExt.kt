package com.simplecity.amp_library.utils

import android.os.Bundle
import androidx.fragment.app.Fragment

inline fun <T : Fragment> T.withArgs(
    argsBuilder: Bundle.() -> Unit
): T = this.apply { arguments = Bundle().apply(argsBuilder) }