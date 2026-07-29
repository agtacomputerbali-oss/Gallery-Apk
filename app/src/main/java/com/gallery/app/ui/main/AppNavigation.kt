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
    const val ALBUM_DETAIL = "album_detail/{bucketId}/{bucketName}?smartType={smartType}"
    const val VIEWER = "viewer/{initialPhotoId}?bucketId={bucketId}"
    const val EDITOR = "editor/{photoUri}"
    const val TRASH = "trash"
    const val VAULT = "vault"
    const val SETTINGS = "settings"
    const val DUPLICATE_FINDER = "duplicate_finder"
    const val VIDEO_PLAYER = "video_player/{videoUri}"

    fun viewerRoute(photoId: Long, bucketId: Long? = null): String {
        return if (bucketId != null && bucketId != -1L) {
            "viewer/$photoId?bucketId=$bucketId"
        } else {
            "viewer/$photoId?bucketId=-1"
        }
    }
    fun albumDetailRoute(bucketId: Long, bucketName: String): String =
        "album_detail/$bucketId/${Uri.encode(bucketName)}"
    fun smartAlbumDetailRoute(smartType: String, displayName: String): String =
        "album_detail/-1/${Uri.encode(displayName)}?smartType=$smartType"
    fun videoPlayerRoute(videoUri: Uri): String = "video_player/${Uri.encode(videoUri.toString())}"
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
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.GALLERY_HOME) {
            GalleryHomeScreen(
                onPhotoClick = { photo ->
                    if (photo.mimeType.startsWith("video/")) {
                        navController.navigate(Routes.videoPlayerRoute(photo.uri))
                    } else {
                        navController.navigate(Routes.viewerRoute(photo.id))
                    }
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
                onBackClick = { navController.popBackStack() },
                onDuplicateClick = { navController.navigate(Routes.DUPLICATE_FINDER) }
            )
        }

        composable(Routes.DUPLICATE_FINDER) {
            com.gallery.app.ui.duplicate.DuplicateScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.TRASH) {
            TrashScreen(
                onBackClick = { navController.popBackStack() },
                onGalleryClick = {
                    navController.navigate(Routes.GALLERY_HOME) {
                        popUpTo(Routes.GALLERY_HOME) { inclusive = true }
                    }
                },
                onAlbumClick = { navController.navigate(Routes.ALBUM_LIST) },
                onVaultClick = { navController.navigate(Routes.VAULT) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.VAULT) {
            VaultScreen(
                onBackClick = { navController.popBackStack() },
                onGalleryClick = {
                    navController.navigate(Routes.GALLERY_HOME) {
                        popUpTo(Routes.GALLERY_HOME) { inclusive = true }
                    }
                },
                onAlbumClick = { navController.navigate(Routes.ALBUM_LIST) },
                onTrashClick = { navController.navigate(Routes.TRASH) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.ALBUM_LIST) {
            AlbumListScreen(
                onAlbumClick = { album ->
                    navController.navigate(Routes.albumDetailRoute(album.id, album.name))
                },
                onSmartAlbumClick = { smartAlbum ->
                    navController.navigate(Routes.smartAlbumDetailRoute(smartAlbum.type.name, smartAlbum.displayName))
                },
                onBackClick = { navController.popBackStack() },
                onGalleryClick = {
                    navController.navigate(Routes.GALLERY_HOME) {
                        popUpTo(Routes.GALLERY_HOME) { inclusive = true }
                    }
                },
                onTrashClick = { navController.navigate(Routes.TRASH) },
                onVaultClick = { navController.navigate(Routes.VAULT) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.ALBUM_DETAIL,
            arguments = listOf(
                navArgument("bucketId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("bucketName") { type = NavType.StringType; defaultValue = "Album" },
                navArgument("smartType") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val bucketId = backStackEntry.arguments?.getLong("bucketId") ?: -1L
            AlbumDetailScreen(
                onPhotoClick = { photo ->
                    if (photo.mimeType.startsWith("video/")) {
                        navController.navigate(Routes.videoPlayerRoute(photo.uri))
                    } else {
                        navController.navigate(Routes.viewerRoute(photo.id, bucketId))
                    }
                },
                onBackClick = { navController.popBackStack() },
                onGalleryClick = {
                    navController.navigate(Routes.GALLERY_HOME) {
                        popUpTo(Routes.GALLERY_HOME) { inclusive = true }
                    }
                },
                onAlbumClick = { navController.navigate(Routes.ALBUM_LIST) },
                onTrashClick = { navController.navigate(Routes.TRASH) },
                onVaultClick = { navController.navigate(Routes.VAULT) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.VIEWER,
            arguments = listOf(
                navArgument("initialPhotoId") { type = NavType.LongType },
                navArgument("bucketId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val initialPhotoId = backStackEntry.arguments?.getLong("initialPhotoId") ?: 0L
            ViewerScreen(
                initialPhotoId = initialPhotoId,
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
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { newUri ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("savedPhotoUri", newUri.toString())
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.VIDEO_PLAYER,
            arguments = listOf(
                navArgument("videoUri") { type = NavType.StringType }
            )
        ) {
            com.gallery.app.ui.video.VideoPlayerScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

