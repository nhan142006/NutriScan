package vn.edu.vlu.milestone2.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.core.ui.collectWithLifecycle
import vn.edu.vlu.milestone2.core.ui.showSnackbar
import vn.edu.vlu.milestone2.databinding.FragmentLabelCameraBinding
import vn.edu.vlu.milestone2.ui.NavArgs

/** CameraFragment (Module 3.2): chụp nhãn → nén JPEG < 1 MB → ScanResult PENDING → WorkManager. */
class LabelCameraFragment : Fragment() {

    private var _binding: FragmentLabelCameraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LabelCameraViewModel by viewModels { LabelCameraViewModel.Factory }

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else showPermissionDenied()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLabelCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applySystemBarInsets()
        binding.buttonClose.setOnClickListener { findNavController().navigateUp() }
        binding.buttonShutter.setOnClickListener { takePhoto() }

        collectWithLifecycle(viewModel.isSaving) { saving ->
            binding.progressCapture.visibility = if (saving) View.VISIBLE else View.GONE
            binding.buttonShutter.isEnabled = !saving
            binding.textStatus.setText(if (saving) R.string.camera_status_saving else R.string.camera_status_ready)
        }
        collectWithLifecycle(viewModel.events) { event ->
            when (event) {
                is CaptureEvent.Saved -> findNavController().navigate(
                    R.id.action_camera_to_analysis,
                    bundleOf(NavArgs.SCAN_ID to event.scanId)
                )
                CaptureEvent.Failed -> binding.root.showSnackbar(getString(R.string.camera_error))
            }
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(requireContext())
        providerFuture.addListener({
            val currentBinding = _binding ?: return@addListener
            try {
                val provider = providerFuture.get()
                cameraProvider = provider
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(currentBinding.previewView.surfaceProvider)
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                    .also { imageCapture = it }
                provider.unbindAll()
                provider.bindToLifecycle(viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
            } catch (error: Exception) {
                currentBinding.root.showSnackbar(getString(R.string.camera_unavailable))
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return
        // Bước 1 (Proposal 4.4): ghi ảnh gốc vào file tạm trong cacheDir.
        val file = viewModel.newCaptureFile()
        viewModel.onCaptureStarted()
        capture.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    viewModel.onPhotoCaptured(file)
                }

                override fun onError(exception: ImageCaptureException) {
                    viewModel.onCaptureFailed(file)
                }
            }
        )
    }

    private fun showPermissionDenied() {
        binding.textPermission.visibility = View.VISIBLE
        binding.buttonShutter.isEnabled = false
    }

    private fun applySystemBarInsets() {
        val baseTop = resources.getDimensionPixelSize(R.dimen.spacing_medium)
        val baseBottom = resources.getDimensionPixelSize(R.dimen.spacing_xlarge)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.buttonClose.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = baseTop + bars.top }
            binding.buttonShutter.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = baseBottom + bars.bottom }
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    override fun onDestroyView() {
        cameraProvider?.unbindAll() // giải phóng camera khi rời màn hình
        cameraProvider = null
        imageCapture = null
        super.onDestroyView()
        _binding = null
    }
}
