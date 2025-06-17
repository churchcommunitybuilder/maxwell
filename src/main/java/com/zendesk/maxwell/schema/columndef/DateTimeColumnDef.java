package com.zendesk.maxwell.schema.columndef;

import com.zendesk.maxwell.producer.MaxwellOutputConfig;

import java.sql.Timestamp;

public class DateTimeColumnDef extends ColumnDefWithLength {

	private final boolean isTimestamp = getType().equals("timestamp");
	private static final int DATETIME_BASE_LENGTH = 19;
	private static final int DATETIME_WITH_DOT_LENGTH = 20;
	private static final int MAX_FRACTIONAL_DIGITS = 6;
	private static final String ZERO_DATE_TIME = "0000-00-00 00:00:00";

	private DateTimeColumnDef(String name, String type, short pos, Long columnLength) {
		super(name, type, pos, columnLength);
	}

	public static DateTimeColumnDef create(String name, String type, short pos, Long columnLength) {
		DateTimeColumnDef temp = new DateTimeColumnDef(name, type, pos, columnLength);
		return (DateTimeColumnDef) INTERNER.intern(temp);
	}

	protected String formatValue(Object value, MaxwellOutputConfig config) throws ColumnDefCastException {
		// special case for those broken mysql dates.
		if ( value instanceof String ) {
			String dateStr = (String) value;
			// bootstrapper just gives up on bothering with date processing
			if ( config.zeroDatesAsNull && dateStr.length() == DATETIME_BASE_LENGTH && 
			((dateStr.charAt(0) == '0' && dateStr.charAt(1) == '0' && dateStr.charAt(2) == '0' && dateStr.charAt(3) == '0') ||
            (dateStr.charAt(5) == '0' && dateStr.charAt(6) == '0') ||
            (dateStr.charAt(8) == '0' && dateStr.charAt(9) == '0'))) {
				return null;
			} else if (dateStr.length() > DATETIME_BASE_LENGTH && dateStr.charAt(DATETIME_BASE_LENGTH) == '.') { 
				long columnLength = getColumnLength();
				if (columnLength > 0 && columnLength <= MAX_FRACTIONAL_DIGITS) { 
					int expectedLength = DATETIME_WITH_DOT_LENGTH + (int) columnLength;
					int currentLength = dateStr.length(); 
					if (currentLength == expectedLength) { 
						return dateStr;
					} else if (currentLength < expectedLength) {
						return dateStr + "0".repeat(expectedLength - currentLength); 
					} else { 
						return dateStr.substring(0, expectedLength); 
					}
				}
				return dateStr; 
			}
			 else {
				return appendFractionalSeconds(dateStr, 0, getColumnLength());
			}
		} else if ( value instanceof Long ) {
			Long v = (Long) value;
			if ( v == Long.MIN_VALUE || (v == 0L && isTimestamp) ) {
				if ( config.zeroDatesAsNull )
					return null;
				else
					return appendFractionalSeconds(ZERO_DATE_TIME, 0, getColumnLength());
			}
		}

		try {
			Timestamp ts = DateFormatter.extractTimestamp(value);
			String dateString = DateFormatter.formatDateTime(value, ts);
			return appendFractionalSeconds(dateString, ts.getNanos(), getColumnLength());
		} catch ( IllegalArgumentException e ) {
			throw new ColumnDefCastException(this, value);
		}
	}

	@Override
	public Object asJSON(Object value, MaxwellOutputConfig config) throws ColumnDefCastException {
		if ( value instanceof String ) {
			String dateStr = (String) value;
			// bootstrapper just gives up on bothering with date processing
			if ( dateStr.equals(ZERO_DATE_TIME) ) {
				return null;
			} else if ( config.zeroDatesAsNull && dateStr.length() == DATETIME_BASE_LENGTH &&
				((dateStr.charAt(0) == '0' && dateStr.charAt(1) == '0' && dateStr.charAt(2) == '0' && dateStr.charAt(3) == '0') ||
				(dateStr.charAt(5) == '0' && dateStr.charAt(6) == '0') ||
				(dateStr.charAt(8) == '0' && dateStr.charAt(9) == '0'))) {
				return null;
			} else {
				return formatValue(value, config);
			}
		} else if ( value instanceof Long && (Long) value == Long.MIN_VALUE ) {
			if ( config.zeroDatesAsNull )
				return null;
			else
				return ZERO_DATE_TIME;
		}

		try {
			return formatValue(value, config);
		} catch ( IllegalArgumentException e ) {
			throw new ColumnDefCastException(this, value);
		}
	}
}
