package vn.edu.vlu.milestone2.ui.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.data.repository.AuthError
import vn.edu.vlu.milestone2.data.repository.AuthRepository
import vn.edu.vlu.milestone2.data.repository.AuthResult
import vn.edu.vlu.milestone2.data.repository.AuthUser

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private class FakeAuthRepository(var nextResult: AuthResult) : AuthRepository {
        var signInCalls = 0
        override val currentUser: AuthUser? = null
        override fun observeAuthState(): Flow<AuthUser?> = flowOf(null)
        override suspend fun signIn(email: String, password: String): AuthResult {
            signInCalls++
            return nextResult
        }
        override suspend fun register(displayName: String, email: String, password: String) = nextResult
        override fun signOut() = Unit
    }

    private val user = AuthUser(uid = "uid", email = "a@b.com", displayName = "A")
    private val emailValidator: (String) -> Boolean = { it.contains('@') && it.contains('.') }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun login_invalidInput_doesNotCallFirebase() = runTest {
        val repository = FakeAuthRepository(AuthResult.Success(user))
        val viewModel = AuthViewModel(repository, emailValidator)

        viewModel.login(email = "not-an-email", password = "123")

        val state = viewModel.uiState.value
        assertEquals(R.string.auth_validation_email, state.emailError)
        assertEquals(R.string.auth_validation_password, state.passwordError)
        assertEquals(0, repository.signInCalls)
    }

    @Test
    fun login_success_setsAuthenticated() = runTest {
        val viewModel = AuthViewModel(FakeAuthRepository(AuthResult.Success(user)), emailValidator)

        viewModel.login(email = "a@b.com", password = "123456")

        assertTrue(viewModel.uiState.value.isAuthenticated)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun login_networkFailure_exposesError() = runTest {
        val viewModel = AuthViewModel(FakeAuthRepository(AuthResult.Failure(AuthError.NETWORK)), emailValidator)

        viewModel.login(email = "a@b.com", password = "123456")

        assertEquals(AuthError.NETWORK, viewModel.uiState.value.authError)
        assertFalse(viewModel.uiState.value.isAuthenticated)
    }

    @Test
    fun register_passwordMismatch_showsConfirmError() = runTest {
        val viewModel = AuthViewModel(FakeAuthRepository(AuthResult.Success(user)), emailValidator)

        viewModel.register("A", "a@b.com", "123456", "654321")

        assertEquals(R.string.auth_validation_confirm, viewModel.uiState.value.confirmPasswordError)
        assertFalse(viewModel.uiState.value.isAuthenticated)
    }
}
