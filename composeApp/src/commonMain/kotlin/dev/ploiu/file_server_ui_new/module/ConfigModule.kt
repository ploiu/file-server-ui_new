package dev.ploiu.file_server_ui_new.module

import dagger.Module
import dagger.Provides
import dev.ploiu.file_server_ui_new.config.ServerConfig
import file_server_ui_new.composeapp.generated.resources.Res
import kotlinx.coroutines.runBlocking
import java.util.*
import jakarta.inject.Singleton
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class Auth(val username: String, val password: String) {
    @ExperimentalEncodingApi
    fun basicAuth(): String {
        return "Basic " + Base64.encode(String.format("%s:%s", username, password).toByteArray())
    }
}

@Module
interface ConfigModule {
    companion object {

        @Provides
        @Singleton
        fun getConfig(): ByteArray = runBlocking {
            Res.readBytes("files/app.properties")
        }

        @Provides
        fun getAuth(propBytes: ByteArray): Auth {
            return propBytes.inputStream().use { stream ->
                val props = Properties()
                props.load(stream)
                Auth(props.getProperty("auth.username"), props.getProperty("auth.password"))
            }
        }

        @Provides
        @Singleton
        fun getServerConfig(propBytes: ByteArray): ServerConfig {
            return propBytes.inputStream().use {
                val props = Properties()
                props.load(it)
                val host = props.getProperty("server.address")
                val port = props.getProperty("server.port").toInt()
                val compatibleVersion = props.getProperty("server.compatible.version")
                val certificateHash = props.getProperty("server.certificate.hash")
                ServerConfig("https://${host}:${port}", compatibleVersion, host, port, certificateHash)
            }
        }
    }
}
