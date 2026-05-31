package com.example.myapplication

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.rectangle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel = hiltViewModel(),
    goToProfile: (String) -> Unit = {},
    goToCamera: () -> Unit = {},
    modifier: Modifier
) {
    val pagedEntries = viewModel.pagedEntries.collectAsLazyPagingItems()
    var text by remember { mutableStateOf("") }
    with(LocalSharedTransitionScope.current!!) {
        Column(
            modifier = modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("New Entry") },
                modifier = Modifier
                    .fillMaxWidth()
                    .sharedBounds(
                        rememberSharedContentState(key = "textfield"),
                        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                        enter = fadeIn(initialAlpha = 0f),
                        exit = fadeOut(),
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                    ),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.addEntry(text)
                    text = ""
                },
                modifier = Modifier
                    .align(Alignment.End)
            ) {
                Text("Add")
            }
            Button(
                onClick = {
                    goToCamera()
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .sharedBoundsWithMorphableShape("camera",
                        sharedTransitionScope = LocalSharedTransitionScope.current!!,
                        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                        startShape = RoundedPolygon.circle(numVertices = 8),
                        enter = fadeIn(initialAlpha = 0f),
                        exit = fadeOut(),
                        endShape   = RoundedPolygon.rectangle())
            ) {
                Text("Camera")
            }


            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(pagedEntries.itemCount, key = { pagedEntries[it]?.text ?: "" }) { index ->
                    val entry = pagedEntries[index]
                    if (entry != null) {
                        Text(
                            text = entry.text,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .sharedBounds(
                                    rememberSharedContentState(key = entry.text),
                                    animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                                )
                                .clickable {
                                    goToProfile(entry.text)
                                },
                        )
                    }
                }

                pagedEntries.apply {
                    when {
                        loadState.refresh is LoadState.Loading -> {
                            item {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentWidth(Alignment.CenterHorizontally)
                                )
                            }
                        }

                        loadState.append is LoadState.Loading -> {
                            item {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentWidth(Alignment.CenterHorizontally)
                                )
                            }
                        }

                        loadState.refresh is LoadState.Error -> {
                            val e = loadState.refresh as LoadState.Error
                            item {
                                Text(
                                    text = "Error: ${'$'}{e.error.message}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentWidth(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    modifier: Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    with(LocalSharedTransitionScope.current!!) {
        Column(
            modifier = modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Profile: ",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = uiState,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.sharedBounds(
                        rememberSharedContentState(key = uiState),
                        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                    )
                )
            }
        }
    }
}