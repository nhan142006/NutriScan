package vn.edu.vlu.milestone2

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.databinding.ActivityMainBinding
import vn.edu.vlu.milestone2.ui.auth.SessionViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    // Activity không gọi Firebase trực tiếp – mọi thao tác phiên đi qua SessionViewModel.
    private val sessionViewModel: SessionViewModel by viewModels { SessionViewModel.Factory }

    private val authDestinations = setOf(R.id.loginFragment, R.id.registerFragment)
    private val topLevelDestinations = setOf(
        R.id.homeFragment, R.id.scanHubFragment, R.id.historyFragment, R.id.recipesFragment, R.id.profileFragment
    )
    /** Màn hình camera toàn màn hình: ẩn toolbar + bottom nav. */
    private val fullScreenDestinations = setOf(R.id.barcodeScannerFragment, R.id.labelCameraFragment)

    private var lastInsets: WindowInsetsCompat? = null
    private var isOnline = true

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Session persistence: Firebase còn lưu phiên → vào thẳng Trang chủ.
        // Luôn setGraph (kể cả khi xoay màn hình) để NavController khôi phục back stack đã lưu.
        val graph = navController.navInflater.inflate(R.navigation.nav_graph)
        graph.setStartDestination(if (sessionViewModel.isLoggedIn) R.id.homeFragment else R.id.loginFragment)
        navController.setGraph(graph, null)

        binding.toolbar.setupWithNavController(
            navController,
            AppBarConfiguration(topLevelDestinations + authDestinations)
        )
        binding.bottomNavigation.setupWithNavController(navController)
        binding.toolbar.inflateMenu(R.menu.menu_main)
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_language) {
                val language = if (resources.configuration.locales[0].language == "vi") "en" else "vi"
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
                true
            } else if (item.itemId == R.id.action_logout) {
                sessionViewModel.signOut()
                true
            } else {
                false
            }
        }

        navController.addOnDestinationChangedListener { controller, destination, _ ->
            val isAuth = destination.id in authDestinations
            // Giữ start destination đúng với trạng thái đăng nhập để tab BottomNav popUpTo chính xác.
            val desiredStart = if (isAuth) R.id.loginFragment else R.id.homeFragment
            if (controller.graph.startDestinationId != desiredStart && (isAuth || destination.id in topLevelDestinations)) {
                controller.graph.setStartDestination(desiredStart)
            }
            val isFullScreen = destination.id in fullScreenDestinations
            binding.toolbar.visibility = if (isFullScreen) View.GONE else View.VISIBLE
            binding.toolbar.menu.findItem(R.id.action_logout)?.isVisible = destination.id == R.id.profileFragment
            renderOfflineBanner()
            applyInsets()
            if (isAuth) binding.toolbar.subtitle = null
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            lastInsets = insets
            applyInsets()
            insets // không consume: BottomNavigationView & màn hình camera tự xử lý phần của chúng
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sessionViewModel.currentUser.collect { user ->
                        val destinationId = navController.currentDestination?.id
                        if (user == null && destinationId != null && destinationId !in authDestinations) {
                            // Đăng xuất / phiên hết hạn → về Đăng nhập và xóa back stack.
                            navController.navigate(
                                R.id.loginFragment,
                                null,
                                navOptions { popUpTo(R.id.nav_graph) { inclusive = true } }
                            )
                        }
                    }
                }
                launch {
                    sessionViewModel.isOnline.collect { online ->
                        isOnline = online
                        renderOfflineBanner()
                    }
                }
            }
        }
    }

    private fun renderOfflineBanner() {
        val destinationId = navController.currentDestination?.id
        val hidden = destinationId in fullScreenDestinations
        binding.textOfflineBanner.visibility = if (!isOnline && !hidden) View.VISIBLE else View.GONE
    }

    /** Edge-to-edge (bắt buộc từ Android 15): tự đệm toolbar / bottom nav / bàn phím. */
    private fun applyInsets() {
        val insets = lastInsets ?: return
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
        val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        val destinationId = navController.currentDestination?.id
        val isFullScreen = destinationId in fullScreenDestinations
        val showBottomNav = destinationId in topLevelDestinations && !imeVisible

        binding.root.updatePadding(left = bars.left, right = bars.right)
        binding.toolbar.updatePadding(top = bars.top)
        binding.bottomNavigation.visibility = if (showBottomNav) View.VISIBLE else View.GONE
        binding.navHostFragment.updatePadding(
            bottom = when {
                imeVisible -> imeBottom
                showBottomNav || isFullScreen -> 0
                else -> bars.bottom
            }
        )
    }
}
