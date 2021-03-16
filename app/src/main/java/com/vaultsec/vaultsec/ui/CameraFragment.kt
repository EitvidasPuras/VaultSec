package com.vaultsec.vaultsec.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.camera.core.*
import androidx.camera.extensions.HdrImageCaptureExtender
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.databinding.FragmentCameraBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
@SuppressLint("UnsafeExperimentalUsageError")
class CameraFragment : Fragment(R.layout.fragment_camera) {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private var imageCaptureBuilder: ImageCapture.Builder? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var flashMode: Int = ImageCapture.FLASH_MODE_OFF
    private var hdrMode: HdrImageCaptureExtender? = null

    private val orientationEventList by lazy {
        object : OrientationEventListener(requireContext()) {
            override fun onOrientationChanged(orientation: Int) {
                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                if (imageCapture != null) {
                    imageCapture!!.targetRotation = rotation
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val provideCameraFuture = ProcessCameraProvider.getInstance(requireContext())

        provideCameraFuture.addListener({
            val cameraProvider: ProcessCameraProvider = provideCameraFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            imageCaptureBuilder = ImageCapture.Builder()
            imageCapture = imageCaptureBuilder!!.setFlashMode(flashMode).build()
//            imageCapture = ImageCapture.Builder()
//                .setFlashMode(flashMode).build()

            hdrMode = HdrImageCaptureExtender.create(imageCaptureBuilder!!)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            if (hdrMode!!.isExtensionAvailable(cameraSelector)) {
                hdrMode!!.enableExtension(cameraSelector)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

                val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector?): Boolean {
                        if (camera != null) {
                            val scale =
                                camera!!.cameraInfo.zoomState.value?.zoomRatio?.times(detector!!.scaleFactor)
                            if (scale != null) {
                                camera!!.cameraControl.setZoomRatio(scale)
                                return true
                            }
                        }
                        return false
                    }
                }
                val scaleGestureDetector = ScaleGestureDetector(requireContext(), listener)

                binding.viewFinder.setOnTouchListener { _, motionEvent ->
                    scaleGestureDetector.onTouchEvent(motionEvent)

                    if (motionEvent.action == MotionEvent.ACTION_UP) {
                        val factory = SurfaceOrientedMeteringPointFactory(
                            binding.viewFinder.width.toFloat(), binding.viewFinder.height.toFloat()
                        )
                        val autoFocusPoint = factory.createPoint(motionEvent.x, motionEvent.y)
                        try {
                            camera!!.cameraControl.startFocusAndMetering(
                                FocusMeteringAction.Builder(
                                    autoFocusPoint, FocusMeteringAction.FLAG_AF
                                ).build()
                            )
                            return@setOnTouchListener true
                        } catch (e: Exception) {
                            return@setOnTouchListener false
                        }
                    }
                    return@setOnTouchListener true
                }
            } catch (e: Exception) {
                Log.e("Binding failed", e.message!!)
            }
        }, ContextCompat.getMainExecutor(requireContext()))

        binding.imagebuttonCapture.setOnClickListener {
            val scaleX = binding.imagebuttonCapture.scaleX
            val scalyY = binding.imagebuttonCapture.scaleY
            /*
            * The most amazing way to animate a button click
            * */
            lifecycleScope.launchWhenStarted {
                binding.imagebuttonCapture.animate().scaleX(binding.imagebuttonCapture.scaleX + 4F)
                    .scaleY(binding.imagebuttonCapture.scaleY + 4F).setListener(null).start()
                delay(40)
                binding.imagebuttonCapture.animate().scaleX(scaleX)
                    .scaleY(scalyY).setListener(null).start()
            }

            takeAPhoto()
        }

        binding.imagebuttonFlash.setOnClickListener {
            if (camera != null) {
                if (camera!!.cameraInfo.hasFlashUnit()) {
                    when (flashMode) {
                        ImageCapture.FLASH_MODE_OFF -> {
                            flashMode = ImageCapture.FLASH_MODE_ON
                            camera!!.cameraControl.enableTorch(true)
                            lifecycleScope.launchWhenStarted {
                                delay(150)
                                binding.imagebuttonFlash.setImageResource(R.drawable.ic_flash_off)
                            }
                        }
                        ImageCapture.FLASH_MODE_ON -> {
                            flashMode = ImageCapture.FLASH_MODE_OFF
                            camera!!.cameraControl.enableTorch(false)
                            lifecycleScope.launchWhenStarted {
                                delay(150)
                                binding.imagebuttonFlash.setImageResource(R.drawable.ic_flash_on)
                            }
                        }
                    }
                } else {
                    Snackbar.make(
                        binding.root,
                        "This phone does not have flashlight",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun takeAPhoto() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(ContextCompat.getMainExecutor(requireContext()), object :
            ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                if (image.image != null) {
                    analyzeText(image)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("com.vaultsec.vaultsec.ui.CameraActivity", "Failed to capture an image")
            }
        })
    }

    private fun analyzeText(image: ImageProxy) {
        val cleanImage = InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees)
        recognizeTextOnDevice(cleanImage).addOnCompleteListener {
            image.close()
            return@addOnCompleteListener
        }
        return
    }

    private fun recognizeTextOnDevice(image: InputImage): Task<Text> {
        val detector = TextRecognition.getClient()
        val languageIdentifier = LanguageIdentification.getClient(
//            LanguageIdentificationOptions.Builder()
//                .setConfidenceThreshold(0.4f).build()
        )
        return detector.process(image)
            .addOnSuccessListener {
                if (it.text.isNotEmpty()) {
                    /*
                    * PROBABLY according to MVVM the text should be sent over the viewmodel and so
                    * it could immediately be read in the AddEditNoteFragment
                    * */
                    setFragmentResult(
                        "com.vaultsec.vaultsec.ui.CameraFragment.recognizedText",
                        bundleOf(
                            "Text" to it.text
                        )
                    )
                }
                requireActivity().supportFragmentManager.setFragmentResult(
                    "com.vaultsec.vaultsec.ui.CameraFragment.closeCamera",
                    bundleOf(
                        "CloseCamera" to true
                    )
                )
                findNavController().popBackStack()
                languageIdentifier.identifyPossibleLanguages(it.text)
                    .addOnSuccessListener { identifiedLanguages ->
                        for (language in identifiedLanguages) {
                            val lang = language.languageTag
                            val confidence = language.confidence
                            Log.e(lang, "$confidence")
                        }
                    }
            }
            .addOnFailureListener {
                Log.e("Failed", it.message!!)
            }
    }

    override fun onStart() {
        super.onStart()
        orientationEventList.enable()
    }

    override fun onStop() {
        super.onStop()
        orientationEventList.disable()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}