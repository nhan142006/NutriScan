package vn.edu.vlu.milestone2.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.core.ui.collectWithLifecycle
import vn.edu.vlu.milestone2.core.ui.navigateToTab
import vn.edu.vlu.milestone2.core.ui.showSnackbar
import vn.edu.vlu.milestone2.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels { AuthViewModel.Factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editEmail.doAfterTextChanged { viewModel.onEmailChanged(it.toString()) }
        binding.editPassword.doAfterTextChanged {
            viewModel.onPasswordChanged(it.toString())
            viewModel.onConfirmChanged(it.toString(), binding.editConfirm.text.toString())
        }
        binding.editConfirm.doAfterTextChanged {
            viewModel.onConfirmChanged(binding.editPassword.text.toString(), it.toString())
        }
        binding.buttonRegister.setOnClickListener {
            viewModel.register(
                name = binding.editName.text.toString(),
                email = binding.editEmail.text.toString().trim(),
                password = binding.editPassword.text.toString(),
                confirm = binding.editConfirm.text.toString()
            )
        }
        binding.buttonGoLogin.setOnClickListener { findNavController().navigateUp() }

        collectWithLifecycle(viewModel.form) { state ->
            binding.layoutName.error = state.nameError?.let { getString(it) }
            binding.layoutEmail.error = state.emailError?.let { getString(it) }
            binding.layoutPassword.error = state.passwordError?.let { getString(it) }
            binding.layoutConfirm.error = state.confirmError?.let { getString(it) }
            binding.progressRegister.visibility = if (state.isLoading) View.VISIBLE else View.INVISIBLE
            binding.buttonRegister.isEnabled = !state.isLoading
        }
        collectWithLifecycle(viewModel.events) { event ->
            when (event) {
                // Tài khoản mới → tạo Hồ sơ sức khỏe ngay để AI cá nhân hóa kết quả.
                AuthEvent.Registered, AuthEvent.LoggedIn -> {
                    findNavController().navigate(R.id.action_register_to_home)
                    navigateToTab(R.id.profileFragment)
                }
                is AuthEvent.ShowError -> binding.root.showSnackbar(getString(event.message), long = true)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
