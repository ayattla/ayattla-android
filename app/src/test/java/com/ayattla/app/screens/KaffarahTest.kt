package com.ayattla.app.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class KaffarahTest {
    @Test fun meals() {
        assertEquals(10, Kaffarah.meals(Kaffarah.Kind.Oath, 1))
        assertEquals(30, Kaffarah.meals(Kaffarah.Kind.Oath, 3))
        assertEquals(29, Kaffarah.meals(Kaffarah.Kind.Fidya, 29))
        assertEquals(60, Kaffarah.meals(Kaffarah.Kind.Ramadan, 1))
    }
}
