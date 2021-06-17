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
  val listener: OnMatcherListener
) : TextWatcher {

  override fun afterTextChanged(text: Editable?) {
    if (text.isNullOrEmpty()) return
    listener.onApplyStyle(text)
    Log.e("TAG", "onTextChanged:afterTextChanged")

  }

  override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) {
  }

  override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {

    Log.e("TAG", "onTextChanged: text $text")
    Log.e("TAG", "onTextChanged: start $start")
    Log.e("TAG", "onTextChanged: before $before")
    Log.e("TAG", "onTextChanged: count $count")

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
        listener.onMatched(it, target)
      } else {
        listener.onMatched(it, null)
      }
    }
  }
}
