package com.gallery.app.ui.main

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gallery.app.ui.album.AlbumDetailScreen
import com.gallery.app.ui.album.AlbumListScreen
import com.gallery.app.ui.editor.EditorScreen
import com.gallery.app.ui.gallery.GalleryHomeScreen
import com.gallery.app.ui.permission.PermissionScreen
import com.gallery.app.ui.permission.PermissionViewModel
import com.gallery.app.ui.settings.SettingsScreen
import com.gallery.app.ui.trash.TrashScreen
import com.gallery.app.ui.vault.VaultScreen
import com.gallery.app.ui.viewer.ViewerScreen

object Routes {
    const val PERMISSION = "permission"
    const val GALLERY_HOME = "gallery_home"
    const val ALBUM_LIST = "album_list"
    const val ALBUM_DETAIL = "album_detail/{bucketId}/{bucketName}"
    const val VIEWER = "viewer/{initialIndex}"
    const val EDITOR = "editor/{photoUri}"
    const val TRASH = "trash"
    const val VAULT = "vault"
    const val SETTINGS = "settings"

    fun viewerRoute(initialIndex: Int): String = "viewer/$initialIndex"
    fun albumDetailRoute(bucketId: Long, bucketName: String): String =
        "album_detail/$bucketId/${Uri.encode(bucketName)}"
    fun editorRoute(photoUri: Uri): String = "editor/${Uri.encode(photoUri.toString())}"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.PERMISSION
    ) {
        composable(Routes.PERMISSION) {
            val permissionViewModel: PermissionViewModel = hiltViewModel()
            PermissionScreen(
                viewModel = permissionViewModel,
                onPermissionGranted = {
                    navController.navigate(Routes.GALLERY_HOME) {
                        popUpTo(Routes.PERMISSION) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.GALLERY_HOME) {
            GalleryHomeScreen(
                onPhotoClick = { index ->
                    navController.navigate(Routes.viewerRoute(index))
                },
                onAlbumClick = {
                    navController.navigate(Routes.ALBUM_LIST)
                },
                onTrashClick = {
                    navController.navigate(Routes.TRASH)
                },
                onVaultClick = {
                    navController.navigate(Routes.VAULT)
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.TRASH) {
            TrashScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.VAULT) {
            VaultScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.ALBUM_LIST) {
            AlbumListScreen(
                onAlbumClick = { album ->
                    navController.navigate(Routes.albumDetailRoute(album.id, album.name))
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.ALBUM_DETAIL,
            arguments = listOf(
                navArgument("bucketId") { type = NavType.LongType },
                navArgument("bucketName") { type = NavType.StringType }
            )
        ) {
            AlbumDetailScreen(
                onPhotoClick = { index ->
                    navController.navigate(Routes.viewerRoute(index))
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.VIEWER,
            arguments = listOf(
                navArgument("initialIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val initialIndex = backStackEntry.arguments?.getInt("initialIndex") ?: 0
            ViewerScreen(
                initialIndex = initialIndex,
                onBackClick = { navController.popBackStack() },
                onEditClick = { uri ->
                    navController.navigate(Routes.editorRoute(uri))
                }
            )
        }

        composable(
            route = Routes.EDITOR,
            arguments = listOf(
                navArgument("photoUri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("photoUri") ?: ""
            val uri = Uri.parse(Uri.decode(encodedUri))
            EditorScreen(
                photoUri = uri,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

