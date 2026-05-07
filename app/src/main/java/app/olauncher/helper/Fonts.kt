package app.olauncher.helper

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import app.olauncher.R
import app.olauncher.data.Constants

fun resolveCustomTypeface(context: Context, fontFamily: Int): Typeface? {
    val resId = when (fontFamily) {
        Constants.FontFamily.INTER -> R.font.inter_regular
        Constants.FontFamily.JETBRAINS_MONO -> R.font.jetbrains_mono_regular
        Constants.FontFamily.ATKINSON_HYPERLEGIBLE -> R.font.atkinson_hyperlegible_regular
        Constants.FontFamily.OPEN_DYSLEXIC -> R.font.open_dyslexic_regular
        else -> return null
    }
    return ResourcesCompat.getFont(context, resId)
}

fun View.applyTypefaceRecursively(typeface: Typeface) {
    if (this is TextView) {
        this.typeface = Typeface.create(typeface, this.typeface?.style ?: Typeface.NORMAL)
    }
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            getChildAt(i).applyTypefaceRecursively(typeface)
        }
    }
}
