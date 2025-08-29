package com.pepej.gammanetwork.utils

import com.pepej.papi.services.Services


inline fun <reified T> getServiceUnchecked(): T {
    return Services.getNullable(T::class.java)!!
}



