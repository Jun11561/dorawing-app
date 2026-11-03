package com.team3.dorawing.ui.debug

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.team3.dorawing.ui.guardian.await
import com.team3.dorawing.util.Prefs

// 에뮬레이터 한 대로 보호자 연결을 확인하려고 두 번째 FirebaseApp으로 다른 익명 사용자(가상 보호자)를 만든다.
// 서버 규칙을 그대로 거치므로 진짜 보호자 폰과 같은 길로 확인된다
object FakeGuardian {
    private const val APP_NAME = "fakeGuardian"

    private fun app(context: Context): FirebaseApp =
        FirebaseApp.getApps(context).firstOrNull { it.name == APP_NAME }
            ?: FirebaseApp.initializeApp(context, FirebaseApp.getInstance().options, APP_NAME)

    private fun store(context: Context) = FirebaseFirestore.getInstance(app(context))

    private suspend fun uid(context: Context): String {
        val auth = FirebaseAuth.getInstance(app(context))
        return auth.currentUser?.uid ?: auth.signInAnonymously().await().user!!.uid
    }

    private fun elderDoc(context: Context, elder: String) = store(context).collection("elders").document(elder)

    // 어르신 폰에 지금 떠 있는 코드로 연결 요청을 만든다
    suspend fun request(context: Context): String {
        val code = Prefs.getCode(context) ?: return "지금 떠 있는 코드가 없어요. 먼저 연결 코드 화면을 열어 주세요"
        val elder = Prefs.getElderUid(context) ?: return "어르신 uid가 없어요. 먼저 연결 코드 화면을 열어 주세요"
        val me = uid(context)
        val data = mapOf("name" to "가상 보호자", "code" to code, "requestedAt" to FieldValue.serverTimestamp())
        elderDoc(context, elder).collection("requests").document(me).set(data).await()
        return "가상 보호자 요청을 보냈어요 (코드 $code). 연결 코드 화면으로 가면 승인 창이 떠요"
    }

    // 가상 보호자 권한으로 어르신 문서와 최근 요약을 읽는다
    suspend fun read(context: Context): String {
        val elder = Prefs.getElderUid(context) ?: return "어르신 uid가 없어요"
        uid(context)
        val guardians = try {
            (elderDoc(context, elder).get().await().get("guardians") as? List<*>).orEmpty()
        } catch (e: Exception) {
            return "가상 보호자: 어르신 문서 읽기 막힘 (${e.message})"
        }
        val docs = elderDoc(context, elder).collection("summaries")
            .orderBy("updatedAt", Query.Direction.DESCENDING).limit(1).get().await()
        val data = docs.documents.firstOrNull()?.data ?: return "가상 보호자: 보호자 ${guardians.size}명, 올라온 요약 없음"
        return "가상 보호자: 보호자 ${guardians.size}명\n요약 칸: ${data.keys.sorted().joinToString()}\n$data"
    }

    // 보호자 쪽에서 연결 끊기 (자기 uid만 뺄 수 있다)
    suspend fun leave(context: Context): String {
        val elder = Prefs.getElderUid(context) ?: return "어르신 uid가 없어요"
        elderDoc(context, elder).update("guardians", FieldValue.arrayRemove(uid(context))).await()
        return "가상 보호자가 연결을 끊었어요"
    }
}
