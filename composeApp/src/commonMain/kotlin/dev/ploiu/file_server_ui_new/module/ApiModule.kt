package dev.ploiu.file_server_ui_new.module

import dev.ploiu.file_server_ui_new.client.*
import dev.ploiu.file_server_ui_new.config.ServerConfig
import dev.ploiu.file_server_ui_new.service.ApiService
import dev.ploiu.file_server_ui_new.service.FileService
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
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.text.Charsets.UTF_8

// global objects are bad, but since creds aren't known at compile time, we need to have a global static object that contains them
object GLOBAL {
    var username: String? = null
    var password: String? = null

    val creds: String
        get() = "Basic " + Base64.encode(String.format("%s:%s", username, password).toByteArray())
}

val clientModule = module {
    single<OkHttpClient> { okHttpClient() }
    single<Retrofit> { retrofitClient(get(), get()) }
    single<PreviewClient> { previewClient(get(), get()) }
    single<ApiClient> { apiClient(get()) }
    single<TagClient> { tagClient(get()) }
    single<FileClient> { fileClient(get()) }
    single<FolderClient> { folderClient(get()) }
}

val serviceModule = module {
    single<ApiService> { ApiService(get(), get()) }
    single<FolderService> { FolderService(get()) }
    single<FileService> { FileService(get()) }
}

private fun readServerCerts(): HandshakeCertificates = runBlocking {
    // I'm too lazy to figure out how to select only production vs local without adding a new variable to the app.properties, so I'm including both certs
    val local = String(Res.readBytes("files/cert_local.x509"), UTF_8).decodeCertificatePem()
    val production = String(Res.readBytes("files/cert_production.x509"), UTF_8).decodeCertificatePem()
    HandshakeCertificates.Builder().addTrustedCertificate(local).addTrustedCertificate(production).build()
}

private val jsonParser = Json { ignoreUnknownKeys = true }

private fun okHttpClient(): OkHttpClient {
    val certificates = readServerCerts()
    return OkHttpClient
        .Builder()
        .sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder().addHeader("Authorization", GLOBAL.creds).build()
            chain.proceed(req)
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(0, TimeUnit.MILLISECONDS)
        .build()
}

private fun previewClient(serverConfig: ServerConfig, client: OkHttpClient) = PreviewClient(client, serverConfig)

@OptIn(ExperimentalEncodingApi::class)
private fun retrofitClient(serverConfig: ServerConfig, client: OkHttpClient): Retrofit {
    return Retrofit
        .Builder()
        .baseUrl(serverConfig.baseUrl)
        .addConverterFactory(jsonParser.asConverterFactory("application/json; charset=utf-8".toMediaType()))
        .client(client)
        .build()
}

private fun apiClient(retrofit: Retrofit): ApiClient = retrofit.create(ApiClient::class.java)

private fun tagClient(retrofit: Retrofit): TagClient = retrofit.create(TagClient::class.java)

private fun fileClient(retrofit: Retrofit): FileClient = retrofit.create(FileClient::class.java)

private fun folderClient(retrofit: Retrofit): FolderClient = retrofit.create(FolderClient::class.java)
