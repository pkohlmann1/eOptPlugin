package com.github.pkohlmann1.testplugin

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.InspectionBundle"

object InspectionBundle : DynamicBundle(BUNDLE) {

    fun message(key: @PropertyKey(resourceBundle = BUNDLE) String, vararg params: Any) =
        getMessage(key, *params)

}