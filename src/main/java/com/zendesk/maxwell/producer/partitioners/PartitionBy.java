package com.zendesk.maxwell.producer.partitioners;

public enum PartitionBy {
	DATABASE,
	TABLE,
	DATABASE_TABLE,
	PRIMARY_KEY,
	TRANSACTION_ID,
	THREAD_ID,
	COLUMN,
	RANDOM
}
