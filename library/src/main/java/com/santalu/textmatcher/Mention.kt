package com.santalu.textmatcher

data class Mention(
  val mentionId: String,
  val mentionText: String,
  var offset: Int
)