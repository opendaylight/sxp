/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.time;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.DateAndTime;

public final class TimeConv {

    private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /**
     * Converts Long to DateAndTime with accuracy to seconds
     *
     * @param currentTime Long value that will be converted
     * @return DateAndTime generated from specified value
     */
    public static synchronized DateAndTime toDt(long currentTime) {
        return new DateAndTime(df.format(new Date(currentTime)));
    }

    /**
     * Set Time zone used for date format,
     * if format is unknown GMT+0 is set
     *
     * @param timeZone Time zone to be set
     */
    public static void setTimeZone(String timeZone) {
        if (timeZone != null) {
            df.setTimeZone(TimeZone.getTimeZone(timeZone));
        } else {
            throw new IllegalArgumentException("TimeZone cannot be null");
        }
    }

    /**
     * Converts DateAndTime to Long value with accuracy to seconds
     *
     * @param dateAndTime DateAndValue that will be converted
     * @return Long value representing specified value
     */
    public static synchronized long toLong(DateAndTime dateAndTime) {
        if (dateAndTime == null || dateAndTime.getValue() == null || dateAndTime.getValue().isEmpty()) {
            return -1;
        }
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(df.parse(dateAndTime.getValue()));
        } catch (ParseException e) {
            throw new IllegalArgumentException("Unrecognized date and time format: \"" + dateAndTime.getValue() + "\"");
        }
        return calendar.getTimeInMillis();
    }
}
