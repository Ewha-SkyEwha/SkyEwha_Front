import com.squareup.moshi.Json
data class UploadResponse(
    @Json(name = "video_title") val videoTitle: String,
    @Json(name = "video_id") val videoId: Int,
    @Json(name = "user_id") val userId: Int,
    @Json(name = "upload_date") val uploadDate: String
)