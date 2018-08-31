/**
 * personium.io
 * Copyright 2014 FUJITSU LIMITED
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *Range Process the request header RangeHeaderHandler.parse () parses the value of the Range header field and uses it.
 *The returned value is used by checking whether it is a valid header specification with isValid (). If invalid, header specification should be ignored.
 *Also when it is valid and isSatisfiable (), if there is no byte-range-spec in which the Range start specification is normal, 416 response should be returned.
 *If byte-range-spec to be processed exists, getByteRangeSpecCount () acquires the number of valid byte-range-spec,
 *Get necessary information for response generation with getFirstBytePos (), getLastBytePos (), getContentLength (), makeContentRangeHeaderField ().
 */
public class RangeHeaderHandler {

    //the term
    /**
     * bytes-unit.
     */
    public static final String BYTES_UNIT = "bytes";

    //There is no definition in RFC, but since apache has been implemented with a maximum of 13 implementations, it is combined
    static final int BYTE_RANGE_SPEC_MAX = 13;
    //String of Range header field
    private String rangeHeaderField = "";
    //Manage range-byte-spec
    private List<ByteRangeSpec> byteRangeSpecList = new ArrayList<ByteRangeSpec>();;

    //Manage validity and invalidity of Range header
    private boolean valid = false;

    /**
     *constructor.
     */
    private RangeHeaderHandler(String rangeHeader) {
        this.rangeHeaderField = rangeHeader;
    }

    /**
     *Pass the value of the Range header and the size of the target file and parse it to generate an object of this class.
     *@ param rangeHeader Range header value (ex. bytes = 500 - 600, 601 - 999)
     *@ param entitySize Range File size to be specified
     *@return Object of this class
     */
    public static final RangeHeaderHandler parse(final String rangeHeader, final long entitySize) {
        RangeHeaderHandler range = new RangeHeaderHandler(rangeHeader);

        //Ignore if Range header is not specified
        if (rangeHeader == null) {
            return range;
        }

        //Extraction of byte-range-set part
        String regexByteRangesSpecifier = "^bytes\\s*=\\s*(.+)$";
        Pattern pByteRangesSpecifier = Pattern.compile(regexByteRangesSpecifier);
        Matcher mByteRangesSpecifier = pByteRangesSpecifier.matcher(rangeHeader);
        if (!mByteRangesSpecifier.matches()) {
            return range;
        }

        //Because there is a possibility that there are multiple byte-range-specs,
        String[] byteRangeSpecArray = mByteRangesSpecifier.group(1).split(",");

        //When the byte-range-spec exceeds the upper limit, the Range header invalid
        if (byteRangeSpecArray.length > BYTE_RANGE_SPEC_MAX) {
            return range;
        }

        //Parse processing of each byte-range-spec
        List<ByteRangeSpec> byteRangeSpecList = new ArrayList<ByteRangeSpec>();
        for (String byteRangeSpec : byteRangeSpecArray) {
            ByteRangeSpec brs = ByteRangeSpec.parse(byteRangeSpec, entitySize);
            if (brs == null) {
                return range;
            }
            if (!brs.isInEntitySize()) {

                continue;
            }
            byteRangeSpecList.add(brs);
        }
        range.setValid();
        range.setByteRangeSpec(byteRangeSpecList);
        return range;
    }

    /**
     *Returns the character string of the Range header field.
     *@return Range header field
     */
    public String getRangeHeaderField() {
        return this.rangeHeaderField;
    }

    /**
     *Range Returns whether the header is valid, or false if there is no valid Range specification.
     *@return true Enabled
     */
    public boolean isValid() {
        return this.valid;
    }
    private void setValid() {
        this.valid = true;
    }

    /**
     *Returns the number of byte-range-specs specified in the Range header.
     *Number of @return Ranges
     */
    public int getByteRangeSpecCount() {
        return this.byteRangeSpecList.size();
    }

    /**
     *Check if a byte-range-spec that is within the range of the file exists. If there is any byte-range-spec in the file that is within the file, return it with 206.
     * @return bool
     */
    public boolean isSatisfiable() {
        if (this.byteRangeSpecList.size() > 0) {
            return true;
        }
        return false;
    }

    private void setByteRangeSpec(final List<ByteRangeSpec> brs) {
        this.byteRangeSpecList = brs;
    }

    /**
     *Return a list of valid ByteRangeSpec.
     *@return List of valid ByteRangeSpec
     */
    public List<ByteRangeSpec> getByteRangeSpecList() {
        return this.byteRangeSpecList;
    }
}
