package dev.ploiu.file_server_ui_new.module

import dev.ploiu.file_server_ui_new.config.ServerConfig
import file_server_ui_new.composeapp.generated.resources.Res
import kotlinx.coroutines.runBlocking
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class Auth(val username: String, val password: String) {
    @ExperimentalEncodingApi
    fun basicAuth(): String {
        return "Basic " + Base64.encode(String.format("%s:%s", username, password).toByteArray())
    }
}

val configModule = module {
    single<ByteArray>(named("properties")) {
        runBlocking {
            Res.readBytes("files/app.properties")
        }
    }
    single<Auth> { getAuth(get(named("properties"))) }
    single<ServerConfig> { getServerConfig(get(named("properties"))) }
}

fun getAuth(propBytes: ByteArray): Auth {
    return propBytes.inputStream().use { stream ->
        val props = Properties()
        props.load(stream)
        Auth(props.getProperty("auth.username"), props.getProperty("auth.password"))
    }
}

fun getServerConfig(propBytes: ByteArray): ServerConfig {
    return propBytes.inputStream().use {
        val props = Properties()
        props.load(it)
        val host = props.getProperty("server.address")
        val port = props.getProperty("server.port").toInt()
        val compatibleVersion = props.getProperty("server.compatible.version")
        ServerConfig("https://${host}:${port}", compatibleVersion, host, port)
    }
}
