package com.example.safewalk.data.remote

import com.example.safewalk.data.model.*
import retrofit2.http.*

interface SafeWalkApiService {
    // AUTH ENDPOINTS
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse
    
    @POST("auth/logout")
    suspend fun logout()
    
    // ALERT ENDPOINTS
    @POST("alerts/trigger")
    suspend fun triggerAlert(@Body request: TriggerAlertRequest): Alert
    
    @PUT("alerts/{id}/status")
    suspend fun updateAlertStatus(
        @Path("id") id: String,
        @Body request: UpdateAlertRequest
    ): Alert
    
    @POST("alerts/location")
    suspend fun recordAlertLocation(@Body request: RecordLocationRequest): AlertLocation
    
    @GET("alerts")
    suspend fun getAlertHistory(@Query("limit") limit: Int = 20): List<Alert>
    
    @GET("alerts/{id}")
    suspend fun getAlert(@Path("id") id: String): Alert
    
    @DELETE("alerts/{id}")
    suspend fun cancelAlert(@Path("id") id: String)
    
    // CONTACT ENDPOINTS
    @POST("contacts")
    suspend fun addContact(@Body request: AddContactRequest): EmergencyContact
    
    @GET("contacts")
    suspend fun getContacts(): List<EmergencyContact>
    
    @GET("contacts/{id}")
    suspend fun getContact(@Path("id") id: String): EmergencyContact
    
    @PUT("contacts/{id}")
    suspend fun updateContact(
        @Path("id") id: String,
        @Body request: AddContactRequest
    ): EmergencyContact
    
    @DELETE("contacts/{id}")
    suspend fun deleteContact(@Path("id") id: String)
}
