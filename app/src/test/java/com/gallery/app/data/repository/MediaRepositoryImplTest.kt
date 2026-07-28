package com.gallery.app.data.repository

import android.content.Context
import com.gallery.app.domain.model.PhotoItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class MediaRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var repository: MediaRepositoryImpl

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        repository = MediaRepositoryImpl(context)
    }

    @Test
    fun getPhotos_returnsPagingDataFlow() = runTest {
        val flow = repository.getPhotos()
        assertNotNull(flow)
        val pagingData = flow.first()
        assertNotNull(pagingData)
    }
}
