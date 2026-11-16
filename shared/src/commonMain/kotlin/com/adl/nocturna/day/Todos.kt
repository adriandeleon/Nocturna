package com.adl.nocturna.day

import com.adl.nocturna.data.Settings
import com.adl.nocturna.data.TodoItem
import kotlinx.datetime.LocalDate

/*
 * The to-do list on day mode's sticky note: a few lines to tick off. A ticked item stays crossed off for
 * the rest of the day, so you can see what you got done, and is gone the next.
 */

/** The items to show on [today]: everything still to do, and what was ticked off today. */
fun todosForDay(items: List<TodoItem>, today: LocalDate): List<TodoItem> =
    items.filter { !it.done || it.doneOn == today.toString() }

/** Ticks the item at [index] off on [today], or back on when it was done; other items are left as they are. */
fun toggleTodo(items: List<TodoItem>, index: Int, today: LocalDate): List<TodoItem> = items.mapIndexed { i, item ->
    if (i != index) item else item.copy(doneOn = if (item.done) "" else today.toString())
}

/**
 * [items] with [text] added at the end: trimmed and cut to [Settings.MAX_TODO_LENGTH]. Items done before
 * [today] are dropped on the way, and when the list is full the new one isn't added. Blank text adds nothing.
 */
fun addTodo(items: List<TodoItem>, text: String, today: LocalDate): List<TodoItem> {
    val clean = text.trim().replace(Regex("\\s+"), " ").take(Settings.MAX_TODO_LENGTH)
    val kept = todosForDay(items, today)
    if (clean.isEmpty() || kept.size >= Settings.MAX_TODOS) return kept
    return kept + TodoItem(clean)
}

/** [items] without the one at [index]. */
fun removeTodo(items: List<TodoItem>, index: Int): List<TodoItem> = items.filterIndexed { i, _ -> i != index }
