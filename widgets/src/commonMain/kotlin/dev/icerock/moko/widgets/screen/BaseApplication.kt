/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.icerock.moko.widgets.screen

import kotlin.reflect.KClass

abstract class BaseApplication {
    abstract fun setup(): ScreenDesc<Args.Empty>

    val rootScreen: ScreenDesc<Args.Empty> = setup()

    fun <Arg : Args, T : Screen<Arg>> registerScreen(
        kClass: KClass<T>,
        factory: ScreenFactory<Arg, T>.() -> T
    ): TypedScreenDesc<Arg, T> = TypedScreenDesc(kClass, factory)
}
