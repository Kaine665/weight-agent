package com.weightagent.app.data.saf

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.safDataStore: DataStore<Preferences> by preferencesDataStore(name = "saf_folders")

/**
 * 持久化多个 [android.content.Intent.ACTION_OPEN_DOCUMENT_TREE] 返回的目录 URI（字符串形式）。
 */
class SafFolderStore(private val context: Context) {

    private val keyUris = stringSetPreferencesKey("tree_uris")

    val treeUriStrings: Flow<Set<String>> = context.safDataStore.data.map { prefs ->
        prefs[keyUris].orEmpty().filter { it.isNotBlank() }.toSortedSet()
    }

    suspend fun readUriStrings(): Set<String> = treeUriStrings.first()

    suspend fun add(uriString: String) {
        val s = uriString.trim()
        if (s.isBlank()) return
        context.safDataStore.edit { prefs ->
            val next = (prefs[keyUris] ?: emptySet()).toMutableSet()
            next.add(s)
            prefs[keyUris] = next
        }
    }

    suspend fun remove(uriString: String) {
        context.safDataStore.edit { prefs ->
            val cur = prefs[keyUris] ?: return@edit
            val next = cur.toMutableSet().apply { remove(uriString) }
            if (next.isEmpty()) {
                prefs.remove(keyUris)
            } else {
                prefs[keyUris] = next
            }
        }
    }

    suspend fun clear() {
        context.safDataStore.edit { it.remove(keyUris) }
    }
}
