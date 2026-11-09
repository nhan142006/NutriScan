package vn.edu.vlu.milestone2.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
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

        binding.inputEmail.doAfterTextChanged { viewModel.clearErrors() }
        binding.inputPassword.doAfterTextChanged { viewModel.clearErrors() }
        binding.inputPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else {
                false
            }
        }
        binding.buttonLogin.setOnClickListener { submit() }
        binding.buttonOpenRegister.setOnClickListener {
            viewModel.clearErrors()
            findNavController().navigate(R.id.registerFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun submit() {
        viewModel.signIn(
            emailInput = binding.inputEmail.text?.toString().orEmpty(),
            password = binding.inputPassword.text?.toString().orEmpty()
        )
    }

    private fun render(state: AuthUiState) = with(binding) {
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
