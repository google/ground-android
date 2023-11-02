package com.google.android.ground.persistence.local.room.converter

import androidx.room.TypeConverter
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.collections.immutable.toPersistentMap
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber


object LoiPropertiesMapConverter {
  @TypeConverter
  fun toString(properties: Map<String, Any>?): String? {
    if (properties ==null) {
      return null
    }
    var jsonString: String? = null
    try {
      jsonString =  JSONObject(properties).toString()
    } catch (e: JSONException) {
      Timber.e(e, "Error building JSON")
    }
    return jsonString
  }

  @TypeConverter
  fun fromString(jsonString: String?): Map<String, Any> {
    if (jsonString == null) {
      return mapOf()
    }
    var map = mutableMapOf<String, Any>()
    try {
      val type = object: TypeToken<Map<String, Any>>(){}.type
      map = Gson().fromJson(
        jsonString,
       type
      )
    } catch (e: JsonSyntaxException) {
      Timber.e(e, "Error parsing JSON string")
    }
    return map.toPersistentMap()
  }
}