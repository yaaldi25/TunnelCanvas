package com.tunnelcanvas

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/** Builds a sing-box JSON config for a Trojan + TLS + WebSocket outbound.
 * This generates configuration only; a libbox/VpnService runtime must consume it to connect.
 */
object TrojanSingBoxConfig {
    data class Profile(
        val name: String,
        val server: String,
        val port: Int,
        val password: String,
        val sni: String,
        val wsHost: String,
        val wsPath: String
    )

    fun fromShareUri(raw: String, fallbackName: String = "Trojan profile"): Profile {
        val uri = Uri.parse(raw.trim())
        require(uri.scheme.equals("trojan", ignoreCase = true)) { "Link harus menggunakan skema trojan://" }
        val host = uri.host?.takeIf { it.isNotBlank() } ?: error("Hostname server tidak ditemukan")
        val parsedPort = uri.port
        val port = if (parsedPort in 1..65535) parsedPort else 443
        val password = uri.userInfo?.takeIf { it.isNotBlank() } ?: error("Password Trojan tidak ditemukan dalam link")
        val tls = uri.getQueryParameter("security")
        val type = uri.getQueryParameter("type")
        require(tls.isNullOrBlank() || tls.equals("tls", true)) { "Profil ini bukan TLS" }
        require(type.isNullOrBlank() || type.equals("ws", true)) { "Profil ini bukan WebSocket" }
        val path = uri.getQueryParameter("path")?.takeIf { it.isNotBlank() } ?: "/"
        val sni = uri.getQueryParameter("sni")?.takeIf { it.isNotBlank() } ?: host
        val wsHost = uri.getQueryParameter("host")?.takeIf { it.isNotBlank() } ?: sni
        val label = uri.fragment?.takeIf { it.isNotBlank() } ?: fallbackName
        return Profile(label, host, port, password, sni, wsHost, path)
    }

    fun build(profile: Profile): String {
        require(profile.server.isNotBlank()) { "Hostname server wajib diisi" }
        require(profile.port in 1..65535) { "Port harus antara 1 dan 65535" }
        require(profile.password.isNotBlank()) { "Password Trojan wajib diisi" }
        require(profile.wsPath.startsWith("/")) { "Path WebSocket harus diawali /" }
        val outbound = JSONObject()
            .put("type", "trojan")
            .put("tag", "proxy")
            .put("server", profile.server)
            .put("server_port", profile.port)
            .put("password", profile.password)
            .put("tls", JSONObject()
                .put("enabled", true)
                .put("server_name", profile.sni.ifBlank { profile.server })
                .put("alpn", JSONArray().put("http/1.1")))
            .put("transport", JSONObject()
                .put("type", "ws")
                .put("path", profile.wsPath)
                .put("headers", JSONObject().put("Host", profile.wsHost.ifBlank { profile.server })))
        return JSONObject()
            .put("log", JSONObject().put("level", "info"))
            .put("dns", JSONObject()
                .put("servers", JSONArray().put(JSONObject()
                    .put("type", "udp")
                    .put("tag", "dns-remote")
                    .put("server", "1.1.1.1")))
                .put("final", "dns-remote"))
            .put("inbounds", JSONArray().put(JSONObject()
                .put("type", "tun")
                .put("tag", "tun-in")
                .put("address", JSONArray().put("172.19.0.1/30").put("fdfe:dcba:9876::1/126"))
                .put("auto_route", true)
                .put("strict_route", true)))
            .put("outbounds", JSONArray()
                .put(outbound)
                .put(JSONObject().put("type", "direct").put("tag", "direct"))
                .put(JSONObject().put("type", "block").put("tag", "block")))
            .put("route", JSONObject().put("auto_detect_interface", true).put("final", "proxy"))
            .toString(2)
    }
}
