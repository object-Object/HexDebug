package gay.`object`.hexdebug.splicing

import net.minecraft.world.Container

interface ISplicingTable : Container {
    /** Runs the given action, and returns an updated selection (or null if the selection should be removed). */
    fun runAction(action: Action, selection: Selection?): Selection?

    companion object {
        const val CONTAINER_SIZE = 2
        const val LIST_INDEX = 0
        const val CLIPBOARD_INDEX = 1
    }

    enum class Action {
        NUDGE_LEFT,
        NUDGE_RIGHT,
        DUPLICATE,
        DELETE,
        UNDO,
        REDO,
        CUT,
        COPY,
        PASTE,
        PASTE_SPLAT,
    }
}
