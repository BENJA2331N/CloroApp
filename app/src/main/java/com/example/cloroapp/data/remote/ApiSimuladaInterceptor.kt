package com.example.cloroapp.data.remote

import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer

/**
 * Responde las peticiones de Retrofit con datos ficticios sin salir a la red.
 * Así la app consume una API REST real a nivel de código (Retrofit + JSON) sin necesitar un servidor.
 */
class ApiSimuladaInterceptor(private val gson: Gson = Gson()) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val peticion = chain.request()
        val ruta = peticion.url.encodedPath
        val ahora = System.currentTimeMillis()

        val (codigo, cuerpo) = when {
            peticion.method == "GET" && ruta.endsWith("/api/puntos") ->
                200 to gson.toJson(SimuladorCloro.puntosDto())

            peticion.method == "GET" && ruta.endsWith("/api/mediciones/ultimas") ->
                200 to gson.toJson(SimuladorCloro.ultimas(ahora))

            peticion.method == "POST" && ruta.endsWith("/api/mediciones") -> {
                val buffer = Buffer()
                peticion.body?.writeTo(buffer)
                201 to buffer.readUtf8()
            }

            else -> 404 to """{"error":"Recurso no encontrado"}"""
        }

        return Response.Builder()
            .request(peticion)
            .protocol(Protocol.HTTP_1_1)
            .code(codigo)
            .message(if (codigo in 200..299) "OK" else "Error")
            .body(cuerpo.toResponseBody("application/json; charset=utf-8".toMediaType()))
            .build()
    }
}
