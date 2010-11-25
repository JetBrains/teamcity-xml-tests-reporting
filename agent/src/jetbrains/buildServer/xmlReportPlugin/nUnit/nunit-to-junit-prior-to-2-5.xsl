<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes"/>

    <xsl:template name="get-file-name">
        <xsl:param name="string"/>
        <xsl:choose>
            <xsl:when test="contains($string, '/')">
                <xsl:call-template name="get-file-name">
                    <xsl:with-param name="string"
                                    select="substring-after($string, '/')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="contains($string, '\')">
                <xsl:call-template name="get-file-name">
                    <xsl:with-param name="string"
                                    select="substring-after($string, '\')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$string"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="/test-results">
        <testsuites>
            <xsl:for-each select="test-suite">

                <xsl:variable name="suiteName">
                    <xsl:call-template name="get-file-name">
                        <xsl:with-param name="string" select="@name"/>
                    </xsl:call-template>
                </xsl:variable>

                <xsl:variable name="timestamp"
                              select="concat(@date, concat('T', @time))"/>

                <testsuite name="{$suiteName}"
                           tests="{count(//test-case)}" time="{@time}"
                           failures="{count(//test-case/failure)}" errors="0"
                           skipped="{count(//test-case[@executed='False'])}"
                           timestamp="{$timestamp}">

                    <xsl:for-each select="results//test-case[1]">

                        <xsl:for-each select="../..">

                            <xsl:variable name="suitefailure"
                                          select="./failure"/>

                            <xsl:for-each select="*/test-case">

                                <testcase name="{@name}"
                                          time="{@time}"
                                          executed="{@executed}">


                                    <xsl:if test="./failure">
                                        <xsl:variable name="failstack"
                                                      select="count(./failure/stack-trace/*) + count(./failure/stack-trace/text())"/>
                                        <xsl:choose>
                                            <xsl:when test="$failstack &gt; 0 or not($suitefailure)">
                                                <xsl:variable name="mess"
                                                              select="./failure/message"/>
                                                <failure message="{$mess}">
                                                    <xsl:value-of select="./failure/stack-trace"/>
                                                </failure>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:variable name="mess"
                                                              select="$suitefailure/message"/>
                                                <failure message="{$mess}">
                                                    <xsl:value-of select="$suitefailure/stack-trace"/>
                                                </failure>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:if>
                                </testcase>
                            </xsl:for-each>
                        </xsl:for-each>
                    </xsl:for-each>
                </testsuite>
            </xsl:for-each>
        </testsuites>
    </xsl:template>
</xsl:stylesheet>
