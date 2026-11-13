package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AccountOpeningBalances
import com.example.domain.util.PinHashUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.hisabDataStore: DataStore<Preferences> by preferencesDataStore(name = "hisab_settings")

data class PinLockConfig(
    val isEnabled: Boolean = false,
    val salt: String = "",
    val hash: String = ""
)

class OpeningBalanceDataStore(private val context: Context) {
    private val legacyOpeningBalanceKey = doublePreferencesKey("opening_balance")
    private val cashOpeningKey = doublePreferencesKey("opening_cash")
    private val bankOpeningKey = doublePreferencesKey("opening_bank")
    private val upiOpeningKey = doublePreferencesKey("opening_upi")

    private val pinEnabledKey = booleanPreferencesKey("pin_enabled")
    private val pinSaltKey = stringPreferencesKey("pin_salt")
    private val pinHashKey = stringPreferencesKey("pin_hash")

    val accountOpenings: Flow<AccountOpeningBalances> = context.hisabDataStore.data.map { prefs ->
        val legacy = prefs[legacyOpeningBalanceKey] ?: 0.0
        val cash = prefs[cashOpeningKey] ?: legacy
        val bank = prefs[bankOpeningKey] ?: 0.0
        val upi = prefs[upiOpeningKey] ?: 0.0
        AccountOpeningBalances(
            cash = if (cash.isFinite() && cash >= 0.0) cash else 0.0,
            bank = if (bank.isFinite() && bank >= 0.0) bank else 0.0,
            upi = if (upi.isFinite() && upi >= 0.0) upi else 0.0
        )
    }

    val openingBalance: Flow<Double> = accountOpenings.map { it.total }

    val pinLockConfig: Flow<PinLockConfig> = context.hisabDataStore.data.map { prefs ->
        PinLockConfig(
            isEnabled = prefs[pinEnabledKey] ?: false,
            salt = prefs[pinSaltKey] ?: "",
            hash = prefs[pinHashKey] ?: ""
        )
    }

    suspend fun setOpeningBalance(amount: Double) {
        val safeAmount = if (amount.isFinite() && amount >= 0.0) amount else 0.0
        context.hisabDataStore.edit { prefs ->
            prefs[cashOpeningKey] = safeAmount
            prefs[legacyOpeningBalanceKey] = safeAmount + (prefs[bankOpeningKey] ?: 0.0) + (prefs[upiOpeningKey] ?: 0.0)
        }
    }

    suspend fun setAccountOpenings(openings: AccountOpeningBalances) {
        val safeCash = if (openings.cash.isFinite() && openings.cash >= 0.0) openings.cash else 0.0
        val safeBank = if (openings.bank.isFinite() && openings.bank >= 0.0) openings.bank else 0.0
        val safeUpi = if (openings.upi.isFinite() && openings.upi >= 0.0) openings.upi else 0.0
        context.hisabDataStore.edit { prefs ->
            prefs[cashOpeningKey] = safeCash
            prefs[bankOpeningKey] = safeBank
            prefs[upiOpeningKey] = safeUpi
            prefs[legacyOpeningBalanceKey] = safeCash + safeBank + safeUpi
        }
    }

    suspend fun setPin(pin: String) {
        require(PinHashUtil.isValidPinFormat(pin)) { "PIN must be 4 digits" }
        val salt = PinHashUtil.generateSalt()
        val hash = PinHashUtil.hashPin(pin, salt)
        context.hisabDataStore.edit { prefs ->
            prefs[pinEnabledKey] = true
            prefs[pinSaltKey] = salt
            prefs[pinHashKey] = hash
        }
    }

    suspend fun disablePin() {
        context.hisabDataStore.edit { prefs ->
            prefs[pinEnabledKey] = false
            prefs.remove(pinSaltKey)
            prefs.remove(pinHashKey)
        }
    }
}
