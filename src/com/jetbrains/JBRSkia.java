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
    int ABI_ID = Integer.parseInt("111");

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
     * Capability bit: command streams can draw simple UTF-16 text runs with JBR-owned font resolution.
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
     * Capability bit: command streams may evict one cached image key from the current destination context.
     */
    long COMMAND_CAP64_EVICT_IMAGE_CACHE_KEY = Long.parseLong("549755813888");

    /**
     * 64-bit command capability bit: native text commands carry a UTF-16 font-family name resolved by JBR.
     */
    long COMMAND_CAP64_TEXT_FONT_FAMILY = Long.parseLong("1099511627776");

    /**
     * 64-bit command capability bit: command streams can fill rectangles with a JBR-owned image shader.
     */
    long COMMAND_CAP64_FILL_RECT_IMAGE_SHADER = Long.parseLong("2199023255552");

    /**
     * 64-bit command capability bit: command streams can stroke rectangles with serialized linear-gradient paint.
     */
    long COMMAND_CAP64_STROKE_RECT_LINEAR_GRADIENT = Long.parseLong("4398046511104");

    /**
     * 64-bit command capability bit: command streams can stroke rounded rectangles with serialized linear-gradient paint.
     */
    long COMMAND_CAP64_STROKE_ROUND_RECT_LINEAR_GRADIENT = Long.parseLong("8796093022208");

    /**
     * 64-bit command capability bit: command streams can stroke rectangles with serialized radial-gradient paint.
     */
    long COMMAND_CAP64_STROKE_RECT_RADIAL_GRADIENT = Long.parseLong("17592186044416");

    /**
     * 64-bit command capability bit: command streams can stroke rounded rectangles with serialized radial-gradient paint.
     */
    long COMMAND_CAP64_STROKE_ROUND_RECT_RADIAL_GRADIENT = Long.parseLong("35184372088832");

    /**
     * 64-bit command capability bit: command streams can stroke rectangles with serialized sweep-gradient paint.
     */
    long COMMAND_CAP64_STROKE_RECT_SWEEP_GRADIENT = Long.parseLong("70368744177664");

    /**
     * 64-bit command capability bit: command streams can stroke rounded rectangles with serialized sweep-gradient paint.
     */
    long COMMAND_CAP64_STROKE_ROUND_RECT_SWEEP_GRADIENT = Long.parseLong("140737488355328");

    /**
     * 64-bit command capability bit: command streams can fill rectangles with supported blend modes.
     */
    long COMMAND_CAP64_FILL_RECT_BLEND_MODE = Long.parseLong("281474976710656");

    /**
     * 64-bit command capability bit: command streams can fill rectangles with a supported color filter.
     */
    long COMMAND_CAP64_FILL_RECT_COLOR_FILTER = Long.parseLong("562949953421312");

    /**
     * 64-bit command capability bit: command streams can stroke lines with a dash path effect.
     */
    long COMMAND_CAP64_STROKE_LINE_DASH_PATH_EFFECT = Long.parseLong("1125899906842624");

    /**
     * 64-bit command capability bit: command streams can save layers with a supported color filter.
     */
    long COMMAND_CAP64_SAVE_LAYER_COLOR_FILTER = Long.parseLong("2251799813685248");

    /**
     * 64-bit command capability bit: command streams can draw cached images with a supported color filter.
     */
    long COMMAND_CAP64_DRAW_IMAGE_REF_COLOR_FILTER = Long.parseLong("4503599627370496");

    /**
     * 64-bit command capability bit: command streams can define reusable color-filter descriptors.
     */
    long COMMAND_CAP64_DEFINE_COLOR_FILTER_TINT = Long.parseLong("9007199254740992");

    /**
     * 64-bit command capability bit: command streams can fill rectangles through a color-filter handle.
     */
    long COMMAND_CAP64_FILL_RECT_COLOR_FILTER_REF = Long.parseLong("18014398509481984");

    /**
     * 64-bit command capability bit: command streams may evict scoped color-filter descriptors.
     */
    long COMMAND_CAP64_EVICT_COLOR_FILTER_HANDLE = Long.parseLong("36028797018963968");

    /**
     * 64-bit command capability bit: command streams can define reusable effect descriptors.
     */
    long COMMAND_CAP64_DEFINE_EFFECT_DESCRIPTOR = Long.parseLong("72057594037927936");

    /**
     * 64-bit command capability bit: command streams can save layers with alpha and a supported blend mode.
     */
    long COMMAND_CAP64_SAVE_LAYER_BLEND_MODE = Long.parseLong("144115188075855872");

    /**
     * 64-bit command capability bit: command streams can save layers with alpha, a supported blend
     * mode, and a tint/SrcIn color filter.
     */
    long COMMAND_CAP64_SAVE_LAYER_BLEND_COLOR_FILTER = Long.parseLong("288230376151711744");

    /**
     * Capability bit: command streams may define color-matrix color-filter effect descriptors.
     */
    long COMMAND_CAP64_EFFECT_DESCRIPTOR_COLOR_MATRIX_FILTER = Long.parseLong("576460752303423488");

    /**
     * Capability bit: command streams may define lighting color-filter effect descriptors.
     */
    long COMMAND_CAP64_EFFECT_DESCRIPTOR_LIGHTING_FILTER = Long.parseLong("1152921504606846976");

    /**
     * Capability bit: command streams can save layers with a previously defined color-filter descriptor handle.
     */
    long COMMAND_CAP64_SAVE_LAYER_COLOR_FILTER_REF = Long.parseLong("2305843009213693952");

    /**
     * Capability bit: command streams can draw cached images with a previously defined color-filter descriptor handle.
     */
    long COMMAND_CAP64_DRAW_IMAGE_REF_COLOR_FILTER_REF = Long.parseLong("4611686018427387904");

    /**
     * Capability bit: command streams can save layers with a blend mode and a previously defined color-filter descriptor handle.
     */
    long COMMAND_CAP64_SAVE_LAYER_BLEND_COLOR_FILTER_REF = Long.parseLong("-9223372036854775808");

    /**
     * High capability bit: command streams can save layers with a previously defined image-filter descriptor handle.
     */
    long COMMAND_CAP64_HIGH_SAVE_LAYER_IMAGE_FILTER_REF = Long.parseLong("1");

    /**
     * High capability bit: command streams may define offset image-filter effect descriptors.
     */
    long COMMAND_CAP64_HIGH_EFFECT_DESCRIPTOR_OFFSET_IMAGE_FILTER = Long.parseLong("2");

    /**
     * High capability bit: command streams may define image-filter effect descriptors with child inputs.
     */
    long COMMAND_CAP64_HIGH_EFFECT_DESCRIPTOR_CHAIN_IMAGE_FILTER = Long.parseLong("4");

    /**
     * High capability bit: command streams may define shader descriptors and draw rectangles with shader handles.
     */
    long COMMAND_CAP64_HIGH_SHADER_DESCRIPTOR_REF = Long.parseLong("8");

    /**
     * High capability bit: command streams may define RuntimeEffect color-filter descriptors.
     */
    long COMMAND_CAP64_HIGH_EFFECT_DESCRIPTOR_RUNTIME_COLOR_FILTER = Long.parseLong("16");

    /**
     * High capability bit: command streams may stroke rectangles with dash path-effect metadata.
     */
    long COMMAND_CAP64_HIGH_STROKE_RECT_DASH_PATH_EFFECT = Long.parseLong("32");

    /**
     * High capability bit: command streams may stroke rounded rectangles with dash path-effect metadata.
     */
    long COMMAND_CAP64_HIGH_STROKE_ROUND_RECT_DASH_PATH_EFFECT = Long.parseLong("64");

    /**
     * High capability bit: command streams may stroke arbitrary paths with a dash path effect.
     */
    long COMMAND_CAP64_HIGH_STROKE_PATH_DASH_PATH_EFFECT = Long.parseLong("128");

    /**
     * High capability bit: command streams may draw paths with descriptor-backed path effects.
     */
    long COMMAND_CAP64_HIGH_PATH_EFFECT_DESCRIPTOR_REF = Long.parseLong("256");

    /**
     * High capability bit: command streams may concatenate a 3x3 transform matrix.
     */
    long COMMAND_CAP64_HIGH_CONCAT_MATRIX33 = Long.parseLong("512");

    /**
     * High capability bit: command streams may draw Skia shadow geometry for a path.
     */
    long COMMAND_CAP64_HIGH_DRAW_SHADOW_PATH = Long.parseLong("1024");
    /** Supports shader descriptors that apply typed color-filter descriptor handles. */
    long COMMAND_CAP64_HIGH_SHADER_DESCRIPTOR_COLOR_FILTER = Long.parseLong("2048");
    /** Supports drawPoints(PointMode.Points)-style stroked point clouds with cap metadata. */
    long COMMAND_CAP64_HIGH_DRAW_POINTS = Long.parseLong("4096");
    /** Supports shader descriptors that wrap child shader handles in a local transform matrix. */
    long COMMAND_CAP64_HIGH_SHADER_DESCRIPTOR_TRANSFORM = Long.parseLong("8192");
    /** Supports JBR-owned font-data descriptors for simple native text replay. */
    long COMMAND_CAP64_HIGH_DEFINE_FONT_DATA = Long.parseLong("16384");

    /**
     * High-word capability for solid color shader descriptors.
     */
    long COMMAND_CAP64_HIGH_SHADER_DESCRIPTOR_COLOR = Long.parseLong("32768");

    /** Supports JBR-owned Perlin/noise shader descriptors for fractal noise and turbulence. */
    long COMMAND_CAP64_HIGH_SHADER_DESCRIPTOR_PERLIN_NOISE = Long.parseLong("65536");

    /** Supports serialized Canvas.drawVertices payloads with positions, texture coordinates, colors, and indices. */
    long COMMAND_CAP64_HIGH_DRAW_VERTICES = Long.parseLong("131072");

    /** Supports stroked arbitrary paths with serialized linear-gradient paint. */
    long COMMAND_CAP64_HIGH_STROKE_PATH_LINEAR_GRADIENT = Long.parseLong("262144");

    /** Supports stroked arbitrary paths with serialized radial-gradient paint. */
    long COMMAND_CAP64_HIGH_STROKE_PATH_RADIAL_GRADIENT = Long.parseLong("524288");

    /** Supports stroked arbitrary paths with serialized sweep-gradient paint. */
    long COMMAND_CAP64_HIGH_STROKE_PATH_SWEEP_GRADIENT = Long.parseLong("1048576");

    /** Supports stroked rectangles with shader descriptor handles. */
    long COMMAND_CAP64_HIGH_STROKE_RECT_SHADER_REF = Long.parseLong("2097152");

    /** Supports stroked rectangles with JBR-owned image shaders. */
    long COMMAND_CAP64_HIGH_STROKE_RECT_IMAGE_SHADER = Long.parseLong("4194304");
    /** Supports compact save plus translate records. */
    long COMMAND_CAP64_HIGH_SAVE_TRANSLATE = Long.parseLong("8388608");
    /** Supports compact repeated restore records. */
    long COMMAND_CAP64_HIGH_RESTORE_N = Long.parseLong("16777216");
    /** Supports compact save plus translate plus saveLayer records. */
    long COMMAND_CAP64_HIGH_SAVE_TRANSLATE_LAYER = Long.parseLong("33554432");
    /** Supports compact full-source image reference draw records with default alpha. */
    long COMMAND_CAP64_HIGH_DRAW_IMAGE_REF_FULL = Long.parseLong("67108864");
    /** Supports compact filled round-rectangle records. */
    long COMMAND_CAP64_HIGH_FILL_ROUND_RECT = Long.parseLong("134217728");
    /** Supports compact runs of full-source image reference draw records. */
    long COMMAND_CAP64_HIGH_DRAW_IMAGE_REF_FULL_RUN = Long.parseLong("1073741824");
    /** Supports compact saveLayer plus clipRect records. */
    long COMMAND_CAP64_HIGH_SAVE_LAYER_CLIP_RECT = Long.parseLong("2147483648");
    /** Supports compact full-source image reference draw plus filled rectangle records. */
    long COMMAND_CAP64_HIGH_DRAW_IMAGE_REF_FULL_FILL_RECT = Long.parseLong("4294967296");
    /** Supports compact stroke-line plus full-source image reference run records. */
    long COMMAND_CAP64_HIGH_STROKE_LINE_DRAW_IMAGE_REF_FULL_RUN = Long.parseLong("8589934592");
    /** Supports compact translated-layer plus nested translated-save records. */
    long COMMAND_CAP64_HIGH_SAVE_TRANSLATE_LAYER_SAVE_TRANSLATE = Long.parseLong("17179869184");
    /** Supports compact full-source image reference draw plus restore records. */
    long COMMAND_CAP64_HIGH_DRAW_IMAGE_REF_FULL_RESTORE = Long.parseLong("34359738368");
    /** Supports compact full-source image reference draw plus restoreN records. */
    long COMMAND_CAP64_HIGH_DRAW_IMAGE_REF_FULL_RESTORE_N = Long.parseLong("68719476736");

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
     * Evicts one cached image key from the current destination context.
     */
    int COMMAND_EVICT_IMAGE_CACHE_KEY = Integer.parseInt("33");

    /**
     * Command-list operation: fill a rectangle with a cached image shader owned by JBR's Skia runtime.
     */
    int COMMAND_FILL_RECT_IMAGE_SHADER = Integer.parseInt("34");

    /**
     * Command-list operation: stroke a rectangle with serialized linear-gradient paint.
     */
    int COMMAND_STROKE_RECT_LINEAR_GRADIENT = Integer.parseInt("35");

    /**
     * Command-list operation: stroke a rounded rectangle with serialized linear-gradient paint.
     */
    int COMMAND_STROKE_ROUND_RECT_LINEAR_GRADIENT = Integer.parseInt("36");

    /**
     * Command-list operation: stroke a rectangle with serialized radial-gradient paint.
     */
    int COMMAND_STROKE_RECT_RADIAL_GRADIENT = Integer.parseInt("37");

    /**
     * Command-list operation: stroke a rounded rectangle with serialized radial-gradient paint.
     */
    int COMMAND_STROKE_ROUND_RECT_RADIAL_GRADIENT = Integer.parseInt("38");

    /**
     * Command-list operation: stroke a rectangle with serialized sweep-gradient paint.
     */
    int COMMAND_STROKE_RECT_SWEEP_GRADIENT = Integer.parseInt("39");

    /**
     * Command-list operation: stroke a rounded rectangle with serialized sweep-gradient paint.
     */
    int COMMAND_STROKE_ROUND_RECT_SWEEP_GRADIENT = Integer.parseInt("40");

    /**
     * Command-list operation: fill a rectangle with one ARGB color and an explicit blend mode.
     */
    int COMMAND_FILL_RECT_BLEND_MODE = Integer.parseInt("41");

    /**
     * Command-list operation: fill a rectangle with one ARGB color and an explicit color filter.
     */
    int COMMAND_FILL_RECT_COLOR_FILTER = Integer.parseInt("42");

    /**
     * Command-list operation: stroke a line with a dash path effect.
     */
    int COMMAND_STROKE_LINE_DASH_PATH_EFFECT = Integer.parseInt("43");

    /**
     * Command-list operation: save a layer with alpha and a supported color filter.
     */
    int COMMAND_SAVE_LAYER_COLOR_FILTER = Integer.parseInt("44");

    /**
     * Command-list operation: draw a cached image with alpha and a supported color filter.
     */
    int COMMAND_DRAW_IMAGE_REF_COLOR_FILTER = Integer.parseInt("45");

    /**
     * Command-list operation: define a reusable tint color-filter descriptor in the current command frame.
     */
    int COMMAND_DEFINE_COLOR_FILTER_TINT = Integer.parseInt("46");

    /**
     * Command-list operation: fill a rectangle through a previously defined color-filter descriptor handle.
     */
    int COMMAND_FILL_RECT_COLOR_FILTER_REF = Integer.parseInt("47");

    /**
     * Command-list operation: evict a reusable color-filter descriptor from the current destination context.
     */
    int COMMAND_EVICT_COLOR_FILTER_HANDLE = Integer.parseInt("48");

    /**
     * Command-list operation: define a reusable effect descriptor in the current destination context.
     */
    int COMMAND_DEFINE_EFFECT_DESCRIPTOR = Integer.parseInt("49");

    /**
     * Command-list operation: save a layer with alpha and a supported blend mode.
     */
    int COMMAND_SAVE_LAYER_BLEND_MODE = Integer.parseInt("50");

    /**
     * Command-list operation: save a layer with alpha, a supported blend mode, and a tint/SrcIn
     * color filter.
     */
    int COMMAND_SAVE_LAYER_BLEND_COLOR_FILTER = Integer.parseInt("51");

    /**
     * Command-list operation: save a layer with alpha and a previously defined color-filter descriptor handle.
     */
    int COMMAND_SAVE_LAYER_COLOR_FILTER_REF = Integer.parseInt("52");

    /**
     * Command-list operation: draw a cached image with alpha and a previously defined color-filter descriptor handle.
     */
    int COMMAND_DRAW_IMAGE_REF_COLOR_FILTER_REF = Integer.parseInt("53");

    /**
     * Command-list operation: save a layer with alpha, a blend mode, and a previously defined color-filter descriptor handle.
     */
    int COMMAND_SAVE_LAYER_BLEND_COLOR_FILTER_REF = Integer.parseInt("54");

    /**
     * Command-list operation: save a layer with alpha and a previously defined image-filter descriptor handle.
     */
    int COMMAND_SAVE_LAYER_IMAGE_FILTER_REF = Integer.parseInt("55");

    /**
     * Command-list operation: define a shader descriptor handle.
     */
    int COMMAND_DEFINE_SHADER_DESCRIPTOR = Integer.parseInt("56");

    /**
     * Command-list operation: evict a shader descriptor handle.
     */
    int COMMAND_EVICT_SHADER_HANDLE = Integer.parseInt("57");

    /**
     * Command-list operation: fill a rectangle with a previously defined shader descriptor handle.
     */
    int COMMAND_FILL_RECT_SHADER_REF = Integer.parseInt("58");

    /**
     * Command-list operation: stroke a rectangle with dash path-effect metadata.
     */
    int COMMAND_STROKE_RECT_DASH_PATH_EFFECT = Integer.parseInt("59");

    /**
     * Command-list operation: stroke a rounded rectangle with dash path-effect metadata.
     */
    int COMMAND_STROKE_ROUND_RECT_DASH_PATH_EFFECT = Integer.parseInt("60");

    /**
     * Stroke an arbitrary path with a dash path effect.
     */
    int COMMAND_STROKE_PATH_DASH_PATH_EFFECT = Integer.parseInt("61");

    /**
     * Draw an arbitrary path with a descriptor-backed path effect.
     */
    int COMMAND_DRAW_PATH_PATH_EFFECT_REF = Integer.parseInt("62");

    /**
     * Concatenate a 3x3 transform matrix encoded as nine raw float bits in SkMatrix order:
     * scaleX, skewX, translateX, skewY, scaleY, translateY, perspective0, perspective1, perspective2.
     */
    int COMMAND_CONCAT_MATRIX33 = Integer.parseInt("63");

    /**
     * Draw Skia shadow geometry for an arbitrary path.
     */
    int COMMAND_DRAW_SHADOW_PATH = Integer.parseInt("64");

    /**
     * Draw stroked points using Skia point-mode semantics.
     */
    int COMMAND_DRAW_POINTS = Integer.parseInt("65");

    /**
     * Define a JBR-owned font-data descriptor for later simple native text commands.
     */
    int COMMAND_DEFINE_FONT_DATA = Integer.parseInt("66");

    /**
     * Draw serialized vertices using Skia vertex-mode semantics.
     */
    int COMMAND_DRAW_VERTICES = Integer.parseInt("67");

    /**
     * Stroke an arbitrary path with serialized linear-gradient paint.
     */
    int COMMAND_STROKE_PATH_LINEAR_GRADIENT = Integer.parseInt("68");

    /**
     * Stroke an arbitrary path with serialized radial-gradient paint.
     */
    int COMMAND_STROKE_PATH_RADIAL_GRADIENT = Integer.parseInt("69");

    /**
     * Stroke an arbitrary path with serialized sweep-gradient paint.
     */
    int COMMAND_STROKE_PATH_SWEEP_GRADIENT = Integer.parseInt("70");

    /**
     * Stroke a rectangle with a previously defined shader descriptor handle.
     */
    int COMMAND_STROKE_RECT_SHADER_REF = Integer.parseInt("71");

    /**
     * Stroke a rectangle with a JBR-owned image shader.
     */
    int COMMAND_STROKE_RECT_IMAGE_SHADER = Integer.parseInt("72");

    /**
     * Command-list operation: save canvas state and translate the current canvas transform.
     */
    int COMMAND_SAVE_TRANSLATE = Integer.parseInt("74");
    /**
     * Command-list operation: restore canvas state repeatedly.
     */
    int COMMAND_RESTORE_N = Integer.parseInt("75");
    /**
     * Command-list operation: save canvas state, translate, and save an alpha layer.
     */
    int COMMAND_SAVE_TRANSLATE_LAYER = Integer.parseInt("76");
    /**
     * Command-list operation: draw a full-source image reference with default alpha.
     */
    int COMMAND_DRAW_IMAGE_REF_FULL = Integer.parseInt("77");
    /**
     * Command-list operation: fill a rounded rectangle with one ARGB color.
     */
    int COMMAND_FILL_ROUND_RECT = Integer.parseInt("78");
    /**
     * Command-list operation: draw a run of full-source image references with default alpha.
     */
    int COMMAND_DRAW_IMAGE_REF_FULL_RUN = Integer.parseInt("81");
    /**
     * Command-list operation: save an alpha layer and immediately clip a rectangle.
     */
    int COMMAND_SAVE_LAYER_CLIP_RECT = Integer.parseInt("82");
    /**
     * Command-list operation: draw a full-source image reference, then fill a rectangle.
     */
    int COMMAND_DRAW_IMAGE_REF_FULL_FILL_RECT = Integer.parseInt("83");
    /**
     * Command-list operation: stroke a line, then draw a run of full-source image references.
     */
    int COMMAND_STROKE_LINE_DRAW_IMAGE_REF_FULL_RUN = Integer.parseInt("84");
    /**
     * Command-list operation: create a translated layer, then create a nested translated save.
     */
    int COMMAND_SAVE_TRANSLATE_LAYER_SAVE_TRANSLATE = Integer.parseInt("85");
    /**
     * Command-list operation: draw a full-source image reference, then restore one save.
     */
    int COMMAND_DRAW_IMAGE_REF_FULL_RESTORE = Integer.parseInt("86");
    /**
     * Command-list operation: draw a full-source image reference, then restore multiple saves.
     */
    int COMMAND_DRAW_IMAGE_REF_FULL_RESTORE_N = Integer.parseInt("87");

    /**
     * Effect descriptor type: tint color filter.
     */
    int COMMAND_EFFECT_DESCRIPTOR_TINT_COLOR_FILTER = Integer.parseInt("1");

    /**
     * Effect descriptor type: color-matrix color filter.
     */
    int COMMAND_EFFECT_DESCRIPTOR_COLOR_MATRIX_FILTER = Integer.parseInt("2");

    /**
     * Effect descriptor type: lighting color filter.
     */
    int COMMAND_EFFECT_DESCRIPTOR_LIGHTING_FILTER = Integer.parseInt("3");

    /**
     * Effect descriptor type: blur image filter.
     */
    int COMMAND_EFFECT_DESCRIPTOR_BLUR_IMAGE_FILTER = Integer.parseInt("4");

    /**
     * Effect descriptor type: offset image filter.
     */
    int COMMAND_EFFECT_DESCRIPTOR_OFFSET_IMAGE_FILTER = Integer.parseInt("5");

    /**
     * Effect descriptor type: blur image filter with a child input descriptor.
     */
    int COMMAND_EFFECT_DESCRIPTOR_BLUR_IMAGE_FILTER_WITH_INPUT = Integer.parseInt("6");

    /**
     * Effect descriptor type: offset image filter with a child input descriptor.
     */
    int COMMAND_EFFECT_DESCRIPTOR_OFFSET_IMAGE_FILTER_WITH_INPUT = Integer.parseInt("7");

    /**
     * Effect descriptor type: Skia RuntimeEffect color filter with inline SKSL and uniform payload.
     */
    int COMMAND_EFFECT_DESCRIPTOR_RUNTIME_COLOR_FILTER = Integer.parseInt("8");

    /**
     * Effect descriptor type: a corner path effect with one float payload, radius.
     */
    int COMMAND_EFFECT_DESCRIPTOR_CORNER_PATH_EFFECT = Integer.parseInt("9");

    /**
     * Effect descriptor type: a stamped/path-1D path effect.
     */
    int COMMAND_EFFECT_DESCRIPTOR_STAMPED_PATH_EFFECT = Integer.parseInt("10");

    /**
     * Effect descriptor type: a composed path effect with outer and inner child descriptor handles.
     */
    int COMMAND_EFFECT_DESCRIPTOR_CHAIN_PATH_EFFECT = Integer.parseInt("11");

    /**
     * Effect descriptor schema version 1.
     */
    int COMMAND_EFFECT_DESCRIPTOR_VERSION_1 = Integer.parseInt("1");

    /**
     * Shader descriptor type: linear gradient.
     */
    int COMMAND_SHADER_DESCRIPTOR_LINEAR_GRADIENT = Integer.parseInt("1");

    /**
     * Shader descriptor type: radial gradient.
     */
    int COMMAND_SHADER_DESCRIPTOR_RADIAL_GRADIENT = Integer.parseInt("2");

    /**
     * Shader descriptor type: sweep gradient.
     */
    int COMMAND_SHADER_DESCRIPTOR_SWEEP_GRADIENT = Integer.parseInt("3");

    /**
     * Shader descriptor type: cached image shader.
     */
    int COMMAND_SHADER_DESCRIPTOR_IMAGE = Integer.parseInt("4");

    /**
     * Shader descriptor type: blend of two child shader descriptors.
     */
    int COMMAND_SHADER_DESCRIPTOR_COMPOSITE = Integer.parseInt("5");

    /**
     * Shader descriptor type: Skia RuntimeEffect shader with inline SKSL and uniform payload.
     */
    int COMMAND_SHADER_DESCRIPTOR_RUNTIME_EFFECT = Integer.parseInt("6");
    /** Shader descriptor type whose payload is shader-handle high/low and color-filter-handle high/low. */
    int COMMAND_SHADER_DESCRIPTOR_COLOR_FILTER = Integer.parseInt("7");
    /** Shader descriptor type whose payload is child shader-handle high/low followed by a 3x3 fixed1000 matrix. */
    int COMMAND_SHADER_DESCRIPTOR_TRANSFORM = Integer.parseInt("8");

    /**
     * Shader descriptor type for a solid ARGB color shader.
     */
    int COMMAND_SHADER_DESCRIPTOR_COLOR = Integer.parseInt("9");

    /** Shader descriptor type for JBR-owned Perlin/noise shaders. */
    int COMMAND_SHADER_DESCRIPTOR_PERLIN_NOISE = Integer.parseInt("10");

    /**
     * Shader descriptor schema version 1.
     */
    int COMMAND_SHADER_DESCRIPTOR_VERSION_1 = Integer.parseInt("1");

    /**
     * Blend-mode payload value: plus/additive blending.
     */
    int COMMAND_BLEND_MODE_PLUS = Integer.parseInt("1");

    /**
     * Blend-mode payload value: source-in compositing.
     */
    int COMMAND_BLEND_MODE_SRC_IN = Integer.parseInt("2");

    /**
     * Blend-mode payload value: multiply blending.
     */
    int COMMAND_BLEND_MODE_MULTIPLY = Integer.parseInt("3");

    /**
     * Blend-mode payload value: screen blending.
     */
    int COMMAND_BLEND_MODE_SCREEN = Integer.parseInt("4");

    /**
     * Blend-mode payload value: overlay blending.
     */
    int COMMAND_BLEND_MODE_OVERLAY = Integer.parseInt("5");

    /**
     * Darken blend mode for solid fill-rectangle commands.
     */
    int COMMAND_BLEND_MODE_DARKEN = Integer.parseInt("6");

    /**
     * Lighten blend mode for solid fill-rectangle commands.
     */
    int COMMAND_BLEND_MODE_LIGHTEN = Integer.parseInt("7");

    /**
     * Difference blend mode for solid fill-rectangle commands.
     */
    int COMMAND_BLEND_MODE_DIFFERENCE = Integer.parseInt("8");

    /**
     * Exclusion blend mode for solid fill-rectangle commands.
     */
    int COMMAND_BLEND_MODE_EXCLUSION = Integer.parseInt("9");

    /**
     * Color Dodge blend mode for solid fill-rectangle commands.
     */
    int COMMAND_BLEND_MODE_COLOR_DODGE = Integer.parseInt("10");

    /**
     * Color Burn blend mode for solid fill-rectangle commands.
     */
    int COMMAND_BLEND_MODE_COLOR_BURN = Integer.parseInt("11");

    /**
     * Hardlight blend mode for solid fill-rectangle commands.
     */
    int COMMAND_BLEND_MODE_HARDLIGHT = Integer.parseInt("12");

    /**
     * Softlight blend mode for solid fill-rectangle commands.
     */
    int COMMAND_BLEND_MODE_SOFTLIGHT = Integer.parseInt("13");

    /**
     * Hue blend mode for solid fill-rectangle commands.
     */
    int COMMAND_BLEND_MODE_HUE = Integer.parseInt("14");

    /**
     * Saturation blend mode for solid fill-rectangle commands.
     */
    int COMMAND_BLEND_MODE_SATURATION = Integer.parseInt("15");

    /**
     * Color blend mode for solid fill-rectangle commands.
     */
    int COMMAND_BLEND_MODE_COLOR = Integer.parseInt("16");

    /**
     * Luminosity blend mode for solid fill-rectangle commands.
     */
    int COMMAND_BLEND_MODE_LUMINOSITY = Integer.parseInt("17");

    /**
     * Source-over blend mode for shader descriptor composition.
     */
    int COMMAND_BLEND_MODE_SRC_OVER = Integer.parseInt("18");

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
     * Returns the second 64-bit command capability mask. ABI 81 introduces this extension word so
     * command negotiation can continue after the low 64-bit mask became full in ABI 80.
     *
     * <p>Clients must require any future {@code COMMAND_CAP64_HIGH_*} bits through this value and
     * fall back when required high-word bits are absent.</p>
     *
     * @return high bitset composed from future {@code COMMAND_CAP64_HIGH_*} constants.
     */
    long getCommandCapabilities64High();

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
         *     <li>{@link JBRSkia#COMMAND_SAVE_LAYER_CLIP_RECT}: {@code [op, 52, flags,
         *     layerX, layerY, layerWidth, layerHeight, alpha1000, clipX, clipY, clipWidth, clipHeight, clipOp]}</li>
         *     <li>{@link JBRSkia#COMMAND_DRAW_IMAGE_REF_FULL_FILL_RECT}: {@code [op, 64, fillFlags,
         *     imageFlags, dstLeft1000, dstTop1000, dstRight1000, dstBottom1000, cacheKeyHigh, cacheKeyLow,
         *     argb, x, y, width, height, radius]}</li>
         *     <li>{@link JBRSkia#COMMAND_STROKE_LINE_DRAW_IMAGE_REF_FULL_RUN}: {@code [op,
         *     56 + imageCount * 24, lineFlags, imageFlags, argb, x1, y1, x2, y2, strokeWidth, strokeCap,
         *     strokeJoin, strokeMiter1000, imageCount, dstLeft1000, dstTop1000, dstRight1000, dstBottom1000,
         *     cacheKeyHigh, cacheKeyLow, ...]}</li>
         *     <li>{@link JBRSkia#COMMAND_SAVE_TRANSLATE_LAYER_SAVE_TRANSLATE}: {@code [op, 48, 0,
         *     layerDx1000, layerDy1000, layerX, layerY, layerWidth, layerHeight, alpha1000,
         *     nestedDx1000, nestedDy1000]}</li>
         *     <li>{@link JBRSkia#COMMAND_DRAW_IMAGE_REF_FULL_RESTORE}: {@code [op, 36, imageFlags,
         *     dstLeft1000, dstTop1000, dstRight1000, dstBottom1000, cacheKeyHigh, cacheKeyLow]}</li>
         *     <li>{@link JBRSkia#COMMAND_DRAW_IMAGE_REF_FULL_RESTORE_N}: {@code [op, 40, imageFlags,
         *     dstLeft1000, dstTop1000, dstRight1000, dstBottom1000, cacheKeyHigh, cacheKeyLow,
         *     extraRestoreCount]}</li>
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
         *     <li>{@link JBRSkia#COMMAND_FILL_ROUND_RECT}: {@code [op, 40, flags, argb,
         *     left1000, top1000, right1000, bottom1000, radiusX1000, radiusY1000]}</li>
         *     <li>{@link JBRSkia#COMMAND_FILL_RECT_LINEAR_GRADIENT}: {@code [op, 52 + colorCount * 8, flags,
         *     left1000, top1000, right1000, bottom1000, fromX1000, fromY1000, toX1000, toY1000,
         *     tileMode, colorCount, argb0, stop1000_0, ...]}, with 2..16 colors and stops in [0, 1000].</li>
         *     <li>{@link JBRSkia#COMMAND_STROKE_RECT_LINEAR_GRADIENT}: {@code [op, 68 + colorCount * 8, flags,
         *     left1000, top1000, right1000, bottom1000, strokeWidth1000, strokeCap, strokeJoin, strokeMiter1000,
         *     fromX1000, fromY1000, toX1000, toY1000, tileMode, colorCount, argb0, stop1000_0, ...]},
         *     with 2..16 colors and stops in [0, 1000].</li>
         *     <li>{@link JBRSkia#COMMAND_STROKE_ROUND_RECT_LINEAR_GRADIENT}: {@code [op, 76 + colorCount * 8, flags,
         *     left1000, top1000, right1000, bottom1000, radiusX1000, radiusY1000,
         *     strokeWidth1000, strokeCap, strokeJoin, strokeMiter1000,
         *     fromX1000, fromY1000, toX1000, toY1000, tileMode, colorCount, argb0, stop1000_0, ...]},
         *     with 2..16 colors and stops in [0, 1000].</li>
         *     <li>{@link JBRSkia#COMMAND_STROKE_RECT_RADIAL_GRADIENT}: {@code [op, 64 + colorCount * 8, flags,
         *     left1000, top1000, right1000, bottom1000, strokeWidth1000, strokeCap, strokeJoin, strokeMiter1000,
         *     centerX1000, centerY1000, radius1000, tileMode, colorCount, argb0, stop1000_0, ...]},
         *     with 2..16 colors and stops in [0, 1000].</li>
         *     <li>{@link JBRSkia#COMMAND_STROKE_ROUND_RECT_RADIAL_GRADIENT}: {@code [op, 72 + colorCount * 8, flags,
         *     left1000, top1000, right1000, bottom1000, radiusX1000, radiusY1000,
         *     strokeWidth1000, strokeCap, strokeJoin, strokeMiter1000,
         *     centerX1000, centerY1000, radius1000, tileMode, colorCount, argb0, stop1000_0, ...]},
         *     with 2..16 colors and stops in [0, 1000].</li>
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
         *     <li>{@link JBRSkia#COMMAND_STROKE_RECT_SWEEP_GRADIENT}: {@code [op, 56 + colorCount * 8, flags,
         *     left1000, top1000, right1000, bottom1000, strokeWidth1000, strokeCap, strokeJoin,
         *     strokeMiter1000, centerX1000, centerY1000, colorCount, argb0, stop1000_0, ...]},
         *     with 2..16 colors and stops in [0, 1000].</li>
         *     <li>{@link JBRSkia#COMMAND_FILL_ROUND_RECT_SWEEP_GRADIENT}: {@code [op, 48 + colorCount * 8, flags,
         *     left1000, top1000, right1000, bottom1000, radiusX1000, radiusY1000, centerX1000, centerY1000,
         *     colorCount, argb0, stop1000_0, ...]}, with 2..16 colors and stops in [0, 1000].</li>
         *     <li>{@link JBRSkia#COMMAND_STROKE_ROUND_RECT_SWEEP_GRADIENT}: {@code [op, 64 + colorCount * 8, flags,
         *     left1000, top1000, right1000, bottom1000, radiusX1000, radiusY1000,
         *     strokeWidth1000, strokeCap, strokeJoin, strokeMiter1000, centerX1000, centerY1000,
         *     colorCount, argb0, stop1000_0, ...]}, with 2..16 colors and stops in [0, 1000].</li>
         *     <li>{@link JBRSkia#COMMAND_FILL_PATH_SWEEP_GRADIENT}: {@code [op, 20 + pathDataLength * 4
         *     + colorCount * 8, flags, fillType, pathDataLength, pathVerb0, ..., centerX1000, centerY1000,
         *     colorCount, argb0, stop1000_0, ...]}, where path data is a sequence of
         *     {@code COMMAND_PATH_VERB_*} records using fixed-point coordinates scaled by 1000, with 2..16
         *     colors and stops in [0, 1000].</li>
         *     <li>{@link JBRSkia#COMMAND_STROKE_PATH_LINEAR_GRADIENT}: {@code [op, 60 + pathDataLength * 4
         *     + colorCount * 8, flags, strokeWidth1000, strokeCap, strokeJoin, strokeMiter1000,
         *     fillType, pathDataLength, pathVerb0, ..., fromX1000, fromY1000, toX1000, toY1000,
         *     tileMode, colorCount, argb0, stop1000_0, ...]}, where path data is a sequence of
         *     {@code COMMAND_PATH_VERB_*} records using fixed-point coordinates scaled by 1000, with 2..16
         *     colors and stops in [0, 1000].</li>
         *     <li>{@link JBRSkia#COMMAND_STROKE_PATH_RADIAL_GRADIENT}: {@code [op, 56 + pathDataLength * 4
         *     + colorCount * 8, flags, strokeWidth1000, strokeCap, strokeJoin, strokeMiter1000,
         *     fillType, pathDataLength, pathVerb0, ..., centerX1000, centerY1000, radius1000,
         *     tileMode, colorCount, argb0, stop1000_0, ...]}, where path data is a sequence of
         *     {@code COMMAND_PATH_VERB_*} records using fixed-point coordinates scaled by 1000, with 2..16
         *     colors and stops in [0, 1000].</li>
         *     <li>{@link JBRSkia#COMMAND_STROKE_PATH_SWEEP_GRADIENT}: {@code [op, 48 + pathDataLength * 4
         *     + colorCount * 8, flags, strokeWidth1000, strokeCap, strokeJoin, strokeMiter1000,
         *     fillType, pathDataLength, pathVerb0, ..., centerX1000, centerY1000, colorCount,
         *     argb0, stop1000_0, ...]}, where path data is a sequence of
         *     {@code COMMAND_PATH_VERB_*} records using fixed-point coordinates scaled by 1000, with 2..16
         *     colors and stops in [0, 1000].</li>
         *     <li>{@link JBRSkia#COMMAND_FILL_RECT_BLEND_MODE}: {@code [op, 36, flags, argb, blendMode,
         *     x, y, width, height]}, with {@code blendMode} currently limited to
         *     {@link JBRSkia#COMMAND_BLEND_MODE_PLUS} and
         *     {@link JBRSkia#COMMAND_BLEND_MODE_MULTIPLY} and
         *     {@link JBRSkia#COMMAND_BLEND_MODE_SCREEN} and
         *     {@link JBRSkia#COMMAND_BLEND_MODE_OVERLAY} and
     *     {@link JBRSkia#COMMAND_BLEND_MODE_DARKEN} and
     *     {@link JBRSkia#COMMAND_BLEND_MODE_LIGHTEN} and
     *     {@link JBRSkia#COMMAND_BLEND_MODE_DIFFERENCE} and
     *     {@link JBRSkia#COMMAND_BLEND_MODE_EXCLUSION} and
     *     {@link JBRSkia#COMMAND_BLEND_MODE_COLOR_DODGE} and
     *     {@link JBRSkia#COMMAND_BLEND_MODE_COLOR_BURN} and
     *     {@link JBRSkia#COMMAND_BLEND_MODE_HARDLIGHT} and
     *     {@link JBRSkia#COMMAND_BLEND_MODE_SOFTLIGHT} and
     *     {@link JBRSkia#COMMAND_BLEND_MODE_HUE} and
     *     {@link JBRSkia#COMMAND_BLEND_MODE_SATURATION} and
     *     {@link JBRSkia#COMMAND_BLEND_MODE_COLOR} and
     *     {@link JBRSkia#COMMAND_BLEND_MODE_LUMINOSITY}.</li>
         *     <li>{@link JBRSkia#COMMAND_FILL_RECT_COLOR_FILTER}: {@code [op, 40, flags, argb,
         *     colorFilterArgb, colorFilterBlendMode, x, y, width, height]}, with
         *     {@code colorFilterBlendMode} currently limited to {@link JBRSkia#COMMAND_BLEND_MODE_SRC_IN}.</li>
         *     <li>{@link JBRSkia#COMMAND_STROKE_LINE_DASH_PATH_EFFECT}: {@code [op, 56 + intervalCount * 4,
         *     flags, argb, x1, y1, x2, y2, strokeWidth, strokeCap, strokeJoin, strokeMiter1000,
         *     phase1000, intervalCount, interval1000_0, ...]}, with 2..16 positive intervals.</li>
         *     <li>{@link JBRSkia#COMMAND_TRANSLATE}: {@code [op, 20, 0, dx1000, dy1000]}</li>
         *     <li>{@link JBRSkia#COMMAND_SCALE}: {@code [op, 20, 0, sx1000, sy1000]}</li>
         *     <li>{@link JBRSkia#COMMAND_ROTATE}: {@code [op, 16, 0, degrees1000]}</li>
         *     <li>{@link JBRSkia#COMMAND_SAVE_LAYER}: {@code [op, 32, 0, x, y, width, height, alpha1000]}</li>
         *     <li>{@link JBRSkia#COMMAND_SAVE_LAYER_BLEND_MODE}: {@code [op, 36, 0,
         *     x, y, width, height, alpha1000, blendMode]}, with {@code blendMode} limited to the same
         *     direct Skia blend-mode values accepted by {@link JBRSkia#COMMAND_FILL_RECT_BLEND_MODE}.</li>
         *     <li>{@link JBRSkia#COMMAND_SAVE_LAYER_COLOR_FILTER}: {@code [op, 40, 0,
         *     x, y, width, height, alpha1000, colorFilterArgb, colorFilterBlendMode]}, with
         *     {@code alpha1000} in [0, 1000] and {@code colorFilterBlendMode} currently limited to
         *     {@link JBRSkia#COMMAND_BLEND_MODE_SRC_IN}.</li>
         *     <li>{@link JBRSkia#COMMAND_SAVE_LAYER_BLEND_COLOR_FILTER}: {@code [op, 44, 0,
         *     x, y, width, height, alpha1000, blendMode, colorFilterArgb, colorFilterBlendMode]},
         *     with {@code blendMode} limited to direct Skia blend-mode values and
         *     {@code colorFilterBlendMode} currently limited to {@link JBRSkia#COMMAND_BLEND_MODE_SRC_IN}.</li>
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
         *     <li>{@link JBRSkia#COMMAND_DRAW_IMAGE_REF_COLOR_FILTER}: {@code [op, 76, flags,
         *     srcLeft1000, srcTop1000, srcRight1000, srcBottom1000, dstLeft1000, dstTop1000,
         *     dstRight1000, dstBottom1000, cacheKeyHigh, cacheKeyLow, imageWidth, imageHeight,
         *     alpha1000, filterQuality, colorFilterArgb, colorFilterBlendMode]}, with
         *     {@code colorFilterBlendMode} currently limited to
         *     {@link JBRSkia#COMMAND_BLEND_MODE_SRC_IN}.</li>
         *     <li>{@link JBRSkia#COMMAND_DEFINE_COLOR_FILTER_TINT}: {@code [op, 28, 0,
         *     handleHigh, handleLow, colorFilterArgb, colorFilterBlendMode]}, with
         *     {@code colorFilterBlendMode} currently limited to
         *     {@link JBRSkia#COMMAND_BLEND_MODE_SRC_IN}. This tint-specific form is kept for
         *     compatibility; new streams should prefer {@link JBRSkia#COMMAND_DEFINE_EFFECT_DESCRIPTOR}.
         *     Handles are cached for the current destination context until explicitly evicted or the
         *     context is invalidated.</li>
         *     <li>{@link JBRSkia#COMMAND_DEFINE_EFFECT_DESCRIPTOR}: {@code [op,
         *     32 + payloadIntCount * 4, 0, handleHigh, handleLow, descriptorType,
         *     descriptorVersion, payloadIntCount, payload0, ...]}, defining a descriptor in the
         *     current destination context. Version 1 tint color-filter descriptors use
         *     {@code descriptorType = COMMAND_EFFECT_DESCRIPTOR_TINT_COLOR_FILTER} and payload
         *     {@code [colorFilterArgb, COMMAND_BLEND_MODE_SRC_IN]}. Version 1 color-matrix
         *     color-filter descriptors use
         *     {@code descriptorType = COMMAND_EFFECT_DESCRIPTOR_COLOR_MATRIX_FILTER} and a
         *     20-int payload containing {@code Float.floatToRawIntBits(...)} for the row-major
         *     4x5 matrix. Version 1 lighting color-filter descriptors use
         *     {@code descriptorType = COMMAND_EFFECT_DESCRIPTOR_LIGHTING_FILTER} and payload
         *     {@code [multiplyArgb, addArgb]}. Version 1 RuntimeEffect color-filter descriptors
         *     use {@code descriptorType = COMMAND_EFFECT_DESCRIPTOR_RUNTIME_COLOR_FILTER} and
         *     payload {@code [skslLength, uniformFloatCount, childCount, namedUniformCount,
         *     namedChildCount, sourceHashHigh, sourceHashLow, childHandleHigh, childHandleLow, ...,
         *     uniformOffset, uniformFloatCount, uniformNameLength, uniformNameChar0, ...,
         *     childIndex, childNameLength, childNameChar0, ..., skslChar0, ..., uniformRawBits0, ...]}.</li>
         *     <li>{@link JBRSkia#COMMAND_DEFINE_SHADER_DESCRIPTOR}: {@code [op,
         *     32 + payloadIntCount * 4, 0, handleHigh, handleLow, descriptorType,
         *     descriptorVersion, payloadIntCount, payload0, ...]}, defining a shader descriptor in
         *     the current destination context. Version 1 color shader descriptors use
         *     {@code descriptorType = COMMAND_SHADER_DESCRIPTOR_COLOR} and payload {@code [argb]}.
         *     Version 1 Perlin/noise shader descriptors use
         *     {@code descriptorType = COMMAND_SHADER_DESCRIPTOR_PERLIN_NOISE} and payload
         *     {@code [kind, baseFrequencyX1000000, baseFrequencyY1000000, numOctaves, seed1000,
         *     tileWidth, tileHeight]}, where {@code kind} is 0 for fractal noise and 1 for turbulence.
         *     Version 1 transform shader descriptors use
         *     {@code descriptorType = COMMAND_SHADER_DESCRIPTOR_TRANSFORM} and payload
         *     {@code [childHandleHigh, childHandleLow, m00, m01, m02, m10, m11, m12, m20, m21, m22]},
         *     where matrix entries are fixed-point values scaled by 1000.</li>
         *     <li>{@link JBRSkia#COMMAND_FILL_RECT_COLOR_FILTER_REF}: {@code [op, 40, flags,
         *     argb, handleHigh, handleLow, x, y, width, height]}, where the handle must be defined
         *     earlier in the same command frame or already cached for the current destination context.</li>
         *     <li>{@link JBRSkia#COMMAND_SAVE_LAYER_COLOR_FILTER_REF}: {@code [op, 40, 0,
         *     x, y, width, height, alpha1000, handleHigh, handleLow]}, where the handle must be defined
         *     earlier in the same command frame or already cached for the current destination context.</li>
         *     <li>{@link JBRSkia#COMMAND_DRAW_IMAGE_REF_COLOR_FILTER_REF}: {@code [op, 76, flags,
         *     srcLeft1000, srcTop1000, srcRight1000, srcBottom1000, dstLeft1000, dstTop1000,
         *     dstRight1000, dstBottom1000, cacheKeyHigh, cacheKeyLow, imageWidth, imageHeight,
         *     alpha1000, filterQuality, handleHigh, handleLow]}, where both the image cache key and
         *     color-filter descriptor handle must be defined for the current destination context.</li>
         *     <li>{@link JBRSkia#COMMAND_SAVE_LAYER_BLEND_COLOR_FILTER_REF}: {@code [op, 44, 0,
         *     x, y, width, height, alpha1000, blendMode, handleHigh, handleLow]}, where
         *     {@code blendMode} is one of the direct Skia blend-mode command values and the handle
         *     must be defined for the current destination context.</li>
         *     <li>{@link JBRSkia#COMMAND_EVICT_COLOR_FILTER_HANDLE}: {@code [op, 20, 0,
         *     handleHigh, handleLow]}, evicting one cached color-filter descriptor from the current
         *     destination context.</li>
         *     <li>{@link JBRSkia#COMMAND_FILL_RECT_IMAGE_SHADER}: {@code [op, 56, flags,
         *     left1000, top1000, right1000, bottom1000, cacheKeyHigh, cacheKeyLow, imageWidth,
         *     imageHeight, tileModeX, tileModeY, alpha1000]}, where the image must already be defined
         *     through {@link JBRSkia#COMMAND_DEFINE_IMAGE_ARGB} in the current destination context.</li>
         *     <li>{@link JBRSkia#COMMAND_STROKE_RECT_SHADER_REF}: {@code [op, 56, flags,
         *     handleHigh, handleLow, left1000, top1000, right1000, bottom1000, strokeWidth1000,
         *     strokeCap, strokeJoin, strokeMiter1000, alpha1000]}, where the shader descriptor handle must
         *     be defined earlier in the same command frame or already cached for the current destination context.</li>
         *     <li>{@link JBRSkia#COMMAND_STROKE_RECT_IMAGE_SHADER}: {@code [op, 72, flags,
         *     left1000, top1000, right1000, bottom1000, cacheKeyHigh, cacheKeyLow, imageWidth, imageHeight,
         *     tileModeX, tileModeY, alpha1000, strokeWidth1000, strokeCap, strokeJoin, strokeMiter1000]},
         *     where the image must already be defined through {@link JBRSkia#COMMAND_DEFINE_IMAGE_ARGB}
         *     in the current destination context.</li>
         *     <li>{@link JBRSkia#COMMAND_DEFINE_FONT_DATA}: {@code [op, 24 + byteCount * 4, 0,
         *     handleHigh, handleLow, byteCount, byte0, ...]}, defining one JBR-owned font-data handle
         *     for simple native text commands. Each byte payload word must be in {@code 0..255}.</li>
         *     <li>{@link JBRSkia#COMMAND_DRAW_TEXT_UTF16}: {@code [op, 48 + (fontFamilyCharCount + charCount) * 4, flags,
         *     x1000, baseline1000, fontSize1000, argb, fontWeight, fontWidth, fontSlant,
     *     fontFamilyCharCount, familyCodeUnit0, ...,
         *     charCount, codeUnit0, ...]}</li>
         *     <li>{@link JBRSkia#COMMAND_CLEAR_IMAGE_CACHE}: {@code [op, 12, 0]}</li>
         *     <li>{@link JBRSkia#COMMAND_EVICT_IMAGE_CACHE_KEY}: {@code [op, 20, 0,
         *     cacheKeyHigh, cacheKeyLow]}</li>
         *     <li>{@link JBRSkia#COMMAND_DRAW_PARAGRAPH_UTF16}: {@code [op, 88 + (fontFamilyCharCount + charCount) * 4, flags,
         *     x1000, y1000, width1000, fontSize1000, argb, fontWeight, fontWidth, fontSlant,
         *     fontFamilyCharCount, familyCodeUnit0, ..., textAlign, textDirection, lineHeightMultiplier1000, maxLines, ellipsisMode,
         *     decorationMask, letterSpacing1000, backgroundSpecified, backgroundArgb, charCount,
         *     codeUnit0, ...]}</li>
         *     <li>{@link JBRSkia#COMMAND_DRAW_POINTS}: {@code [op, 36 + pointCount * 8, flags,
         *     argb, strokeWidth, strokeCap, strokeJoin, strokeMiter1000, pointCount, x0, y0, ...]},
         *     with {@code pointCount} in [1, 4096] and Skia point-mode stroke/cap semantics.</li>
         *     <li>{@link JBRSkia#COMMAND_DRAW_VERTICES}: {@code [op, 32 + vertexCount * 20 + indexCount * 4, flags,
         *     vertexMode, blendMode, paintArgb, vertexCount, indexCount,
         *     x0, y0, ..., texX0Bits, texY0Bits, ..., color0, ..., index0, ...]},
         *     with {@code vertexCount} in [3, 4096], {@code indexCount} in [0, 8192], and texture
         *     coordinates encoded as raw IEEE-754 float bits.</li>
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
