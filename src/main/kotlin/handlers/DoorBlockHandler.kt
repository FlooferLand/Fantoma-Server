package handlers

import extensions.*
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.utils.NamespaceID

class DoorBlockHandler : BlockHandler {    
    override fun onPlace(placement: BlockHandler.Placement) {
        val instance = placement.instance
        val blockPos = placement.blockPosition
        val block = placement.block
        
        // Breaking if placed on air (unnecessary unless placed by the player)
        val below = instance.getBlock(blockPos.down())
        if (below.isAir) {
            instance.setBlock(blockPos, Block.AIR)
            return
        }
        
        if (!below.compare(block, Block.Comparator.ID)) {
            instance.setBlock(blockPos, block.withProperty("half", "lower"))
            instance.setBlock(blockPos.up(), block.withProperty("half", "upper"))
        }
    }
    
    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        val instance = interaction.instance
        val blockPos = interaction.blockPosition
        val block = interaction.block

        val below = instance.getBlock(blockPos.down())
        val above = instance.getBlock(blockPos.up())
        
        var neighbour: Pair<Point, Block>? = null
        if (below.compare(block, Block.Comparator.ID))
            neighbour = Pair(blockPos.down(), below)
        else if (above.compare(block, Block.Comparator.ID))
            neighbour = Pair(blockPos.up(), above)
        
        val open = if (block.getProperty("open") == "true") "false" else "true"
        if (neighbour != null) {
            instance.setBlock(blockPos, block.withProperty("open", open))
            instance.setBlock(neighbour.first, neighbour.second.withProperty("half", open))
            return true
        }
        
        return false
    }
    
    override fun getNamespaceId(): NamespaceID {
        return NamespaceID.from("minecraft:door_handler")
    }
}