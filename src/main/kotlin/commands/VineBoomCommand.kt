package commands

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.minestom.server.command.builder.Command
import net.minestom.server.entity.Player

class VineBoomCommand : Command("vineBoom") {
    init {
        setDefaultExecutor() { sender, context ->
            if (sender is Player) {
                // TODO: Play vine boom for all players
                sender.instance.playSound(Sound.sound(Key.key("silly.vineboom"), Sound.Source.PLAYER, 1.0f, 1.0f), sender.position)
            }
        }
    }
}