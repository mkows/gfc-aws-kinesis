package com.gilt.gfc.aws.kinesis.akka

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration
import com.gilt.gfc.aws.kinesis.client.{KCLConfiguration, KCLWorkerRunner, KinesisRecordReader}

class KinesisStreamConsumer[T](
  streamConfig: KinesisStreamConsumerConfig[T],
  handler: KinesisStreamHandler[T]
) (
  implicit private val evReader: KinesisRecordReader[T]
) {

  private val maxRecords: Int = streamConfig.maxRecordsPerBatch.orElse(
    streamConfig.dynamoDBKinesisAdapterClient.map(_ => 1000)
  ).getOrElse(KinesisClientLibConfiguration.DEFAULT_MAX_RECORDS)

  private val kclConfig = KCLConfiguration(
    streamConfig.applicationName,
    streamConfig.streamName,
    streamConfig.kinesisCredentialsProvider,
    streamConfig.dynamoCredentialsProvider,
    streamConfig.cloudWatchCredentialsProvider,
    streamConfig.regionName,
    streamConfig.initialPositionInStream,
    streamConfig.kinesisClientEndpoints,
    streamConfig.failoverTimeoutMillis,
    maxRecords,
    streamConfig.idleTimeBetweenReads
  )

  private def createWorker = KCLWorkerRunner(
    kclConfig,
    dynamoDBKinesisAdapter = streamConfig.dynamoDBKinesisAdapterClient,
    metricsFactory = Some(streamConfig.metricsFactory),
    checkpointInterval = streamConfig.checkPointInterval,
    initialize = handler.onInit,
    shutdown = handler.onShutdown,
    initialRetryDelay = streamConfig.retryConfig.initialDelay,
    maxRetryDelay = streamConfig.retryConfig.retryDelay,
    numRetries = streamConfig.retryConfig.maxRetries
  )

  /***
    * Creates the worker and runs it
    */
  def run() = {
    val worker = createWorker
    worker.runSingleRecordProcessor(handler.onRecord)
  }
}

