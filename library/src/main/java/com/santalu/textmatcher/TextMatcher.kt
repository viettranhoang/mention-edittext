package com.santalu.textmatcher

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import com.santalu.textmatcher.rule.Rule

/**
 *
 * Simple text watcher matches appropriate targets according to given [rules]
 */

class TextMatcher(
  val rules: List<Rule>,
  private val listener: OnMatcherListener
) : TextWatcher {

  override fun afterTextChanged(text: Editable?) {
    if (text.isNullOrEmpty()) return
    rules.forEach {
      it.applyStyle(text)
    }
    listener.onApplyStyle(text)
  }

  override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) {
  }

  override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {

    listener.onUpdatePosition(start, before, count)

    rules.forEach {
      if (text.isNullOrEmpty()) {
        listener.onMatched(it, null)
        return@forEach
      }

      val position = if (start > 0 && before > count) start - 1 else start

      // find closest target's boundaries from selection
      val targetStart = it.getTargetStart(text, position)
      val targetEnd = it.getTargetEnd(text, position)
      val target = text.substring(targetStart, targetEnd)

      if (it.isMatches(target)) {
        listener.onMatched(it, target.removeRange(0, 1))
      } else {
        listener.onMatched(it, null)
      }
    }
  }
}
