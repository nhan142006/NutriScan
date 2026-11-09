package vn.edu.vlu.milestone2.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels { AuthViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonRegister.setOnClickListener {
            viewModel.register(
                displayName = binding.inputDisplayName.text?.toString().orEmpty(),
                email = binding.inputEmail.text?.toString().orEmpty(),
                password = binding.inputPassword.text?.toString().orEmpty(),
                confirmPassword = binding.inputConfirmPassword.text?.toString().orEmpty()
            )
        }
        binding.buttonGoLogin.setOnClickListener { findNavController().navigateUp() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: AuthUiState) = with(binding) {
        layoutDisplayName.error = state.nameError?.let { getString(it) }
        layoutEmail.error = state.emailError?.let { getString(it) }
        layoutPassword.error = state.passwordError?.let { getString(it) }
        layoutConfirmPassword.error = state.confirmPasswordError?.let { getString(it) }

        textAuthError.visibility = if (state.authError != null) View.VISIBLE else View.GONE
        state.authError?.let { textAuthError.setText(it.toMessageRes()) }

        progressAuth.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        buttonRegister.isEnabled = !state.isLoading
        buttonGoLogin.isEnabled = !state.isLoading

        if (state.isAuthenticated) {
            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.registerFragment) {
                navController.navigate(R.id.action_register_to_home)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
