/*
 * Copyright 2015 Sean Bridges. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * 
 * 
 */

package com.github.sbridges.ephemeralfs;

import java.util.regex.PatternSyntaxException;

/**
 * Util for converting globs to regexes 
 */
class GlobUtil {

    //http://openjdk.java.net/projects/nio/javadoc/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String)
    public static String globToRegex(String glob, boolean unixSeperators) {
        
        StringBuilder answer = new StringBuilder("^");
        CharStream stream = new CharStream(glob);
        
        
        //states
        // normal
        // in square brackets
        // in curly brackets
        
        
        while(stream.hasNext()) {
            char current = stream.current();
            switch(current) {
            case  '[' :
                matchBracketExpression(stream, unixSeperators, answer);
                break;
            case  '{' :
                matchSubpattern(stream, unixSeperators, answer);
                break;
            case '*' :
                handleStar(unixSeperators, answer, stream);
                break;
            case '?' :
                matchAllButPath(answer, unixSeperators);
                break;
            case '/' :
                answer.append(sperator(unixSeperators));
                break; 
            case '\\' :
                handleEscape(answer, stream);
                break;
            default :
                appendEscaped(answer, current);
                
            }
            
            stream.next();
        }
        return answer.toString();
    }

    public static void handleStar(boolean unixSeperators, StringBuilder answer,
            CharStream stream) {
        if(stream.isNext('*')) {
            //**, match any characters
            stream.next();
            answer.append(".*");
        } else {
            //* any character except path separators
            matchAllButPath(answer, unixSeperators);
            answer.append("*");                    
        }
    }

    /**
     * Stream should be positioned at the {, will not consume the trailing } 
     */
    private static void matchSubpattern(CharStream stream,
            boolean unixSeperators, StringBuilder answer) {
        
        stream.next();
        //non capturing group
        answer.append("((:?");
        
        int state = 0;  //0 initial
                        //1 comma just read
                        //2 in group
        
        while(stream.hasNext()) {
            char current = stream.current();
            switch(current) {
            case '{' :
                fail(stream, "can't nest sub patterns");
            case '}' :
                if(state != 2) {
                    fail(stream, "group not started");
                }
                answer.append("))");
                return;
            case '[' :
                matchBracketExpression(stream, unixSeperators, answer);
                break;
            case ',' :
                if(state == 1) {
                    fail(stream, "multiple consecutive commas");
                }
                if(state == 2) {
                    answer.append(")|(?:");    
                }
                state = 1;
                break;
            case '*' :
                handleStar(unixSeperators, answer, stream);
                break;
            case '?' :
                matchAllButPath(answer, unixSeperators);
                break;
            case '\\' :
                handleEscape(answer, stream);
                break;
            default :
                appendEscaped(answer, current);
                state = 2;
            }
            stream.next();
        }
        
        fail(stream, "unterminated sub pattern");
        
    }


    private static void handleEscape(StringBuilder answer, CharStream stream) {
        if(!stream.hasNext()) {
            fail(stream, "unterminated esacpe sequence");
        }
        stream.next();
        appendEscaped(answer, stream.current());
    }

    private static void appendEscaped(StringBuilder answer, char current) {
        //cheap way to escape characters in a regex
        if(Character.isAlphabetic(current) || Character.isDigit(current)) {
            answer.append(current);
        } else if(current == '.') {
            answer.append("\\.");
        } else if(current == '*') {
            answer.append("\\*");
        }
        else {
            answer.append(String.format ("\\u%04x", (int) current));    
        }
    }
    
    
    /**
     * Stream should be positioned at the [, will not consume the trailing ] 
     */
    private static void matchBracketExpression(CharStream stream, boolean unixSeperators, StringBuilder answer) {
        stream.next();
        answer.append('[');
        
        if(stream.current() == '!') {
            answer.append("^");
            stream.next();
        }
        
        boolean rangeStartAllowed = false;
        if(stream.current() == '-') {
            answer.append('-');
            rangeStartAllowed = true;
            stream.next();
        }
        
        boolean inRange = false;
        
        while(stream.hasNext()) {
            char current = stream.current();
            switch(current) {
            case '[' :
                fail(stream, "can't nest bracket expressions");
            case ']' :
                if(inRange) {
                    fail(stream, "unterminated range in bracket expression");
                }
                answer.append("&&[^").append(unixSeperators ? "/" : "\\\\").append("]]");
                return;
            
            case '-' :
                if(!rangeStartAllowed) {
                    fail(stream, "range requires a start token");
                }
                inRange = true;
                answer.append("-");
                break;
            default :
                inRange = false;
                rangeStartAllowed = true;
                appendEscaped(answer, current);
            }
            stream.next();
        }
        
        fail(stream, "unterminated bracket expression");
    }


    private static void fail(CharStream stream, String reason) {
        throw new PatternSyntaxException(reason, stream.getValue(), stream.getIndex());
    }


    private static void matchAllButPath(StringBuilder answer, boolean unixSeperators) {
        answer.append("[^").append(sperator(unixSeperators)).append("]");
    }


    private static String sperator(boolean unixSeperators) {
        if(unixSeperators) {
            return "/";
        }
        return "\\\\";
        
    }


    static class CharStream {
        private final String value;
        private int index;
        
        public CharStream(String value) {
            super();
            this.value = value;
        }

        public int getIndex() {
            return index;
        }

        public String getValue() {
            return value;
        }

        boolean hasNext() {
            return index < value.length();
        }
        
        void next() {
            index++;
        }
        
        boolean isNext(char c) {
            if(index +1  < value.length()) {
                return value.charAt(index + 1) == c;
            }
            return false;
        }
        
        char current() {
            return value.charAt(index);
           
        }
    }
    
}
