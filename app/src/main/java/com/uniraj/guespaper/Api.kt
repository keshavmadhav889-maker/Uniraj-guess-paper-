package com.uniraj.guespaper

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

// Replace this with the deployed HTTPS backend before release.
private const val BASE_URL = "https://YOUR-BACKEND-DOMAIN.example/"

data class Paper(val id: Long, val title: String, val subject: String, val semester: String?, val price_paise: Int, val pdfUrl: String? = null)
data class User(val id: Long, val name: String, val email: String, val role: String = "user")
data class AuthResponse(val user: User, val token: String)
data class AuthRequest(val email: String, val password: String)
data class RegisterRequest(val name: String, val email: String, val password: String)
data class OrderResponse(val orderRef: String, val paperId: Long, val amount: Int, val upiId: String, val status: String)
data class SubmitPaymentRequest(val upiTxnId: String)
data class Purchase(val id: Long, val title: String, val subject: String, val semester: String?, val price_paise: Int, val pdfUrl: String?, val purchased_at: String)
data class Order(val id: Long, val payment_ref: String, val upi_txn_id: String?, val amount_paise: Int, val status: String, val created_at: String, val title: String, val subject: String)

interface UnirajApi {
 @POST("api/auth/login") suspend fun login(@Body body: AuthRequest): AuthResponse
 @POST("api/auth/register") suspend fun register(@Body body: RegisterRequest): AuthResponse
 @GET("api/papers") suspend fun papers(): List<Paper>
 @POST("api/orders") suspend fun createOrder(@Header("Authorization") token: String, @Body body: Map<String, Long>): OrderResponse
 @POST("api/orders/{ref}/submit-payment") suspend fun submitPayment(@Header("Authorization") token: String, @Path("ref") ref: String, @Body body: SubmitPaymentRequest): Map<String, Any>
 @GET("api/me/orders") suspend fun orders(@Header("Authorization") token: String): List<Order>
 @GET("api/me/purchases") suspend fun purchases(@Header("Authorization") token: String): List<Purchase>
 @GET("api/papers/{id}/access") suspend fun access(@Header("Authorization") token: String, @Path("id") id: Long): Map<String, String>
}

object ApiProvider {
 val api: UnirajApi = Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build().create(UnirajApi::class.java)
}
