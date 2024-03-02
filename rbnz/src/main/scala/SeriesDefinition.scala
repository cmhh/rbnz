package org.cmhh

/**
 * Case class representing series definition.
 */
case class SeriesDefinition(
  group: String, seriesName: String, seriesId: String, unit: Option[String], note: Option[String]
)

case object SeriesDefinition {
  def apply(group: String, seriesName: String, seriesId: String, unit: String, note: String): SeriesDefinition =
    SeriesDefinition(group, seriesName, seriesId, Some(unit), Some(note))

  def apply(group: String, seriesName: String, seriesId: String, unit: String): SeriesDefinition =
    SeriesDefinition(group, seriesName, seriesId, Some(unit), None)
}