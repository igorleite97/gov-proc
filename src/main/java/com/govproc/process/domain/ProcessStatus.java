package com.govproc.process.domain;

public enum ProcessStatus {
    CAPTURED,
    UNDER_ANALYSIS,
    ANALYSIS_APPROVED,
    ANALYSIS_REJECTED,
    IN_QUOTATION,
    QUOTED,
    IN_DISPUTE,
    WINNER,
    LOSER,
    POST_BID,
    CONTRACT_ACTIVE,
    CLOSED
}
