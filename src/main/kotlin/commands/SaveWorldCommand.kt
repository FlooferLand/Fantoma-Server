package commands

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.minestom.server.command.builder.Command
import net.minestom.server.entity.Player

class SaveWorldCommand : Command("save-world", "save-all") {
    init {
        setDefaultExecutor() { sender, context ->
            if (sender is Player) {
                sender.instance.sendMessage(Component.text("Saved the server!"))
                sender.instance.saveChunksToStorage()
            }
        }
    }
}