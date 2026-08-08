package com.sounds.legion

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class LegionMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "LegionSound"
        val body = message.notification?.body ?: message.data["body"] ?: "Tienes contenido nuevo."
        NotificationHelper.show(applicationContext, title, body)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().reference
            .child("usuarios")
            .child(userId)
            .child("fcmToken")
            .setValue(token)
    }
}