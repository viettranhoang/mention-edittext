package com.santalu.textmatcher.widget

import android.content.Context
import android.text.Editable
import android.text.Spanned
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatEditText
import com.santalu.textmatcher.Mention
import com.santalu.textmatcher.OnMatchListener
import com.santalu.textmatcher.OnMatcherListener
import com.santalu.textmatcher.TextMatcher
import com.santalu.textmatcher.rule.HashtagRule
import com.santalu.textmatcher.rule.MentionRule
import com.santalu.textmatcher.rule.Rule
import com.santalu.textmatcher.style.MentionStyle
import kotlin.math.max

/**
 * @author vietth
 * @since 17/06/2021
 */
class PostDetailsCommentEditText : AppCompatEditText, OnMatcherListener {

  constructor(context: Context?) : super(context)

  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

  constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
      super(context, attrs, defStyleAttr)

  private var matcher: TextMatcher? = null
  private val style = MentionStyle()
  private val rules = listOf(MentionRule(), HashtagRule())
  private var matchListener: OnMatchListener? = null
  private val mentions = mutableListOf<Mention>()

  init {
    matcher = TextMatcher(rules, this)
    addTextChangedListener(matcher)
  }

  fun setOnMatchListener(listener: OnMatchListener?) {
    this.matchListener = listener
  }

  fun addMention(mentionId: String, mentionText: String) {
    val offset = getOffset()
    if (offset != -1 && replace(mentionText)) {
      mentions.add(Mention(mentionId, mentionText, offset))
      onApplyStyle(editableText)
    }
  }

  fun getMentions() = mentions

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    removeTextChangedListener(matcher)
    matcher = null
  }

  override fun onMatched(rule: Rule, textMatched: String?) {
    matchListener?.invoke(rule, textMatched)
  }

  override fun onApplyStyle(editable: Editable) {
    // clear previous styles in case targets are invalidated
    editable.getSpans(0, editable.length, style::class.java)
      .forEach {
        editable.removeSpan(it)
      }

    mentions.forEach {
      editable.setSpan(
        style.clone(),
        it.offset,
        it.offset + it.mentionText.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
      )
    }
  }

  override fun onUpdatePosition(start: Int, before: Int, count: Int) {
    with(mentions.iterator()) {
      forEach { mention ->
        Log.e(
          "TAG",
          "onUpdatePosition: start $start before $before count $count offset ${mention.offset} length ${mention.mentionText.length}"
        )

        val startPosition = start + before


        val endPosition = if (before > count)
          (mention.offset + mention.mentionText.length + 1)
        else
          (mention.offset + mention.mentionText.length)

        if (startPosition > mention.offset && startPosition < endPosition) {
          remove()
        } else if (startPosition <= mention.offset) {
          mention.offset += (count - before)
        }
      }
    }
  }

  private fun getOffset(): Int {
    val position = max(0, selectionStart - 1)
    return rules[0].getTargetStart(editableText, position)
  }

  /**
   * Replaces matching target in selection with [newText]
   *
   * Not: does nothing if there's no matching target in selection
   */
  private fun replace(newText: String): Boolean {
    matcher?.let {
      val editable = editableText
      if (editable.isNullOrEmpty()) {
        setText("$newText ")
        return true
      }

      it.rules.forEach { rule ->
        // find closest target's boundaries from selection
        val position = max(0, selectionStart - 1)
        val start = rule.getTargetStart(editable, position)
        val end = rule.getTargetEnd(editable, position)
        val target = editable.substring(start, end)

        if (rule.isMatches(target)) {
          editable.replace(start, end, newText)
          // add whitespace at the end if needed
          val cursor = start + newText.length
          val following = editable.getOrNull(cursor)
          if (following == null || !following.isWhitespace()) {
            editable.insert(cursor, " ")
          }
          return true
        }
      }

      return false
    }

    return false
  }

}

