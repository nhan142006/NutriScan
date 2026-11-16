package vn.edu.vlu.milestone2.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.core.ui.collectWithLifecycle
import vn.edu.vlu.milestone2.core.ui.showSnackbar
import vn.edu.vlu.milestone2.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels { AuthViewModel.Factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editEmail.doAfterTextChanged { viewModel.onEmailChanged(it.toString()) }
        binding.editPassword.doAfterTextChanged { viewModel.onPasswordChanged(it.toString()) }
        binding.editPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else {
                false
            }
        }
        binding.buttonLogin.setOnClickListener { submit() }
        binding.buttonGoRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        collectWithLifecycle(viewModel.form) { state ->
            binding.layoutEmail.error = state.emailError?.let { getString(it) }
            binding.layoutPassword.error = state.passwordError?.let { getString(it) }
            binding.progressLogin.visibility = if (state.isLoading) View.VISIBLE else View.INVISIBLE
            binding.buttonLogin.isEnabled = !state.isLoading
            binding.buttonGoRegister.isEnabled = !state.isLoading
        }
        collectWithLifecycle(viewModel.events) { event ->
            when (event) {
                AuthEvent.LoggedIn, AuthEvent.Registered -> findNavController().navigate(R.id.action_login_to_home)
                is AuthEvent.ShowError -> binding.root.showSnackbar(getString(event.message), long = true)
            }
        }
    }

    private fun submit() {
        viewModel.signIn(binding.editEmail.text.toString().trim(), binding.editPassword.text.toString())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
