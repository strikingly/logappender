package com.strikingly.data.logappender

import java.io.Serializable

import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSClientBuilder}
import org.apache.logging.log4j.core.{Filter, Layout, LogEvent}
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.plugins.PluginAttribute
import org.apache.logging.log4j.core.config.plugins.PluginElement
import org.apache.logging.log4j.core.config.plugins.PluginFactory
import org.apache.logging.log4j.status.StatusLogger

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

object SqsAppender {
  lazy val sqsClient: AmazonSQS = {
    val region = {
      val _region = System.getProperty("AWS_REGION")
      if (_region == null) "cn-north-1"
      else _region
    }
    AmazonSQSClientBuilder.standard().withRegion(region).build()
  }
  var sqsUrl: Option[String] = None

  def initSqsUrl(queueName: String): Unit = {
    if (sqsUrl.isEmpty) {
      val url = Try[String] {
        SqsAppender.sqsClient.getQueueUrl(queueName).getQueueUrl
      }
      sqsUrl = Option(url.getOrElse(null))
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

    StatusLogger.getLogger.info(s"Get SQS queue name: $queueName")
    initSqsUrl(queueName)

    new SqsAppender(name, filter, layout, ignoreExceptions)
  }
}

@Plugin(name = "SqsAppender", category = "Core", elementType = "appender", printObject = true)
class SqsAppender(name: String, filter: Filter, layout: Layout[_ <: Serializable],
                  ignoreExceptions: Boolean) extends AbstractAppender(name, filter, layout, ignoreExceptions) {
  override def append(event: LogEvent): Unit = {
    import com.amazonaws.services.sqs.model.SendMessageRequest

    SqsAppender.sqsUrl.foreach { url =>
      val msg = getLayout.toSerializable(event).toString
      Future { SqsAppender.sqsClient.sendMessage(new SendMessageRequest(url, msg)) }
    }
  }
}
