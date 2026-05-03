package com.pupil.app.core.data.local.entity

data class CategoryTotalEntity(
    val categoryId: Long,
    val category: String,
    val icon: String?,
    val color: Int?,
    val totalPaise: Long
)
