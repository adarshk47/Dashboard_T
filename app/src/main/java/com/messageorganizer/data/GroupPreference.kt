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
    }

    fun getCustomGroups(): List<MessageGroup> {
        val json = prefs.getString("groups", "[]") ?: "[]"
        val array = JSONArray(json)
        val groups = mutableListOf<MessageGroup>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val keywordsArray = obj.optJSONArray("keywords") ?: JSONArray()
            val keywords = (0 until keywordsArray.length()).map { keywordsArray.getString(it) }
            groups.add(
                MessageGroup(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    icon = GroupIcon.valueOf(obj.optString("icon", "FOLDER")),
                    groupType = GroupType.CUSTOM,
                    keywords = keywords,
                    senderPattern = obj.optString("senderPattern").takeIf { it.isNotBlank() },
                    isCustom = true
                )
            )
        }
        return groups
    }

    private fun saveAll(groups: List<MessageGroup>) {
        val array = JSONArray()
        groups.forEach { group ->
            val obj = JSONObject()
            obj.put("id", group.id)
            obj.put("name", group.name)
            obj.put("icon", group.icon.name)
            val keywordsArray = JSONArray()
            group.keywords.forEach { keywordsArray.put(it) }
            obj.put("keywords", keywordsArray)
            obj.put("senderPattern", group.senderPattern ?: "")
            array.put(obj)
        }
        prefs.edit().putString("groups", array.toString()).apply()
    }
}
