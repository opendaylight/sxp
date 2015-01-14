/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.time;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.DateAndTime;

public final class TimeConv {

    private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public static void main(String[] args) throws Exception {
        System.out.print("Time='" + toLong(new DateAndTime("2014-11-22T15:54:00Z")) + "'");
    }

    public static synchronized DateAndTime toDt(long currentTime) {
        return new DateAndTime(df.format(new Date(currentTime)));
    }

    public static synchronized long toLong(DateAndTime dateAndTime) throws Exception {
        if (dateAndTime == null || dateAndTime.getValue() == null || dateAndTime.getValue().isEmpty()) {
            return -1;
        }
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(df.parse(dateAndTime.getValue()));
        } catch (Exception e) {
            throw new Exception("Unrecognized date and time format: \"" + dateAndTime.getValue() + "\"");
        }
        return calendar.getTimeInMillis();
    }
}
