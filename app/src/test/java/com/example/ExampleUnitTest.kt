package com.example

import com.example.model.CallDirection
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.model.CallingConfig
import com.example.model.ConnectionState
import com.example.model.DeliveryStatus
import com.example.model.FirestoreMessage
import com.example.model.Message
import com.example.model.MessageType
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testCallingConfig_stunAndTurnServersConfigured() {
    val config = CallingConfig()
    assertTrue("STUN servers must be configured", config.stunServers.isNotEmpty())
    assertTrue("TURN servers must be configured for NAT traversal", config.turnServers.isNotEmpty())
    assertTrue(config.stunServers.any { it.contains("stun.l.google.com") })
    assertTrue(config.turnServers.any { it.uri.contains("openrelay.metered.ca") })
  }

  @Test
  fun testAudioMessage_hasMediaUrlAndDuration() {
    val msg = Message(
      id = "msg_audio_123",
      conversationId = "conv_1",
      senderId = "user_1",
      senderName = "Alice",
      content = "Voice memo (5s)",
      timestamp = System.currentTimeMillis(),
      formattedTime = "12:00",
      type = MessageType.AUDIO,
      deliveryStatus = DeliveryStatus.SENT,
      isOutgoing = true,
      mediaDurationSeconds = 5,
      mediaFileName = "voice_123.m4a",
      mediaFileSize = "40 KB (AAC)",
      mediaUrl = "https://firebasestorage.googleapis.com/v0/b/bucket/o/voice.m4a"
    )

    assertEquals(MessageType.AUDIO, msg.type)
    assertEquals(5, msg.mediaDurationSeconds)
    assertTrue(msg.mediaUrl.startsWith("https://"))
    assertTrue(msg.mediaFileName.endsWith(".m4a"))
  }

  @Test
  fun testFirestoreMessage_serializationFields() {
    val fm = FirestoreMessage(
      messageId = "fm_1",
      conversationId = "conv_1",
      senderId = "user_1",
      receiverId = "user_2",
      messageText = "Voice memo",
      messageType = "audio",
      mediaFileName = "voice_1.m4a",
      mediaFileSize = "32 KB",
      mediaDuration = 4,
      mediaUrl = "https://storage.googleapis.com/test.m4a"
    )

    assertEquals("audio", fm.messageType)
    assertEquals(4, fm.mediaDuration)
    assertEquals("https://storage.googleapis.com/test.m4a", fm.mediaUrl)
  }

  @Test
  fun testCallRecord_creationAndFormatting() {
    val callRecord = CallRecord(
      id = "call_test_1",
      contactId = "user_peer",
      contactName = "Bob",
      contactAvatarInitials = "B",
      avatarColorHex = 0xFF00F0FF,
      callType = CallType.VIDEO,
      direction = CallDirection.OUTGOING,
      timestamp = System.currentTimeMillis(),
      formattedDate = "Just now",
      durationSeconds = 65,
      formattedDuration = "01:05",
      networkQualityRating = "HD Audio • 48kbps"
    )

    assertEquals(CallType.VIDEO, callRecord.callType)
    assertEquals(CallDirection.OUTGOING, callRecord.direction)
    assertEquals("01:05", callRecord.formattedDuration)
    assertEquals(65, callRecord.durationSeconds)
  }

  @Test
  fun testNetworkQualityMetrics_pingAndPacketLoss() {
    val statsMetrics = com.example.model.NetworkQualityMetrics(
      bars = 4,
      tier = com.example.model.NetworkTier.EXCELLENT,
      rttMs = 32,
      packetLossPercent = 0.15f,
      jitterMs = 3,
      currentAudioBitrateKbps = 48
    )

    assertEquals(32, statsMetrics.rttMs)
    assertEquals(0.15f, statsMetrics.packetLossPercent, 0.001f)
    assertEquals(3, statsMetrics.jitterMs)
    assertEquals(4, statsMetrics.bars)
    assertEquals(com.example.model.NetworkTier.EXCELLENT, statsMetrics.tier)
  }
}

