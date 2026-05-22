package com.example.solitarefore_ink

import android.content.Context
import com.google.gson.Gson

data class SolitaireSettings(
    val drawMode: Int,
    val cardBackStyle: CardBackStyle,
    val autoPlace: Boolean,
    val autoComplete: Boolean
)

class PersistenceManager(context: Context) {
    private val prefs = context.getSharedPreferences("solitaire_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveGameState(state: GameState) {
        val json = gson.toJson(state)
        prefs.edit().putString("game_state", json).apply()
    }

    fun loadGameState(): GameState? {
        val json = prefs.getString("game_state", null) ?: return null
        return try {
            gson.fromJson(json, GameState::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun saveSettings(drawMode: Int, cardBackStyle: CardBackStyle, autoPlace: Boolean, autoComplete: Boolean) {
        prefs.edit()
            .putInt("draw_mode", drawMode)
            .putString("card_back_style", cardBackStyle.name)
            .putBoolean("auto_place", autoPlace)
            .putBoolean("auto_complete", autoComplete)
            .apply()
    }

    fun loadSettings(): SolitaireSettings {
        val drawMode = prefs.getInt("draw_mode", 1)
        val cardBackStyleName = prefs.getString("card_back_style", CardBackStyle.CROSSHATCH.name)
        val autoPlace = prefs.getBoolean("auto_place", true)
        val autoComplete = prefs.getBoolean("auto_complete", true)
        val style = try {
            CardBackStyle.valueOf(cardBackStyleName!!)
        } catch (e: Exception) {
            CardBackStyle.CROSSHATCH
        }
        return SolitaireSettings(drawMode, style, autoPlace, autoComplete)
    }
    
    fun clearGameState() {
        prefs.edit().remove("game_state").apply()
    }
}
