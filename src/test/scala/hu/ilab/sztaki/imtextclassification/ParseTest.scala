package hu.ilab.sztaki.imtextclassification

import breeze.linalg.SparseVector
import hu.sztaki.ilab.imtextclassification.PassiveAggressiveMultiClassTraining
import org.scalatest._

class ParseTest extends FlatSpec with Matchers {

  import PassiveAggressiveMultiClassTraining._

  "Parser" should "parse labeled vec" in {

    val line = "123 4:0.3 8:0.4"
    val vecLength = 20

    parseLabeled(vecLength)(line) should be ((SparseVector[Double](vecLength)((4, 0.3), (8, 0.4)), 123))

  }

  it should "parse unlabeled vec with id" in {

    val line = "4:0.3 8:0.4;10"
    val vecLength = 20

    parseUnlabeledWithId(vecLength)(line) should be ((10, SparseVector[Double](vecLength)((4, 0.3), (8, 0.4))))
  }

  it should "write and parse model param" in {

    val paramWithId = (10, Array(0.1,0.2,0.3,0.4))
    val line = "10;0.1,0.2,0.3,0.4"
    val vecLength = 4

    writeParameter(paramWithId) should be (line)
    val parsed = readParameter(vecLength)(line)

    parsed._1 should be (paramWithId._1)
    parsed._2.toSeq should be (paramWithId._2.toSeq)
  }

}
