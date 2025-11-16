package com.example.xchat2.util

import com.example.xchat2.chat.ChatUser
import com.example.xchat2.chat.Sex
import com.example.xchat2.ui.main.repos.ChatRepository
import com.example.xchat2.ui.main.repos.Chatroom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLDecoder
import java.util.regex.Matcher

/**
 * Centralized constants for XChat API scraping.
 * Tune here if site changes (e.g., params, UA, timeouts).
 */
object ChatApiConstants {

    const val GUEST_INDEX_PATH = "/~guest~/index.php"
    const val BASE_URL = "https://www.xchat.cz"
    const val TIMEOUT_MS = 15000L
    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36 Edge/12.10240"
    const val JS_PARAM = "1"
    const val SKIN_PARAM = "2"

    // Common query params
    val COMMON_PARAMS = mapOf(
        "js" to JS_PARAM,
        "skin" to SKIN_PARAM
    )

    // Login-specific
    val LOGIN_PATH = "/login/"
    val LOGIN_EXTRA_PARAMS = mapOf(
        "x" to "0",
        "y" to "0"
    )

    // Room enter/info
    const val TOKEN_ROOM_INTRO = "/{token}/room/intro.php"
    val ENTER_EXTRA_PARAMS = mapOf(
        "sexwarn" to "1",
        "disclaim" to "1",
        "_btn_enter" to "wanna_enter_man%3F"
    )

    const val ROOM_CONTENT_PATH = "/{token}/modchat"
    const val ROOM_CONTENT_QUERY = "?op=roomtopng&rid={roomId}"

    const val SEND_MESSAGE_PATH = "/{token}/modchat"
    val SEND_MESSAGE_PARAMS = mapOf(
        "op" to "textpageng",
        "aid" to "0",
        "target" to "~",
        "submit_text" to "Poslat"
    )
    const val SEND_MESSAGE_CHARSET = "ISO-8859-2"

    const val EXIT_ROOM_QUERY = "?op=mainframeset&menuaction=leave&leftroom={roomId}&cid=16"
    const val SEND_TOKEN_QUERY = "?op=textpageng&rid={roomId}"
    const val USER_LIST_QUERY = "?op=userspage&rid={roomId}&cid=16"
    const val ROOM_INFO_QUERY = "?op=infopage&rid={roomId}"
}

/**
 * Suspend extension helpers for Jsoup network calls on IO dispatcher.
 * All sync Jsoup calls wrapped to be non-blocking.
 */
suspend fun ChatRepository.jsoupRequest(
    path: String,
    method: Connection.Method = Connection.Method.GET,
    extraData: Map<String, String> = emptyMap(),
    postCharset: String? = null,
    followRedirects: Boolean = true
): Connection.Response = withContext(Dispatchers.IO) {
    Jsoup.connect(ChatApiConstants.BASE_URL + path)
        .userAgent(ChatApiConstants.USER_AGENT)
        .timeout(ChatApiConstants.TIMEOUT_MS.toInt())
        .method(method)
        .followRedirects(followRedirects)
        .apply {
            extraData.forEach { (k, v) -> data(k, v) }
            postCharset?.let { postDataCharset(it) }
        }
        .execute()
}

suspend fun Connection.Response.isSuccessful(): Boolean = withContext(Dispatchers.IO) {
    hasHeader("location")
}

suspend fun Connection.Response.getUserHashtag(): String = withContext(Dispatchers.IO) {
    header("location")?.let { location ->
        val start = location.indexOf("~").takeIf { it >= 0 } ?: return@withContext ""
        val end = location.indexOf("/", start).takeIf { it > start } ?: location.length
        location.substring(start, end)
    } ?: ""
}

suspend fun Document.toRoomList(): List<Chatroom> = withContext(Dispatchers.IO) {
    select("select#room > option")
        .mapNotNull { option ->
            val id = option.attr("value").takeIf { it.matches(Regex("\\d{6,8}")) }?.toIntOrNull()
            val name = option.text().substringBefore(" (")
            val count = option.text().substringAfterLast(" (").substringBeforeLast(")")
            id?.let { Chatroom(it, name, count) }
        }
}

suspend fun ChatRepository.createEnterRoomRequest(token: String, roomId: Int): Connection.Response =
    jsoupRequest(
        path = ChatApiConstants.TOKEN_ROOM_INTRO.replace("{token}", token) + "?rid=$roomId",
        extraData = ChatApiConstants.ENTER_EXTRA_PARAMS + mapOf("rid" to roomId.toString())
    )

suspend fun ChatRepository.createGetRoomContentRequest(token: String, roomId: Int): Connection.Response =
    jsoupRequest(
        path = ChatApiConstants.ROOM_CONTENT_PATH.replace("{token}", token),
        extraData = ChatApiConstants.COMMON_PARAMS + mapOf(
            "op" to "roomtopng",
            "rid" to roomId.toString()
        )
    )

fun Connection.Response.getRoomHtmlString(): String {
    val doc: Document = parse()
    val docc = doc.toString()

    return try {
        val start: Int = docc.indexOf("Array('") + "Array('".length
        val end: Int = docc.indexOf("if (top.", start) - 9
        var output = docc.substring(start, end)
        output = output.replace("\',\n'".toRegex(), "</div><div>")
        output = "<div>$output"
        output = URLDecoder.decode(output, "UTF-8")
        output = output.replace("\\\"", "\"")
        output = output.replace("href=\"https://redi.*?url=".toRegex(), Matcher.quoteReplacement("href=\""))
        output
    } catch (r: Exception) {
        r.printStackTrace()
        " "
    }
}

suspend fun ChatRepository.createRoomExitRequest(token: String, roomId: Int): Connection.Response =
    jsoupRequest(
        path = ChatApiConstants.ROOM_CONTENT_PATH.replace("{token}", token) +
                ChatApiConstants.EXIT_ROOM_QUERY.replace("{roomId}", roomId.toString())
    )

suspend fun ChatRepository.createGetSendTokenRequest(roomId: Int, token: String): Connection.Response =
    jsoupRequest(
        path = ChatApiConstants.ROOM_CONTENT_PATH.replace("{token}", token) +
                ChatApiConstants.SEND_TOKEN_QUERY.replace("{roomId}", roomId.toString())
    )

suspend fun ChatRepository.createSendMessageRequest(
    message: String,
    roomId: Int,
    token: String,
    sendToken: String
): Connection.Response =
    jsoupRequest(
        path = ChatApiConstants.SEND_MESSAGE_PATH.replace("{token}", token),
        method = Connection.Method.POST,
        extraData = ChatApiConstants.SEND_MESSAGE_PARAMS + mapOf(
            "rid" to roomId.toString(),
            "wtkn" to sendToken,
            "textarea" to message
        ),
        postCharset = ChatApiConstants.SEND_MESSAGE_CHARSET
    )

suspend fun ChatRepository.createGetUserListRequest(roomId: Int, token: String): Connection.Response =
    jsoupRequest(
        path = ChatApiConstants.ROOM_CONTENT_PATH.replace("{token}", token) +
                ChatApiConstants.USER_LIST_QUERY.replace("{roomId}", roomId.toString())
    )

suspend fun Document.getUserList(): List<ChatUser> = withContext(Dispatchers.IO) {
    getElementById("clist")?.select("p")
        ?.map { element ->
            val name = element.text().trim().takeIf { it.isNotEmpty() } ?: return@map null
            val html = element.html().lowercase()
            val sex = if (html.contains("muž")) Sex.MUZ else Sex.ZENA
            ChatUser(name, sex)
        }
        ?.filterNotNull()
        ?: emptyList()
}

suspend fun ChatRepository.createGetRoomInfoRequest(roomId: Int, token: String): Connection.Response =
    jsoupRequest(
        path = ChatApiConstants.ROOM_CONTENT_PATH.replace("{token}", token) +
                ChatApiConstants.ROOM_INFO_QUERY.replace("{roomId}", roomId.toString())
    )

suspend fun ChatRepository.createLoginRequest(name: String, password: String): Connection.Response =
    jsoupRequest(
        path = ChatApiConstants.LOGIN_PATH,
        method = Connection.Method.POST,
        extraData = ChatApiConstants.COMMON_PARAMS + ChatApiConstants.LOGIN_EXTRA_PARAMS + mapOf(
            "name" to name,
            "pass" to password
        ), followRedirects = false
    )

suspend fun ChatRepository.getGuestIndexDocument(): Document = jsoupRequest(
    path = ChatApiConstants.GUEST_INDEX_PATH
).parseDocument()

suspend fun Connection.Response.parseDocument(): Document = withContext(Dispatchers.IO) {
    parse()  // Now safe: wrapped in IO
}