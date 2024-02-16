package com.pepej.gammanetwork.shadow.entity

import com.pepej.papi.shadow.Field
import com.pepej.papi.shadow.Shadow
import com.pepej.papi.shadow.bukkit.NmsClassTarget

@NmsClassTarget("EntityPlayer")
interface EntityPlayerShadow : Shadow {

    @Field
    fun getPing(): Int

    @Field
    fun getLastSentExp(): Int
}