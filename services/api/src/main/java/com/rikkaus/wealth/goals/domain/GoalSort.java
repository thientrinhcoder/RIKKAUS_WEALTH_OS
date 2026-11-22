package com.rikkaus.wealth.goals.domain;

import com.rikkaus.wealth.shared.search.SortKey;

/** The orders the goal list can be returned in. The first is the default. */
public enum GoalSort implements SortKey {
    /** Nearest target date first, then by name: the order when no sort is asked for. */
    TARGET_DATE_ASC("targetDate"),
    TARGET_DATE_DESC("-targetDate"),
    NAME_ASC("name"),
    NAME_DESC("-name");

    private final String token;

    GoalSort(String token) {
        this.token = token;
    }

    @Override
    public String token() {
        return token;
    }
}
