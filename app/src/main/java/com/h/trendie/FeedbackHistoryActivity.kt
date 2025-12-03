package com.h.trendie

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h.trendie.network.ApiClient
import com.h.trendie.network.MyFeedbackItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class FeedbackHistoryActivity : AppCompatActivity() {

    private lateinit var adapter: FeedbackHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback_history)

        setupSimpleToolbarText("피드백 보고서 내역")

        val rv = findViewById<RecyclerView>(R.id.rvFeedbackHistory).apply {
            layoutManager = LinearLayoutManager(this@FeedbackHistoryActivity)
        }

        adapter = FeedbackHistoryAdapter(mutableListOf()) { item ->
            val nickname = readNicknameOrNull()
            startActivity(
                Intent(this, FeedbackReportActivity::class.java).apply {
                    putExtra("historyId", item.id)
                    putExtra("videoTitle", item.title)
                    if (!nickname.isNullOrBlank()) {
                        putExtra(ExtraKeys.NICKNAME, nickname)
                    }
                    putExtra("hideToolbar", true)

                    putStringArrayListExtra(ExtraKeys.TITLES, ArrayList(item.titles))
                    putStringArrayListExtra(ExtraKeys.HASHTAGS, ArrayList(item.hashtags))
                    putStringArrayListExtra(ExtraKeys.THUMBS, ArrayList(item.thumbs))

                    item.feedbackId?.let { putExtra(ExtraKeys.FEEDBACK_ID, it) }
                }
            )
        }
        rv.adapter = adapter

        val deco = DividerItemDecoration(this, LinearLayoutManager.VERTICAL).apply {
            ContextCompat.getDrawable(this@FeedbackHistoryActivity, R.drawable.divider_horizontal)
                ?.let { setDrawable(it) }
        }
        rv.addItemDecoration(deco)

        fetchFromServer()
    }

    private fun fetchFromServer() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val resp = ApiClient.apiService.getMyFeedbacks()

                    if (resp.isSuccessful) {
                        // 정상 응답
                        Result.success(resp.body().orEmpty())

                    } else if (resp.code() == 404) {
                        // 조건에 맞는 피드백 없을 때
                        Result.success(emptyList())

                    } else {
                        // 기타 서버 오류
                        Result.failure(
                            IOException("피드백을 불러오는 데 실패했어요.")
                        )
                    }
                } catch (e: IOException) {
                    Result.failure(e)
                } catch (e: HttpException) {
                    Result.failure(e)
                } catch (e: Throwable) {
                    Result.failure(e)
                }
            }

            result
                .onSuccess { items: List<MyFeedbackItem> ->
                    if (items.isEmpty()) {
                        Toast.makeText(
                            this@FeedbackHistoryActivity,
                            "서버에 저장된 피드백이 없습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                        adapter.updateData(emptyList())
                        return@onSuccess
                    }

                    adapter.updateData(items.map { it.toHistoryItem() })
                }
                .onFailure { e ->
                    e.printStackTrace()

                    Toast.makeText(
                        this@FeedbackHistoryActivity,
                        "피드백을 불러오지 못했어요. 네트워크 상태를 확인해 주세요.",
                        Toast.LENGTH_LONG
                    ).show()

                    adapter.updateData(emptyList())
                }
        }
    }

    private fun readNicknameOrNull(): String? {
        runCatching { com.h.trendie.util.NicknameStore.get(this) }
            .getOrNull()
            ?.let { if (it.isNotBlank()) return it }

        val prefsCandidates = listOf("user_prefs", "Userprefs", "UserPrefs", "profile_prefs", "mypage_prefs")
        val keyCandidates = listOf("nickname", "user_nickname", "nick_name", "name", "userName")

        for (p in prefsCandidates) {
            val sp = getSharedPreferences(p, Context.MODE_PRIVATE)
            for (k in keyCandidates) {
                val v = sp.getString(k, null)
                if (!v.isNullOrBlank()) return v
            }
        }
        return null
    }
}