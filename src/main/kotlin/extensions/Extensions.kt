package extensions

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.anvil.AnvilLoader
import java.nio.file.Path

public inline fun AnvilLoader.getLevelPath(): Path {
    return javaClass.getDeclaredField("path").let {
        it.isAccessible = true
        val value = it.get(this) as Path
        return@let value;
    }
}

public inline fun Point.up(): Point {
    return this.add(0.0, 1.0, 0.0)
}
public inline fun Point.down(): Point {
    return this.sub(0.0, 1.0, 0.0)
}
