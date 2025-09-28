package gay.`object`.hexdebug.config

import at.petrak.hexcasting.api.utils.asTranslatedComponent
import com.mojang.blaze3d.platform.InputConstants
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.gui.config.startColorList
import gay.`object`.hexdebug.gui.splicing.SplicingTableScreen
import gay.`object`.hexdebug.splicing.SplicingTableAction
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.ConfigHolder
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.annotation.ConfigEntry.Category
import me.shedaniel.autoconfig.annotation.ConfigEntry.ColorPicker
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
import java.lang.reflect.ParameterizedType

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
                { i18n, field, config, defaults, _ ->
                    entryBuilder.startModifierKeyCodeField(
                        when (config) {
                            is ClientConfig.SplicingTableKeybinds,
                            is ClientConfig.SplicingTableKeybinds.Enlightened -> {
                                SplicingTableScreen.buttonText(field.name.camelToSnakeCase(), null)
                            }
                            else -> i18n.asTranslatedComponent
                        },
                        getUnsafely(field, config, ConfigModifierKey()).inner,
                    )
                        .setModifierDefaultValue { (getUnsafely(field, defaults) as ConfigModifierKey).inner }
                        .setModifierSaveConsumer { setUnsafely(field, config, ConfigModifierKey(it)) }
                        .build()
                        .toList()
                },
                ConfigModifierKey::class.java,
            )

            // list of color pickers
            registerAnnotationProvider(
                { i18n, field, config, defaults, _ ->
                    entryBuilder.startColorList(i18n.asTranslatedComponent, getUnsafely(field, config))
                        .setDefaultValue { getUnsafely(field, defaults) }
                        .setSaveConsumer { setUnsafely(field, config, it) }
                        .build()
                        .toList()
                },
                { field ->
                    val typeArgs = (field.genericType as? ParameterizedType)?.actualTypeArguments
                    List::class.java.isAssignableFrom(field.type)
                        && typeArgs?.size == 1
                        && typeArgs[0] == Integer::class.java
                },
                ColorPicker::class.java,
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
        val enableSplicingTableRainbowBrackets: Boolean = true

        // split Turbo into 8 samples, took the middle 6
        @Tooltip
        @ColorPicker
        val rainbowBracketColors: List<Int> = listOf(
            0xda3907,
            0xfe9b2d,
            0xd1e934,
            0x62fc6b,
            0x1bcfd5,
            0x4676ee,
        )

        @Tooltip
        @CollapsibleObject
        val splicingTableKeybinds = SplicingTableKeybinds()

        @Suppress("MemberVisibilityCanBePrivate")
        class SplicingTableKeybinds {
            @Tooltip
            val enabled: Boolean = true

            @Tooltip
            val overrideVanillaArrowKeys = true

            @Tooltip
            val sendHexicalTelepathy = true

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
            @CollapsibleObject(startExpanded = true)
            val enlightened = Enlightened()

            fun getActionMap() = mapOf(
                SplicingTableAction.VIEW_LEFT to viewLeft,
                SplicingTableAction.VIEW_LEFT_PAGE to viewLeftPage,
                SplicingTableAction.VIEW_LEFT_FULL to viewLeftFull,
                SplicingTableAction.VIEW_RIGHT to viewRight,
                SplicingTableAction.VIEW_RIGHT_PAGE to viewRightPage,
                SplicingTableAction.VIEW_RIGHT_FULL to viewRightFull,
                SplicingTableAction.CURSOR_LEFT to cursorLeft,
                SplicingTableAction.CURSOR_RIGHT to cursorRight,
                SplicingTableAction.EXPAND_SELECTION_LEFT to expandSelectionLeft,
                SplicingTableAction.EXPAND_SELECTION_RIGHT to expandSelectionRight,
                SplicingTableAction.MOVE_SELECTION_LEFT to moveSelectionLeft,
                SplicingTableAction.MOVE_SELECTION_RIGHT to moveSelectionRight,
                SplicingTableAction.SELECT_NONE to selectNone,
                SplicingTableAction.SELECT_ALL to selectAll,
                SplicingTableAction.UNDO to undo,
                SplicingTableAction.REDO to redo,
                SplicingTableAction.NUDGE_LEFT to nudgeLeft,
                SplicingTableAction.NUDGE_RIGHT to nudgeRight,
                SplicingTableAction.DUPLICATE to duplicate,
                SplicingTableAction.DELETE to delete,
                SplicingTableAction.BACKSPACE to backspace,
                SplicingTableAction.CUT to cut,
                SplicingTableAction.COPY to copy,
                SplicingTableAction.PASTE_SPLAT to pasteSplat,
                SplicingTableAction.PASTE_VERBATIM to pasteVerbatim,
            )

            fun getActionForKey(keyCode: Int, scanCode: Int): SplicingTableAction? {
                for ((action, key) in getActionMap().entries) {
                    if (key.inner.matchesKey(keyCode, scanCode)) {
                        return action
                    }
                }
                return null
            }

            fun getKeyForAction(action: SplicingTableAction): ConfigModifierKey? {
                return getActionMap()[action]
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
}

// https://stackoverflow.com/a/60010299
private val camelRegex = "(?<=[a-zA-Z])[A-Z]".toRegex()

private fun String.camelToSnakeCase() = replace(camelRegex) { "_${it.value}" }.lowercase()

private fun <T> T.toList() = listOf(this)
