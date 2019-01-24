import org.slf4j.LoggerFactory

object testSqsAppender {
//  val logger = LoggerFactory.getLogger("SQSTestLogger")
  val logger = LoggerFactory.getLogger(this.getClass.getName)

  def main(args: Array[String]): Unit = {
    while (true) {
      logger.error("hello world")
      logger.error("hello world again")
      Thread.sleep(10000)
    }

  }
}
