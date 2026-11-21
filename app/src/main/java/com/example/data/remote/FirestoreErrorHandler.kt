package com.example.data.remote

import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException

object FirestoreErrorHandler {
    fun messageFor(throwable: Throwable): String = when (throwable) {
        is FirebaseFirestoreException -> when (throwable.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Sin permisos para realizar esta acción."
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                "Conexión lenta con la nube. Verifica tu internet e intenta de nuevo."
            FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                "Tu sesión expiró. Vuelve a iniciar sesión con Google."
            else ->
                "No se pudo sincronizar con NexoLive (${throwable.code.name})."
        }
        is FirebaseAuthException ->
            "Error de autenticación con Google (${throwable.errorCode})."
        is FirebaseException ->
            throwable.localizedMessage ?: "Error de conexión con NexoLive."
        else ->
            throwable.localizedMessage ?: "No se pudo completar la operación."
    }

    fun log(tag: String, operation: String, throwable: Throwable) {
        Log.e(tag, "Firestore [$operation] failed: ${throwable.message}", throwable)
    }
}
