package com.gilt.gfc.aws.kinesis.client

import java.util.UUID
import scala.concurrent.duration._

import com.amazonaws.auth.{AWSCredentialsProvider, DefaultAWSCredentialsProviderChain}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{InitialPositionInStream, KinesisClientLibConfiguration}

/** Configures KCL
  *
  * http://docs.aws.amazon.com/kinesis/latest/dev/kinesis-record-processor-implementation-app-java.html
  * https://github.com/aws/aws-sdk-java/blob/master/src/samples/AmazonKinesisApplication/SampleKinesisApplication.java
  *
  * https://github.com/awslabs/amazon-kinesis-client
  */
case class KinesisClientEndpoints(dynamoDBEndpoint: String, kinesisEndpoint: String)
object KCLConfiguration {

  val HostName = {
    import scala.sys.process._
    "hostname".!!.trim()
  }

  assert( HostName != null   , "Couldn't determine hostname, got null" )
  assert( ! HostName.isEmpty , "Couldn't determine hostname, got empty string" )

  /** Provides some initial config, can be further customized.
    * Mainly a point of reference for imports and doc links.
    *
    * @param applicationName name of the consumer
    *
    * @param streamName kinesis stream name
    */
  def apply(applicationName: String
            , streamName: String
            , kinesisCredentialsProvider: AWSCredentialsProvider = new DefaultAWSCredentialsProviderChain()
            , dynamoCredentialsProvider: AWSCredentialsProvider = new DefaultAWSCredentialsProviderChain()
            , cloudWatchCredentialsProvider: AWSCredentialsProvider = new DefaultAWSCredentialsProviderChain()
            , regionName: Option[String] = None
            , initialPositionInStream: InitialPositionInStream = InitialPositionInStream.LATEST
            , endpointConfiguration: Option[KinesisClientEndpoints] = None
            , failoverTimeoutMillis: Long = KinesisClientLibConfiguration.DEFAULT_FAILOVER_TIME_MILLIS
            , maxRecordsPerBatch: Int = KinesisClientLibConfiguration.DEFAULT_MAX_RECORDS
            , idleTimeBetweenReads: FiniteDuration = KinesisClientLibConfiguration.DEFAULT_IDLETIME_BETWEEN_READS_MILLIS.millis): KinesisClientLibConfiguration = {

    val dynamoTableName = (s"${applicationName}.${streamName}")
      .replaceAll("[^a-zA-Z0-9_.-]", "-")

    val conf = new KinesisClientLibConfiguration(
      dynamoTableName,
      streamName,
      kinesisCredentialsProvider,
      dynamoCredentialsProvider,
      cloudWatchCredentialsProvider,
      s"${HostName}:${UUID.randomUUID()}"
    ).withRegionName(regionName.orNull)
     .withInitialPositionInStream(initialPositionInStream)
     .withFailoverTimeMillis(failoverTimeoutMillis)
     .withMaxRecords(maxRecordsPerBatch)
     .withIdleTimeBetweenReadsInMillis(idleTimeBetweenReads.toMillis)

    endpointConfiguration.fold(conf)( endpoints =>
      conf.withDynamoDBEndpoint(endpoints.dynamoDBEndpoint)
          .withKinesisEndpoint(endpoints.kinesisEndpoint)
    )

  }
}
