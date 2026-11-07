package chat.oi.app

import android.app.Application

class OiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // High-speed cold start: zero main-thread blocking operations.
    }
}
