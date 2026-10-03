package com.example.data.model

enum class SyncStatus {
    SYNCED,
    SYNCING,
    OFFLINE,
    ERROR
}

data class Account(
    val id: String = "",
    val name: String = "",
    val icon: String = "💵",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "icon" to icon,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Account {
            return Account(
                id = id,
                name = map["name"] as? String ?: "Account",
                icon = map["icon"] as? String ?: "💵",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

data class TransactionEntry(
    val id: String = "",
    val accountId: String = "",
    val type: String = "in", // "in" or "out"
    val amount: Double = 0.0,
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "accountId" to accountId,
        "type" to type,
        "amount" to amount,
        "details" to details,
        "timestamp" to timestamp
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): TransactionEntry {
            return TransactionEntry(
                id = id,
                accountId = map["accountId"] as? String ?: "",
                type = map["type"] as? String ?: "in",
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                details = map["details"] as? String ?: "",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

data class TrashItem(
    val id: String = "",
    val originalId: String = "",
    val itemType: String = "entry", // "entry" or "account"
    val title: String = "",
    val amount: Double = 0.0,
    val details: String = "",
    val accountId: String = "",
    val entryType: String = "in",
    val deletedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "originalId" to originalId,
        "itemType" to itemType,
        "title" to title,
        "amount" to amount,
        "details" to details,
        "accountId" to accountId,
        "entryType" to entryType,
        "deletedAt" to deletedAt
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): TrashItem {
            return TrashItem(
                id = id,
                originalId = map["originalId"] as? String ?: "",
                itemType = map["itemType"] as? String ?: "entry",
                title = map["title"] as? String ?: "",
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                details = map["details"] as? String ?: "",
                accountId = map["accountId"] as? String ?: "",
                entryType = map["entryType"] as? String ?: "in",
                deletedAt = (map["deletedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

data class UserSession(
    val uid: String,
    val email: String? = null,
    val displayName: String? = null,
    val isAnonymous: Boolean = false
)
