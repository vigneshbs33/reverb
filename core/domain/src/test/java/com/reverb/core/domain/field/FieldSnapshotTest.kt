package com.reverb.core.domain.field

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldSnapshotTest {

    @Test
    fun `target is the whole text when nothing is selected`() {
        val snapshot = FieldSnapshot("hello world")
        assertFalse(snapshot.hasSelection)
        assertEquals("hello world", snapshot.target)
    }

    @Test
    fun `a collapsed cursor is not a selection`() {
        assertFalse(FieldSnapshot("hello", 3, 3).hasSelection)
    }

    @Test
    fun `target is the selection when there is one`() {
        assertEquals("world", FieldSnapshot("hello world", 6, 11).target)
    }

    @Test
    fun `reversed selection is normalised`() {
        assertEquals("world", FieldSnapshot("hello world", 11, 6).target)
    }

    @Test
    fun `out of range selection is ignored`() {
        val snapshot = FieldSnapshot("hello", 2, 40)
        assertNull(snapshot.selectionRange)
        assertEquals("hello", snapshot.target)
    }

    @Test
    fun `replacing a selection keeps the text around it`() {
        val updated = FieldSnapshot("Hi team, pls fix asap. Thanks", 9, 22).replaceTarget("Please fix this soon.")
        assertEquals("Hi team, Please fix this soon. Thanks", updated.text)
        assertFalse(updated.hasSelection)
    }

    @Test
    fun `replacing without a selection replaces the whole text`() {
        assertEquals("new", FieldSnapshot("old text").replaceTarget("new").text)
    }

    @Test
    fun `selection at the very start and end of the text works`() {
        assertTrue(FieldSnapshot("abc", 0, 3).hasSelection)
        assertEquals("X", FieldSnapshot("abc", 0, 3).replaceTarget("X").text)
    }
}
