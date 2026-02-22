package com.dldev.docscanlite.core.cv

import org.junit.Test
import org.junit.Assert.*
import org.opencv.core.Point

class PointOrderingTest {

    @Test
    fun orderPoints_rectangleAlreadyOrdered_returnsSameOrder() {
        val points = listOf(
            Point(10.0, 10.0),  // top-left
            Point(100.0, 10.0), // top-right
            Point(100.0, 80.0), // bottom-right
            Point(10.0, 80.0)   // bottom-left
        )
        val ordered = DocumentDetector.orderPoints(points)

        assertEquals(4, ordered.size)
        // top-left should have the smallest x+y sum
        val topLeft = ordered[0]
        assertTrue(topLeft.x < ordered[1].x)
        assertTrue(topLeft.y < ordered[3].y)
    }

    @Test
    fun orderPoints_shuffledInput_producesCorrectTL() {
        val points = listOf(
            Point(200.0, 50.0), // top-right
            Point(50.0, 200.0), // bottom-left
            Point(50.0, 50.0),  // top-left
            Point(200.0, 200.0) // bottom-right
        )
        val ordered = DocumentDetector.orderPoints(points)

        // top-left should be (50, 50)
        val tl = ordered[0]
        assertTrue(tl.x <= 50.0 + 1e-6)
        assertTrue(tl.y <= 50.0 + 1e-6)
    }

    @Test
    fun orderPoints_asymmetricQuad_noException() {
        val points = listOf(
            Point(30.0, 15.0),
            Point(220.0, 40.0),
            Point(210.0, 190.0),
            Point(20.0, 175.0)
        )
        val ordered = DocumentDetector.orderPoints(points)
        assertEquals(4, ordered.size)
    }
}
