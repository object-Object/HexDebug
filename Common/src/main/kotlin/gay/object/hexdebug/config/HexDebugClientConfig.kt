package gay.`object`.hexdebug.config

import at.petrak.hexcasting.api.utils.asTranslatedComponent
import com.mojang.blaze3d.platform.InputConstants
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.gui.splicing.SplicingTableScreen
import gay.`object`.hexdebug.splicing.SplicingTableAction
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.ConfigHolder
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.annotation.ConfigEntry.Category
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.*
import me.shedaniel.autoconfig.serializer.PartitioningSerializer
import me.shedaniel.autoconfig.serializer.PartitioningSerializer.GlobalData
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer
import me.shedaniel.autoconfig.util.Utils.getUnsafely
import me.shedaniel.autoconfig.util.Utils.setUnsafely
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.api.Modifier
import me.shedaniel.clothconfig2.api.ModifierKeyCode
import net.minecraft.world.InteractionResult

object HexDebugClientConfig {
    @JvmStatic
    lateinit var holder: ConfigHolder<GlobalConfig>

    @JvmStatic
    val config get() = holder.config.client

    fun init() {
        AutoConfig.getGuiRegistry(GlobalConfig::class.java).apply {
            val entryBuilder = ConfigEntryBuilder.create()

            // ModifierKeyCode
            registerTypeProvider(
                { i18n, field, config, defaults, _ -> listOf(
                    entryBuilder.startModifierKeyCodeField(
                        when (config) {
                            is ClientConfig.SplicingTableKeybinds,
                            is ClientConfig.SplicingTableKeybinds.Enlightened -> {
                                SplicingTableScreen.buttonText(field.name.camelToSnakeCase())
                            }
                            else -> i18n.asTranslatedComponent
                        },
                        getUnsafely(field, config, ConfigModifierKey()).inner,
                    )
                        .setModifierDefaultValue { (getUnsafely(field, defaults) as ConfigModifierKey).inner }
                        .setModifierSaveConsumer { setUnsafely(field, config, ConfigModifierKey(it)) }
                        .build()
                ) },
                ConfigModifierKey::class.java,
            )
        }

        holder = AutoConfig.register(
            GlobalConfig::class.java,
            PartitioningSerializer.wrap(::Toml4jConfigSerializer),
        )

        // when we change the server config in the client gui, also send it to the server config class
        holder.registerSaveListener { _, config ->
            HexDebugServerConfig.holder.config = HexDebugServerConfig.GlobalConfig(config.server)
            InteractionResult.PASS
        }
    }

    @Config(name = HexDebug.MODID)
    class GlobalConfig : GlobalData() {
        @Category("client")
        @TransitiveObject
        val client = ClientConfig()

        @Category("server")
        @TransitiveObject
        val server = HexDebugServerConfig.ServerConfig()
    }

    @Config(name = "client")
    class ClientConfig : ConfigData {
        @Tooltip
        val openDebugPort: Boolean = true

        @Tooltip
        val debugPort: Int = 4444

        @Tooltip
        val smartDebuggerSneakScroll: Boolean = true

        @Tooltip
        val debuggerDisplayMode: DebuggerDisplayMode = DebuggerDisplayMode.ENABLED

        @Tooltip
        val showDebugClientLineNumber: Boolean = false

        @Tooltip
        val invertSplicingTableScrollDirection: Boolean = false

        @Tooltip
        @CollapsibleObject
        val splicingTableKeybinds = SplicingTableKeybinds()

        @Suppress("MemberVisibilityCanBePrivate")
        class SplicingTableKeybinds {
            @Tooltip
            val enabled: Boolean = true

            @Tooltip
            val overrideVanillaArrowKeys = true

            val viewLeft = ConfigModifierKey(InputConstants.KEY_UP)
            val viewLeftPage = ConfigModifierKey(InputConstants.KEY_PAGEUP)
            val viewLeftFull = ConfigModifierKey(InputConstants.KEY_HOME)
            val viewRight = ConfigModifierKey(InputConstants.KEY_DOWN)
            val viewRightPage = ConfigModifierKey(InputConstants.KEY_PAGEDOWN)
            val viewRightFull = ConfigModifierKey(InputConstants.KEY_END)

            val cursorLeft = ConfigModifierKey(InputConstants.KEY_LEFT)
            val cursorRight = ConfigModifierKey(InputConstants.KEY_RIGHT)

            val expandSelectionLeft = ConfigModifierKey(InputConstants.KEY_LEFT, shift = true)
            val expandSelectionRight = ConfigModifierKey(InputConstants.KEY_RIGHT, shift = true)
            val moveSelectionLeft = ConfigModifierKey(InputConstants.KEY_LEFT, ctrl = true, shift = true)
            val moveSelectionRight = ConfigModifierKey(InputConstants.KEY_RIGHT, ctrl = true, shift = true)

            val selectNone = ConfigModifierKey(InputConstants.KEY_A, ctrl = true, shift = true)
            val selectAll = ConfigModifierKey(InputConstants.KEY_A, ctrl = true)

            val undo = ConfigModifierKey(InputConstants.KEY_Z, ctrl = true)
            val redo = ConfigModifierKey(InputConstants.KEY_Y, ctrl = true)

            val nudgeLeft = ConfigModifierKey(InputConstants.KEY_LEFT, ctrl = true)
            val nudgeRight = ConfigModifierKey(InputConstants.KEY_RIGHT, ctrl = true)

            val duplicate = ConfigModifierKey(InputConstants.KEY_D, ctrl = true)
            val delete = ConfigModifierKey(InputConstants.KEY_DELETE)
            val backspace = ConfigModifierKey(InputConstants.KEY_BACKSPACE)

            val cut = ConfigModifierKey(InputConstants.KEY_X, ctrl = true)
            val copy = ConfigModifierKey(InputConstants.KEY_C, ctrl = true)
            val pasteSplat = ConfigModifierKey(InputConstants.KEY_V, ctrl = true)
            val pasteVerbatim = ConfigModifierKey(InputConstants.KEY_V, ctrl = true, shift = true)

            @Tooltip
            @CollapsibleObject
            val enlightened = Enlightened()

            fun getActionForKey(keyCode: Int, scanCode: Int): SplicingTableAction? {
                for ((key, action) in arrayOf(
                    viewLeft to SplicingTableAction.VIEW_LEFT,
                    viewLeftPage to SplicingTableAction.VIEW_LEFT_PAGE,
                    viewLeftFull to SplicingTableAction.VIEW_LEFT_FULL,
                    viewRight to SplicingTableAction.VIEW_RIGHT,
                    viewRightPage to SplicingTableAction.VIEW_RIGHT_PAGE,
                    viewRightFull to SplicingTableAction.VIEW_RIGHT_FULL,
                    cursorLeft to SplicingTableAction.CURSOR_LEFT,
                    cursorRight to SplicingTableAction.CURSOR_RIGHT,
                    expandSelectionLeft to SplicingTableAction.EXPAND_SELECTION_LEFT,
                    expandSelectionRight to SplicingTableAction.EXPAND_SELECTION_RIGHT,
                    moveSelectionLeft to SplicingTableAction.MOVE_SELECTION_LEFT,
                    moveSelectionRight to SplicingTableAction.MOVE_SELECTION_RIGHT,
                    selectNone to SplicingTableAction.SELECT_NONE,
                    selectAll to SplicingTableAction.SELECT_ALL,
                    undo to SplicingTableAction.UNDO,
                    redo to SplicingTableAction.REDO,
                    nudgeLeft to SplicingTableAction.NUDGE_LEFT,
                    nudgeRight to SplicingTableAction.NUDGE_RIGHT,
                    duplicate to SplicingTableAction.DUPLICATE,
                    delete to SplicingTableAction.DELETE,
                    backspace to SplicingTableAction.BACKSPACE,
                    cut to SplicingTableAction.CUT,
                    copy to SplicingTableAction.COPY,
                    pasteSplat to SplicingTableAction.PASTE_SPLAT,
                    pasteVerbatim to SplicingTableAction.PASTE_VERBATIM,
                )) {
                    if (key.matchesKey(keyCode, scanCode)) {
                        return action
                    }
                }
                return null
            }

            class Enlightened {
                val cast = ConfigModifierKey(InputConstants.KEY_RETURN, ctrl = true)
            }
        }
    }
}

enum class DebuggerDisplayMode {
    DISABLED,
    NOT_CONNECTED,
    ENABLED,
}

// NOTE: this must have a no-arg constructor
// otherwise, Gson uses unsafe allocation to construct it, which breaks the lazy property
data class ConfigModifierKey(
    val key: String = InputConstants.UNKNOWN.name,
    val alt: Boolean = false,
    val ctrl: Boolean = false,
    val shift: Boolean = false,
) {
    constructor(
        keyCode: Int,
        alt: Boolean = false,
        ctrl: Boolean = false,
        shift: Boolean = false,
    ) : this(
        key = InputConstants.Type.KEYSYM.getOrCreate(keyCode).name,
        alt = alt,
        ctrl = ctrl,
        shift = shift,
    )

    constructor(inner: ModifierKeyCode) : this(
        key = inner.keyCode.name,
        alt = inner.modifier.hasAlt(),
        ctrl = inner.modifier.hasControl(),
        shift = inner.modifier.hasShift(),
    )

    // transient to make Gson and Toml4j ignore the backing field
    @delegate:Transient
    val inner: ModifierKeyCode by lazy {
        ModifierKeyCode.of(
            InputConstants.getKey(key),
            Modifier.of(alt, ctrl, shift),
        )
    }

    fun matchesKey(keyCode: Int, scanCode: Int) = inner.matchesKey(keyCode, scanCode)
}

// https://stackoverflow.com/a/60010299
private val camelRegex = "(?<=[a-zA-Z])[A-Z]".toRegex()

private fun String.camelToSnakeCase() = replace(camelRegex) { "_${it.value}" }.lowercase()
