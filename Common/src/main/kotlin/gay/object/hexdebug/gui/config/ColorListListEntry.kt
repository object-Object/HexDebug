package gay.`object`.hexdebug.gui.config

import at.petrak.hexcasting.api.utils.asTranslatedComponent
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.gui.entries.AbstractTextFieldListListEntry
import me.shedaniel.clothconfig2.gui.widget.ColorDisplayWidget
import me.shedaniel.clothconfig2.impl.builders.AbstractListBuilder
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import java.util.*
import java.util.function.Consumer
import java.util.function.Supplier

// note: alpha support is not implemented
class ColorListListEntry(
    fieldName: Component,
    value: List<Int>,
    defaultExpanded: Boolean,
    tooltipSupplier: Supplier<Optional<Array<Component>>>?,
    saveConsumer: Consumer<List<Int>>,
    defaultValue: Supplier<List<Int>>?,
    resetButtonKey: Component,
    requiresRestart: Boolean = false,
    deleteButtonEnabled: Boolean = true,
    insertInFront: Boolean = true,
) : AbstractTextFieldListListEntry<Int, ColorListListEntry.ColorListCell, ColorListListEntry>(
    fieldName,
    value,
    defaultExpanded,
    tooltipSupplier,
    saveConsumer,
    defaultValue,
    resetButtonKey,
    requiresRestart,
    deleteButtonEnabled,
    insertInFront,
    ::ColorListCell,
) {
    override fun self() = this

    class ColorListCell(
        value: Int,
        listListEntry: ColorListListEntry,
    ) : AbstractTextFieldListCell<Int, ColorListCell, ColorListListEntry>(
        value,
        listListEntry,
    ) {
        init {
            widget.value = "#" + value.toUInt().toString(16)
        }

        private val colorWidgetSize = widget.height
        private val colorWidget = ColorDisplayWidget(widget, 0, 0, colorWidgetSize, getValue())

        override fun render(
            graphics: GuiGraphics,
            index: Int,
            y: Int,
            x: Int,
            entryWidth: Int,
            entryHeight: Int,
            mouseX: Int,
            mouseY: Int,
            isSelected: Boolean,
            delta: Float
        ) {
            val textX = if (Minecraft.getInstance().font.isBidirectional) {
                colorWidget.x = x + entryWidth - colorWidgetSize
                x
            } else {
                colorWidget.x = x
                x + colorWidgetSize + 3
            }
            super.render(graphics, index, y, textX, entryWidth - colorWidgetSize - 3, entryHeight, mouseX, mouseY, isSelected, delta)
            colorWidget.y = y - 4
            colorWidget.setColor(0xff000000.toInt() or value)
            colorWidget.render(graphics, mouseX, mouseY, delta)
        }

        override fun getError(): Optional<Component> {
            if (unsignedValue == null) {
                return Optional.of("text.cloth-config.error.color.invalid_color".asTranslatedComponent)
            }
            return Optional.empty()
        }

        override fun getValue(): Int = unsignedValue?.toInt() ?: -1

        // this must return valid for "" and "#", otherwise you can't clear the field
        override fun isValidText(text: String): Boolean =
            text.removePrefix("#").isEmpty() || parseColor(text) != null

        override fun substituteDefault(value: Int?): Int = value ?: 0

        private val unsignedValue get() = parseColor(widget.value)

        private fun parseColor(color: String): UInt? {
            return try {
                if (color.startsWith("#")) {
                    color.removePrefix("#").takeIf { it.length <= 6 }?.toUInt(16)
                } else {
                    color.toUInt()
                }?.takeIf { it <= 0xffffffu }
            } catch (e: NumberFormatException) {
                null
            }
        }
    }
}

class ColorListBuilder(
    resetButtonKey: Component,
    fieldNameKey: Component,
) : AbstractListBuilder<Int, ColorListListEntry, ColorListBuilder>(
    resetButtonKey,
    fieldNameKey,
) {
    constructor(
        resetButtonKey: Component,
        fieldNameKey: Component,
        value: List<Int>,
    ) : this(resetButtonKey, fieldNameKey) {
        this.value = value
    }

    override fun build(): ColorListListEntry {
        val entry = ColorListListEntry(
            fieldName = fieldNameKey,
            value = value,
            defaultExpanded = isExpanded,
            tooltipSupplier = null,
            saveConsumer = saveConsumer,
            defaultValue = defaultValue,
            resetButtonKey = resetButtonKey,
            requiresRestart = isRequireRestart,
            deleteButtonEnabled = isDeleteButtonEnabled,
            insertInFront = isInsertInFront,
        )
        entry.isInsertButtonEnabled = isInsertButtonEnabled
        entry.cellErrorSupplier = cellErrorSupplier
        entry.setTooltipSupplier { tooltipSupplier.apply(entry.value) }
        entry.addTooltip = addTooltip
        entry.removeTooltip = removeTooltip
        errorSupplier?.let { entry.setErrorSupplier { it.apply(entry.value) } }
        return finishBuilding(entry)
    }
}

fun ConfigEntryBuilder.startColorList(fieldNameKey: Component, value: List<Int>) =
    ColorListBuilder(resetButtonKey, fieldNameKey, value)
