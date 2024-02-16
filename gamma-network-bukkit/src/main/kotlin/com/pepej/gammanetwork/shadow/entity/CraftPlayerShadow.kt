package com.pepej.gammanetwork.shadow.entity

import com.pepej.papi.shadow.Shadow
import com.pepej.papi.shadow.Target
import com.pepej.papi.shadow.bukkit.BukkitShadowFactory
import com.pepej.papi.shadow.bukkit.ObcClassTarget
import org.bukkit.entity.Player
import org.jetbrains.annotations.Contract

@ObcClassTarget("entity.CraftPlayer")
interface CraftPlayerShadow : Shadow {

    fun getHandle(): EntityPlayerShadow


    companion object {
        @Contract("!null -> new;")
        @JvmStatic
        fun create(handle: Player): CraftPlayerShadow {
            return BukkitShadowFactory.global().shadow(CraftPlayerShadow::class.java, handle)

        }
    }
}