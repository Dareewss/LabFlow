package com.labflow.companion

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: String?
)

data class HealthDto(
    val status: String?,
    val app: String?,
    val apiVersion: String?
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class UserDto(
    val id: Int,
    val username: String?,
    val fullName: String?,
    val role: String?,
    val displayName: String?
)

data class EquipmentDto(
    val id: Int,
    val name: String?,
    val category: String?,
    val description: String?,
    val location: String?,
    val status: String?,
    val qrCode: String?,
    val qrCodePath: String?,
    val serialNumber: String?,
    val manufacturer: String?,
    val model: String?,
    val purchaseDate: String?,
    val lastMaintenanceDate: String?,
    val notes: String?,
    val activeBorrowRecordId: Int?,
    val activeBorrowUserId: Int? = null,
    val activeBorrowUsername: String? = null
)

data class FaultReportRequest(
    val reportedByUserId: Int,
    val description: String,
    val severity: String
)

data class BorrowRequest(
    val userId: Int,
    val expectedReturnDate: String?,
    val notes: String?
)

data class ReturnRequest(
    val borrowRecordId: Int,
    val returnCondition: String,
    val notes: String?,
    val defectDescription: String?
)

data class ActivityLogDto(
    val id: Int,
    val userId: Int?,
    val username: String?,
    val action: String?,
    val entityType: String?,
    val entityId: Int?,
    val description: String?,
    val timestamp: String?
)

data class LabSummaryDto(
    val id: Int,
    val name: String?,
    val inviteCode: String?,
    val role: String?,
    val palette: String?
)

data class CompanionStatsDto(
    val totalEquipment: Int,
    val borrowedEquipment: Int,
    val availableEquipment: Int,
    val containerCount: Int,
    val myBorrowedCount: Int,
    val myTotalBorrows: Int = 0,
    val notificationCount: Int,
    val faultCount: Int = 0,
    val maintenanceDueCount: Int = 0,
    val overdueBorrows: Int = 0,
    val healthScore: Int = 100,
    val myPoints: Int = 0,
    val myRank: Int = 0
)

data class CompanionBorrowedItemDto(
    val borrowRecordId: Int,
    val equipmentId: Int,
    val name: String?,
    val location: String?,
    val expectedReturnDate: String?,
    val status: String?
)

data class CompanionBorrowHistoryItemDto(
    val borrowRecordId: Int,
    val equipmentId: Int,
    val name: String?,
    val location: String?,
    val borrowDate: String?,
    val expectedReturnDate: String?,
    val actualReturnDate: String?,
    val status: String?,
    val returnCondition: String?
)

data class CompanionContainerDto(
    val id: Int,
    val name: String?,
    val itemCount: Int
)

data class CompanionRiskyEquipmentDto(
    val equipmentId: Int,
    val equipmentName: String?,
    val score: Int,
    val level: String?,
    val reasons: List<String>?
)

data class CompanionNotificationDto(
    val id: Int,
    val title: String?,
    val message: String?,
    val type: String?,
    val read: Boolean,
    val createdAt: String?
)

data class CompanionHomeDto(
    val lab: LabSummaryDto?,
    val labs: List<LabSummaryDto>?,
    val stats: CompanionStatsDto?,
    val borrowedItems: List<CompanionBorrowedItemDto>?,
    val borrowHistory: List<CompanionBorrowHistoryItemDto>?,
    val equipmentStatusCounts: Map<String, Int>?,
    val recentActivity: List<ActivityLogDto>?,
    val topRiskyEquipment: List<CompanionRiskyEquipmentDto>?,
    val containers: List<CompanionContainerDto>?,
    val notifications: List<CompanionNotificationDto>?
)
