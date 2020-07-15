package com.epam.drill.proxy

import com.browserup.bup.BrowserUpProxy
import com.browserup.bup.BrowserUpProxyServer
import com.browserup.bup.proxy.CaptureType
import io.netty.handler.codec.http.HttpHeaderNames
import java.util.logging.Level
import java.util.logging.Logger

const val COOKIES_SEPARATOR = "; "
const val DRILL_SUFFIX = "drill-"
const val HTTP_PORT_ENV_VARIABLE_NAME = "DRILL_PROXY_HTTP_PORT"

private val logger = Logger.getLogger("proxy")

private const val DEFAULT_PORT = 7777

fun main() {
    val httpPort = System.getenv(HTTP_PORT_ENV_VARIABLE_NAME)?.toInt() ?: DEFAULT_PORT
    val proxy: BrowserUpProxy = BrowserUpProxyServer()
    proxy.start(httpPort)
    logger.info("Port: ${proxy.port}")
    proxy.enableHarCaptureTypes(CaptureType.getRequestCaptureTypes())
    proxy.addRequestFilter { request, _, messageInfo ->
        messageInfo.originalRequest.headers().get(HttpHeaderNames.COOKIE)?.let { rawCookieLine ->
            rawCookieLine.split(COOKIES_SEPARATOR).associate {
                val (k, v) = it.split("=")
                k to v
            }.filterKeys { it.startsWith(DRILL_SUFFIX) }
                .apply { forEach { k, v -> logger.log(Level.FINE) { "$k: $v" } } }
                .forEach { (t, u) -> request.headers().add(t, u) }


        }
        null
    }
}