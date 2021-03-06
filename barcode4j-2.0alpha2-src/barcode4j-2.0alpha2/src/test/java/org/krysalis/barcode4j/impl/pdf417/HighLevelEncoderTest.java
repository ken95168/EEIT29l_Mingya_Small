/*
 * Copyright 2006 Jeremias Maerki.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id: HighLevelEncoderTest.java,v 1.3 2007/01/14 11:50:14 jmaerki Exp $ */

package org.krysalis.barcode4j.impl.pdf417;

import org.krysalis.barcode4j.impl.pdf417.PDF417HighLevelEncoder;
import org.krysalis.barcode4j.tools.TestHelper;

import junit.framework.TestCase;

public class HighLevelEncoderTest extends TestCase {

    public void testFindNumericSequence() throws Exception {
        String msg = "1234567890ABC";
        int count = PDF417HighLevelEncoder.determineConsecutiveDigitCount(msg, 0);
        assertEquals(10, count);
    }
    
    public void testFindTextSequence() throws Exception {
        String msg = "1234567890ABCD?123";
        int count = PDF417HighLevelEncoder.determineConsecutiveTextCount(msg, 0);
        assertEquals(14, count);

        msg = "123456789012345ABCD?123";
        count = PDF417HighLevelEncoder.determineConsecutiveTextCount(msg, 0);
        assertEquals(0, count); //0 because the string has a 13+ numeric sequence
    }
    
    public void testFindBinarySequence() throws Exception {
        String msg = "???Test???1234567890";
        byte[] bytes = PDF417HighLevelEncoder.getBytesForMessage(msg);
        int count = PDF417HighLevelEncoder.determineConsecutiveBinaryCount(msg, bytes, 0);
        assertEquals(10, count);

        msg = "???Test";
        bytes = PDF417HighLevelEncoder.getBytesForMessage(msg);
        count = PDF417HighLevelEncoder.determineConsecutiveBinaryCount(msg, bytes, 0);
        assertEquals(7, count);

        msg = "???Test????1234567890";
        bytes = PDF417HighLevelEncoder.getBytesForMessage(msg);
        try {
            count = PDF417HighLevelEncoder.determineConsecutiveBinaryCount(msg, bytes, 0);
            fail("The Euro character is not encodable in cp437."
                    + " An IllegalArgumentException is expected.");
        } catch (IllegalArgumentException iae) {
            //exception is expected
        }
    }

    public void testEncodeText() throws Exception {
        String msg = "PDF417";
        StringBuffer sb = new StringBuffer();
        PDF417HighLevelEncoder.encodeText(msg, 0, msg.length(), sb);
        String expected = "\u01c5\u00b2\u0079\u00ef";
        assertEquals(expected, sb.toString());

        msg = "PDF417 Symbology Standard";
        sb.setLength(0);
        PDF417HighLevelEncoder.encodeText(msg, 0, msg.length(), sb);
        //There was a bug with an endless loop here, just check that it doesn't hang.
    }
    
    public void testEncodeNumeric() throws Exception {
        String msg = "000213298174000";
        StringBuffer sb = new StringBuffer();
        PDF417HighLevelEncoder.encodeNumeric(msg, 0, msg.length(), sb);
        String expected = "\u0001\u0270\u01b2\u0278\u011a\u00c8";
        assertEquals(expected, sb.toString());
    }

    private void log(String expected, String actual) {
        /*
        System.out.println("expected: " + expected);
        System.out.println("actual: " + actual);
        */
    }
    
    public void testEncodeBinary() throws Exception {
        //Example from Annex C
        byte[] bytes = new byte[] {(byte)231, 101, 11, 97, (byte)205, 2};
        String msg = new String(bytes, "cp437");
        StringBuffer sb = new StringBuffer();
        PDF417HighLevelEncoder.encodeBinary(msg, bytes, 0, msg.length(), sb);
        String expected = "924 387 700 208 213 302";
        log(expected, TestHelper.visualize(sb.toString()));
        assertEquals(expected, TestHelper.visualize(sb.toString()));

        msg = msg + msg + msg.substring(0, 1);
        sb.setLength(0);
        bytes = PDF417HighLevelEncoder.getBytesForMessage(msg);
        PDF417HighLevelEncoder.encodeBinary(msg, bytes, 0, msg.length(), sb);
        expected = TestHelper.visualize("\u0385\u0183\u02bc\u00d0\u00d5\u012e\u0183\u02bc\u00d0\u00d5\u012e\u00e7");
        assertEquals(expected, TestHelper.visualize(sb.toString()));
        
        msg = "????????????"; //12 binary characters = 2x6
        bytes = PDF417HighLevelEncoder.getBytesForMessage(msg);
        sb.setLength(0);
        PDF417HighLevelEncoder.encodeBinary(msg, bytes, 0, msg.length(), sb);
        expected = "924 222 69 238 51 792 222 69 238 51 792";
        assertEquals(expected, TestHelper.visualize(sb.toString()));

        msg = "??????????"; //10 binary characters = 1x6 + 4
        bytes = PDF417HighLevelEncoder.getBytesForMessage(msg);
        sb.setLength(0);
        PDF417HighLevelEncoder.encodeBinary(msg, bytes, 0, msg.length(), sb);
        expected = "901 222 69 238 51 792 132 132 132 132";
        assertEquals(expected, TestHelper.visualize(sb.toString()));
    }
    
    public void testEncodeHighLevel() throws Exception {
        String msg = "000213298174000";
        String result = TestHelper.visualize(PDF417HighLevelEncoder.encodeHighLevel(msg));
        String expected = "902 1 624 434 632 282 200";
        assertEquals(expected, result);

        msg = "PDF417";
        result = TestHelper.visualize(PDF417HighLevelEncoder.encodeHighLevel(msg));
        expected = "453 178 121 239";
        assertEquals(expected, result);

        msg = "000213298174000PDF417";
        result = TestHelper.visualize(PDF417HighLevelEncoder.encodeHighLevel(msg));
        expected = "902 1 624 434 632 282 200 900 453 178 121 239";
        assertEquals(expected, result);

        msg = "TestTest????????????"; //12 binary characters = 2x6
        result = TestHelper.visualize(PDF417HighLevelEncoder.encodeHighLevel(msg));
        //System.out.println(result);
        expected = "597 138 597 574 559 924 222 69 238 51 792 222 69 238 51 792";
        assertEquals(expected, result);

        msg = "TestTest??????????"; //10 binary characters = 1x6 + 4
        result = TestHelper.visualize(PDF417HighLevelEncoder.encodeHighLevel(msg));
        //System.out.println(result);
        expected = "597 138 597 574 559 901 222 69 238 51 792 132 132 132 132";
        log(expected, result);
        assertEquals(expected, result);
    }
    
}
