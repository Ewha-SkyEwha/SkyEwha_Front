package com.h.trendie

import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar
import com.h.trendie.ui.theme.applyTopInsetPadding

fun AppCompatActivity.setupSimpleToolbar(@StringRes titleRes: Int? = null) {
    if (setupMaterialToolbar(titleRes, null)) return
    setupLegacyToolbar(titleRes, null)
}

fun AppCompatActivity.setupSimpleToolbarText(title: String) {
    val tb = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.headerToolbar)
        ?: findViewById(R.id.toolbar) ?: return
    tb.title = title
    tb.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

    androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(tb) { v, insets ->
        val top = insets.getInsets(
            androidx.core.view.WindowInsetsCompat.Type.statusBars()
                    or androidx.core.view.WindowInsetsCompat.Type.displayCutout()
        ).top
        v.setPadding(v.paddingLeft, top, v.paddingRight, v.paddingBottom)
        insets
    }
}

private fun AppCompatActivity.setupMaterialToolbar(
    @StringRes titleRes: Int?,
    titleText: CharSequence?
): Boolean {
    val tb = findMaterialToolbar() ?: return false
    tb.menu?.clear()
    when {
        titleRes != null -> tb.setTitle(titleRes)
        !titleText.isNullOrBlank() -> tb.title = titleText
    }
    tb.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    ViewCompat.setOnApplyWindowInsetsListener(tb) { v, insets ->
        val top = insets.getInsets(
            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
        ).top
        v.updatePadding(top = top)
        insets
    }
    return true
}

private fun AppCompatActivity.findMaterialToolbar(): MaterialToolbar? {
    val nameCandidates = listOf("headerToolbar", "fbToolbar", "bookmarkToolbar", "toolbar")
    for (name in nameCandidates) {
        val id = idOrNull(name) ?: continue
        val v = findViewById<View?>(id)
        if (v is MaterialToolbar) return v
    }
    val dynId = resources.getIdentifier("toolbar", "id", packageName)
    if (dynId != 0) {
        val v = findViewById<View?>(dynId)
        if (v is MaterialToolbar) return v
    }
    return null
}

private fun AppCompatActivity.setupLegacyToolbar(
    @StringRes titleRes: Int?,
    titleText: CharSequence?
) {
    val root = findLegacyToolbarRoot() ?: return
    root.applyTopInsetPadding()

    val tvTitleId = idOrNull("tvTitle")
    val btnBackId = idOrNull("btnBack")

    val tvTitle = tvTitleId?.let { root.findViewById<TextView?>(it) }
    when {
        titleRes != null -> tvTitle?.setText(titleRes)
        !titleText.isNullOrBlank() -> tvTitle?.text = titleText
    }

    val btnBack = btnBackId?.let { root.findViewById<View?>(it) }
    btnBack?.setOnClickListener { finish() }
}

private fun AppCompatActivity.findLegacyToolbarRoot(): View? {
    val nameCandidates = listOf("settingsToolbar", "prefToolbar", "headerToolbar", "fbToolbar", "bookmarkToolbar", "toolbar")
    for (name in nameCandidates) {
        val id = idOrNull(name) ?: continue
        val v = findViewById<View?>(id) ?: continue
        val hasChild = (idOrNull("tvTitle")?.let { v.findViewById<View?>(it) } != null) ||
                (idOrNull("btnBack")?.let { v.findViewById<View?>(it) } != null)
        if (hasChild) return v
    }
    return null
}

@IdRes
private fun AppCompatActivity.idOrNull(name: String): Int? {
    val id = resources.getIdentifier(name, "id", packageName)
    return if (id != 0) id else null
}
