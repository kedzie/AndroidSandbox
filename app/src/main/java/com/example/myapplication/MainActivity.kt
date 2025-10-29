package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.myapplication.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint
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



sealed class Route {
    @Serializable
    object Home : Route()
    @Serializable
    object Favorites : Route()
    @Serializable
    class Profile(val id: String) : Route()
}


@PreviewScreenSizes
@Composable
fun MyApplicationApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    val navController = rememberNavController()

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { navController.navigate(it.route); currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            NavHost(navController = navController, startDestination = Route.Home) {
                composable<Route.Home> { HomeScreen(
                    goToProfile = {navController.navigate(Route.Profile(it))},
                    modifier = Modifier.padding(innerPadding)
                ) }
                composable<Route.Favorites> { FavoritesScreen(
                    modifier = Modifier.padding(innerPadding)
                ) }
                composable<Route.Profile> { backStackEntry ->
                    val profile: Route.Profile = backStackEntry.toRoute()
                    ProfileScreen(
                    modifier = Modifier.padding(innerPadding)
                ) }
            }

        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
    val route: Route,
) {
    HOME("Home", Icons.Default.Home, Route.Home),
    FAVORITES("Favorites", Icons.Default.Favorite, Route.Favorites),
}

@Composable
fun HomeScreen(viewModel: MainViewModel = hiltViewModel(),
               goToProfile : (String)->Unit = {},
               modifier: Modifier) {
    val pagedEntries = viewModel.pagedEntries.collectAsLazyPagingItems()
    var text by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Top
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("New Entry") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                viewModel.addEntry(text)
                text = ""
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Add")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(pagedEntries.itemCount) { index ->
                val entry = pagedEntries[index]
                if (entry != null) {
                    Text(
                        text = "• ${entry.text}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 4.dp).clickable {
                            goToProfile(entry.text)
                        },
                    )
                }
            }

            pagedEntries.apply {
                when {
                    loadState.refresh is androidx.paging.LoadState.Loading -> {
                        item { CircularProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        ) }
                    }
                    loadState.append is androidx.paging.LoadState.Loading -> {
                        item { CircularProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        ) }
                    }
                    loadState.refresh is androidx.paging.LoadState.Error -> {
                        val e = loadState.refresh as androidx.paging.LoadState.Error
                        item { Text(
                            text = "Error: ${'$'}{e.error.message}",
                            modifier = Modifier.fillMaxWidth()
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )  }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(modifier: Modifier) {
    Text(
        text = "Favorites",
        modifier = modifier
    )
}


@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel(),
                  modifier: Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Text(
        text = "Profile: $uiState",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun FavoritesScreenPreview() {
    MyApplicationTheme {
        FavoritesScreen(Modifier)
    }
}