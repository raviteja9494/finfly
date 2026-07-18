/* Domain-layer transaction direction independent of Firefly or Android types. */
package com.teja.finfly.domain.model

/** Describes whether money left, entered, or moved between owned accounts. */
enum class TransactionType { WITHDRAWAL, DEPOSIT, TRANSFER }
