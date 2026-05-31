package com.messageorganizer.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class GroupPreference(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("custom_groups", Context.MODE_PRIVATE)

    fun saveCustomGroup(group: MessageGroup) {
        val existing = getCustomGroups().toMutableList()
        existing.removeAll { it.id == group.id }
        existing.add(group)
        saveAll(existing)
    }

    fun deleteCustomGroup(groupId: String) {
        val existing = getCustomGroups().toMutableList()
        existing.removeAll { it.id == groupId }
        saveAll(existing)
        // also remove from bookmarks
        val bookmarks = getBookmarkedIds().toMutableSet()
        bookmarks.remove(groupId)
        prefs.edit().putStringSet("bookmarks", bookmarks).apply()
    }

    fun renameGroup(groupId: String, newName: String) {
        val existing = getCustomGroups().toMutableList()
        val idx = existing.indexOfFirst { it.id == groupId }
        if (idx >= 0) {
            existing[idx] = existing[idx].copy(name = newName)
            saveAll(existing)
        }
    }

    fun getCustomGroups(): List<MessageGroup> {
        val json = prefs.getString("groups", "[]") ?: "[]"
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val kwArray = obj.optJSONArray("keywords") ?: JSONArray()
            MessageGroup(
                id = obj.getString("id"),
                name = obj.getString("name"),
                icon = GroupIcon.valueOf(obj.optString("icon", "FOLDER")),
                groupType = GroupType.CUSTOM,
                keywords = (0 until kwArray.length()).map { kwArray.getString(it) },
                senderPattern = obj.optString("senderPattern").takeIf { it.isNotBlank() },
                isCustom = true
            )
        }
    }

    fun getBookmarkedIds(): Set<String> =
        prefs.getStringSet("bookmarks", emptySet()) ?: emptySet()

    fun toggleBookmark(groupId: String) {
        val bookmarks = getBookmarkedIds().toMutableSet()
        if (bookmarks.contains(groupId)) bookmarks.remove(groupId) else bookmarks.add(groupId)
        prefs.edit().putStringSet("bookmarks", bookmarks).apply()
    }

    private fun saveAll(groups: List<MessageGroup>) {
        val array = JSONArray()
        groups.forEach { group ->
            val obj = JSONObject()
            obj.put("id", group.id)
            obj.put("name", group.name)
            obj.put("icon", group.icon.name)
            val kwArray = JSONArray()
            group.keywords.forEach { kwArray.put(it) }
            obj.put("keywords", kwArray)
            obj.put("senderPattern", group.senderPattern ?: "")
            array.put(obj)
        }
        prefs.edit().putString("groups", array.toString()).apply()
    }
}
