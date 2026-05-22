package com.labflow.companion

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface LabFlowApi {
    @GET("api/health")
    suspend fun health(): Response<ApiResponse<HealthDto>>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<UserDto>>

    @GET("api/companion/labs/{userId}")
    suspend fun labs(
        @Header("Authorization") authorization: String,
        @Path("userId") userId: Int
    ): Response<ApiResponse<List<LabSummaryDto>>>

    @GET("api/companion/home/{userId}/{labId}")
    suspend fun home(
        @Header("Authorization") authorization: String,
        @Path("userId") userId: Int,
        @Path("labId") labId: Int
    ): Response<ApiResponse<CompanionHomeDto>>

    @GET("api/equipment/by-qr/{qrCode}")
    suspend fun equipmentByQr(
        @Header("Authorization") authorization: String,
        @Path("qrCode") qrCode: String
    ): Response<ApiResponse<EquipmentDto>>

    @GET("api/equipment/{id}")
    suspend fun equipmentById(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Response<ApiResponse<EquipmentDto>>

    @GET("api/equipment/{id}/history")
    suspend fun history(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Response<ApiResponse<List<ActivityLogDto>>>

    @POST("api/equipment/{id}/fault-report")
    suspend fun faultReport(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int,
        @Body request: FaultReportRequest
    ): Response<ApiResponse<Map<String, Any>>>

    @POST("api/equipment/{id}/borrow")
    suspend fun borrow(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int,
        @Body request: BorrowRequest
    ): Response<ApiResponse<Map<String, Any>>>

    @POST("api/equipment/{id}/return")
    suspend fun returnEquipment(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int,
        @Body request: ReturnRequest
    ): Response<ApiResponse<Map<String, Any>>>
}
