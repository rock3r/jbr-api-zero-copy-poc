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
    int ABI_ID = Integer.parseInt("3");

    /**
     * Exact runtime build identity. This field intentionally uses a non-constant initializer so
     * compile-only clients cannot accidentally inline stale values.
     */
    String BUILD_ID = "skia-interop-poc:" + Integer.parseInt("3");

    /**
     * Command-stream magic value ({@code JSK3}) that identifies framed command payloads.
     */
    int COMMAND_STREAM_MAGIC = Integer.parseInt("1246972723");

    /**
     * Number of integers in the command-stream header.
     */
    int COMMAND_STREAM_HEADER_SIZE = Integer.parseInt("4");

    /**
     * Command-stream flags value for the current unextended payload format.
     */
    int COMMAND_STREAM_FLAGS_NONE = Integer.parseInt("0");

    /**
     * Command-list operation: clear/fill the destination with one ARGB color.
     */
    int COMMAND_CLEAR = Integer.parseInt("1");

    /**
     * Command-list operation: fill a rectangle with one ARGB color.
     */
    int COMMAND_FILL_RECT = Integer.parseInt("2");

    /**
     * Command-list operation: stroke a line with one ARGB color.
     */
    int COMMAND_STROKE_LINE = Integer.parseInt("3");

    /**
     * Command-list operation: fill an oval with one ARGB color.
     */
    int COMMAND_FILL_OVAL = Integer.parseInt("4");

    /**
     * Command-list operation: stroke an oval with one ARGB color.
     */
    int COMMAND_STROKE_OVAL = Integer.parseInt("5");

    /**
     * Command-list operation: clear a rectangle using destination clear blending.
     */
    int COMMAND_CLEAR_RECT = Integer.parseInt("6");

    /**
     * Command-list operation: save canvas state.
     */
    int COMMAND_SAVE = Integer.parseInt("7");

    /**
     * Command-list operation: restore canvas state.
     */
    int COMMAND_RESTORE = Integer.parseInt("8");

    /**
     * Command-list operation: intersect the current clip with a rectangle.
     */
    int COMMAND_CLIP_RECT = Integer.parseInt("9");

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
         * Renders a JBR-owned diagnostic frame into this scope.
         *
         * <p>This PoC-only method proves that Skiko/CMP can route painting through a JBR-owned scope
         * without wrapping the destination texture with Skiko's own Metal context. The production fast
         * path replaces this Java-level diagnostic with the versioned native Skia C ABI.</p>
         *
         * @param width user-space width of the component being painted.
         * @param height user-space height of the component being painted.
         * @param frameTimeNanos frame timestamp supplied by the caller.
         * @return {@code true} when the diagnostic frame was painted.
         */
        boolean renderDiagnosticFrame(int width, int height, long frameTimeNanos);

        /**
         * Renders a minimal JBR-owned Skia command stream into this scope.
         *
         * <p>The command stream is a PoC ABI used to prove that Skiko can supply drawing operations
         * while JBR owns the Skia context, Metal queue, and destination texture. The integer encoding
         * is intentionally temporary and will be replaced by the versioned native C ABI.</p>
         *
         * <p>The stream starts with a four-integer header:
         * {@code [COMMAND_STREAM_MAGIC, ABI_ID, COMMAND_STREAM_FLAGS_NONE, payloadLength]}.
         * {@code payloadLength} is the number of integers after the header.</p>
         *
         * <ul>
         *     <li>{@link JBRSkia#COMMAND_CLEAR}: {@code [op, argb]}</li>
         *     <li>{@link JBRSkia#COMMAND_FILL_RECT}: {@code [op, argb, x, y, width, height, radius]}</li>
         *     <li>{@link JBRSkia#COMMAND_STROKE_LINE}: {@code [op, argb, x1, y1, x2, y2, strokeWidth]}</li>
         *     <li>{@link JBRSkia#COMMAND_FILL_OVAL}: {@code [op, argb, x, y, width, height]}</li>
         *     <li>{@link JBRSkia#COMMAND_STROKE_OVAL}: {@code [op, argb, x, y, width, height, strokeWidth]}</li>
         *     <li>{@link JBRSkia#COMMAND_CLEAR_RECT}: {@code [op, x, y, width, height]}</li>
         *     <li>{@link JBRSkia#COMMAND_SAVE}: {@code [op]}</li>
         *     <li>{@link JBRSkia#COMMAND_RESTORE}: {@code [op]}</li>
         *     <li>{@link JBRSkia#COMMAND_CLIP_RECT}: {@code [op, x, y, width, height]}</li>
         * </ul>
         *
         * @param width user-space width of the component being painted.
         * @param height user-space height of the component being painted.
         * @param frameTimeNanos frame timestamp supplied by the caller.
         * @param commands framed integer command stream.
         * @return {@code true} when the command frame was painted.
         */
        boolean renderCommandFrame(int width, int height, long frameTimeNanos, int[] commands);

        /**
         * Replays a serialized Skia picture into this scope.
         *
         * <p>This PoC method lets Skiko record real rendering into an {@code SkPicture}, serialize it
         * as data, and ask JBR-owned Skia to deserialize/replay it on JBR's Metal queue. The byte
         * payload must only be used when {@link JBRSkia#BUILD_ID} confirms both sides agree on the
         * Skia revision and ABI contract.</p>
         *
         * @param width user-space width of the component being painted.
         * @param height user-space height of the component being painted.
         * @param frameTimeNanos frame timestamp supplied by the caller.
         * @param pictureData serialized Skia picture bytes.
         * @return {@code true} when the picture frame was painted.
         */
        boolean renderPictureFrame(int width, int height, long frameTimeNanos, byte[] pictureData);

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
