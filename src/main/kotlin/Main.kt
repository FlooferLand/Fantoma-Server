import commands.SaveWorldCommand
import commands.VineBoomCommand
import dev.emortal.nbstom.NBS
import dev.lu15.voicechat.VoiceChat
import handlers.DoorBlockHandler
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import net.minestom.server.command.CommandManager
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.ItemEntity
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.item.PickupItemEvent
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerBlockBreakEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.event.trait.PlayerEvent
import net.minestom.server.extras.MojangAuth
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.anvil.AnvilLoader
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.scoreboard.Sidebar
import java.nio.file.Path
import java.time.Duration
import java.util.*
import kotlinx.coroutines.future.await
import net.minestom.server.extras.velocity.VelocityProxy
import extensions.*
import net.minestom.server.entity.Entity
import net.minestom.server.event.instance.InstanceTickEvent
import net.minestom.server.event.player.PlayerBlockPlaceEvent
import org.slf4j.LoggerFactory

val serverInfo = ServerInfo("0.0.0.0", 25568)
data class ServerInfo(val address: String, val port: Int) {
    val voicePort: Int = port - 1
}

val usingVelocity = false
var resourcePack: ResourcePackInfo? = null
var logger = LoggerFactory.getLogger("fantoma")

suspend fun main(args: Array<String>) {
    val server = MinecraftServer.init()

    // Instance
    val instanceManager = MinecraftServer.getInstanceManager()
    val instance = instanceManager.createInstanceContainer()
    val scheduler = MinecraftServer.getSchedulerManager()
    
    // Load/save
    // TODO: Add a mechanism to stop the world from saving.
    //       Separate the player server from the build server, and only let the build server (private to builders) save the world
    instance.chunkLoader = AnvilLoader("worlds/lobby")
    val saveServer = {
        instanceManager.instances.forEach() { instance ->
            logger.info("Saving world '${((instance as InstanceContainer).chunkLoader as AnvilLoader).getLevelPath()}'")
            instance.saveChunksToStorage()
        }
    }
    scheduler.buildShutdownTask(saveServer)
    scheduler.buildTask(saveServer).repeat(Duration.ofMinutes(20)).delay(Duration.ofMinutes(1)).schedule()
    
    // Chunk generator
    instance.setGenerator { unit ->
        unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK)
    }
    
    // Lighting
    instance.setChunkSupplier(::LightingChunk)

    // Events
    val globalEv = MinecraftServer.getGlobalEventHandler()
    run {
        val allNode = EventNode.all("main")
        globalEv.addChild(allNode)
        registerMiscEvents(instance, allNode)
        
        val playerNode = EventNode.type("players", EventFilter.PLAYER)
        globalEv.addChild(playerNode)
        registerPlayerEvents(instance, playerNode)
    }
    
    // Resource pack
    resourcePack = ResourcePackInfo.resourcePackInfo()
        .id(UUID.randomUUID())
        .uri({}.javaClass.getResource("packs/main/main.zip").toURI())
        .computeHashAndBuild().await()
    
    // Commands
    registerCommands(instance, MinecraftServer.getCommandManager())
    
    // Voice chat
    // TODO: Disable this for the large-scale public server
    val voiceChat = VoiceChat.builder(serverInfo.address, serverInfo.voicePort).enable()
    
    // Starting and initializing the server
    if (usingVelocity) {
        VelocityProxy.enable(System.getenv("VELOCITY"))
        logger.info("Starting server at port ${serverInfo.port}")
    } else {
        MojangAuth.init()
    }
    server.start("0.0.0.0", serverInfo.port)
}

fun registerNewBlock(block: Block) : Block {
    return when (block) {
        Block.OAK_DOOR -> block.withHandler(DoorBlockHandler())
        else -> block
    }
}

fun registerMiscEvents(instance: InstanceContainer, handler: EventNode<Event>) {
    // Item pickup
    handler.addListener(PickupItemEvent::class.java) { event ->
        val itemStack = event.itemStack
        val entity = event.livingEntity
        if (entity is Player) {
            entity.inventory.addItemStack(itemStack)
        }
    }
}

fun registerCommands(instance: InstanceContainer, manager: CommandManager) {
    manager.register(VineBoomCommand())
    manager.register(SaveWorldCommand())
}

fun registerPlayerEvents(instance: InstanceContainer, handler: EventNode<PlayerEvent>) {
    // Configuration
    handler.addListener(AsyncPlayerConfigurationEvent::class.java) { event ->
        val player = event.player
        event.spawningInstance = instance
        player.respawnPoint = Pos(0.0, 40.0 + 2.0, 0.0)

        if (resourcePack != null) {
            player.sendResourcePacks(resourcePack!!)
        } else {
            player.sendMessage("Resource pack failed to download! (Server pack is null)")
        }
    }
    
    // On spawn
    handler.addListener(PlayerSpawnEvent::class.java) { event ->
        val player = event.player
        
        // Sidebar
        Sidebar(Component.text("-- Fantoma --"))
            .createLine(Sidebar.ScoreboardLine("line_0", Component.text("Welcome ${player.name}!"), 0))

        // Doors
        event.instance.setBlock(0, 40, 5, registerNewBlock(Block.OAK_DOOR))

        // Music
        // TODO: Make an original track list in NBS for the public release
        // TODO: Make it global, so the entire server plays the same song at the same time
        val songList = arrayOf(
            "Stayed gone by michalkmiecinski544",
            "The Penis (Eek!) by Minhhy",
            "BIRDS OF A FEATHER by Trioplane"
        )
        val playing = "music/${songList.random()}.nbs"
        player.sendMessage(Component.text("Playing $playing").decorate(TextDecoration.ITALIC).color(NamedTextColor.GRAY))
        val song = NBS(Path.of({}.javaClass.getResource(playing)?.toURI()))
        NBS.play(song, player)
    }

    // On block break
    handler.addListener(PlayerBlockPlaceEvent::class.java) { event ->
        instance.setBlock(event.blockPosition.up(), registerNewBlock(event.block))
    }

    // On block break
    handler.addListener(PlayerBlockBreakEvent::class.java) { event ->
        val material = event.block.registry().material()
        if (material != null) {
            val itemStack = ItemStack.of(material)
            val entity = ItemEntity(itemStack)
            entity.setInstance(event.instance, event.blockPosition.add(0.5, 0.5, 0.5))
            entity.setPickupDelay(Duration.ofMillis(50))
        }
    }

    // On item drop
    handler.addListener(ItemDropEvent::class.java) { event ->
        val entity = ItemEntity(event.itemStack)
        entity.setInstance(event.instance, event.player.position)
        entity.setVelocity(event.player.position.add(0.0, 1.0, 0.0).direction().mul(8.0))
        entity.setPickupDelay(Duration.ofMillis(300))
    }
}
