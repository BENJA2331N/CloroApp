package com.example.cloroapp.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

object ApiConfig {
    /**
     * Para conectar una API real (por ejemplo un Spring Boot) cambia [USAR_API_SIMULADA] a false
     * y apunta [BASE_URL] al servidor (en el emulador, http://10.0.2.2:8080/).
     * Con la API simulada no se hace ninguna conexión de red: los datos son ficticios (respuestas 11 y 30).
     */
    const val BASE_URL = "https://api.cloroapp.example/"
    const val USAR_API_SIMULADA = true
}

interface CloroApiService {
    @GET("api/puntos")
    suspend fun obtenerPuntos(): List<PuntoDto>

    @GET("api/mediciones/ultimas")
    suspend fun obtenerUltimasMediciones(): List<MedicionDto>

    @POST("api/mediciones")
    suspend fun enviarMedicion(@Body medicion: MedicionDto): MedicionDto
}

object ApiFactory {
    fun crearServicio(): CloroApiService {
        val cliente = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .apply { if (ApiConfig.USAR_API_SIMULADA) addInterceptor(ApiSimuladaInterceptor()) }
            .build()

        return Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloroApiService::class.java)
    }
}
