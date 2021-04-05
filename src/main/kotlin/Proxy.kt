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

import io.netty.channel.*
import io.netty.handler.codec.http.*
import org.littleshoot.proxy.*
import org.littleshoot.proxy.extras.*
import org.littleshoot.proxy.impl.*
import java.net.*
import java.util.logging.*


private val logger = Logger.getLogger("proxy")

const val COOKIES_SEPARATOR = "; "
const val DRILL_SUFFIX = "drill-"
const val HTTP_PORT_ENV_VARIABLE_NAME = "DRILL_PROXY_HTTP_PORT"
private const val DEFAULT_PORT = 7777

fun main() {
    val httpPort = System.getenv(HTTP_PORT_ENV_VARIABLE_NAME)?.toInt() ?: DEFAULT_PORT
    val server = DefaultHttpProxyServer.bootstrap()
        .withTransparent(true)
        .withAddress(InetSocketAddress(httpPort))
        .withManInTheMiddle(SelfSignedMitmManager(SelfSignedSslEngineSource(true)))
        .withFiltersSource(
            object : HttpFiltersSource {
                override fun filterRequest(originalRequest: HttpRequest?, ctx: ChannelHandlerContext?): HttpFilters {
                    return AddHeadersFromCookiesFilter(originalRequest, ctx)
                }

                override fun getMaximumRequestBufferSizeInBytes(): Int {
                    return 0
                }

                override fun getMaximumResponseBufferSizeInBytes(): Int {
                    return 0
                }
            }
        ).start()
    logger.info("Address: ${server.listenAddress}")
}

class AddHeadersFromCookiesFilter(
    originalRequest: HttpRequest?,
    ctx: ChannelHandlerContext?
) : HttpFiltersAdapter(originalRequest, ctx) {
    override fun clientToProxyRequest(httpObject: HttpObject?): HttpResponse? {
        if (httpObject is HttpRequest) {
            httpObject.headers().get(HttpHeaderNames.COOKIE)?.let { rawCookieLine ->
                rawCookieLine.split(COOKIES_SEPARATOR).associate {
                    val (k, v) = it.split("=")
                    k to v
                }.filterKeys { it.startsWith(DRILL_SUFFIX) }
                    .onEach { (k, v) -> logger.log(Level.FINE) { "$k: $v" } }
                    .forEach { (t, u) -> httpObject.headers().add(t, u) }
            }
        }
        return null
    }
}
