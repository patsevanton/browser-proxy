/**
 * Copyright 2020 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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