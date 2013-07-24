package pl.edu.icm.coansys.citations.util

import pl.edu.icm.coansys.citations.util.libsvm_util._
import org.testng.annotations.Test
import org.testng.Assert._

/**
 * @author Mateusz Fedoryszak (m.fedoryszak@icm.edu.pl)
 */
class libsvm_utilTest {
  @Test(groups = Array("fast"))
  def featureVectorToLibSvmLineTest() {
    val fv = Array(1.0, 0.0)

    val line = featureVectorToLibSvmLine(fv, 1)
    assertEquals(line, "1 1:1.0 2:0.0")
  }
}
