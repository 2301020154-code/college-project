package com.example.trackback

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
@Serializable
data class Item(
    var id: String = "",
    var name: String = "",
    var category: String = "",
    var description: String = "",
    var location: String = "",
    var phoneNumber: String = "",
    var imageUrl: String = "",
    var userId: String = "",
    var status: String = "FOUND"
)