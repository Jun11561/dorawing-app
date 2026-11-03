package com.team3.dorawing.ui.guardian

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.team3.dorawing.util.Prefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// 라이브러리를 더하지 않으려고 Firebase 작업이 끝나기를 직접 기다린다
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) cont.resume(task.result)
        else cont.resumeWithException(task.exception ?: CancellationException("취소됨"))
    }
}

// 보호자 연결에 쓰는 Firebase 호출. 화면들과 GameRecorder, 자가진단이 같이 쓴다
//   elders/{어르신 uid}: guardians(보호자 uid, 최대 2), createdAt
//   elders/{어르신 uid}/requests/{보호자 uid}: name(호칭), code, requestedAt
//   elders/{어르신 uid}/summaries/{날짜}: Summary의 칸만
//   pairCodes/{6자리}: elderUid, expiresAt, used
object Cloud {
    enum class CodeCheck { OK, WRONG, EXPIRED }

    data class Request(val guardianUid: String, val name: String, val code: String)

    // 인터넷이 없을 때 오래 기다리지 않게
    private const val TIMEOUT = 15_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val syncLock = Mutex()
    private var uploadJob: Job? = null

    private val auth get() = FirebaseAuth.getInstance()
    private val store get() = FirebaseFirestore.getInstance()

    private fun elderDoc(uid: String) = store.collection("elders").document(uid)
    private fun codeDoc(code: String) = store.collection("pairCodes").document(code)
    private fun summaries(uid: String) = elderDoc(uid).collection("summaries")
    private fun requests(uid: String) = elderDoc(uid).collection("requests")

    private suspend fun <T> Task<T>.awaitNet(): T = withTimeout(TIMEOUT) { await() }

    // google-services.json 없이 빌드하면 Firebase가 없다
    fun isReady(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    // 익명 로그인. 앱을 켤 때가 아니라 보호자 연결 화면에서 처음 부른다
    suspend fun signIn(): String {
        auth.currentUser?.let { return it.uid }
        return auth.signInAnonymously().awaitNet().user?.uid ?: throw IllegalStateException("로그인 실패")
    }

    private fun guardiansOf(snap: DocumentSnapshot): List<String> =
        (snap.get("guardians") as? List<*>).orEmpty().filterIsInstance<String>()

    private fun isDenied(e: Exception) =
        e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED

    // ---- 어르신 폰 ----

    // 어르신 문서가 없으면 만들고 보호자 uid 목록을 돌려준다
    suspend fun elderGuardians(uid: String): List<String> {
        val snap = elderDoc(uid).get(Source.SERVER).awaitNet()
        if (snap.exists()) return guardiansOf(snap)
        elderDoc(uid).set(mapOf("guardians" to emptyList<String>(), "createdAt" to FieldValue.serverTimestamp())).awaitNet()
        return emptyList()
    }

    // 새 코드를 서버에 만든다. 다른 어르신이 쓰는 번호와 겹치면 서버가 막으니 다시 뽑는다
    suspend fun newCode(uid: String): Pair<String, Long> {
        repeat(5) {
            val code = PairRules.newCode()
            val expiresAt = PairRules.expiresAt(System.currentTimeMillis())
            val data = mapOf("elderUid" to uid, "expiresAt" to Timestamp(Date(expiresAt)), "used" to false)
            try {
                codeDoc(code).set(data).awaitNet()
                return code to expiresAt
            } catch (e: Exception) {
                if (!isDenied(e)) throw e
            }
        }
        throw IllegalStateException("코드를 만들지 못함")
    }

    // 지난 코드는 지운다. 못 지워도 10분 뒤에는 못 쓰는 코드라 그냥 둔다
    suspend fun deleteCode(code: String) {
        try {
            codeDoc(code).delete().awaitNet()
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
        }
    }

    fun listenElder(uid: String, onChange: (List<String>) -> Unit): ListenerRegistration =
        elderDoc(uid).addSnapshotListener { snap, _ -> if (snap != null && snap.exists()) onChange(guardiansOf(snap)) }

    fun listenRequests(uid: String, onChange: (List<Request>) -> Unit): ListenerRegistration =
        requests(uid).addSnapshotListener { snap, _ ->
            if (snap == null) return@addSnapshotListener
            onChange(snap.documents.map { Request(it.id, it.getString("name").orEmpty(), it.getString("code").orEmpty()) })
        }

    // 최종 승인: 보호자 추가, 코드 사용 처리, 요청 지우기를 한 번에
    suspend fun approve(uid: String, request: Request, markCodeUsed: Boolean) {
        val batch = store.batch()
        batch.update(elderDoc(uid), "guardians", FieldValue.arrayUnion(request.guardianUid))
        if (markCodeUsed) batch.update(codeDoc(request.code), "used", true)
        batch.delete(requests(uid).document(request.guardianUid))
        batch.commit().awaitNet()
    }

    suspend fun reject(uid: String, request: Request) {
        requests(uid).document(request.guardianUid).delete().awaitNet()
    }

    // 어르신이 보호자 한 명을 끊는다. 남은 보호자가 없으면 요약도 지운다
    suspend fun removeGuardian(context: Context, uid: String, guardianUid: String) {
        elderDoc(uid).update("guardians", FieldValue.arrayRemove(guardianUid)).awaitNet()
        Prefs.removeGuardianName(context, guardianUid)
        val left = guardiansOf(elderDoc(uid).get(Source.SERVER).awaitNet())
        if (left.isEmpty()) {
            deleteSummaries(uid)
            Prefs.setHasGuardian(context, false)
            Prefs.setUploadPending(context, false)
        }
    }

    private suspend fun deleteSummaries(uid: String) {
        val docs = summaries(uid).get(Source.SERVER).awaitNet().documents
        if (docs.isEmpty()) return
        val batch = store.batch()
        docs.forEach { batch.delete(it.reference) }
        batch.commit().awaitNet()
    }

    // 기록이 바뀔 때 부른다. 기다리지 않고 뒤에서 올린다
    fun uploadLater(context: Context) {
        val app = context.applicationContext
        // 연결된 보호자가 없으면 아무것도 올리지 않는다
        if (!isReady(app) || !Prefs.hasGuardian(app)) return
        Prefs.setUploadPending(app, true)
        // 이미 올리는 중이면 그 작업이 끝날 때 한 번 더 올린다
        if (uploadJob?.isActive == true) return
        uploadJob = scope.launch { sync(app) }
    }

    // 앱을 열 때 부른다. 못 올린 요약을 다시 올리고, 보호자가 모두 떠났으면 요약을 지운다
    fun syncLater(context: Context) {
        val app = context.applicationContext
        if (!isReady(app) || !Prefs.hasGuardian(app)) return
        scope.launch { sync(app) }
    }

    private suspend fun sync(context: Context) {
        val uid = auth.currentUser?.uid ?: return
        syncLock.withLock {
            var uploading = false
            try {
                val guardians = guardiansOf(elderDoc(uid).get(Source.SERVER).awaitNet())
                if (guardians.isEmpty()) {
                    // 보호자가 스스로 연결을 끊었으면 서버 요약을 지운다
                    deleteSummaries(uid)
                    Prefs.setHasGuardian(context, false)
                    Prefs.setUploadPending(context, false)
                    return
                }
                while (Prefs.isUploadPending(context)) {
                    Prefs.setUploadPending(context, false)
                    uploading = true
                    val summary = Summary.collect(context)
                    summaries(uid).document(summary.day).set(summary.toMap()).awaitNet()
                    uploading = false
                }
            } catch (e: Exception) {
                if (e is CancellationException && e !is TimeoutCancellationException) throw e
                // 인터넷이 없으면 "올릴 것 있음"을 남겨 두고 다음에 다시
                if (uploading) Prefs.setUploadPending(context, true)
            }
        }
    }

    // ---- 보호자 폰 ----

    // 코드가 있는지, 쓸 수 있는지. 쓸 수 있으면 어르신 uid도 돌려준다
    suspend fun checkCode(code: String): Pair<CodeCheck, String?> {
        val snap = codeDoc(code).get(Source.SERVER).awaitNet()
        if (!snap.exists()) return CodeCheck.WRONG to null
        val expiresAt = snap.getTimestamp("expiresAt")?.toDate()?.time ?: 0L
        val used = snap.getBoolean("used") ?: true
        if (!PairRules.isUsable(expiresAt, used, System.currentTimeMillis())) return CodeCheck.EXPIRED to null
        return CodeCheck.OK to snap.getString("elderUid")
    }

    // 서버 규칙이 코드를 다시 확인한다. 그사이 시간이 지났으면 false
    suspend fun sendRequest(elderUid: String, myUid: String, code: String, name: String): Boolean {
        val data = mapOf("name" to name, "code" to code, "requestedAt" to FieldValue.serverTimestamp())
        return try {
            requests(elderUid).document(myUid).set(data).awaitNet()
            true
        } catch (e: Exception) {
            if (isDenied(e)) false else throw e
        }
    }

    // 승인 전에는 어르신 문서를 읽을 수 없어서 false
    suspend fun isApproved(elderUid: String, myUid: String): Boolean = try {
        myUid in guardiansOf(elderDoc(elderUid).get(Source.SERVER).awaitNet())
    } catch (e: Exception) {
        if (isDenied(e)) false else throw e
    }

    // 가장 최근 요약. 연결이 끊겼으면 null, 아직 올라온 요약이 없으면 빈 맵
    suspend fun loadSummary(elderUid: String, myUid: String): Map<String, Any?>? {
        val elder = try {
            elderDoc(elderUid).get().awaitNet()
        } catch (e: Exception) {
            if (isDenied(e)) return null else throw e
        }
        // 디버그 메뉴에서 어르신 폰의 기록을 보호자 화면으로 미리 볼 때는 본인 uid
        if (!elder.exists() || (myUid !in guardiansOf(elder) && myUid != elderUid)) return null
        // 문서 이름(날짜)으로 거꾸로 정렬하면 따로 색인을 만들어야 해서, 자동 색인이 있는 updatedAt으로 정렬한다
        val docs = summaries(elderUid).orderBy("updatedAt", Query.Direction.DESCENDING).limit(1).get().awaitNet()
        return docs.documents.firstOrNull()?.data ?: emptyMap()
    }

    // 보호자가 스스로 연결을 끊는다. 서버 규칙상 자기 uid만 뺄 수 있다
    suspend fun leave(elderUid: String, myUid: String) {
        elderDoc(elderUid).update("guardians", FieldValue.arrayRemove(myUid)).awaitNet()
    }
}
