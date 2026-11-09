package vn.edu.vlu.milestone2.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
firebase-authentication
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.data.repository.AuthRepository
import vn.edu.vlu.milestone2.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels {
        AuthViewModelFactory(AuthRepository(requireContext()))
    }

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels { AuthViewModel.Factory }
main

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

firebase-authentication
        binding.inputEmail.doAfterTextChanged { viewModel.clearErrors() }
        binding.inputPassword.doAfterTextChanged { viewModel.clearErrors() }

        binding.buttonLogin.setOnClickListener { submit() }
main
        binding.inputPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else {
                false
            }
        }
firebase-authentication
        binding.buttonLogin.setOnClickListener { submit() }
        binding.buttonOpenRegister.setOnClickListener {
            viewModel.clearErrors()
            findNavController().navigate(R.id.registerFragment)

        binding.buttonGoRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
main
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun submit() {
firebase-authentication
        viewModel.signIn(
            emailInput = binding.inputEmail.text?.toString().orEmpty(),

        viewModel.login(
            email = binding.inputEmail.text?.toString().orEmpty(),
main
            password = binding.inputPassword.text?.toString().orEmpty()
        )
    }

    private fun render(state: AuthUiState) = with(binding) {
 firebase-authentication
        inputEmailLayout.error = state.emailError
        inputPasswordLayout.error = state.passwordError
        textAuthError.text = state.errorMessage.orEmpty()
        textAuthError.isVisible = state.errorMessage != null
        progressLogin.isVisible = state.isLoading
        buttonLogin.text = if (state.isLoading) "" else getString(R.string.login_action)
        buttonLogin.isEnabled = !state.isLoading
        buttonOpenRegister.isEnabled = !state.isLoading

        if (state.isAuthenticated) navigateToHome()
    }

    private fun navigateToHome() {
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.loginFragment) return
        val options = NavOptions.Builder()
            .setPopUpTo(R.id.loginFragment, true)
            .build()
        navController.navigate(R.id.healthProfileFragment, null, options)

        layoutEmail.error = state.emailError?.let { getString(it) }
        layoutPassword.error = state.passwordError?.let { getString(it) }

        textAuthError.visibility = if (state.authError != null) View.VISIBLE else View.GONE
        state.authError?.let { textAuthError.setText(it.toMessageRes()) }

        progressAuth.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        buttonLogin.isEnabled = !state.isLoading
        buttonGoRegister.isEnabled = !state.isLoading

        if (state.isAuthenticated) {
            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.loginFragment) {
                navController.navigate(R.id.action_login_to_home)
            }
        }
main
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
