package com.h.trendie.data

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.h.trendie.network.ApiClient
import com.h.trendie.network.ApiService
import com.h.trendie.network.MyFeedbackItem

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FeedbackHistoryRepository(
    private val api: ApiService = ApiClient.apiService
) {

    val itemsLiveData = MutableLiveData<List<MyFeedbackItem>>()
    val errorLiveData = MutableLiveData<String?>()

    suspend fun refreshMyFeedbacks(rawToken: String?) {
        withContext(Dispatchers.IO) {
            try {
                val bearer = rawToken?.let {
                    if (it.startsWith("Bearer ")) it else "Bearer $it"
                }

                val res = api.getMyFeedbacks()
                if (res.isSuccessful) {
                    itemsLiveData.postValue(res.body().orEmpty())
                    errorLiveData.postValue(null)
                } else {
                    Log.w("FeedbackHistoryRepo", "HTTP ${res.code()} ${res.message()}")
                    itemsLiveData.postValue(emptyList())
                    errorLiveData.postValue("HTTP ${res.code()}")
                }
            } catch (e: Exception) {
                Log.e("FeedbackHistoryRepo", "refresh error: ${e.message}", e)
                itemsLiveData.postValue(emptyList())
                errorLiveData.postValue(e.localizedMessage)
            }
        }
    }
}
