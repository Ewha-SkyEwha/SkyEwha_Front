import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PopularVideosRes(
    @Json(name = "results") val results: List<PopularVideoDto>
)

@JsonClass(generateAdapter = true)
data class PopularVideoDto(
    @Json(name = "video_id") val videoId: String,
    @Json(name = "thumbnail_url") val thumbnailUrl: String
)
