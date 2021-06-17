package com.santalu.textmatcher.internal

import android.text.Editable
import android.text.Spanned
import android.text.method.LinkMovementMethod
import com.santalu.textmatcher.Mention
import com.santalu.textmatcher.OnMatchClickListener
import com.santalu.textmatcher.OnMatchListener
import com.santalu.textmatcher.TextMatcher
import com.santalu.textmatcher.rule.Rule
import com.santalu.textmatcher.style.MentionStyle
import kotlin.math.max

/**
 * Created by fatih.santalu on 9/9/2019
 */

internal class MatcherPresenter(private val view: MatcherView) {

  private val rules = mutableListOf<Rule>()
  private var matcher: TextMatcher? = null
  private val mentions = mutableListOf<Mention>()
  private val style = MentionStyle()

  var matchListener: OnMatchListener? = null
  var matchClickListener: OnMatchClickListener? = null
    set(value) {
      field = value
      setMovementMethod()
    }

  /**
   * Re-instantiates [matcher] with current [rules] and attaches to view
   */
  fun attach() {
    if (rules.isNullOrEmpty()) return
//    matcher = TextMatcher(rules, ::notifyMatch, ::applyStyle)
    view.addTextChangedListener(matcher)
  }

  /**
   * Releases the [matcher] and detaches from view
   */
  fun detach() {
    view.removeTextChangedListener(matcher)
    matcher = null
  }

  /**
   * Triggers [matchListener]
   */
  private fun notifyMatch(rule: Rule, text: String?) {
    matchListener?.invoke(rule, text)
  }

  /**
   * Triggers [matchClickListener]
   */
  fun notifyClick(text: String) {
    matchClickListener?.invoke(text)
  }

  private fun applyStyle(text: Editable) {
    val string = text.toString()
    mentions.toList().forEach {
      if (string.length >= it.offset + it.mentionText.length) {
        val textMentionInEditable = string.substring(it.offset, it.offset + it.mentionText.length)
        if (textMentionInEditable != it.mentionText)
          mentions.remove(it)
      } else {
        mentions.remove(it)
      }

    }

    // clear previous styles in case targets are invalidated
    text.getSpans(0, text.length, style::class.java)
      .forEach {
        text.removeSpan(it)
      }

    mentions.forEach {
      text.setSpan(
        style.clone(),
        it.offset,
        it.offset + it.mentionText.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
      )
    }
  }

  private fun setMovementMethod() {
    val movementMethod =
      if (matchClickListener != null) LinkMovementMethod.getInstance() else view.getMovementMethod()
    view.setMovementMethod(movementMethod)
  }

  /**
   * Detaches and then re-attaches [matcher]
   */
  private fun invalidateMatcher() {
    detach()
    attach()
  }

  /**
   * Refreshes the text without losing cursor position
   */
  private fun invalidateText() {
    val selection = view.getSelectionStart()
    view.setText(view.getText())
    view.setSelection(selection)
  }

  /**
   * Adds [rule] into the [rules] if not already exist
   */
  fun addRule(rule: Rule) {
    if (rules.contains(rule)) return
    rules.add(rule)
    invalidateMatcher()
    invalidateText()
  }

  /**
   * Removes [rule] from to [rules]
   */
  fun removeRule(rule: Rule) {
    if (!rules.contains(rule)) return
    rules.remove(rule)
    invalidateMatcher()
    invalidateText()
  }

  /**
   * Replaces matching target in selection with [newText]
   *
   * Not: does nothing if there's no matching target in selection
   */
  fun replace(newText: String): Boolean {
    matcher?.let {

      val editable = view.getEditableText()
      if (editable.isNullOrEmpty()) {
        view.setText("$newText ")
        return true
      }

      it.rules.forEach { rule ->
        // find closest target's boundaries from selection
        val position = max(0, view.getSelectionStart() - 1)
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

  fun addMention(mention: Mention) {
    mentions.add(mention)
    replace(mention.mentionText)
  }

  fun getOffset(): Int {
    val rule = matcher?.rules?.getOrNull(0) ?: return -1
    val editable = view.getEditableText() ?: return -1
    val position = max(0, view.getSelectionStart() - 1)
    return rule.getTargetStart(editable, position)
  }

  fun getMentions() = mentions

}