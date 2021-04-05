package com.epam.drill.proxy.agent

import javassist.*
import java.lang.instrument.*
import java.security.*
import java.util.logging.*

private val logger = Logger.getLogger("proxy")

@Suppress("Unused")
class Agent {
    companion object {
        @JvmStatic
        fun premain(agentArgs: String?, inst: Instrumentation) {
            logger.info { "Agent started" }
            inst.addTransformer(ProxyUtilTransformer())
        }
    }
}

@Suppress("PrivatePropertyName")
class ProxyUtilTransformer : ClassFileTransformer {
    private val proxyUtils = "org/littleshoot/proxy/impl/ProxyUtils"
    private val HttpResponseStatus = "io.netty.handler.codec.http.HttpResponseStatus"
    private val HttpHeaderNames = "io.netty.handler.codec.http.HttpHeaderNames"

    override fun transform(
        loader: ClassLoader?,
        className: String?,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray?
    ): ByteArray? {
        runCatching {
            if (className == proxyUtils) {
                val ctClass = ClassPool.getDefault()[proxyUtils.replace("/", ".")]
                ctClass.getDeclaredMethod("isSwitchingToWebSocketProtocol").setBody(
                    """
                    return ($1.status().code() == $HttpResponseStatus.SWITCHING_PROTOCOLS.code())
                    && $1.headers().contains($HttpHeaderNames.CONNECTION, $HttpHeaderNames.UPGRADE, true)
                    && $1.headers().contains($HttpHeaderNames.UPGRADE, "websocket", true);
                """.trimIndent()
                )
                logger.info { "The $proxyUtils class was successfully transformed" }
                return ctClass.toBytecode()
            }
        }.onFailure {
            logger.warning { "The class was not transformed. WS connection can be broken. Reason: $it " }
        }
        return null
    }
}
