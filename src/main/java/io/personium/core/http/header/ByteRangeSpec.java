/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
 * - FUJITSU LIMITED
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.personium.core.http.header;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Container for managing ByteRangeSpec.
 */
public class ByteRangeSpec {
    private long entitySize;
    private long firstBytePos;
    private long lastBytePos;

    private ByteRangeSpec(final long firstBytePos, final long lastBytePos, final long entitySize) {
        this.entitySize = entitySize;
        this.firstBytePos = firstBytePos;
        this.lastBytePos = lastBytePos;
    }

    /**
     * If byte-range-spec is parsed and syntactically correct, this object is returned, and if it is invalid, null is returned.
     * @param byteRangeSpecString String of byte-range-spec
     * @param entitySize Range Target file size
     * @return this object
     */
    static final ByteRangeSpec parse(final String byteRangeSpecString, final long entitySize) {
        String firstBytePosString;
        String lastBytePosString;
        long firstBytePosLong;
        long lastBytePosLong;

        //Getting start and end
        String regexByteRangeSpec = "^([^-]*)-([^-]*)$";
        Pattern pByteRangeSpec = Pattern.compile(regexByteRangeSpec);
        Matcher mByteRangeSpec = pByteRangeSpec.matcher(byteRangeSpecString);
        if (!mByteRangeSpec.matches()) {
            return null;
        }
        firstBytePosString = mByteRangeSpec.group(1).trim();
        lastBytePosString = mByteRangeSpec.group(2).trim();

        //Invalid if both start and end are omitted
        if (firstBytePosString.equals("") && lastBytePosString.equals("")) {
            return null;
        }
        if (lastBytePosString.equals("")) {
            //When the termination is omitted, the processing is terminated up to the file size
            lastBytePosLong = entitySize - 1;
        } else {
            try {
                lastBytePosLong = Long.parseLong(lastBytePosString);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        //Start omitted If there is a termination designation (last - last) until the end
        if (firstBytePosString.equals("")) {
            firstBytePosLong = entitySize - lastBytePosLong;
            if (firstBytePosLong < 0) {
                firstBytePosLong = 0;
            }
            lastBytePosLong = entitySize - 1;
        } else {
            //Check if it is a number
            try {
                firstBytePosLong = Long.parseLong(firstBytePosString);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        //If the start and end positions of the Range are reversed, the Range header invalid
        if (firstBytePosLong > lastBytePosLong) {
            return null;
        }

        //If the termination is greater than the entity size, the terminating value is a value of entity size -1
        if (lastBytePosLong >= entitySize) {
            lastBytePosLong = entitySize - 1;
        }

        return new ByteRangeSpec(firstBytePosLong, lastBytePosLong, entitySize);
    }

    /**
     * Returns true if the starting position of the Range is within the range of the file or checked and is within the range.
     * @return bool
     */
    public boolean isInEntitySize() {
        if (this.getFirstBytePos() > (this.entitySize - 1)) {
            return false;
        }
        return true;
    }

    /**
     * first-byte-pos.
     * @return first-byte-pos
     */
    public long getFirstBytePos() {
        return this.firstBytePos;
    }

    /**
     * last-byte-pos.
     * @return last-byte-pos
     */
    public long getLastBytePos() {
        return this.lastBytePos;
    }

    /**
     * Returning ContentLength considering specification of Range.
     * @return long contentLength
     */
    public long getContentLength() {
        return this.getLastBytePos() + 1 - this.getFirstBytePos();
    }

    /**
     * Format the value of Range to the value of Content - Range header.
     * @return Value of the Content-Range header
     */
    public String makeContentRangeHeaderField() {
        //Return in the format of Content - Range header
        return String.format("bytes %s-%s/%s", this.getFirstBytePos(), this.getLastBytePos(), this.entitySize);
    }
}
