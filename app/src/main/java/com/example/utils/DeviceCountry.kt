package com.example.utils

import android.content.Context
import android.content.res.Resources
import android.telephony.TelephonyManager
import java.util.Currency
import java.util.Locale

/** Where the phone is from and where it is now, as ISO 3166 country codes. */
object DeviceCountry {
    private fun telephony(context: Context): TelephonyManager? =
        runCatching { context.getSystemService(TelephonyManager::class.java) }.getOrNull()

    private fun sim(context: Context): String? = runCatching { telephony(context)?.simCountryIso }.getOrNull()
    private fun network(context: Context): String? = runCatching { telephony(context)?.networkCountryIso }.getOrNull()
    private fun region(): String? = Resources.getSystem().configuration.locales[0].country

    private fun first(vararg candidates: String?): String? =
        candidates.map { it.orEmpty().trim().uppercase(Locale.ROOT) }.firstOrNull { it.length == 2 }

    /** The user's own country: the SIM's, else the mobile network's, else the device region. */
    fun home(context: Context): String? = first(sim(context), network(context), region())

    /** The country the phone is in right now: the mobile network's first, since it changes abroad. */
    fun current(context: Context): String? = first(network(context), sim(context), region())

    /** The currency of [current], e.g. "THB" in Thailand. */
    fun currency(context: Context): String? =
        current(context)?.let { runCatching { Currency.getInstance(Locale("", it)).currencyCode }.getOrNull() }
}
