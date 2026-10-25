package com.reverb.app.field

import com.reverb.app.service.ReverbAccessibilityService
import com.reverb.core.domain.field.FieldSnapshot
import com.reverb.core.domain.field.TextFieldGateway

/** Reaches the focused text field of any app through [ReverbAccessibilityService]. */
object AccessibilityTextFieldGateway : TextFieldGateway {

    override val isAvailable: Boolean
        get() = ReverbAccessibilityService.instance != null

    override fun read(): FieldSnapshot? = ReverbAccessibilityService.instance?.readFocusedField()

    override fun write(snapshot: FieldSnapshot): Boolean =
        ReverbAccessibilityService.instance?.writeFocusedField(snapshot) ?: false
}
