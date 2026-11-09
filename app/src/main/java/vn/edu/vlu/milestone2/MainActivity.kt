package vn.edu.vlu.milestone2

import android.os.Bundle
import android.view.View
firebase-authentication

import androidx.activity.viewModels
main
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.data.repository.AuthUser
import vn.edu.vlu.milestone2.databinding.ActivityMainBinding
import vn.edu.vlu.milestone2.ui.auth.SessionViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    // Activity không gọi Firebase trực tiếp - mọi thao tác phiên đi qua SessionViewModel.
    private val sessionViewModel: SessionViewModel by viewModels { SessionViewModel.Factory }

    private val authDestinations = setOf(R.id.loginFragment, R.id.registerFragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Session persistence: nếu Firebase còn lưu phiên, vào thẳng màn hình Hồ sơ.
        val graph = navController.navInflater.inflate(R.navigation.nav_graph)
        graph.setStartDestination(
            if (sessionViewModel.isLoggedIn) R.id.healthProfileFragment else R.id.loginFragment
        )
        navController.setGraph(graph, null)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.loginFragment,
                R.id.healthProfileFragment,
                R.id.scannerFragment,
                R.id.cameraFragment,
                R.id.analysisFragment,
                R.id.recipeFragment
            )
        )
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)
        binding.bottomNavigation.setupWithNavController(navController)

firebase-authentication
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.visibility = if (
                destination.id == R.id.loginFragment || destination.id == R.id.registerFragment
            ) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
        binding.toolbar.inflateMenu(R.menu.menu_main)
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_logout) {
                sessionViewModel.signOut()
                true
            } else {
                false
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isAuthScreen = destination.id in authDestinations
            binding.bottomNavigation.visibility = if (isAuthScreen) View.GONE else View.VISIBLE
            binding.toolbar.menu.findItem(R.id.action_logout)?.isVisible = !isAuthScreen
            renderSubtitle(sessionViewModel.currentUser.value, isAuthScreen)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sessionViewModel.currentUser.collect { user ->
                    val isAuthScreen = navController.currentDestination?.id in authDestinations
                    renderSubtitle(user, isAuthScreen)
                    if (user == null && !isAuthScreen) {
                        // Đăng xuất hoặc phiên hết hạn -> quay về Đăng nhập và xóa back stack.
                        navController.navigate(
                            R.id.loginFragment,
                            null,
                            navOptions { popUpTo(R.id.nav_graph) { inclusive = true } }
                        )
                    }
                }
            }
        }
    }

    private fun renderSubtitle(user: AuthUser?, isAuthScreen: Boolean) {
        binding.toolbar.subtitle = if (user == null || isAuthScreen) {
            null
        } else {
            getString(R.string.auth_signed_in_as, user.displayName?.takeIf { it.isNotBlank() } ?: user.email)
        }
main
    }
}
