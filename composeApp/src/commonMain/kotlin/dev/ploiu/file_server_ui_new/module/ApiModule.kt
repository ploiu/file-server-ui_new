package dev.ploiu.file_server_ui_new.module

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import dagger.Module
import dagger.Provides
import dev.ploiu.file_server_ui_new.ApiService
import dev.ploiu.file_server_ui_new.client.ApiClient
import dev.ploiu.file_server_ui_new.client.FileClient
import dev.ploiu.file_server_ui_new.client.FolderClient
import dev.ploiu.file_server_ui_new.client.TagClient
import dev.ploiu.file_server_ui_new.config.ServerConfig
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.io.encoding.ExperimentalEncodingApi

@Module
interface ApiModule {
 companion object {
     @Provides
     @OptIn(ExperimentalEncodingApi::class)
     fun retrofitClient(serverConfig: ServerConfig, auth: Auth): Retrofit {
         val pinner = CertificatePinner.Builder()
             .add(serverConfig.host, serverConfig.certificateHash)
             .build()
         val client = OkHttpClient.Builder()
             .certificatePinner(pinner)
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
             .addConverterFactory(JacksonConverterFactory.create(ObjectMapper().registerModule(Jdk8Module())))
             .client(client)
             .build()
     }

     @Provides
     fun apiClient(retrofit: Retrofit): ApiClient = retrofit.create(ApiClient::class.java)

     @Provides
     fun tagClient(retrofit: Retrofit): TagClient = retrofit.create(TagClient::class.java)

     @Provides
     fun fileClient(retrofit: Retrofit): FileClient = retrofit.create(FileClient::class.java)

     @Provides
     fun folderClient(retrofit: Retrofit): FolderClient = retrofit.create(FolderClient::class.java)

     @Provides
     fun apiService(client: ApiClient, config: ServerConfig) = ApiService(config, client)
 }
}
