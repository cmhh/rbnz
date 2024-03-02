package org.cmhh

/**
 * Time series frequency.
 */
object frequency {
  sealed trait FREQUENCY
  case object D extends FREQUENCY { override val toString = "D"}
  case object W extends FREQUENCY { override val toString = "W"}
  case object M extends FREQUENCY { override val toString = "M"}
  case object Q extends FREQUENCY { override val toString = "Q"}
  case object Y extends FREQUENCY { override val toString = "Y"}
}