package com.reverb.core.domain.field

/**
 * Read and write access to the text field the user is typing in.
 *
 * On Android the default implementation goes through an accessibility service, but any
 * host (a keyboard, an in-app editor, a desktop client) can provide its own.
 */
interface TextFieldGateway {
    /** False when the host cannot reach any text field right now (for example a disabled service). */
    val isAvailable: Boolean

    /** Reads the focused editable field, or returns null when there is none. */
    fun read(): FieldSnapshot?

    /**
     * Replaces the whole field with [snapshot]'s text and restores its selection when it has one.
     * Returns true when the host accepted the change.
     */
    fun write(snapshot: FieldSnapshot): Boolean
}
