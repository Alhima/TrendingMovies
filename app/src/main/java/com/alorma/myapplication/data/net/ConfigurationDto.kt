package com.alorma.myapplication.data.net

import com.google.gson.annotations.SerializedName

data class ConfigurationResponseDto(@SerializedName("images") val response: ConfigurationDto)

data class ConfigurationDto(@SerializedName("secure_base_url") val imagesUrl: String,
                            @SerializedName("logo_sizes") val imageSize: List<String>,
                            @SerializedName("poster_sizes") val posterSize: List<String>)