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
import java.nio.ByteBuffer;

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
    int ABI_ID = Integer.parseInt("41");

    /**
     * Java-visible version of the native interop metadata block. Bump when the native service
     * identity or lifecycle contract changes independently of the command stream ABI.
     */
    int NATIVE_ABI_VERSION = Integer.parseInt("3");

    /**
     * Pinned Skia revision used by this PoC bridge.
     */
    String SKIA_REVISION = "m147-" + "64a2414108";

    /**
     * Fingerprint of the Skia build configuration expected by this PoC bridge.
     */
    String SKIA_FLAGS_HASH = "macos-release-metal-poc:" + Integer.parseInt("1");

    /**
     * Exact runtime build identity. This field intentionally uses a non-constant initializer so
     * compile-only clients cannot accidentally inline stale values.
     */
    String BUILD_ID = "skia=" + SKIA_REVISION +
            ";flags=" + SKIA_FLAGS_HASH +
            ";abi=" + ABI_ID +
            ";native=" + NATIVE_ABI_VERSION;

    /**
     * Command-stream magic value ({@code JSK3}) that identifies framed command payloads.
     */
    int COMMAND_STREAM_MAGIC = Integer.parseInt("1246972723");

    /**
     * Number of integers in the command-stream header.
     */
    int COMMAND_STREAM_HEADER_SIZE = Integer.parseInt("6");

    /**
     * Command-stream flags value for the current unextended payload format.
     */
    int COMMAND_STREAM_FLAGS_NONE = Integer.parseInt("0");

    /**
     * Command coordinates are Swing user-space pixels.
     */
    int COMMAND_COORDINATE_SPACE_SWING_USER = Integer.parseInt("1");

    /**
     * Paint payloads are solid non-premultiplied ARGB integers.
     */
    int COMMAND_PAINT_FORMAT_SOLID_ARGB = Integer.parseInt("1");

    /**
     * Number of bytes in every command-record header:
     * {@code [op, recordByteLength, recordFlags]}.
     */
    int COMMAND_RECORD_HEADER_SIZE_BYTES = Integer.parseInt("12");

    /**
     * Command-record flags value for records without optional paint flags.
     */
    int COMMAND_RECORD_FLAGS_NONE = Integer.parseInt("0");

    /**
     * Command-record flag: draw this record with antialiasing enabled where the operation supports it.
     */
    int COMMAND_RECORD_FLAG_ANTIALIAS = Integer.parseInt("1");

    /**
     * Capability bit: command streams may clear/fill the current destination.
     */
    int COMMAND_CAP_CLEAR = Integer.parseInt("1");

    /**
     * Capability bit: command streams may fill rectangles.
     */
    int COMMAND_CAP_FILL_RECT = Integer.parseInt("2");

    /**
     * Capability bit: command streams may stroke lines.
     */
    int COMMAND_CAP_STROKE_LINE = Integer.parseInt("4");

    /**
     * Capability bit: command streams may fill ovals.
     */
    int COMMAND_CAP_FILL_OVAL = Integer.parseInt("8");

    /**
     * Capability bit: command streams may stroke ovals.
     */
    int COMMAND_CAP_STROKE_OVAL = Integer.parseInt("16");

    /**
     * Capability bit: command streams may clear rectangles.
     */
    int COMMAND_CAP_CLEAR_RECT = Integer.parseInt("32");

    /**
     * Capability bit: command streams may save and restore drawing state.
     */
    int COMMAND_CAP_SAVE_RESTORE = Integer.parseInt("64");

    /**
     * Capability bit: command streams may intersect the current clip with a rectangle.
     */
    int COMMAND_CAP_CLIP_RECT = Integer.parseInt("128");

    /**
     * Capability bit: command coordinates are expressed in Swing user-space pixels.
     */
    int COMMAND_CAP_USER_SPACE_COORDINATES = Integer.parseInt("256");

    /**
     * Capability bit: command records may carry per-record antialiasing flags.
     */
    int COMMAND_CAP_RECORD_ANTIALIAS = Integer.parseInt("512");

    /**
     * Capability bit: stroked command records carry cap, join, and fixed-point miter metadata.
     */
    int COMMAND_CAP_STROKE_METADATA = Integer.parseInt("1024");

    /**
     * Capability bit: command streams can carry translate, scale, and rotate records.
     */
    int COMMAND_CAP_BASIC_TRANSFORMS = Integer.parseInt("2048");

    /**
     * Capability bit: clip-rectangle records carry an explicit intersect/difference operation.
     */
    int COMMAND_CAP_CLIP_RECT_OP = Integer.parseInt("4096");

    /**
     * Capability bit: command streams can save bounded layers with alpha.
     */
    int COMMAND_CAP_SAVE_LAYER = Integer.parseInt("8192");

    /**
     * Capability bit: command streams can draw inline ARGB raster images.
     */
    int COMMAND_CAP_DRAW_IMAGE_ARGB = Integer.parseInt("16384");

    /**
     * Capability bit: command streams can define ARGB images once and draw later frames by cache key.
     */
    int COMMAND_CAP_IMAGE_CACHE = Integer.parseInt("32768");

    /**
     * Capability bit: command streams can draw simple UTF-16 text runs with a JBR-owned default font.
     */
    int COMMAND_CAP_DRAW_TEXT_UTF16 = Integer.parseInt("65536");

    /**
     * Capability bit: command streams can explicitly clear the JBR-side ARGB image cache.
     */
    int COMMAND_CAP_CLEAR_IMAGE_CACHE = Integer.parseInt("131072");

    /**
     * Capability bit: command streams can draw JBR-owned shaped UTF-16 paragraphs.
     */
    int COMMAND_CAP_DRAW_PARAGRAPH_UTF16 = Integer.parseInt("262144");

    /**
     * Capability bit: paragraph text commands include Skia font style metadata.
     */
    int COMMAND_CAP_PARAGRAPH_FONT_STYLE = Integer.parseInt("524288");

    /**
     * Capability bit: paragraph text commands include alignment and direction metadata.
     */
    int COMMAND_CAP_PARAGRAPH_LAYOUT = Integer.parseInt("1048576");

    /**
     * Capability bit: paragraph text commands include line-height multiplier metadata.
     */
    int COMMAND_CAP_PARAGRAPH_LINE_HEIGHT = Integer.parseInt("2097152");

    /**
     * Capability bit: paragraph text commands include max-lines and ellipsis metadata.
     */
    int COMMAND_CAP_PARAGRAPH_OVERFLOW = Integer.parseInt("4194304");

    /**
     * Capability bit: paragraph text commands include Compose text decoration metadata.
     */
    int COMMAND_CAP_PARAGRAPH_DECORATION = Integer.parseInt("8388608");

    /**
     * Capability bit: paragraph text commands include letter-spacing metadata.
     */
    int COMMAND_CAP_PARAGRAPH_LETTER_SPACING = Integer.parseInt("16777216");

    /**
     * Capability bit: paragraph text commands include background paint metadata.
     */
    int COMMAND_CAP_PARAGRAPH_BACKGROUND = Integer.parseInt("33554432");

    /**
     * Capability bit: command streams can clip with serialized path verbs.
     */
    int COMMAND_CAP_CLIP_PATH = Integer.parseInt("67108864");

    /**
     * Capability bit: command streams can draw serialized path verbs.
     */
    int COMMAND_CAP_DRAW_PATH = Integer.parseInt("134217728");

    /**
     * Capability bit: command streams can draw solid-color arcs.
     */
    int COMMAND_CAP_DRAW_ARC = Integer.parseInt("268435456");

    /**
     * Capability bit: command streams can draw solid-color rounded rectangles.
     */
    int COMMAND_CAP_DRAW_ROUND_RECT = Integer.parseInt("536870912");

    /**
     * Capability bit: command streams can fill rectangles with serialized linear-gradient paint.
     */
    int COMMAND_CAP_FILL_RECT_LINEAR_GRADIENT = Integer.parseInt("1073741824");

    /**
     * 64-bit command capability bit: command streams can fill rectangles with serialized linear-gradient paint.
     */
    long COMMAND_CAP64_FILL_RECT_LINEAR_GRADIENT = Long.parseLong("1073741824");

    /**
     * 64-bit command capability bit: command streams can fill rounded rectangles with serialized linear-gradient paint.
     */
    long COMMAND_CAP64_FILL_ROUND_RECT_LINEAR_GRADIENT = Long.parseLong("2147483648");

    /**
     * 64-bit command capability bit: command streams can fill rectangles with serialized radial-gradient paint.
     */
    long COMMAND_CAP64_FILL_RECT_RADIAL_GRADIENT = Long.parseLong("4294967296");

    /**
     * 64-bit command capability bit: command streams can fill rounded rectangles with serialized radial-gradient paint.
     */
    long COMMAND_CAP64_FILL_ROUND_RECT_RADIAL_GRADIENT = Long.parseLong("8589934592");

    /**
     * 64-bit command capability bit: command streams can fill paths with serialized linear-gradient paint.
     */
    long COMMAND_CAP64_FILL_PATH_LINEAR_GRADIENT = Long.parseLong("17179869184");

    /**
     * 64-bit command capability bit: command streams can fill paths with serialized radial-gradient paint.
     */
    long COMMAND_CAP64_FILL_PATH_RADIAL_GRADIENT = Long.parseLong("34359738368");

    /**
     * 64-bit command capability bit: command streams can fill rectangles with serialized sweep-gradient paint.
     */
    long COMMAND_CAP64_FILL_RECT_SWEEP_GRADIENT = Long.parseLong("68719476736");

    /**
     * 64-bit command capability bit: command streams can fill rounded rectangles with serialized sweep-gradient paint.
     */
    long COMMAND_CAP64_FILL_ROUND_RECT_SWEEP_GRADIENT = Long.parseLong("137438953472");

    /**
     * 64-bit command capability bit: command streams can fill paths with serialized sweep-gradient paint.
     */
    long COMMAND_CAP64_FILL_PATH_SWEEP_GRADIENT = Long.parseLong("274877906944");

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
     * Clip operation payload value: intersect the current clip with the rectangle.
     */
    int COMMAND_CLIP_OP_INTERSECT = Integer.parseInt("0");

    /**
     * Clip operation payload value: subtract the rectangle from the current clip.
     */
    int COMMAND_CLIP_OP_DIFFERENCE = Integer.parseInt("1");

    /**
     * Command-list operation: translate the current canvas transform.
     */
    int COMMAND_TRANSLATE = Integer.parseInt("10");

    /**
     * Command-list operation: scale the current canvas transform.
     */
    int COMMAND_SCALE = Integer.parseInt("11");

    /**
     * Command-list operation: rotate the current canvas transform in degrees.
     */
    int COMMAND_ROTATE = Integer.parseInt("12");

    /**
     * Command-list operation: save a bounded layer with alpha.
     */
    int COMMAND_SAVE_LAYER = Integer.parseInt("13");

    /**
     * Command-list operation: draw an inline ARGB raster image.
     */
    int COMMAND_DRAW_IMAGE_ARGB = Integer.parseInt("14");

    /**
     * Command-list operation: define an ARGB raster image in the JBR-side image cache.
     */
    int COMMAND_DEFINE_IMAGE_ARGB = Integer.parseInt("15");

    /**
     * Command-list operation: draw an image previously defined in the JBR-side image cache.
     */
    int COMMAND_DRAW_IMAGE_REF = Integer.parseInt("16");

    /**
     * Command-list operation: draw a simple UTF-16 text run.
     */
    int COMMAND_DRAW_TEXT_UTF16 = Integer.parseInt("17");

    /**
     * Command-list operation: clear the JBR-side ARGB image cache.
     */
    int COMMAND_CLEAR_IMAGE_CACHE = Integer.parseInt("18");

    /**
     * Command-list operation: draw one JBR-owned shaped UTF-16 paragraph.
     */
    int COMMAND_DRAW_PARAGRAPH_UTF16 = Integer.parseInt("19");

    /**
     * Command-list operation: clip with serialized path verbs.
     */
    int COMMAND_CLIP_PATH = Integer.parseInt("20");

    /**
     * Command-list operation: draw a solid-color fill or stroke with serialized path verbs.
     */
    int COMMAND_DRAW_PATH = Integer.parseInt("21");

    /**
     * Command-list operation: draw a solid-color arc.
     */
    int COMMAND_DRAW_ARC = Integer.parseInt("22");

    /**
     * Command-list operation: draw a solid-color rounded rectangle.
     */
    int COMMAND_DRAW_ROUND_RECT = Integer.parseInt("23");

    /**
     * Command-list operation: fill a rectangle with serialized linear-gradient paint.
     */
    int COMMAND_FILL_RECT_LINEAR_GRADIENT = Integer.parseInt("24");

    /**
     * Command-list operation: fill a rounded rectangle with serialized linear-gradient paint.
     */
    int COMMAND_FILL_ROUND_RECT_LINEAR_GRADIENT = Integer.parseInt("25");

    /**
     * Command-list operation: fill a rectangle with serialized radial-gradient paint.
     */
    int COMMAND_FILL_RECT_RADIAL_GRADIENT = Integer.parseInt("26");

    /**
     * Command-list operation: fill a rounded rectangle with serialized radial-gradient paint.
     */
    int COMMAND_FILL_ROUND_RECT_RADIAL_GRADIENT = Integer.parseInt("27");

    /**
     * Command-list operation: fill a path with serialized linear-gradient paint.
     */
    int COMMAND_FILL_PATH_LINEAR_GRADIENT = Integer.parseInt("28");

    /**
     * Command-list operation: fill a path with serialized radial-gradient paint.
     */
    int COMMAND_FILL_PATH_RADIAL_GRADIENT = Integer.parseInt("29");

    /**
     * Command-list operation: fill a rectangle with serialized sweep-gradient paint.
     */
    int COMMAND_FILL_RECT_SWEEP_GRADIENT = Integer.parseInt("30");

    /**
     * Command-list operation: fill a rounded rectangle with serialized sweep-gradient paint.
     */
    int COMMAND_FILL_ROUND_RECT_SWEEP_GRADIENT = Integer.parseInt("31");

    /**
     * Command-list operation: fill a path with serialized sweep-gradient paint.
     */
    int COMMAND_FILL_PATH_SWEEP_GRADIENT = Integer.parseInt("32");

    /**
     * Paint-style payload value: fill.
     */
    int COMMAND_PAINT_STYLE_FILL = Integer.parseInt("0");

    /**
     * Paint-style payload value: stroke.
     */
    int COMMAND_PAINT_STYLE_STROKE = Integer.parseInt("1");

    /**
     * Path fill-type payload value: non-zero winding.
     */
    int COMMAND_PATH_FILL_NON_ZERO = Integer.parseInt("0");

    /**
     * Path fill-type payload value: even-odd.
     */
    int COMMAND_PATH_FILL_EVEN_ODD = Integer.parseInt("1");

    /**
     * Path verb payload value: move to one point.
     */
    int COMMAND_PATH_VERB_MOVE = Integer.parseInt("0");

    /**
     * Path verb payload value: line to one point.
     */
    int COMMAND_PATH_VERB_LINE = Integer.parseInt("1");

    /**
     * Path verb payload value: quadratic curve with one control point and one end point.
     */
    int COMMAND_PATH_VERB_QUAD = Integer.parseInt("2");

    /**
     * Path verb payload value: cubic curve with two control points and one end point.
     */
    int COMMAND_PATH_VERB_CUBIC = Integer.parseInt("3");

    /**
     * Path verb payload value: close the current contour.
     */
    int COMMAND_PATH_VERB_CLOSE = Integer.parseInt("4");

    /**
     * Returns the command stream capabilities supported by this runtime.
     *
     * <p>Clients must treat missing required capability bits as an interop-unavailable condition
     * and fall back to their old rendering path.</p>
     *
     * @return bitset composed from {@code COMMAND_CAP_*} constants.
     */
    int getCommandCapabilities();

    /**
     * Returns the 64-bit command capability mask. Clients for ABI 31 and newer must use this value
     * for compatibility checks so future commands can use bits beyond the signed {@code int} range.
     *
     * @return bitset composed from {@code COMMAND_CAP64_*} and widened {@code COMMAND_CAP_*} constants.
     */
    long getCommandCapabilities64();

    /**
     * Runtime native metadata block version. This must match {@link #NATIVE_ABI_VERSION}.
     *
     * @return the native metadata block version exposed by the acquired service
     */
    int getNativeAbiVersion();

    /**
     * Runtime command stream ABI id. This mirrors {@link #ABI_ID} but is served from the acquired service.
     *
     * @return the command stream ABI id exposed by the acquired service
     */
    int getNativeCommandStreamAbiId();

    /**
     * Runtime build id. This mirrors {@link #BUILD_ID} but is served from the acquired service.
     *
     * @return the build id exposed by the acquired service
     */
    String getNativeBuildId();

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
         * Returns an opaque destination surface identity that is stable for cached state while the
         * underlying Java2D/Metal destination remains valid.
         *
         * <p>Clients may compare this value between paint scopes to discard cached state after
         * resize, surface loss, or screen migration. {@code 0} means no stable native destination
         * identity is available.</p>
         *
         * @return opaque destination surface id.
         */
        long getSurfaceId();

        /**
         * Returns an opaque destination context identity that is stable while the underlying
         * Java2D/Metal context remains valid.
         *
         * <p>Clients may compare this value separately from {@link #getSurfaceId()} to distinguish
         * a same-context surface replacement, such as resize, from a context/device migration.
         * {@code 0} means no stable native context identity is available.</p>
         *
         * @return opaque destination context id.
         */
        long getContextId();

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
         * <p>The stream starts with a six-integer header:
         * {@code [COMMAND_STREAM_MAGIC, ABI_ID, COMMAND_STREAM_FLAGS_NONE, payloadLength,
         * COMMAND_COORDINATE_SPACE_SWING_USER, COMMAND_PAINT_FORMAT_SOLID_ARGB]}.
         * {@code payloadLength} is the number of integers after the header.</p>
         *
         * <p>Every command record starts with {@code [op, recordByteLength, recordFlags]}, where
         * {@code recordByteLength} is the total byte length of the aligned command record including
         * the three-field header, and {@code recordFlags} must be
         * either {@link JBRSkia#COMMAND_RECORD_FLAGS_NONE} or
         * {@link JBRSkia#COMMAND_RECORD_FLAG_ANTIALIAS} for this ABI.</p>
         *
         * <ul>
         *     <li>{@link JBRSkia#COMMAND_CLEAR}: {@code [op, 16, 0, argb]}</li>
         *     <li>{@link JBRSkia#COMMAND_FILL_RECT}: {@code [op, 36, 0, argb, x, y, width, height, radius]}</li>
         *     <li>{@link JBRSkia#COMMAND_STROKE_LINE}: {@code [op, 48, flags, argb, x1, y1, x2, y2, strokeWidth, strokeCap, strokeJoin, strokeMiter1000]}</li>
         *     <li>{@link JBRSkia#COMMAND_FILL_OVAL}: {@code [op, 32, 0, argb, x, y, width, height]}</li>
         *     <li>{@link JBRSkia#COMMAND_STROKE_OVAL}: {@code [op, 48, flags, argb, x, y, width, height, strokeWidth, strokeCap, strokeJoin, strokeMiter1000]}</li>
         *     <li>{@link JBRSkia#COMMAND_CLEAR_RECT}: {@code [op, 28, 0, x, y, width, height]}</li>
         *     <li>{@link JBRSkia#COMMAND_SAVE}: {@code [op, 12, 0]}</li>
         *     <li>{@link JBRSkia#COMMAND_RESTORE}: {@code [op, 12, 0]}</li>
         *     <li>{@link JBRSkia#COMMAND_CLIP_RECT}: {@code [op, 32, flags, x, y, width, height, clipOp]}</li>
         *     <li>{@link JBRSkia#COMMAND_CLIP_PATH}: {@code [op, 24 + pathDataLength * 4, flags,
         *     clipOp, fillType, pathDataLength, pathVerb0, ...]}, where path data is a sequence of
         *     {@code COMMAND_PATH_VERB_*} records using fixed-point coordinates scaled by 1000.</li>
         *     <li>{@link JBRSkia#COMMAND_DRAW_PATH}: {@code [op, 44 + pathDataLength * 4, flags,
         *     paintStyle, argb, strokeWidth, strokeCap, strokeJoin, strokeMiter1000, fillType,
         *     pathDataLength, pathVerb0, ...]}, where stroke fields are ignored for fill style.</li>
         *     <li>{@link JBRSkia#COMMAND_DRAW_ARC}: {@code [op, 64, flags, paintStyle, argb,
         *     left1000, top1000, right1000, bottom1000, startAngle1000, sweepAngle1000, useCenter,
         *     strokeWidth, strokeCap, strokeJoin, strokeMiter1000]}, where stroke fields are ignored for fill style.</li>
         *     <li>{@link JBRSkia#COMMAND_DRAW_ROUND_RECT}: {@code [op, 60, flags, paintStyle, argb,
         *     left1000, top1000, right1000, bottom1000, radiusX1000, radiusY1000,
         *     strokeWidth, strokeCap, strokeJoin, strokeMiter1000]}, where stroke fields are ignored for fill style.</li>
         *     <li>{@link JBRSkia#COMMAND_FILL_RECT_LINEAR_GRADIENT}: {@code [op, 52 + colorCount * 8, flags,
         *     left1000, top1000, right1000, bottom1000, fromX1000, fromY1000, toX1000, toY1000,
         *     tileMode, colorCount, argb0, stop1000_0, ...]}, with 2..16 colors and stops in [0, 1000].</li>
         *     <li>{@link JBRSkia#COMMAND_FILL_ROUND_RECT_LINEAR_GRADIENT}: {@code [op, 60 + colorCount * 8, flags,
         *     left1000, top1000, right1000, bottom1000, radiusX1000, radiusY1000,
         *     fromX1000, fromY1000, toX1000, toY1000, tileMode, colorCount, argb0, stop1000_0, ...]},
         *     with 2..16 colors and stops in [0, 1000].</li>
         *     <li>{@link JBRSkia#COMMAND_FILL_RECT_RADIAL_GRADIENT}: {@code [op, 48 + colorCount * 8, flags,
         *     left1000, top1000, right1000, bottom1000, centerX1000, centerY1000, radius1000,
         *     tileMode, colorCount, argb0, stop1000_0, ...]}, with 2..16 colors and stops in [0, 1000].</li>
         *     <li>{@link JBRSkia#COMMAND_FILL_ROUND_RECT_RADIAL_GRADIENT}: {@code [op, 56 + colorCount * 8, flags,
         *     left1000, top1000, right1000, bottom1000, radiusX1000, radiusY1000,
         *     centerX1000, centerY1000, radius1000, tileMode, colorCount, argb0, stop1000_0, ...]},
         *     with 2..16 colors and stops in [0, 1000].</li>
         *     <li>{@link JBRSkia#COMMAND_FILL_PATH_LINEAR_GRADIENT}: {@code [op, 44 + pathDataLength * 4
         *     + colorCount * 8, flags, fillType, pathDataLength, pathVerb0, ..., fromX1000, fromY1000,
         *     toX1000, toY1000, tileMode, colorCount, argb0, stop1000_0, ...]}, where path data is a
         *     sequence of {@code COMMAND_PATH_VERB_*} records using fixed-point coordinates scaled by 1000,
         *     with 2..16 colors and stops in [0, 1000].</li>
         *     <li>{@link JBRSkia#COMMAND_FILL_PATH_RADIAL_GRADIENT}: {@code [op, 40 + pathDataLength * 4
         *     + colorCount * 8, flags, fillType, pathDataLength, pathVerb0, ..., centerX1000, centerY1000,
         *     radius1000, tileMode, colorCount, argb0, stop1000_0, ...]}, where path data is a sequence of
         *     {@code COMMAND_PATH_VERB_*} records using fixed-point coordinates scaled by 1000, with 2..16
         *     colors and stops in [0, 1000].</li>
         *     <li>{@link JBRSkia#COMMAND_FILL_RECT_SWEEP_GRADIENT}: {@code [op, 40 + colorCount * 8, flags,
         *     left1000, top1000, right1000, bottom1000, centerX1000, centerY1000, colorCount,
         *     argb0, stop1000_0, ...]}, with 2..16 colors and stops in [0, 1000].</li>
         *     <li>{@link JBRSkia#COMMAND_FILL_ROUND_RECT_SWEEP_GRADIENT}: {@code [op, 48 + colorCount * 8, flags,
         *     left1000, top1000, right1000, bottom1000, radiusX1000, radiusY1000, centerX1000, centerY1000,
         *     colorCount, argb0, stop1000_0, ...]}, with 2..16 colors and stops in [0, 1000].</li>
         *     <li>{@link JBRSkia#COMMAND_FILL_PATH_SWEEP_GRADIENT}: {@code [op, 20 + pathDataLength * 4
         *     + colorCount * 8, flags, fillType, pathDataLength, pathVerb0, ..., centerX1000, centerY1000,
         *     colorCount, argb0, stop1000_0, ...]}, where path data is a sequence of
         *     {@code COMMAND_PATH_VERB_*} records using fixed-point coordinates scaled by 1000, with 2..16
         *     colors and stops in [0, 1000].</li>
         *     <li>{@link JBRSkia#COMMAND_TRANSLATE}: {@code [op, 20, 0, dx1000, dy1000]}</li>
         *     <li>{@link JBRSkia#COMMAND_SCALE}: {@code [op, 20, 0, sx1000, sy1000]}</li>
         *     <li>{@link JBRSkia#COMMAND_ROTATE}: {@code [op, 16, 0, degrees1000]}</li>
         *     <li>{@link JBRSkia#COMMAND_SAVE_LAYER}: {@code [op, 32, 0, x, y, width, height, alpha1000]}</li>
         *     <li>{@link JBRSkia#COMMAND_DRAW_IMAGE_ARGB}: {@code [op, 64 + pixelCount * 4, flags,
         *     srcLeft1000, srcTop1000, srcRight1000, srcBottom1000, dstLeft1000, dstTop1000,
         *     dstRight1000, dstBottom1000, imageWidth, imageHeight, alpha1000, filterQuality,
         *     pixelCount, argb0, ...]}</li>
         *     <li>{@link JBRSkia#COMMAND_DEFINE_IMAGE_ARGB}: {@code [op, 32 + pixelCount * 4, 0,
         *     cacheKeyHigh, cacheKeyLow, imageWidth, imageHeight, pixelCount, argb0, ...]}</li>
         *     <li>{@link JBRSkia#COMMAND_DRAW_IMAGE_REF}: {@code [op, 68, flags,
         *     srcLeft1000, srcTop1000, srcRight1000, srcBottom1000, dstLeft1000, dstTop1000,
         *     dstRight1000, dstBottom1000, cacheKeyHigh, cacheKeyLow, imageWidth, imageHeight,
         *     alpha1000, filterQuality]}</li>
         *     <li>{@link JBRSkia#COMMAND_DRAW_TEXT_UTF16}: {@code [op, 32 + charCount * 4, flags,
         *     x1000, baseline1000, fontSize1000, argb, charCount, codeUnit0, ...]}</li>
         *     <li>{@link JBRSkia#COMMAND_CLEAR_IMAGE_CACHE}: {@code [op, 12, 0]}</li>
         *     <li>{@link JBRSkia#COMMAND_DRAW_PARAGRAPH_UTF16}: {@code [op, 84 + charCount * 4, flags,
         *     x1000, y1000, width1000, fontSize1000, argb, fontWeight, fontWidth, fontSlant,
         *     textAlign, textDirection, lineHeightMultiplier1000, maxLines, ellipsisMode,
         *     decorationMask, letterSpacing1000, backgroundSpecified, backgroundArgb, charCount,
         *     codeUnit0, ...]}</li>
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
         * Renders a command stream carried as little-endian 32-bit words.
         *
         * <p>This PoC-only method is a stepping stone from the temporary Java {@code int[]} carrier
         * toward a direct native memory block. It uses the same stream and record layout documented
         * by {@link #renderCommandFrame(int, int, long, int[])}.</p>
         *
         * @param width user-space width of the component being painted.
         * @param height user-space height of the component being painted.
         * @param frameTimeNanos frame timestamp supplied by the caller.
         * @param commands little-endian encoded command stream.
         * @return {@code true} when the command frame was painted.
         */
        boolean renderCommandBufferFrame(int width, int height, long frameTimeNanos, byte[] commands);

        /**
         * Renders a command stream carried by a direct byte buffer.
         *
         * <p>The buffer uses little-endian 32-bit words with the same stream and record layout
         * documented by {@link #renderCommandFrame(int, int, long, int[])}. Direct buffers are the
         * preferred PoC carrier because JBR native code can read the memory block without pinning a
         * Java byte array.</p>
         *
         * @param width user-space width of the component being painted.
         * @param height user-space height of the component being painted.
         * @param frameTimeNanos frame timestamp supplied by the caller.
         * @param commands direct little-endian encoded command stream.
         * @return {@code true} when the command frame was painted.
         */
        boolean renderCommandDirectFrame(int width, int height, long frameTimeNanos, ByteBuffer commands);

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
