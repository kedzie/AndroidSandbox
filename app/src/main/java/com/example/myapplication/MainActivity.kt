@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.myapplication.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MyApplicationApp()
            }
        }
    }
}



sealed class Route : NavKey {
    @Serializable
    object Home : Route()
    @Serializable
    object Favorites : Route()
    @Serializable
    object Camera : Route()
    @Serializable
    class Profile(val id: String) : Route()
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
    val route: Route,
) {
    HOME("Home", Icons.Default.Home, Route.Home),
    FAVORITES("Favorites", Icons.Default.Favorite, Route.Favorites),
    CAMERA("Camera", Icons.Default.AccountBox, Route.Camera),
}

val LocalAnimationDuration = compositionLocalOf { 1000 }

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }


@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3AdaptiveApi::class)
@PreviewScreenSizes
@Composable
fun MyApplicationApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    val backStack = rememberNavBackStack(Route.Home)

    val snackbarHostState = remember { SnackbarHostState() }

    val scope = rememberCoroutineScope()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label,
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = {
                        backStack.clear()
                        backStack.add(it.route)
                        currentDestination = it
                    }
                )
            }
        }
    ) {
        Box(modifier = Modifier
            .fillMaxSize()) {
            val systemInsets = WindowInsets.statusBars.asPaddingValues()
            Icon(
                imageVector = Icons.Filled.Star,
                tint = Color.Red,
                contentDescription = "Red Star",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TopAppBarDefaults.LargeAppBarExpandedHeight + systemInsets.calculateTopPadding())
                    .background(color = Color.Yellow)
                    .graphicsLayer {
                        // GOOD: Reading state INSIDE the Draw phase lambda
                        val currentOffset = scrollBehavior.state.heightOffset
                        val currentFraction = scrollBehavior.state.collapsedFraction

                        translationY = currentOffset * 0.5f
                        alpha = (1f - (currentFraction * 1.5f)).coerceAtLeast(0f)
                    }
            )

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.imePadding() // Automatically pushes up above the software keyboard
                ) { data ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                                data.dismiss()
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = { /* Optional: Render a background highlight when swiping */ },
                        content = {
                            // Your custom interactive UI component
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .background(MaterialTheme.colorScheme.inverseSurface, RoundedCornerShape(12.dp)),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = data.visuals.message,
                                    color = MaterialTheme.colorScheme.inverseOnSurface,
                                    modifier = Modifier.weight(1f).semantics {
                                        contentDescription = "${data.visuals.message} Snackbar"
                                    }
                                )
                                data.visuals.actionLabel?.let { label ->
                                    Button(onClick = { data.performAction() }) {
                                        Text(label)
                                    }
                                }
                            }
                        }
                    )
                }
            },
            topBar = {
                LargeTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text(currentDestination.label,
                            modifier = Modifier.semantics {
                                heading()
                                liveRegion = LiveRegionMode.Polite
                                traversalIndex = -1f
                            })
                    },
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = { if (backStack.isNotEmpty()) {
                            backStack.removeLastOrNull()
                        } }) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Navigate back",
                                modifier = Modifier.semantics {
                                    role = Role.Button
                                    onClick(label = "Navigate back") {
                                        if (backStack.isNotEmpty()) {
                                            backStack.removeLastOrNull()
                                        }
                                        true
                                    }
                                }
                            )
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .padding(4.dp)
                        ) {
                            Button(
                                onClick = { },
                                shape = CircleShape,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                    modifier = Modifier.semantics {
                                        onClick(label = "Do nothing") {
                                            true
                                        }
                                    }
                            ) {
                                Text(
                                    text = "a",
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.semantics {
                                        contentDescription = "Profile Settings"
                                    }
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.BottomEnd)
                                    .border(1.dp, Color.Red, CircleShape)
                                    .background(color = Color.Yellow, shape = CircleShape)
                            )
                        }
                    },
                )
            },
            floatingActionButtonPosition = FabPosition.End,
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    scope.launch  {
                        snackbarHostState.showSnackbar("Hello")
                    }
                }, shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics {
                            onClick(label = "Show snackbar") {
                                scope.launch  {
                                    snackbarHostState.showSnackbar("Hello")
                                }
                                true
                            }
                        }, ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Snackbar",
                        modifier = Modifier
                            .size(16.dp)
                            .border(1.dp, Color.Red, CircleShape)
                            .background(color = Color.Yellow, shape = CircleShape)
                    )
                }
            }
        ) { innerPadding ->

            SharedTransitionLayout(modifier = Modifier.padding(innerPadding)) {
                CompositionLocalProvider(
                    LocalAnimationDuration provides 1000,
                    LocalSharedTransitionScope provides this@SharedTransitionLayout
                ) {
                    val duration = LocalAnimationDuration.current
                    NavDisplay(
                        backStack = backStack,
                        onBack = {
                            if (backStack.isNotEmpty()) {
                                backStack.removeLastOrNull()
                            }
                        },
                        sceneStrategy = rememberListDetailSceneStrategy(),
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator()
                        ),
//                        transitionSpec = {
//                            slideIntoContainer(
//                                towards = AnimatedContentTransitionScope.SlideDirection.Left,
//                                animationSpec = tween(duration)
//                            ) togetherWith slideOutOfContainer(
//                                towards = AnimatedContentTransitionScope.SlideDirection.Left,
//                                animationSpec = tween(duration)
//                            )
//                        },
//                        popTransitionSpec = {
//                            slideIntoContainer(
//                                towards = AnimatedContentTransitionScope.SlideDirection.Right,
//                                animationSpec = tween(duration)
//                            ) togetherWith slideOutOfContainer(
//                                towards = AnimatedContentTransitionScope.SlideDirection.Right,
//                                animationSpec = tween(duration)
//                            )
//                        },
//                        predictivePopTransitionSpec = {
//                            slideIntoContainer(
//                                towards = AnimatedContentTransitionScope.SlideDirection.Right,
//                                animationSpec = tween(duration)
//                            ) togetherWith slideOutOfContainer(
//                                towards = AnimatedContentTransitionScope.SlideDirection.Right,
//                                animationSpec = tween(duration)
//                            )
//                        },
                        entryProvider = entryProvider {
                            entry<Route.Home>(metadata = ListDetailSceneStrategy.listPane {}) {
                                HomeScreen(
                                    goToProfile = {
                                        if (backStack.last() is Route.Profile)
                                            backStack.removeLastOrNull()
                                        backStack.add(Route.Profile(it))
                                    },
                                    goToCamera = {
                                        backStack.add(Route.Camera)
                                    },
                                    modifier = Modifier.background(color = MaterialTheme.colorScheme.background)
                                )
                            }
                            entry<Route.Camera> {
                                CameraScreen(modifier = Modifier)
                            }
                            entry<Route.Favorites>(metadata = NavDisplay.transitionSpec {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(duration)
                                ) togetherWith slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(duration)
                                )
                            }) {
                                OpenAIScreen(
                                    modifier = Modifier
                                )
                            }
                            entry<Route.Profile>(metadata = ListDetailSceneStrategy.detailPane()) {
                                ProfileScreen(
                                    viewModel = hiltViewModel(creationCallback = { factory: ProfileViewModel.ProfileViewModelFactory ->
                                        factory.create(id = it.id)
                                    }),
                                    modifier = Modifier
                                )
                            }
                        }
                    )
                }
            }
        }
    }
    }
}

    @Preview(showBackground = true)
    @Composable
    fun OpenAIScreenPreview() {
        MyApplicationTheme {
            OpenAIScreen(Modifier)
        }
    }


    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    fun OpenAIScreen(modifier: Modifier) {
        with(LocalSharedTransitionScope.current!!) {
        Column(modifier = modifier.fillMaxSize()) {
            var text by remember { mutableStateOf("") }
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Enter Text") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .sharedBounds(
                        rememberSharedContentState(key = "textfield"),
                        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                        enter = fadeIn(initialAlpha = 0f),
                        exit = fadeOut(),
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                    ),
                maxLines = 10
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 4.dp)
                    .imePadding()
            ) {
                Button(
                    onClick = {},
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("English")
                }
                IconButton(
                    onClick = {}
                ) {
                    Icon(
                        imageVector = Icons.Filled.ShoppingCart,
                        contentDescription = "Swap languages",
                    )
                }
                Button(
                    onClick = {},
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Spanish")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Button(
                        onClick = {},
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = "Conversation",
                        )
                    }
                }
                Button(
                    onClick = {},
                    shape = CircleShape,
                    modifier = Modifier.size(96.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ThumbUp,
                        contentDescription = "Text 2 Speech",
                    )
                }
                Column {
                    Button(
                        onClick = {},
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = "Camera",
                        )
                    }
                }
            }
        }
        }

    }
