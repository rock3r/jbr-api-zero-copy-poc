/*
 * Copyright 2000-2026 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jetbrains;

import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Experimental service for rendering into a JBR-owned Skia surface during Java2D painting.
 *
 * <p>This API is intended for tightly versioned interop between JBR and Skiko. Clients must read
 * {@code ABI_ID} and {@code BUILD_ID} reflectively before acquiring the service, and must fall back to
 * their old rendering path when either value is incompatible.</p>
 */
@Service
@Provided
public interface JBRSkia {
    /**
     * Java-level shape of the interop ABI. This field intentionally uses a non-constant initializer so
     * compile-only clients cannot accidentally inline stale values.
     */
    int ABI_ID = Integer.parseInt("1");

    /**
     * Exact runtime build identity. This field intentionally uses a non-constant initializer so
     * compile-only clients cannot accidentally inline stale values.
     */
    String BUILD_ID = "skia-interop-poc:" + Integer.parseInt("1");

    /**
     * Attempts to acquire a Skia paint scope for the supplied Java2D graphics.
     *
     * <p>The returned scope is valid only for the current paint call and must be closed before returning
     * from paint. A {@code null} result means the caller must use its fallback rendering path.</p>
     *
     * @param graphics the current Java2D graphics.
     * @return a scoped Skia canvas, or {@code null} when the current paint target is unsupported.
     */
    ScopedSkiaCanvas acquireCanvas(Graphics2D graphics);

    /**
     * A paint-scoped handle to JBR-owned Skia/Metal state.
     */
    @Provided
    interface ScopedSkiaCanvas extends AutoCloseable {
        /**
         * Metal backend identifier.
         */
        int BACKEND_METAL = Integer.parseInt("1");

        /**
         * Returns an opaque scope id. Clients may compare ids between paints to invalidate cached state.
         *
         * @return opaque scope id.
         */
        long getScopeId();

        /**
         * Returns the rendering backend.
         *
         * @return backend id, such as {@link #BACKEND_METAL}.
         */
        int getBackend();

        /**
         * Returns an opaque {@code SkCanvas*} pointer, or {@code 0} until native Skia wrapping is active.
         *
         * @return native Skia canvas pointer.
         */
        long getCanvasPtr();

        /**
         * Returns an opaque Skia direct context pointer, or {@code 0} until native Skia wrapping is active.
         *
         * @return native Skia direct context pointer.
         */
        long getDirectContextPtr();

        /**
         * Returns the destination {@code MTLTexture*} pointer for diagnostics and handshake validation.
         *
         * @return native Metal texture pointer, or {@code 0} when unavailable.
         */
        long getMetalTexturePtr();

        /**
         * Returns the native pixel format id.
         *
         * @return platform pixel format id.
         */
        int getPixelFormat();

        /**
         * Returns the color space id.
         *
         * @return platform color space id.
         */
        int getColorSpaceId();

        /**
         * Returns the destination sample count.
         *
         * @return sample count.
         */
        int getSampleCount();

        /**
         * Returns the user-space clip snapshot for this paint scope.
         *
         * @return user-space clip, or {@code null} when no clip is set.
         */
        Rectangle getUserSpaceClip();

        /**
         * Flushes Skia work for this scope. Calling after {@link #close()} is invalid.
         */
        void flush();

        /**
         * Closes this scope. Clients must not use any pointer returned by this scope after closing it.
         */
        @Override
        void close();
    }
}
