package gay.`object`.hexdebug.config

import at.petrak.hexcasting.api.utils.asTranslatedComponent
import com.mojang.blaze3d.platform.InputConstants
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.gui.splicing.SplicingTableScreen
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
                            is ClientConfig.EnlightenedSplicingTableKeybinds -> {
                                SplicingTableScreen.buttonText(field.name.camelToSnakeCase())
                            }
                            else -> i18n.asTranslatedComponent
                        },
                        getUnsafely(field, config, ConfigModifierKey(InputConstants.UNKNOWN.name)).inner,
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

        @Tooltip
        @CollapsibleObject
        val enlightenedSplicingTableKeybinds = EnlightenedSplicingTableKeybinds()

        class SplicingTableKeybinds {
            val selectNone = ConfigModifierKey(InputConstants.KEY_A, ctrl = true, shift = true)
            val selectAll = ConfigModifierKey(InputConstants.KEY_A, ctrl = true)
            val undo = ConfigModifierKey(InputConstants.KEY_Z, ctrl = true)
            val redo = ConfigModifierKey(InputConstants.KEY_Y, ctrl = true)
            val nudgeLeft = ConfigModifierKey(InputConstants.KEY_LEFT, ctrl = true)
            val nudgeRight = ConfigModifierKey(InputConstants.KEY_RIGHT, ctrl = true)
            val duplicate = ConfigModifierKey(InputConstants.KEY_D, ctrl = true)
            val delete = ConfigModifierKey(InputConstants.KEY_DELETE)
            val cut = ConfigModifierKey(InputConstants.KEY_X, ctrl = true)
            val copy = ConfigModifierKey(InputConstants.KEY_C, ctrl = true)
            val pasteSplat = ConfigModifierKey(InputConstants.KEY_V, ctrl = true)
            val pasteVerbatim = ConfigModifierKey(InputConstants.KEY_V, ctrl = true, shift = true)
        }

        class EnlightenedSplicingTableKeybinds {
            val cast = ConfigModifierKey(InputConstants.KEY_RETURN, ctrl = true)
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
