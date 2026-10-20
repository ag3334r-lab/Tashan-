package com.example.model

/**
 * Firestore User Wallet Schema
 * Location in Firestore: /users/{uid}/wallet/main or /wallets/{uid}
 */
data class FirestoreUserWallet(
    val uid: String = "",
    val coins: Int = 0,
    val balancePkr: Double = 0.0,
    val totalWins: Int = 0,
    val totalLosses: Int = 0,
    val totalGamesPlayed: Int = 0,
    val totalCoinsEarned: Long = 0L,
    val totalCoinsSpent: Long = 0L,
    val winStreak: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun winRatePercentage(): Double {
        if (totalGamesPlayed <= 0) return 0.0
        return (totalWins.toDouble() / totalGamesPlayed.toDouble()) * 100.0
    }
}

/**
 * Firestore Wallet Transaction Document
 * Location in Firestore: /users/{uid}/wallet_transactions/{transactionId}
 */
data class FirestoreWalletTransaction(
    val transactionId: String = "",
    val uid: String = "",
    val type: String = "GAME_WAGER", // GAME_WAGER, GAME_WIN, GAME_LOSS, COIN_DEPOSIT, GIFT_SENT, WITHDRAWAL
    val amountCoins: Int = 0,
    val amountPkr: Double = 0.0,
    val description: String = "",
    val gameId: String = "",
    val opponentHandle: String = "",
    val status: String = "COMPLETED", // COMPLETED, PENDING, FAILED
    val timestamp: Long = System.currentTimeMillis(),
    val balanceCoinsAfter: Int = 0
)

/**
 * Firestore Game Match Win/Loss History Record
 * Location in Firestore: /users/{uid}/game_history/{matchId}
 */
data class FirestoreGameMatchRecord(
    val matchId: String = "",
    val uid: String = "",
    val gameName: String = "1v1 TikTok PK Battle",
    val opponentHandle: String = "",
    val wagerCoins: Int = 0,
    val prizePoolCoins: Int = 0,
    val isWinner: Boolean = false,
    val coinsEarned: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val roomCode: String = ""
)

/**
 * Deposit / Withdrawal Request Document for Pakistani & Global Gateways
 * Location: /payment_requests/{requestId}
 */
data class PaymentGatewayRequest(
    val requestId: String = "",
    val uid: String = "",
    val userEmail: String = "",
    val userHandle: String = "",
    val gateway: String = "JazzCash", // JazzCash, EasyPaisa, PayPal, Bank IBAN
    val accountTitle: String = "",
    val accountNumberOrIban: String = "",
    val amountCoins: Int = 0,
    val amountPkr: Double = 0.0,
    val transactionTrxId: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, CANCELLED, REJECTED
    val requestType: String = "DEPOSIT", // DEPOSIT, WITHDRAWAL
    val timestamp: Long = System.currentTimeMillis(),
    val adminNote: String = "",
    val verifiedAt: Long = 0L,
    val verifiedBy: String = ""
)

/**
 * Admin Custom Game Created by Admin
 * Location: /admin_custom_games/{gameId}
 */
data class AdminCustomGame(
    val gameId: String = "",
    val title: String = "",
    val description: String = "",
    val webUrl: String = "",
    val entryFeeCoins: Int = 100,
    val rewardCoins: Int = 200,
    val iconEmoji: String = "🎮",
    val createdByAdmin: String = "admin@tashan.pk",
    val isOnlineActive: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
