/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.groundplatform.android.persistence.local.room.fields

import androidx.room.TypeConverter
import org.groundplatform.android.model.task.Expression
import org.groundplatform.android.persistence.local.room.IntEnum

enum class ExpressionEntityType(private val intValue: Int) : IntEnum {
  UNKNOWN(0),
  ANY_OF_SELECTED(1),
  ALL_OF_SELECTED(2),
  ONE_OF_SELECTED(3);

  override fun intValue(): Int = intValue

  fun toExpressionType(): Expression.ExpressionType =
    EXPRESSION_TYPES.getOrDefault(this, Expression.ExpressionType.UNKNOWN)

  companion object {
    private val EXPRESSION_TYPES: Map<ExpressionEntityType, Expression.ExpressionType> =
      mapOf(
        Pair(UNKNOWN, Expression.ExpressionType.UNKNOWN),
        Pair(ANY_OF_SELECTED, Expression.ExpressionType.ANY_OF_SELECTED),
        Pair(ALL_OF_SELECTED, Expression.ExpressionType.ALL_OF_SELECTED),
        Pair(ONE_OF_SELECTED, Expression.ExpressionType.ONE_OF_SELECTED),
      )
    private val REVERSE_EXPRESSION_TYPES: Map<Expression.ExpressionType, ExpressionEntityType> =
      EXPRESSION_TYPES.entries.associateBy({ it.value }) { it.key }

    fun fromExpressionType(type: Expression.ExpressionType): ExpressionEntityType =
      REVERSE_EXPRESSION_TYPES.getOrDefault(type, UNKNOWN)

    @JvmStatic
    @TypeConverter
    fun toInt(value: ExpressionEntityType?): Int = IntEnum.toInt(value, UNKNOWN)

    @JvmStatic
    @TypeConverter
    fun fromInt(intValue: Int): ExpressionEntityType =
      IntEnum.fromInt(entries.toTypedArray(), intValue, UNKNOWN)
  }
}
