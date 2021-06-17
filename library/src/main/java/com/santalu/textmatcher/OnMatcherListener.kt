package com.santalu.textmatcher

import android.text.Editable
import com.santalu.textmatcher.rule.Rule

/**
 * @author vietth
 * @since 17/06/2021
 */
interface OnMatcherListener {
  fun onMatched(rule: Rule, textMatched: String?)

  fun onApplyStyle(editable: Editable)

  fun onUpdatePosition(start: Int, before: Int, count: Int)
}