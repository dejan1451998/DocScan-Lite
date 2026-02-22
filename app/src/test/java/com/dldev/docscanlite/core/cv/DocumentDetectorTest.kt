package com.dldev.docscanlite.core.cv

import org.junit.Test
import org.junit.Assert.*
import org.opencv.core.MatOfPoint
import org.opencv.core.Point

class DocumentDetectorTest {

    @Test
    fun selectLargestQuad_emptyContours_returnsNull() {
        val result = DocumentDetector.selectLargestQuad(emptyList())
        assertNull(result)
    }

    @Test
    fun selectLargestQuad_singleNonQuad_returnsNull() {
        // Triangle contour  approxPolyDP would yield 3 points, not 4
        val triangle = MatOfPoint(
            Point(0.0, 0.0),
            Point(100.0, 0.0),
            Point(50.0, 100.0)
        )
        val result = DocumentDetector.selectLargestQuad(listOf(triangle))
        // With only 3 points, approxPolyDP cannot produce a quad
        assertNull(result)
    }

    @Test
    fun orderPoints_returnsExactlyFourPoints() {
        val input = listOf(
            Point(0.0, 0.0),
            Point(100.0, 0.0),
            Point(100.0, 100.0),
            Point(0.0, 100.0)
        )
        val result = DocumentDetector.orderPoints(input)
        assertEquals(4, result.size)
    }

    @Test
    fun orderPoints_allPointsPreserved() {
        val input = listOf(
            Point(10.0, 20.0),
            Point(300.0, 15.0),
            Point(310.0, 400.0),
            Point(5.0, 395.0)
        )
        val result = DocumentDetector.orderPoints(input)
        val inputSorted = input.sortedWith(compareBy({ it.x }, { it.y }))
        val resultSorted = result.sortedWith(compareBy({ it.x }, { it.y }))

        inputSorted.zip(resultSorted).forEach { (a, b) ->
            assertEquals(a.x, b.x, 1e-6)
            assertEquals(a.y, b.y, 1e-6)
        }
    }
}
