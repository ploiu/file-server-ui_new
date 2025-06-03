package dev.ploiu.file_server_ui_new.module

import dev.ploiu.file_server_ui_new.client.ApiClient
import dev.ploiu.file_server_ui_new.client.FileClient
import dev.ploiu.file_server_ui_new.client.FolderClient
import dev.ploiu.file_server_ui_new.client.TagClient
import dev.ploiu.file_server_ui_new.config.ServerConfig
import dev.ploiu.file_server_ui_new.service.ApiService
import dev.ploiu.file_server_ui_new.service.FolderService
import file_server_ui_new.composeapp.generated.resources.Res
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.decodeCertificatePem
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.text.Charsets.UTF_8

val clientModule = module {
    single<Retrofit> { retrofitClient(get(), get()) }
    single<ApiClient> { apiClient(get()) }
    single<TagClient> { tagClient(get()) }
    single<FileClient> { fileClient(get()) }
    single<FolderClient> { folderClient(get()) }
}

val serviceModule = module {
    single<ApiService> { ApiService(get(), get()) }
    single<FolderService> { FolderService(get()) }
}

fun readServerCerts(): HandshakeCertificates = runBlocking {
    // I'm too lazy to figure out how to select only production vs local without adding a new variable to the app.properties, so I'm including both certs
    // TODO probably should just make it a json or toml file instead of a .properties file...I do like toml but probably just because it's associated with rust
    val local = String(Res.readBytes("files/cert_local.x509"), UTF_8).decodeCertificatePem()
    val production = String(Res.readBytes("files/cert_production.x509"), UTF_8).decodeCertificatePem()
    HandshakeCertificates.Builder()
        .addTrustedCertificate(local)
        .addTrustedCertificate(production)
        .build()
}

@OptIn(ExperimentalEncodingApi::class)
fun retrofitClient(serverConfig: ServerConfig, auth: Auth): Retrofit {
    val certificates = readServerCerts()
    val client = OkHttpClient.Builder()
        .sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder().addHeader("Authorization", auth.basicAuth()).build()
            chain.proceed(req)
        }
        .connectTimeout(1, TimeUnit.DAYS)
        .callTimeout(1, TimeUnit.DAYS)
        .readTimeout(1, TimeUnit.DAYS)
        .writeTimeout(1, TimeUnit.DAYS)
        .build()
    return Retrofit.Builder()
        .baseUrl(serverConfig.baseUrl)
        .addConverterFactory(Json.asConverterFactory("application/json; charset=utf-8".toMediaType()))
        .client(client)
        .build()
}

fun apiClient(retrofit: Retrofit): ApiClient = retrofit.create(ApiClient::class.java)

fun tagClient(retrofit: Retrofit): TagClient = retrofit.create(TagClient::class.java)

fun fileClient(retrofit: Retrofit): FileClient = retrofit.create(FileClient::class.java)

fun folderClient(retrofit: Retrofit): FolderClient = retrofit.create(FolderClient::class.java)
