package com.diffbot.wikistatsextractor

import com.diffbot.wikistatsextractor.util.Util
import com.diffbot.wikistatsextractor.util.Util.PairUriSF
import org.junit.Test

import scala.io.Source
import scala.collection.JavaConverters.asScalaBufferConverter

class UtilTest {
  val text = Source.fromFile("src/test/resources/anarchism").getLines().mkString("\n")
  var MAX_LENGTH_PARAGRAPH: Int = 5000
  var MAX_LENGTH_SF: Int = 80
  var MIN_LENGTH_SF: Int = 2
  var MAX_NB_TOKEN_SF: Int = 6
  var LANGUAGE: String = "en"

  val cleanText = Util.getCleanTextFromPage(text, false, false, false, false).asScala
  @Test
  def cleanTest() = {
    cleanText.foreach(println)
  }

  @Test
  def sfFormsTest() = {
    val acc = collection.mutable.Map.empty[PairUriSF, Int]
    val bestSF = collection.mutable.Map.empty[String, PairUriSF]
    for ( ct <- cleanText) {
      val sf = Util.getSurfaceFormsInString(ct, MAX_LENGTH_SF, MIN_LENGTH_SF, MAX_NB_TOKEN_SF, LANGUAGE).asScala
      if(sf.nonEmpty){
//        println(ct)
        sf.foreach {
          s => acc.put(s, acc.getOrElse(s, 0) + 1)
        }
      }
    }
    acc.toList.sortBy(_._2).foreach(println)

    acc.map{
      case (k,v) => k.surface_form -> if(bestSF.contains(k.surface_form) && acc.getOrElse(bestSF(k.surface_form),0) > acc(k) ) k else
    }
  }
}
