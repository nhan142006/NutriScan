package vn.edu.vlu.milestone2.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
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
import vn.edu.vlu.milestone2.databinding.FragmentBarcodeScannerBinding
import vn.edu.vlu.milestone2.ui.NavArgs
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BarcodeScannerFragment : Fragment() {

    private var _binding: FragmentBarcodeScannerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BarcodeScannerViewModel by viewModels()

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var analyzer: BarcodeAnalyzer? = null
    private lateinit var analysisExecutor: ExecutorService

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else showPermissionDenied()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBarcodeScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        analysisExecutor = Executors.newSingleThreadExecutor()
        applySystemBarInsets()

        binding.buttonClose.setOnClickListener { findNavController().navigateUp() }
        binding.buttonFallback.setOnClickListener { findNavController().navigate(R.id.labelCameraFragment) }
        binding.buttonTorch.addOnCheckedChangeListener { _, isChecked ->
            camera?.cameraControl?.enableTorch(isChecked)
        }

        collectWithLifecycle(viewModel.detected) { barcode ->
            findNavController().navigate(R.id.action_barcode_to_product_detail, bundleOf(NavArgs.BARCODE to barcode))
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
                val barcodeAnalyzer = BarcodeAnalyzer(viewModel::onBarcodeDetected).also { analyzer = it }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(analysisExecutor, barcodeAnalyzer) }
                provider.unbindAll()
                camera = provider.bindToLifecycle(viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                currentBinding.buttonTorch.visibility = if (camera?.cameraInfo?.hasFlashUnit() == true) View.VISIBLE else View.GONE
            } catch (error: Exception) {
                currentBinding.root.showSnackbar(getString(R.string.camera_unavailable))
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun showPermissionDenied() {
        binding.textPermission.visibility = View.VISIBLE
        binding.viewViewfinder.visibility = View.GONE
        binding.textHint.visibility = View.GONE
    }

    /** Màn hình toàn màn hình: đẩy nút Đóng/Flash xuống dưới status bar, nút dưới lên trên nav bar. */
    private fun applySystemBarInsets() {
        val baseMargin = resources.getDimensionPixelSize(R.dimen.spacing_medium)
        val baseBottom = resources.getDimensionPixelSize(R.dimen.spacing_xlarge)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.buttonClose.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = baseMargin + bars.top }
            binding.buttonTorch.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = baseMargin + bars.top }
            binding.buttonFallback.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = baseBottom + bars.bottom }
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    override fun onDestroyView() {
        // Giải phóng tài nguyên CameraX + ML Kit (Proposal 6.1).
        cameraProvider?.unbindAll()
        analyzer?.close()
        analysisExecutor.shutdown()
        camera = null
        cameraProvider = null
        analyzer = null
        super.onDestroyView()
        _binding = null
    }
}
