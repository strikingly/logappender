package com.strikingly.data.logappender

import java.io.Serializable

import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import org.apache.logging.log4j.core.{AbstractLifeCycle, Filter, Layout, LogEvent}
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.plugins.PluginAttribute
import org.apache.logging.log4j.core.config.plugins.PluginElement
import org.apache.logging.log4j.core.config.plugins.PluginFactory
import org.apache.logging.log4j.status.StatusLogger

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

//object SQSService {
//  lazy val region = {
//    val _region = System.getProperty("AWS_REGION")
//    if (_region == null) "cn-north-1"
//    else _region
//  }
//  import awscala.Region
//  implicit lazy val sqs = SQS.at(Region(region))
//
//  def getQueue(name: String): Queue = {
//    sqs.queue(name) match {
//      case Some(queue) => queue
//      case None =>
//        println("create new queue")
//        sqs.createQueueAndReturnQueueName(name)
//    }
//  }
//}

object SqsAppender {
  lazy val sqsClient = {
    val region = {
      val _region = System.getProperty("AWS_REGION")
      if (_region == null) "cn-north-1"
      else _region
    }
    AmazonSQSClientBuilder.standard().withRegion(region).build()
  }
  var sqsUrl: String = null

  def initSqsUrl(queueName: String) = {
    if (SqsAppender.sqsUrl == null) try
      SqsAppender.sqsUrl = SqsAppender.sqsClient.getQueueUrl(queueName).getQueueUrl
    catch {
      case e: Exception =>
        StatusLogger.getLogger.error(s"queue name error: $e")
    }
  }

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
    initSqsUrl(queueName)

    //    if (queue == null) {
    //      queue = SQSService.getQueue(queueName)
    //    }

    new SqsAppender(name, filter, layout, ignoreExceptions)
  }
}

@Plugin(name = "SqsAppender", category = "Core", elementType = "appender", printObject = true)
class SqsAppender(name: String, filter: Filter, layout: Layout[_ <: Serializable],
                  ignoreExceptions: Boolean) extends AbstractAppender(name, filter, layout, ignoreExceptions) {
  val logMsgQueue = new mutable.Queue[String]()
  override def append(event: LogEvent): Unit = {
    import com.amazonaws.services.sqs.model.SendMessageRequest
    if (SqsAppender.sqsUrl != null) {
      val msg = getLayout.toSerializable(event).toString
      Future {
        SqsAppender.sqsClient.sendMessage(new SendMessageRequest(SqsAppender.sqsUrl, msg))
      }
    }
    //    import SQSService.sqs
    //    if (SqsAppender.queue != null) {
    //      logMsgQueue.enqueue(getLayout.toSerializable(event).toString)
    //      Future {
    //        while (logMsgQueue.nonEmpty) {
    //          Future { SqsAppender.queue.add(logMsgQueue.dequeue()) }
    //        }
    //      }
    //    }
    //    if (SqsAppender.sqsUrl != null) {
    //      SqsAppender.sqsClient.sendMessage(new SendMessageRequest(SqsAppender.sqsUrl, getLayout.toSerializable(event).toString))
    //    }
  }
}


