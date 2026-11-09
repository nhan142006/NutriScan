package vn.edu.vlu.milestone2

import android.app.Application
import vn.edu.vlu.milestone2.di.ServiceLocator

class NutriScanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
