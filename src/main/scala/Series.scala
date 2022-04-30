package org.cmhh

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Basic time series representation.
 */
case class Series(data: Vector[Obs]) {
  /**
   * Ordered dates.
   */
  lazy val dates: Vector[LocalDate] = data.map(_._1).sorted

  /**
   * Time series frequency
   */
  lazy val freq: frequency.FREQUENCY = {
    val d = dates.sorted
    val e = d.drop(1).zip(d.dropRight(1)).map(x => ChronoUnit.DAYS.between(x._2, x._1))
    val f = e.sum / e.size

    if (e.min == 1) frequency.D
    else if (e.min == 7) frequency.W
    else if (f <= 31) frequency.M
    else if (f <= 92) frequency.Q
    else frequency.Y
  }

  /**
   * getter
   *
   * @param i index
   * @return observation at index i
   */
  def apply(i: Int): Obs = data(i)

  override lazy val toString: String = {
    def pr(x: Option[Double]): String = x match {
      case None => "."
      case Some(v) => v.toString
    }

    def f(xs: Vector[Obs]) = xs
      .map(x => s"${x._1}\t${pr(x._2)}")
      .mkString("\n")

    if (data.size <= 10) f(data)
    else f(data.take(5)) + "\n....\n" + f(data.takeRight(5))
  }
}