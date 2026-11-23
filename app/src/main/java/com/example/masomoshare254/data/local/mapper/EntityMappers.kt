package com.example.masomoshare254.data.local.mapper

import com.example.masomoshare254.data.local.entity.DownloadEntity
import com.example.masomoshare254.data.local.entity.ResourceEntity
import com.example.masomoshare254.data.local.entity.UserEntity
import com.example.masomoshare254.model.Download
import com.example.masomoshare254.model.Resource
import com.example.masomoshare254.model.User

fun UserEntity.toDomain(): User = User(
    userId = userId,
    fullName = fullName,
    email = email,
    role = role,
    institution = institution
)

fun User.toEntity(): UserEntity = UserEntity(
    userId = userId,
    fullName = fullName,
    email = email,
    role = role,
    institution = institution
)

fun ResourceEntity.toDomain(): Resource = Resource(
    resourceId = resourceId,
    title = title,
    description = description,
    subject = subject,
    educationLevel = educationLevel,
    topic = topic,
    resourceType = resourceType,
    fileUrl = fileUrl,
    contributorId = contributorId
)

fun Resource.toEntity(): ResourceEntity = ResourceEntity(
    resourceId = resourceId,
    title = title,
    description = description,
    subject = subject,
    educationLevel = educationLevel,
    topic = topic,
    resourceType = resourceType,
    fileUrl = fileUrl,
    contributorId = contributorId
)

fun DownloadEntity.toDomain(): Download = Download(
    downloadId = downloadId,
    userId = userId,
    resourceId = resourceId,
    localFilePath = localFilePath,
    downloadDate = downloadDate
)

fun Download.toEntity(): DownloadEntity = DownloadEntity(
    downloadId = downloadId,
    userId = userId,
    resourceId = resourceId,
    localFilePath = localFilePath,
    downloadDate = downloadDate
)