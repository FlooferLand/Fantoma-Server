import net.minestom.server.instance.anvil.AnvilLoader
import java.nio.file.Path

public inline fun AnvilLoader.getLevelPath(): Path {
    return javaClass.getDeclaredField("path").let {
        it.isAccessible = true
        val value = it.get(this) as Path
        return@let value;
    }
}
