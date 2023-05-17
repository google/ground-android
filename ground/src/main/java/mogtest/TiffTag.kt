/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mogtest

import java.io.*

enum class TiffTag(val id: Int, val isArray: Boolean) {
  Artist(315, false),
  BitsPerSample(258, true),
  CellLength(265, false),
  CellWidth(264, false),
  ColorMap(320, false),
  Compression(259, false),
  Copyright(33432, false),
  DateTime(306, false),
  ExtraSamples(338, true),
  FillOrder(266, false),
  FreeByteCounts(289, false),
  FreeOffsets(288, false),
  GrayResponseCurve(291, false),
  GrayResponseUnit(290, false),
  HostComputer(316, false),
  ImageDescription(270, false),
  ImageLength(257, false),
  ImageWidth(256, false),
  Make(271, false),
  MaxSampleValue(281, false),
  MinSampleValue(280, false),
  Model(272, false),
  NewSubfileType(254, false),
  Orientation(274, false),
  PhotometricInterpretation(262, false),
  PlanarConfiguration(284, false),
  ResolutionUnit(296, false),
  RowsPerStrip(278, false),
  SamplesPerPixel(277, false),
  Software(305, false),
  StripByteCounts(279, true),
  StripOffsets(273, true),
  SubfileType(255, false),
  Threshholding(263, false),
  XResolution(282, false),
  YResolution(283, false), // TIFF Extended
  BadFaxLines(326, false),
  CleanFaxData(327, false),
  ClipPath(343, false),
  ConsecutiveBadFaxLines(328, false),
  Decode(433, false),
  DefaultImageColor(434, false),
  DocumentName(269, false),
  DotRange(336, false),
  HalftoneHints(321, false),
  Indexed(346, false),
  JPEGTables(347, false),
  PageName(285, false),
  PageNumber(297, false),
  Predictor(317, false),
  PrimaryChromaticities(319, false),
  ReferenceBlackWhite(532, false),
  SampleFormat(339, true),
  SMinSampleValue(340, false),
  SMaxSampleValue(341, false),
  StripRowCounts(559, true),
  SubIFDs(330, false),
  T4Options(292, false),
  T6Options(293, false),
  TileByteCounts(325, true),
  TileLength(323, false),
  TileOffsets(324, true),
  TileWidth(322, false),
  TransferFunction(301, false),
  WhitePoint(318, false),
  XClipPathUnits(344, false),
  XPosition(286, false),
  YCbCrCoefficients(529, false),
  YCbCrPositioning(531, false),
  YCbCrSubSampling(530, false),
  YClipPathUnits(345, false),
  YPosition(287, false), // JPEG
  JPEGProc(512, false),
  JPEGInterchangeFormat(513, false),
  JPEGInterchangeFormatLength(514, false),
  JPEGRestartInterval(515, false),
  JPEGLosslessPredictors(517, true),
  JPEGPointTransforms(518, true),
  JPEGQTables(519, true),
  JPEGDCTables(520, true),
  JPEGACTables(521, true), // EXIF
  ApertureValue(37378, false),
  ColorSpace(40961, false),
  DateTimeDigitized(36868, false),
  DateTimeOriginal(36867, false),
  ExifIFD(34665, false),
  ExifVersion(36864, false),
  ExposureTime(33434, false),
  FileSource(41728, false),
  Flash(37385, false),
  FlashpixVersion(40960, false),
  FNumber(33437, false),
  ImageUniqueID(42016, false),
  LightSource(37384, false),
  MakerNote(37500, false),
  ShutterSpeedValue(37377, false),
  UserComment(37510, false), // IPTC
  IPTC(33723, false), // ICC
  ICCProfile(34675, false), // XMP
  XMP(700, false), // GDAL
  GDAL_METADATA(42112, false),
  GDAL_NODATA(42113, false), // Photoshop
  Photoshop(34377, false), // GeoTiff
  ModelPixelScale(33550, false),
  ModelTiepoint(33922, false),
  ModelTransformation(34264, false),
  GeoKeyDirectory(34735, false),
  GeoDoubleParams(34736, false),
  GeoAsciiParams(34737, false);
  /**
   * Get the tag id
   *
   * @return tag id
   */
  companion object {
    /** Mapping between ids and field tag types */
    private val idMapping: MutableMap<Int, TiffTag> = HashMap()

    /** Load the id mapping */
    init {
      for (fieldTag in values()) {
        idMapping[fieldTag.id] = fieldTag
      }
    }

    /**
     * Get a field tag type by id
     *
     * @param id tag id
     * @return field tag type
     */
    fun byId(id: Int): TiffTag? {
      return idMapping[id]
    }
  }
}
