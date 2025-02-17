// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ui

import com.intellij.diagnostic.StartUpMeasurer
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.ImageDataByUrlLoader
import com.intellij.openapi.util.Pair
import com.intellij.ui.icons.IconLoadMeasurer
import com.intellij.ui.icons.IconTransform
import com.intellij.ui.icons.ImageDataLoader
import com.intellij.ui.icons.ImageDescriptor
import com.intellij.ui.scale.DerivedScaleType
import com.intellij.ui.scale.JBUIScale
import com.intellij.ui.scale.ScaleContext
import com.intellij.util.ImageLoader
import com.intellij.util.SVGLoader
import com.intellij.util.ui.StartupUiUtil
import org.intellij.lang.annotations.MagicConstant
import java.awt.Image
import java.awt.image.ImageFilter
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.URL

internal class RasterizedImageDataLoader(private val path: String,
                                         private val classLoaderRef: WeakReference<ClassLoader>,
                                         private val originalPath: String,
                                         private val originalClassLoaderRef: WeakReference<ClassLoader>,
                                         private val cacheKey: Int,
                                         private val imageFlags: Int) : ImageDataLoader {
  override fun loadImage(filters: List<ImageFilter>, scaleContext: ScaleContext, isDark: Boolean): Image? {
    val classLoader = classLoaderRef.get() ?: return null
    // use cache key only if path to image is not customized
    try {
      val start = StartUpMeasurer.getCurrentTimeIfEnabled()
      val isSvg: Boolean
      val image = if (originalPath === path) {
        // use cache key only if path to image is not customized
        isSvg = cacheKey != 0
        loadRasterized(path = path,
                       filters = filters,
                       classLoader = classLoader,
                       isDark = isDark,
                       scaleContext = scaleContext,
                       isSvg = isSvg,
                       rasterizedCacheKey = cacheKey,
                       imageFlags = imageFlags,
                       isPatched = false)
      }
      else {
        isSvg = path.endsWith(".svg")
        loadRasterized(path = path,
                       filters = filters,
                       classLoader = classLoader,
                       isDark = isDark,
                       scaleContext = scaleContext,
                       isSvg = isSvg,
                       rasterizedCacheKey = 0,
                       imageFlags = imageFlags,
                       isPatched = true)
      }

      if (start != -1L) {
        IconLoadMeasurer.loadFromResources.end(start)
        IconLoadMeasurer.addLoading(isSvg, start)
      }
      return image
    }
    catch (e: IOException) {
      logger<RasterizedImageDataLoader>().debug(e)
      return null
    }
  }

  override fun getURL() = classLoaderRef.get()?.getResource(path)

  override fun patch(originalPath: String, transform: IconTransform): ImageDataLoader? {
    val classLoader = classLoaderRef.get()
    val patched = transform.patchPath(originalPath, classLoader)
                  ?: return if (path !== this.originalPath && this.originalPath == normalizePath(originalPath)) {
                    RasterizedImageDataLoader(path = this.originalPath,
                                              classLoaderRef = originalClassLoaderRef,
                                              originalPath = this.originalPath,
                                              originalClassLoaderRef = originalClassLoaderRef,
                                              cacheKey = cacheKey,
                                              imageFlags = imageFlags)
                  }
                  else null
    if (patched.first.startsWith("file:/")) {
      val effectiveClassLoader = patched.second ?: classLoader
      return ImageDataByUrlLoader(URL(patched.first), patched.first, effectiveClassLoader, false)
    }
    else {
      return createPatched(this.originalPath, originalClassLoaderRef, patched, cacheKey, imageFlags)
    }
  }

  override fun isMyClassLoader(classLoader: ClassLoader) = classLoaderRef.get() === classLoader

  override fun toString() = "RasterizedImageDataLoader(classLoader=${classLoaderRef.get()}, path=$path)"
}

internal fun createPatched(originalPath: String,
                           originalClassLoaderRef: WeakReference<ClassLoader>,
                           patched: Pair<String, ClassLoader?>,
                           cacheKey: Int,
                           imageFlags: Int): ImageDataLoader {
  val effectivePath = normalizePath(patched.first)
  val effectiveClassLoaderRef = patched.second?.let(::WeakReference) ?: originalClassLoaderRef
  return RasterizedImageDataLoader(path = effectivePath,
                                   classLoaderRef = effectiveClassLoaderRef,
                                   originalPath = originalPath,
                                   originalClassLoaderRef = originalClassLoaderRef,
                                   cacheKey = cacheKey,
                                   imageFlags = imageFlags)
}

private fun normalizePath(patchedPath: String): String {
  return patchedPath.removePrefix("")
}

@Throws(IOException::class)
private fun loadRasterized(path: String,
                           filters: List<ImageFilter>,
                           classLoader: ClassLoader,
                           isDark: Boolean,
                           scaleContext: ScaleContext,
                           isSvg: Boolean,
                           rasterizedCacheKey: Int,
                           @MagicConstant(flagsFromClass = ImageDescriptor::class) imageFlags: Int,
                           isPatched: Boolean): Image? {
  // prefer retina images for HiDPI scale, because downscaling retina images provides a better result than up-scaling non-retina images
  val pixScale = scaleContext.getScale(DerivedScaleType.PIX_SCALE).toFloat()
  val dotIndex = path.lastIndexOf('.')
  val name = if (dotIndex < 0) path else path.substring(0, dotIndex)
  val scale = ImageLoader.adjustScaleFactor(true, pixScale)
  val isRetina = JBUIScale.isHiDPI(pixScale.toDouble())
  val imageScale: Float
  val ext = if (isSvg) "svg" else if (dotIndex < 0 || dotIndex == path.length - 1) "" else path.substring(dotIndex + 1)
  val effectivePath: String
  var isEffectiveDark = isDark
  if (isRetina && isDark && (imageFlags and ImageDescriptor.HAS_DARK_2x) == ImageDescriptor.HAS_DARK_2x) {
    effectivePath = "$name@2x_dark.$ext"
    imageScale = if (isSvg) scale else 2f
  }
  else if (isDark && imageFlags and ImageDescriptor.HAS_DARK == ImageDescriptor.HAS_DARK) {
    effectivePath = "${name}_dark.$ext"
    imageScale = if (isSvg) scale else 1f
  }
  else {
    isEffectiveDark = false
    if (isRetina && imageFlags and ImageDescriptor.HAS_2x == ImageDescriptor.HAS_2x) {
      effectivePath = "$name@2x.$ext"
      imageScale = if (isSvg) scale else 2f
    }
    else {
      effectivePath = path
      imageScale = if (isSvg) scale else 1f
    }
  }

  if (isPatched) {
    return loadPatched(name = name,
                       ext = ext,
                       isSvg = isSvg,
                       scale = scale,
                       path = path,
                       isRetina = isRetina,
                       isDark = isDark,
                       classLoader = classLoader,
                       isEffectiveDark = isEffectiveDark,
                       filters = filters,
                       scaleContext = scaleContext)
  }
  else {
    val image = if (isSvg) {
      SVGLoader.loadFromClassResource(resourceClass = null,
                                      classLoader = classLoader, path = effectivePath,
                                      rasterizedCacheKey = rasterizedCacheKey,
                                      scale = imageScale,
                                      isDark = isEffectiveDark)
    }
    else {
      ImageLoader.loadPngFromClassResource(/* path = */ effectivePath, /* resourceClass = */ null, /* classLoader = */
                                           classLoader, /* scale = */ imageScale, /* originalUserSize = */ null)
    }

    // do not use cache
    var flags = ImageLoader.ALLOW_FLOAT_SCALING
    if (isDark) {
      flags = flags or ImageLoader.USE_DARK
    }

    return ImageLoader.convertImage(image ?: return null, filters, flags, scaleContext, !isSvg, StartupUiUtil.isJreHiDPI(scaleContext),
                                    imageScale, isSvg)
  }
}

private class PatchedIconDescriptor(@JvmField val name: String, @JvmField val scale: Float)

private fun loadPatched(name: String,
                        ext: String,
                        isSvg: Boolean,
                        scale: Float,
                        path: String,
                        isRetina: Boolean,
                        isDark: Boolean,
                        classLoader: ClassLoader,
                        isEffectiveDark: Boolean,
                        filters: List<ImageFilter>,
                        scaleContext: ScaleContext): Image? {
  val retinaDark = PatchedIconDescriptor("$name@2x_dark.$ext", if (isSvg) scale else 2f)
  val dark = PatchedIconDescriptor("${name}_dark.$ext", if (isSvg) scale else 1f)
  val retina = PatchedIconDescriptor("$name@2x.$ext", if (isSvg) scale else 2f)
  val plain = PatchedIconDescriptor(path, if (isSvg) scale else 1f)
  val descriptors = if (isRetina && isDark) {
    listOf(retinaDark, dark, retina, plain)
  }
  else if (isDark) {
    listOf(dark, plain)
  }
  else {
    if (isRetina) listOf(retina, plain) else listOf(plain)
  }

  for (descriptor in descriptors) {
    val image = if (isSvg) {
      SVGLoader.loadFromClassResource(resourceClass = null,
                                      classLoader = classLoader,
                                      path = descriptor.name,
                                      rasterizedCacheKey = 0,
                                      scale = descriptor.scale,
                                      isDark = isEffectiveDark)
    }
    else {
      ImageLoader.loadPngFromClassResource(descriptor.name, null, classLoader, descriptor.scale, null)
    }

    if (image != null) {
      // do not use cache
      var flags = ImageLoader.ALLOW_FLOAT_SCALING
      if (isDark) {
        flags = flags or ImageLoader.USE_DARK
      }

      val isUpScaleNeeded = !isSvg && (descriptor === plain || descriptor === dark)
      return ImageLoader.convertImage(image, filters, flags, scaleContext, isUpScaleNeeded, StartupUiUtil.isJreHiDPI(scaleContext),
                                      descriptor.scale, isSvg)
    }
  }
  return null
}
