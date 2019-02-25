package com.strikingly.data.logappender

import java.io.Serializable

import awscala.sqs.{Queue, SQS}
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import org.apache.logging.log4j.core.{AbstractLifeCycle, Filter, Layout, LogEvent}
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.plugins.PluginAttribute
import org.apache.logging.log4j.core.config.plugins.PluginElement
import org.apache.logging.log4j.core.config.plugins.PluginFactory
import org.apache.logging.log4j.status.StatusLogger
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object SQSService {
  lazy val region = {
    val _region = System.getProperty("AWS_REGION")
    if (_region == null) "cn-north-1"
    else _region
  }
  import awscala.Region
  implicit lazy val sqs = SQS.at(Region(region))

  def getQueue(name: String): Queue = {
    sqs.queue(name) match {
      case Some(queue) => queue
      case None =>
        println("create new queue")
        sqs.createQueueAndReturnQueueName(name)
    }
  }
}

object SqsAppender {
lazy val sqsClient = {
    val region = {
      val _region = System.getProperty("AWS_REGION")
      if (_region == null) "cn-north-1"
      else _region
    }
  }
  var sqsUrl: String = null
  var queue: Queue = null

  @PluginFactory
  def createAppender(@PluginAttribute("name") name: String,
                     @PluginAttribute("QueueName") queueName: String,
                     @PluginElement("Filter") filter: Filter,
                     @PluginElement("Layout") layout: Layout[_ <: Serializable],
                     @PluginAttribute("ignoreExceptions") ignoreExceptions: Boolean): SqsAppender = {
    if (queueName == null) {
      StatusLogger.getLogger.error("no queue name provided")
      return null
    }

    StatusLogger.getLogger.info(s"get SQS queue name: $queueName")
    if (queue == null) {
      queue = SQSService.getQueue(queueName)
    }

//    if (SqsAppender.sqsUrl == null) try
//      SqsAppender.sqsUrl = SqsAppender.sqsClient.getQueueUrl(queueName).getQueueUrl
//    catch {
//      case e: Exception =>
//        println(s"queue name error: $e")
//    }
//    println(queueName)
    new SqsAppender(name, filter, layout, ignoreExceptions)
  }
}

@Plugin(name = "SqsAppender", category = "Core", elementType = "appender", printObject = true)
class SqsAppender(name: String, filter: Filter, layout: Layout[_ <: Serializable],
                  ignoreExceptions: Boolean) extends AbstractAppender(name, filter, layout, ignoreExceptions) {
  override def append(event: LogEvent): Unit = {
    import SQSService.sqs
    if (SqsAppender.queue != null) {
      Future {
        SqsAppender.queue.add(getLayout.toSerializable(event).toString)
      }
    }
//    if (SqsAppender.sqsUrl != null) {
//      SqsAppender.sqsClient.sendMessage(new SendMessageRequest(SqsAppender.sqsUrl, getLayout.toSerializable(event).toString))
//    }
  }


}

