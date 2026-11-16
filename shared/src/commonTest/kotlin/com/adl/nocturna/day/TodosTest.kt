package com.adl.nocturna.day

import com.adl.nocturna.data.Settings
import com.adl.nocturna.data.TodoItem
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class TodosTest {
    private val today = LocalDate(2026, 11, 16)

    @Test fun doneItemsStayForTheDayAndGoTheNext() {
        val items = listOf(TodoItem("Call the bank"), TodoItem("Pay rent", doneOn = "2026-11-16"), TodoItem("Old", doneOn = "2026-11-15"))
        assertEquals(listOf(TodoItem("Call the bank"), TodoItem("Pay rent", doneOn = "2026-11-16")), todosForDay(items, today))
    }

    @Test fun tickingAndUnticking() {
        val ticked = toggleTodo(listOf(TodoItem("A"), TodoItem("B")), 1, today)
        assertEquals(listOf(TodoItem("A"), TodoItem("B", "2026-11-16")), ticked)
        assertEquals(listOf(TodoItem("A"), TodoItem("B")), toggleTodo(ticked, 1, today))
    }

    @Test fun addingTrimsAndDropsOldDoneItems() {
        val items = listOf(TodoItem("Old", doneOn = "2026-11-10"))
        assertEquals(listOf(TodoItem("Buy milk")), addTodo(items, "  Buy   milk ", today))
    }

    @Test fun blankAddsNothing() = assertEquals(emptyList(), addTodo(emptyList(), "   ", today))

    @Test fun aFullListTakesNoMore() {
        val full = List(Settings.MAX_TODOS) { TodoItem("Item $it") }
        assertEquals(full, addTodo(full, "One more", today))
    }

    @Test fun longTextIsCut() =
        assertEquals(Settings.MAX_TODO_LENGTH, addTodo(emptyList(), "x".repeat(200), today).single().text.length)

    @Test fun removing() = assertEquals(listOf(TodoItem("A")), removeTodo(listOf(TodoItem("A"), TodoItem("B")), 1))
}
