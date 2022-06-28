package com.google.android.gnd.model

import com.google.android.gnd.model.mutation.Mutation
import java.util.*

data class MutationLog<T : Mutation>(val id: Long, val type: Mutation.Type = Mutation.Type.UNKNOWN, val userId: String?, val timestamp: Date?, val locationOfInterest: String?, val project: String?)
