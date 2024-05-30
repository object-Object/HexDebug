package gay.`object`.hexdebug.blocks.splicing

import net.minecraft.world.SimpleContainer

class ClientSplicingTable : SimpleContainer(ISplicingTable.CONTAINER_SIZE), ISplicingTable {
    override var iotaHolder by iotaHolderDelegate()
    override var clipboard by clipboardDelegate()
}
