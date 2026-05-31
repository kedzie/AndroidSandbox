package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.toPath
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import kotlin.math.roundToInt

@Composable
fun CameraScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (hasCameraPermission) {
        CameraPreview(modifier)
    } else {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission is required")
            TextButton(
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp)
            ) {
                Text("Grant Permission")
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CameraPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    var isCapturing by remember { mutableStateOf(false) }
    var isStreaming by remember { mutableStateOf(false) }

    // sharedBoundsWithMorphableShape on the outer Box — clips everything including
    // the placeholder, so the black startup frame is never visible through the morph.
    Box(
        modifier = modifier
            .fillMaxSize()
            .sharedBoundsWithMorphableShape(
                key = "camera",
                sharedTransitionScope = LocalSharedTransitionScope.current!!,
                animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                startShape = RoundedPolygon.rectangle(),
                endShape = RoundedPolygon.circle(numVertices = 8),
                enter = fadeIn(initialAlpha = 0f),
                exit = fadeOut(),
            )
    ) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    // StreamState tells us exactly when the first camera frame arrives
                    previewView.previewStreamState.observe(lifecycleOwner) { state ->
                        isStreaming = state == PreviewView.StreamState.STREAMING
                    }
                    val future = ProcessCameraProvider.getInstance(ctx)
                    future.addListener({
                        val cameraProvider = future.get()
                        val preview = Preview.Builder().build()
                            .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                        } catch (_: Exception) {}
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Covers the black PreviewView startup frames with the surface color.
        // AnimatedVisibility fades it out once the camera reports STREAMING state,
        // so the reveal transition sees a solid surface color — never raw black.
        androidx.compose.animation.AnimatedVisibility(
            visible = !isStreaming,
            exit = androidx.compose.animation.fadeOut(
                animationSpec = androidx.compose.animation.core.tween(300)
            )
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
        }

        ShutterButton(
            isCapturing = isCapturing,
            onClick = {
                if (!isCapturing) {
                    isCapturing = true
                    imageCapture.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                image.close()
                                isCapturing = false
                            }
                            override fun onError(e: ImageCaptureException) {
                                isCapturing = false
                            }
                        }
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        )
    }
}

/**
 * Shutter button that morphs from circle → rounded square when capturing, then back.
 *
 * The Morph is built once from two RoundedPolygon shapes. On each frame, toPath()
 * is called with the animated progress and drawn via nativeCanvas inside the
 * Canvas composable's draw lambda — keeping the state read in the Draw phase
 * and avoiding unnecessary recompositions of the parent.
 */
@Composable
fun ShutterButton(
    isCapturing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {


    // Build once: circle (8 vertices for smooth morphing) → rounded square
    val morph = remember {
        val circle = RoundedPolygon.circle(numVertices = 8)
        val square = RoundedPolygon(
            numVertices = 4,
            rounding = CornerRounding(radius = 0.4f)
        )
        Morph(start = circle, end = square)
    }

    val morphProgress by animateFloatAsState(
        targetValue = if (isCapturing) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "shutter_morph"
    )

    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val buttonSize = 80.dp
    val buttonPx = with(LocalDensity.current) { buttonSize.toPx() }
    modifier.onSizeChanged { containerSize = it }
    Box(
        modifier = modifier
            .size(80.dp)
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .clickable { onClick() }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offset = Offset(
                            x = (offset.x + dragAmount.x),
                            y = (offset.y + dragAmount.y)
                        )
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {


        // Outer ring — static, drawn once via simple drawCircle
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White,
                radius = size.minDimension / 2f - 4.dp.toPx(),
                style = Stroke(width = 4.dp.toPx())
            )
        }

        // Inner morphing shape — redraws on every morphProgress change
        Canvas(modifier = Modifier.size(60.dp)) {
                // Morph polygon is in -1..1 space; scale to fill our canvas bounds
                val src = RectF(-1f, -1f, 1f, 1f)
                val dst = RectF(0f, 0f, size.width, size.height)
                val matrix = Matrix().also { it.setRectToRect(src, dst, Matrix.ScaleToFit.FILL) }

                // morphProgress read here — inside Draw phase lambda
                val path = Path()
                morph.toPath(progress = morphProgress, path = path)
                path.transform(matrix)

                drawPath(path.asComposePath(), Color.White)
        }
    }
}
