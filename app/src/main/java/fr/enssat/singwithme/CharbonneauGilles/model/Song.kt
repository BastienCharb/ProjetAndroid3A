package fr.enssat.singwithme.CharbonneauGilles.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Song(val name: String, val artist: String, val locked: Boolean, val path: String? = null)

fun parseSongs(json: String): List<Song> {
    val gson = Gson()
    val type = object : TypeToken<List<Song>>() {}.type
    return gson.fromJson(json, type)
}
