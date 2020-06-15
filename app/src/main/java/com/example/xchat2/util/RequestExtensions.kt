package com.example.xchat2.util

import com.example.xchat2.ui.main.repos.ChatRepository
import com.example.xchat2.ui.main.repos.Chatroom
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.net.URLDecoder
import java.util.regex.Matcher
import java.util.regex.Pattern

fun ChatRepository.createLoginRequest(name: String, password: String) =
    Jsoup.connect("https://www.xchat.cz/login/")
        .userAgent("Mozilla/5.0")
        .timeout(4000)
        .method(Connection.Method.POST)
        .followRedirects(false)
        .data("js", "1")
        .data("name", name)
        .data("pass", password)
        .data("x", "0")
        .data("y", "0")

fun Connection.Response.isSucessful(): Boolean {
    return hasHeader("location")
}

fun Connection.Response.getUserHashtag(): String {
    val temp = header("location").toString()
    val start = temp.indexOf("~")
    val end = temp.indexOf("/", start)
    val finalni = temp.substring(start, end)
    return finalni
}

fun Document.toRoomList(): List<Chatroom> {
    val element = getElementById("room")
    val rooms: String = element.toString()
    val regex = Regex("(\\d{6,8})\">(.*?) \\((\\d*?)\\)</option")
    val matches = regex.findAll(rooms)
    val roomlist = mutableListOf<Chatroom>()
    matches.forEach {
        it.groupValues.getOrNull(1)?.let { id ->
            val roomka = Chatroom(id.toInt(), it.groupValues.get(2), it.groupValues.get(3))
            roomlist.add(roomka)
        }
    }
    return roomlist
}

fun ChatRepository.createEnterRoomRequest(token: String, roomId: Int): Connection {
    val dest = "https://www.xchat.cz/$token/room/intro.php?rid=$roomId&sexwarn=1&disclaim=1&_btn_enter=wanna_enter_man%3F"

    val response = Jsoup.connect(dest)
        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36 Edge/12.10240")
        .timeout(4000)
        .method(Connection.Method.GET)
        .followRedirects(true)
    return response
}

fun ChatRepository.createGetRoomContentRequest(token: String, roomId: Int): Connection {
    val sourceroom = "https://www.xchat.cz/${token}/modchat?op=roomtopng&rid=${roomId}&js=1&skin=2"
    val response = Jsoup.connect(sourceroom)
        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36 Edge/12.10240")
        .method(Connection.Method.GET)
        .timeout(4000)
        .followRedirects(true)
    return response
}

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
 //       val patt = Pattern.compile("https://x.ximg.cz.*?gif")
 //       val match = patt.matcher(output)
//        while (match.find()) {
//            val imageurl = match.group(0)
//            val imageName = imageurl.substring(imageurl.lastIndexOf("/") + 1)
//            val imagepath = arrayOfNulls<String>(1)
//            imagepath[0] = "file://" + getFilesDir().getAbsolutePath().toString() + "/" + imageName
//            output = output.replace(imageurl, imagepath[0])
//            if (!File(getFilesDir().getAbsolutePath().toString() + "/" + imageName).exists()) {
//                println(imageName + "does not exist")
//                download_image(imageName, imageurl)
//            }
//        }
        output
    } catch (r: Exception) {
        r.printStackTrace()
        " "
    }
}