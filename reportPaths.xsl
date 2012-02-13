<!--
  ~ Copyright 2000-2012 JetBrains s.r.o.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:strip-space elements="*"/>

  <xsl:output method="xml" indent="yes"/>

  <xsl:param name="path">not specified</xsl:param>

  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="Jar">
    <Jar>
      <xsl:call-template name="replace">
        <xsl:with-param name="input" select="text()"/>
        <xsl:with-param name="from" select="'C:\work\teamcityworkspace\xml-report-plugin'"/>
        <xsl:with-param name="to" select="$path"/>
      </xsl:call-template>
    </Jar>
  </xsl:template>

  <xsl:template match="SrcDir">
    <SrcDir>
      <xsl:call-template name="replace">
        <xsl:with-param name="input" select="text()"/>
        <xsl:with-param name="from" select="'C:\work\teamcityworkspace\xml-report-plugin'"/>
        <xsl:with-param name="to" select="$path"/>
      </xsl:call-template>
    </SrcDir>
  </xsl:template>

  <xsl:template name="replace">
    <xsl:param name="input"/>
    <xsl:param name="from"/>
    <xsl:param name="to"/>

    <xsl:choose>
      <xsl:when test="contains($input, $from)">
        <xsl:value-of select="substring-before($input, $from)"/>
        <xsl:value-of select="$to"/>


        <xsl:call-template name="replace">
          <xsl:with-param name="input" select="substring-after($input, $from)"/>
          <xsl:with-param name="from" select="$from"/>
          <xsl:with-param name="to" select="$to"/>
        </xsl:call-template>

      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$input"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
