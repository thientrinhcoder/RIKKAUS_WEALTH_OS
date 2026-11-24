# Every observation of an insights body recomputed from the wealth snapshot of the same user, read in
# the same minute. Input: the insights body; $snap: the snapshot body. Prints named checks, all true.
. as $i
| def rule($id): $i.insights[] | select(.ruleId == $id);
def frac($n; $d): if $d == null or $d == 0 then null else (($n / $d) * 1000000 | round) / 1000000 end;
def days($from; $to): (($to + "T00:00:00Z" | fromdateiso8601) - ($from + "T00:00:00Z" | fromdateiso8601)) / 86400;
($snap.kpis.totalAssets.amountVnd) as $total
| ($snap.assetsByCategory[0] // null) as $top
| ([$snap.assetsByLiquidity[] | select(.liquidity == "LOW") | .amountVnd] | first // 0) as $low
| rule("CATEGORY_CONCENTRATION") as $cc
| rule("DEBT_TO_ASSET_RATIO") as $dr
| rule("LOW_LIQUIDITY_SHARE") as $ll
| rule("UPCOMING_LIABILITY_DUE") as $ud
| rule("STALE_VALUATION") as $sv
| rule("STALE_EXCHANGE_RATE") as $sr
| ([$snap.assets[].id] + [$snap.liabilities[].id]) as $ids
| {
  same_day_and_complete: ($i.asOfDate == $snap.asOfDate and $i.complete == $snap.complete),
  six_rules_in_order: ([$i.insights[].ruleId] == ["CATEGORY_CONCENTRATION", "DEBT_TO_ASSET_RATIO",
      "LOW_LIQUIDITY_SHARE", "UPCOMING_LIABILITY_DUE", "STALE_VALUATION", "STALE_EXCHANGE_RATE"]),
  observed_null_iff_unavailable: ($i.insights | all((.observed == null) == (.status == "UNAVAILABLE")
      and (.unavailableReason == null) == (.status != "UNAVAILABLE"))),
  status_follows_comparison: ($i.insights | all(.status == "UNAVAILABLE" or
      (.status == (if (.threshold.comparison == "GREATER_THAN" and .observed.value > .threshold.value)
                     or (.threshold.comparison == "AT_MOST" and .observed.value <= .threshold.value)
                   then "ATTENTION" else "OK" end)))),
  concentration_from_largest_category: ($cc.status == "UNAVAILABLE" or
      ($cc.observed.subject == $top.category and $cc.observed.numeratorVnd == $top.amountVnd
       and $cc.observed.denominatorVnd == $total and $cc.observed.value == frac($top.amountVnd; $total)
       and ([$cc.evidence[].sourceId] | sort) == ([$snap.assets[] | select(.category == $top.category) | .id] | sort))),
  debt_ratio_eq_snapshot_kpi: ($dr.status == "UNAVAILABLE" or
      ($dr.observed.value == $snap.kpis.debtToAssetRatio and $dr.observed.numeratorVnd == $snap.kpis.totalLiabilities.amountVnd
       and $dr.observed.denominatorVnd == $total and [$dr.evidence[].sourceId] == [$snap.liabilities[].id])),
  low_liquidity_from_low_group: ($ll.status == "UNAVAILABLE" or
      ($ll.observed.numeratorVnd == $low and $ll.observed.denominatorVnd == $total and $ll.observed.value == frac($low; $total)
       and [$ll.evidence[].sourceId] == [$snap.assets[] | select(.liquidity == "LOW") | .id])),
  share_rules_unavailable_iff_total_unknown_or_zero: ([$cc, $dr, $ll] | all((.status == "UNAVAILABLE")
      == ($total == null or $total == 0 or ($snap.assets | length) == 0 or $snap.kpis.totalLiabilities.amountVnd == null and .ruleId == "DEBT_TO_ASSET_RATIO"))),
  due_evidence_eq_snapshot_window: ($ud.status != "ATTENTION" or $i.thresholds.dueWindowDays != 30 or
      ([$ud.evidence[] | [.sourceId, .date, .amount, .amountVnd, .overdue]]
       == [$snap.upcomingLiabilityDues.items[] | [.liabilityId, .dueDate, .amount, .amountVnd, .overdue]])),
  due_observed_is_nearest_date: ($ud.status == "UNAVAILABLE" or
      ($ud.observed.date == ([$snap.liabilities[] | .nextPaymentDate, .maturityDate | select(. != null)] | min)
       and $ud.observed.value == days($i.asOfDate; $ud.observed.date))),
  valuation_observed_is_oldest: ($sv.status == "UNAVAILABLE" or
      ($sv.observed.date == ([$snap.assets[].valuationDate] | min) and $sv.observed.value == days($sv.observed.date; $i.asOfDate))),
  rate_observed_is_declared_date: (if $snap.exchangeRate == null then $sr.status == "UNAVAILABLE"
      else ($sr.observed.date == $snap.exchangeRate.effectiveDate
            and $sr.evidence[0].usdToVndRate == $snap.exchangeRate.usdToVndRate
            and $sr.observed.value == days($sr.observed.date; $i.asOfDate)) end),
  evidence_amounts_match_snapshot_lines: ([$i.insights[].evidence[] | select(.sourceType == "ASSET" and .dateKind == "VALUATION_DATE")]
      | all(. as $e | $snap.assets[] | select(.id == $e.sourceId) | .value == $e.amount and .valueVnd == $e.amountVnd and .valuationDate == $e.date)),
  every_source_is_the_users: ([$i.insights[].evidence[] | select(.sourceId != null) | .sourceId] | all(. as $x | $ids | index($x) != null))
}
