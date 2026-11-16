package vn.edu.vlu.milestone2.core.ui

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions

/** Chuyển sang 1 tab của BottomNavigation giống hệt khi người dùng bấm tab (giữ state từng tab). */
fun Fragment.navigateToTab(@IdRes destinationId: Int, args: Bundle? = null) {
    val navController = findNavController()
    navController.navigate(
        destinationId,
        args,
        navOptions {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = args == null
        }
    )
}
