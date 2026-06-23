package com.pupil.app.core.domain.model

data class CategoryTotal(
    val categoryId: Long,
    val category: String,
    val icon: String? = null,
    val color: Int? = null,
    val totalPaise: Long
)
