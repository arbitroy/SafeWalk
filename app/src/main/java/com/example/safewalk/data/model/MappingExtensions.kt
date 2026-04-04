package com.example.safewalk.data.model

fun Alert.toEntity() = AlertEntity(
    id, userId, alertType.name, status.name, triggeredAt, resolvedAt, notes
)

fun AlertEntity.toDomain() = Alert(
    id, userId, AlertType.valueOf(alertType), AlertStatus.valueOf(status),
    triggeredAt, resolvedAt, notes
)

fun AlertLocation.toEntity() = AlertLocationEntity(
    id, alertId, latitude, longitude, accuracy, timestamp
)

fun EmergencyContact.toEntity() = EmergencyContactEntity(
    id, userId, name, phone, email, notificationPreference.name, isActive
)

fun EmergencyContactEntity.toDomain() = EmergencyContact(
    id, userId, name, phone, email, NotificationPreference.valueOf(notificationPreference), isActive
)

fun UserEntity.toDomain() = User(
    id, email, firstName, lastName, phone, createdAt
)

fun User.toEntity() = UserEntity(
    id, email, firstName, lastName, phone, createdAt
)
