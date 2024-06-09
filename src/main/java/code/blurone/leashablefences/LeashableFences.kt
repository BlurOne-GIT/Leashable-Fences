package code.blurone.leashablefences

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LeashHitch
import org.bukkit.entity.Tadpole
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.event.entity.EntityTeleportEvent
import org.bukkit.event.entity.EntityUnleashEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

@Suppress("unused")
class LeashableFences : JavaPlugin(), Listener {
    private val isFenceTadpoleNamespacedKey = NamespacedKey(this, "is_fence_tadpole")

    override fun onEnable() {
        // Plugin startup logic
        server.pluginManager.registerEvents(this, this)
    }

    private fun isFenceTadpole(entity: Entity) =
        entity.type == EntityType.TADPOLE && entity.persistentDataContainer.has(isFenceTadpoleNamespacedKey)

    @EventHandler
    private fun onPlayerInteract(event: PlayerInteractEvent) {
        if (
            event.action != Action.RIGHT_CLICK_BLOCK ||
            event.player.gameMode.ordinal > 1 ||
            event.item?.type != Material.LEAD ||
            !Tag.FENCES.isTagged(event.clickedBlock!!.type)
        ) return

        val world = event.clickedBlock!!.world
        val location = event.clickedBlock!!.location
        val leashHitch = world.getNearbyEntities(location, .5, .5, .5) { it is LeashHitch }.firstOrNull() ?:
            world.spawnEntity(event.clickedBlock!!.location, EntityType.LEASH_HITCH) as LeashHitch

        val tadpole = world.spawnEntity(leashHitch.location, EntityType.TADPOLE) as Tadpole
        tadpole.apply {
            setAI(false)
            isInvisible = true
            isSilent = true
            isCollidable = false
            isInvulnerable = true
            isPersistent = true
            persistentDataContainer.set(isFenceTadpoleNamespacedKey, PersistentDataType.BOOLEAN, true)
        }

        object : BukkitRunnable() {
            override fun run() {
                tadpole.setLeashHolder(event.player)
            }
        }.runTaskLater(this, 0L)

        if (event.hand == EquipmentSlot.HAND)
            event.player.swingMainHand()
        else
            event.player.swingOffHand()

        if (event.player.gameMode != GameMode.CREATIVE)
            --event.item!!.amount
    }

    @EventHandler
    private fun onEntityRemoved(@Suppress("UnstableApiUsage") event: EntityRemoveEvent) {
        if (event.entityType != EntityType.LEASH_HITCH) return

        for (entity in event.entity.getNearbyEntities(.5, .5, .5))
            if (entity.type == EntityType.TADPOLE && entity.persistentDataContainer.has(isFenceTadpoleNamespacedKey))
                entity.remove()
    }

    @EventHandler
    private fun onEntityTeleport(event: EntityTeleportEvent) { event.isCancelled = isFenceTadpole(event.entity) }

    @EventHandler
    private fun onEntityUnleashed(event: EntityUnleashEvent) {
        if (isFenceTadpole(event.entity))
            event.entity.remove()
    }
}
